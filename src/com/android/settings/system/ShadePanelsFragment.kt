/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.system

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.View.LAYOUT_DIRECTION_RTL
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.Utils.isDeviceFoldable
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.support.actionbar.HelpResourceProvider
import com.android.settings.system.ShadePanelsPreferenceController.Companion.isDualShadeEnabled
import com.android.settings.system.ShadePanelsPreferenceController.Companion.setDualShadeEnabled
import com.android.settings.utils.CandidateInfoExtra
import com.android.settings.widget.RadioButtonPickerFragment
import com.android.settings.widget.SettingsSuggestionsPreference
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.search.SearchIndexableRaw
import com.android.settingslib.widget.CandidateInfo
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.UntitledPreferenceCategory
import kotlin.math.roundToInt

/**
 * The preference fragment for the Settings page controlling Notifications & Quick Settings panels,
 * allowing the user to switch between "Dual Shade" and "Single Shade".
 */
@SearchIndexable
class ShadePanelsFragment : RadioButtonPickerFragment(), HelpResourceProvider {

    override fun getPreferenceScreenResId(): Int = R.xml.shade_panels_settings

    // TODO(b/409228328): Add a dedicated Settings enum for Shade Panels in
    //  `stats/enums/app/settings_enums.proto`, and refer to it here.
    override fun getMetricsCategory(): Int = SettingsEnums.PAGE_VISIBLE

    override fun onAttach(context: Context) {
        super.onAttach(context)

        setCategory(R.string.shade_panels_category)
        setIllustrationForSelection(getDefaultKey())
    }

    override fun getCandidates(): List<CandidateInfo> {
        val context = requireContext()
        return listOf(
            // Separate panels option (aka Dual Shade)
            CandidateInfoExtra(
                context.getText(R.string.shade_panels_separate_title),
                context.getText(R.string.shade_panels_separate_summary),
                KEY_DUAL_SHADE_PREFERENCE,
                true
            ),
            // Combined panels option (aka Single Shade)
            CandidateInfoExtra(
                context.getText(R.string.shade_panels_combined_title),
                context.getText(R.string.shade_panels_combined_summary),
                KEY_SINGLE_SHADE_PREFERENCE,
                true
            )
        )
    }

    override fun addStaticPreferences(screen: PreferenceScreen) {
        val context = requireContext()
        if (getDefaultKey() == KEY_DUAL_SHADE_PREFERENCE) {
            // Own category so the slider is a single rounded card. A sibling preference would
            // share its expressive background and square off the bottom corners.
            val sliderCategory = UntitledPreferenceCategory(context).apply {
                key = KEY_SPLIT_RATIO_CATEGORY
            }
            screen.addPreference(sliderCategory)
            sliderCategory.addPreference(createSplitRatioPreference(context))
        }
        if (isDeviceFoldable(context)) {
            screen.addPreference(
                FooterPreference(context).apply {
                    title = context.getText(R.string.shade_panels_foldables_footer_message)
                }
            )
        }

        val suggestionsKey = ShadePanelsSuggestionsController.KEY_SUGGESTIONS
        val suggestionsCategory = UntitledPreferenceCategory(context).apply {
            key = KEY_SUGGESTIONS_CATEGORY
            order = 1000
        }
        screen.addPreference(suggestionsCategory)
        suggestionsCategory.addPreference(
            SettingsSuggestionsPreference(context).apply {
                key = suggestionsKey
                isSelectable = false
            }
        )
        ShadePanelsSuggestionsController(context, suggestionsKey).displayPreference(screen)
    }

    override fun bindPreferenceExtra(
        pref: SelectorWithWidgetPreference,
        key: String,
        info: CandidateInfo?,
        defaultKey: String,
        systemDefaultKey: String?,
    ) {
        // Bind the summary of each radio button, as it's not included in the basic `CandidateInfo`.
        if (info is CandidateInfoExtra) {
            pref.setSummary(info.loadSummary())
        }
    }

    /**
     * Use the standard preference row so the radio button is placed in the end widget
     * frame (right side in LTR) instead of the default left-aligned selector layout.
     */
    override fun getRadioButtonPreferenceCustomLayoutResId(): Int {
        return if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
            com.android.settingslib.widget.theme.R.layout.settingslib_expressive_preference
        } else {
            com.android.settingslib.widget.theme.R.layout.settingslib_preference
        }
    }

    /** Retrieve the persisted value. */
    override fun getDefaultKey(): String {
        val contentResolver = requireContext().contentResolver
        return if (contentResolver.isDualShadeEnabled()) {
            KEY_DUAL_SHADE_PREFERENCE
        } else {
            KEY_SINGLE_SHADE_PREFERENCE
        }
    }

    /** Persist the selected value. */
    override fun setDefaultKey(key: String): Boolean {
        val enableDualShade = key == KEY_DUAL_SHADE_PREFERENCE
        return requireContext().contentResolver.setDualShadeEnabled(enableDualShade)
    }

    override fun onSelectionPerformed(success: Boolean) {
        if (success) {
            setIllustrationForSelection(getDefaultKey())
            updateCandidates()
        }
    }

    /**
     * Material slider for the dual-shade boundary. The value is the share of the screen, measured
     * from the start edge, that opens notifications. The rest opens Quick Settings.
     */
    private fun createSplitRatioPreference(context: Context): SliderPreference {
        val currentValue = currentSplitPercent(context)
        return SliderPreference(context).apply {
            key = Settings.System.STATUS_BAR_SHADE_SPLIT_PERCENTAGE
            title = context.getString(R.string.status_bar_shade_split_percentage_title)
            summary = splitSummary(context, currentValue)
            isPersistent = false
            setMin(MIN_SPLIT_PERCENT)
            setMax(MAX_SPLIT_PERCENT)
            // Snap to whole percents without drawing a tick for every step.
            setSliderIncrement(1)
            setTickVisible(false)
            setShowSliderValue(true)
            setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS)
            setTextStart(R.string.status_bar_shade_split_more_quick_settings)
            setTextEnd(R.string.status_bar_shade_split_more_notifications)
            setLabelFormater { value ->
                context.getString(
                    R.string.status_bar_shade_split_percentage_value,
                    value.toInt(),
                )
            }
            value = currentValue
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    val percentage =
                        (newValue as Int).coerceIn(MIN_SPLIT_PERCENT, MAX_SPLIT_PERCENT)
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.STATUS_BAR_SHADE_SPLIT_PERCENTAGE,
                        percentage,
                    )
                    preference.summary = splitSummary(context, percentage)
                    true
                }
        }
    }

    private fun setIllustrationForSelection(selectedKey: String) {
        val configuration = requireContext().getResources().getConfiguration()
        val isRtl = configuration.getLayoutDirection() == LAYOUT_DIRECTION_RTL
        setIllustration(
            when (selectedKey) {
                KEY_DUAL_SHADE_PREFERENCE ->
                    if (isRtl) R.raw.lottie_shade_panels_separate_rtl
                    else R.raw.lottie_shade_panels_separate_ltr
                KEY_SINGLE_SHADE_PREFERENCE ->
                    if (isRtl) R.raw.lottie_shade_panels_combined_rtl
                    else R.raw.lottie_shade_panels_combined_ltr
                else -> 0 // Indicate no specific animation for unknown keys
            },
            IllustrationType.LOTTIE_ANIMATION
        )
    }

    companion object {
        @VisibleForTesting
        const val KEY_DUAL_SHADE_PREFERENCE = "dual_shade"
        @VisibleForTesting
        const val KEY_SINGLE_SHADE_PREFERENCE = "single_shade"
        private const val KEY_SPLIT_RATIO_CATEGORY = "shade_split_ratio_category"
        private const val KEY_SUGGESTIONS_CATEGORY = "shade_panels_suggestions_category"

        private const val MIN_SPLIT_PERCENT = 10
        private const val MAX_SPLIT_PERCENT = 90
        private const val DEFAULT_SPLIT_PERCENT = 50

        private fun currentSplitPercent(context: Context): Int {
            return Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.STATUS_BAR_SHADE_SPLIT_PERCENTAGE,
                    defaultSplitPercent(context),
                )
                .coerceIn(MIN_SPLIT_PERCENT, MAX_SPLIT_PERCENT)
        }

        /** Matches the unset default used by SystemUI: config_invocationGestureSplitRatio. */
        private fun defaultSplitPercent(context: Context): Int {
            return try {
                val sysuiContext =
                    context.createPackageContext(Utils.SYSTEMUI_PACKAGE_NAME, 0 /* flags */)
                val resId =
                    sysuiContext.resources.getIdentifier(
                        "config_invocationGestureSplitRatio",
                        "dimen",
                        Utils.SYSTEMUI_PACKAGE_NAME,
                    )
                if (resId == 0) {
                    DEFAULT_SPLIT_PERCENT
                } else {
                    (sysuiContext.resources.getFloat(resId) * 100)
                        .roundToInt()
                        .coerceIn(MIN_SPLIT_PERCENT, MAX_SPLIT_PERCENT)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                DEFAULT_SPLIT_PERCENT
            } catch (e: SecurityException) {
                DEFAULT_SPLIT_PERCENT
            }
        }

        private fun splitSummary(context: Context, notificationsPercent: Int): String {
            return context.getString(
                R.string.status_bar_shade_split_percentage_summary,
                notificationsPercent,
                100 - notificationsPercent,
            )
        }

        // Expose as a static field for the @SearchIndexable annotation processor.
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.shade_panels_settings) {

                override fun isPageSearchEnabled(context: Context): Boolean =
                    ShadePanelsPreferenceController.isDualShadeAvailable(context)

                override fun getRawDataToIndex(context: Context, enabled: Boolean): List<SearchIndexableRaw> {
                    return listOf(
                        SearchIndexableRaw(context).apply {
                            title = context.getString(R.string.shade_panels_separate_title)
                            summaryOn = context.getString(R.string.shade_panels_separate_summary)
                            key = KEY_DUAL_SHADE_PREFERENCE
                        },
                        SearchIndexableRaw(context).apply {
                            title = context.getString(R.string.shade_panels_combined_title)
                            summaryOn = context.getString(R.string.shade_panels_combined_summary)
                            key = KEY_SINGLE_SHADE_PREFERENCE
                        },
                        SearchIndexableRaw(context).apply {
                            title =
                                context.getString(
                                    R.string.status_bar_shade_split_percentage_title
                                )
                            summaryOn =
                                splitSummary(context, currentSplitPercent(context))
                            keywords = context.getString(R.string.keywords_shade_split_ratio)
                            key = Settings.System.STATUS_BAR_SHADE_SPLIT_PERCENTAGE
                        },
                    )
                }
            }
    }
}
