/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.dashboard.suggestions

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.settings.core.InstrumentedFragment
import com.android.settings.homepage.SettingsHomepageActivity
import com.android.settings.homepage.SplitLayoutListener
import com.android.settings.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SUGGESTIONS = "derpfest_suggestions"
private const val TAG = "DerpFestSuggestionFrag"
private const val FLAG_IS_DISMISSIBLE = 1 shl 2

/**
 * Fragment to display DerpFest-specific suggestions on the homepage.
 * This is a local implementation that doesn't require the Settings Intelligence service.
 */
class DerpFestSuggestionFragment : InstrumentedFragment(), SplitLayoutListener {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private lateinit var suggestionTile: View
    private var icon: ImageView? = null
    private var iconFrame: View? = null
    private var title: TextView? = null
    private var summary: TextView? = null
    private var dismiss: ImageView? = null
    private var iconVisible = true
    private var startTime: Long = 0
    private var suggestionsRestored = false
    private var splitLayoutSupported = false
    private var loadedThisSession = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        suggestionTile = inflater.inflate(R.layout.suggestion_tile, container, true)
        icon = suggestionTile.findViewById(android.R.id.icon)
        iconFrame = suggestionTile.findViewById(android.R.id.icon_frame)
        title = suggestionTile.findViewById(android.R.id.title)
        summary = suggestionTile.findViewById(android.R.id.summary)
        dismiss = suggestionTile.findViewById(android.R.id.closeButton)
        if (!iconVisible) {
            onSplitLayoutChanged(false)
        }
        // Restore the suggestion and skip reloading
        if (savedInstanceState != null) {
            Log.d(TAG, "Restoring suggestions")
            savedInstanceState.getParcelableArrayList<DerpFestSuggestionProvider.DerpFestSuggestion>(
                SUGGESTIONS
            )?.takeIf { it.isNotEmpty() }?.let { suggestions ->
                suggestionsRestored = true
                loadedThisSession = true
                startTime = SystemClock.uptimeMillis()
                updateState(suggestions)
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(SUGGESTIONS, currentSuggestions)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        loadSuggestions()
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SETTINGS_HOMEPAGE
    }

    override fun setSplitLayoutSupported(supported: Boolean) {
        splitLayoutSupported = supported
    }

    override fun onSplitLayoutChanged(isRegularLayout: Boolean) {
        iconVisible = isRegularLayout
        if (splitLayoutSupported) {
            iconFrame?.visibility = if (iconVisible) View.VISIBLE else View.GONE
        }
    }

    private fun loadSuggestions() {
        if (suggestionsRestored) {
            // Skip first suggestion loading when restored
            suggestionsRestored = false
            return
        }
        if (loadedThisSession) {
            return
        }
        loadedThisSession = true

        startTime = SystemClock.uptimeMillis()
        scope.launch(Dispatchers.IO) {
            Log.d(TAG, "Start loading DerpFest suggestions")
            val suggestions = DerpFestSuggestionProvider.getSuggestions(requireContext())
            Log.d(TAG, "Loaded suggestions: ${suggestions.size}")
            withContext(Dispatchers.Main) {
                updateState(suggestions)
            }
        }
    }

    private fun updateState(suggestions: List<DerpFestSuggestionProvider.DerpFestSuggestion>) {
        currentSuggestions.clear()
        if (suggestions.isEmpty()) {
            Log.d(TAG, "No suggestions available, removing")
            suggestionTile.visibility = View.GONE
            (activity as? SettingsHomepageActivity)?.findViewById<View>(R.id.suggestion_content)
                ?.visibility = View.GONE
            showSuggestionTile(false)
            return
        }
        currentSuggestions.addAll(suggestions)

        // Only take top suggestion; we assume this is the highest priority.
        val suggestion = suggestions.first()
        suggestion.icon?.let {
            icon?.setImageIcon(it)
            icon?.imageTintList = ColorStateList.valueOf(
                requireContext().getColor(
                    com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorOnSecondaryContainer
                )
            )
        }
        title?.text = suggestion.title
        val suggestionSummary = suggestion.summary
        if (suggestionSummary.isNullOrEmpty()) {
            summary?.visibility = View.GONE
        } else {
            summary?.visibility = View.VISIBLE
            summary?.text = suggestionSummary
        }
        if (suggestion.flags and FLAG_IS_DISMISSIBLE != 0) {
            dismiss?.let { dismissView ->
                dismissView.visibility = View.VISIBLE
                dismissView.setOnClickListener {
                    val dismissedId = suggestion.id
                    currentSuggestions.clear()
                    suggestionTile.visibility = View.GONE
                    showSuggestionTile(false)
                    scope.launch(Dispatchers.IO) {
                        context?.let { ctx ->
                            DerpFestSuggestionProvider.dismissSuggestion(ctx, dismissedId)
                        }
                        // After welcome, pick the next card in this session if one is due.
                        if (dismissedId == "derpfest_welcome") {
                            val next = context?.let {
                                DerpFestSuggestionProvider.getSuggestions(it)
                            }.orEmpty()
                            withContext(Dispatchers.Main) {
                                if (next.isNotEmpty()) {
                                    updateState(next)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            dismiss?.visibility = View.GONE
        }
        suggestionTile.setOnClickListener {
            try {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                context?.startActivity(suggestion.intent, options.toBundle())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start suggestion ${suggestion.title}", e)
            }
        }
        suggestionTile.visibility = View.VISIBLE
        // Homepage only applies the first visibility update; keep the container shown
        // when replacing a dismissed card in the same session.
        (activity as? SettingsHomepageActivity)?.findViewById<View>(R.id.suggestion_content)
            ?.visibility = View.VISIBLE
        showSuggestionTile(true)
    }

    private fun showSuggestionTile(show: Boolean) {
        val totalTime = SystemClock.uptimeMillis() - startTime
        Log.d(TAG, "Total loading time: $totalTime ms")
        mMetricsFeatureProvider.action(
            context,
            SettingsEnums.ACTION_CONTEXTUAL_HOME_SHOW,
            totalTime.toInt()
        )
        (activity as? SettingsHomepageActivity)?.showHomepageWithSuggestion(show)
    }

    private companion object {
        val currentSuggestions = arrayListOf<DerpFestSuggestionProvider.DerpFestSuggestion>()
    }
}
