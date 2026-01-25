/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.android.settings.display.ambient

import android.content.Context
import android.hardware.display.AmbientDisplayConfiguration
import android.os.SystemProperties
import android.provider.Settings.Secure.DOZE_ALWAYS_ON
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.display.AODSchedule
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

class AmbientAODSchedulePreference(context: Context) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {

    private val dozeAlwaysOnDataStore = AmbientDisplayStorage(context)
    private val config = AmbientDisplayConfiguration(context)

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.always_on_display_schedule_title

    override val indexable
        get() = true

    override fun dependencies(context: Context) = arrayOf(AmbientDisplayMainSwitchPreference.KEY)

    override fun isEnabled(context: Context): Boolean {
        // Enable only when AOD is on (greyed out when AOD is off, like doze on charge)
        return config.alwaysOnAvailableForUser(UserHandle.USER_CURRENT) &&
            dozeAlwaysOnDataStore.getBoolean(DOZE_ALWAYS_ON)!! &&
            !SystemProperties.getBoolean(PROP_AWARE_AVAILABLE, false)
    }

    override fun isAvailable(context: Context): Boolean {
        // Always show when AOD is available on device; greyed out when AOD is off
        return config.alwaysOnAvailableForUser(UserHandle.USER_CURRENT) &&
            !SystemProperties.getBoolean(PROP_AWARE_AVAILABLE, false)
    }

    override fun getSummary(context: Context): CharSequence {
        val mode = Settings.Secure.getIntForUser(
            context.contentResolver,
            Settings.Secure.DOZE_ALWAYS_ON_AUTO_MODE,
            0,
            UserHandle.USER_CURRENT
        )
        return when (mode) {
            MODE_NIGHT -> context.getString(R.string.night_display_auto_mode_twilight)
            MODE_TIME -> context.getString(R.string.night_display_auto_mode_custom)
            MODE_MIXED_SUNSET -> context.getString(R.string.always_on_display_schedule_mixed_sunset)
            MODE_MIXED_SUNRISE -> context.getString(R.string.always_on_display_schedule_mixed_sunrise)
            else -> context.getString(R.string.disabled)
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.fragment = AODSchedule::class.java.name
    }

    companion object {
        const val KEY = "always_on_display_schedule"

        const val MODE_DISABLED = 0
        const val MODE_NIGHT = 1
        const val MODE_TIME = 2
        const val MODE_MIXED_SUNSET = 3
        const val MODE_MIXED_SUNRISE = 4

        private const val PROP_AWARE_AVAILABLE = "ro.vendor.aware_available"
    }
}
