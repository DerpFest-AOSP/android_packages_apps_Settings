/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display.ambient

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.display.ambient.doze.DozeUtils
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

class AmbientAdditionalSettingsPreference(context: Context) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.doze_additional_settings_title

    override val summary: Int
        get() = R.string.doze_additional_settings_summary

    override val indexable
        get() = true

    override fun getSummary(context: Context): CharSequence? {
        return context.getString(summary)
    }

    override fun isAvailable(context: Context): Boolean {
        return DozeUtils.isDozeAdditionalSettingsAvailable(context) &&
            (DozeUtils.getTiltSensor(context) ||
                DozeUtils.getPickupSensor(context) ||
                DozeUtils.getProximitySensor(context))
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.fragment = AmbientSensorSettingsFragment::class.java.name
    }

    companion object {
        const val KEY = "doze_additional_settings"
    }
}
