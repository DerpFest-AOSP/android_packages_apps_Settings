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
            )?.let { suggestions ->
                suggestionsRestored = true
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
            showSuggestionTile(false)
            return
        }
        currentSuggestions.addAll(suggestions)

        // Only take top suggestion; we assume this is the highest priority.
        val suggestion = suggestions.first()
        suggestion.icon?.let {
            icon?.setImageIcon(it)
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
                    scope.launch(Dispatchers.IO) {
                        // Store dismissed suggestion ID in shared preferences
                        val prefs = context?.getSharedPreferences(
                            "derpfest_suggestions_dismissed",
                            Context.MODE_PRIVATE
                        )
                        prefs?.edit()?.putBoolean(suggestion.id, true)?.apply()
                    }
                    if (suggestions.size > 1) {
                        dismissView.visibility = View.GONE
                        updateState(suggestions.subList(1, suggestions.size))
                    } else {
                        currentSuggestions.clear()
                        suggestionTile.visibility = View.GONE
                    }
                }
            }
        } else {
            dismiss?.visibility = View.GONE
        }
        suggestionTile.setOnClickListener {
            currentSuggestions.clear()
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
