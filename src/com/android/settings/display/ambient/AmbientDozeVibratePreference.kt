/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display.ambient

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.display.ambient.doze.DozeUtils
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

class AmbientDozeVibratePreference(context: Context) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.doze_vibrate

    override val indexable: Boolean
        get() = true

    override fun dependencies(context: Context) = arrayOf(AmbientDisplayMainSwitchPreference.KEY)

    override fun isAvailable(context: Context): Boolean {
        return DozeUtils.isDozeAdditionalSettingsAvailable(context) &&
            (DozeUtils.getTiltSensor(context) ||
                DozeUtils.getPickupSensor(context) ||
                DozeUtils.getProximitySensor(context))
    }

    override fun getSummary(context: Context): CharSequence {
        val ms = Settings.Secure.getIntForUser(
            context.contentResolver,
            "doze_gesture_vibrate",
            0,
            UserHandle.USER_CURRENT
        )
        return if (ms <= 0) context.getString(R.string.disabled)
        else context.getString(R.string.unit_milliseconds).let { u -> "$ms $u" }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.fragment = DozeVibrateFragment::class.java.name
    }

    companion object {
        const val KEY = "doze_gesture_vibrate_settings"
    }
}
