/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.darkmode

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

/**
 * Preference that opens the per-app extended dark theme list.
 * Only available when the expanded dark theme toggle is selected.
 */
class ExtendedDarkThemePerAppPreference(private val modeStorage: DarkThemeModeStorage) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.extended_dark_theme_per_app_title

    override val summary: Int
        get() = R.string.extended_dark_theme_per_app_summary

    override fun isAvailable(context: Context): Boolean {
        if (!android.view.accessibility.Flags.forceInvertColor()) return false
        return modeStorage.getValue(
            ExpandedDarkModeSelectorPreference.KEY,
            Boolean::class.java
        ) == true
    }

    override fun dependencies(context: Context): Array<String> =
        arrayOf(ExpandedDarkModeSelectorPreference.KEY)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.fragment = ExtendedDarkThemePerAppSettings::class.java.name
    }

    companion object {
        const val KEY = "extended_dark_theme_per_app_settings"
    }
}
