/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.android.settings.display.ambient

import android.content.Context
import android.hardware.display.AmbientDisplayConfiguration
import android.os.UserHandle
import android.provider.Settings.Secure.DOZE_ALWAYS_ON
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.SwitchPreference

class AmbientDozeOnChargePreference(context: Context) :
    SwitchPreference(
        KEY,
        R.string.doze_on_charge_title,
        R.string.doze_on_charge_summary,
    ),
    PreferenceAvailabilityProvider {

    private val dataStore = context.dataStore
    private val dozeAlwaysOnDataStore = AmbientDisplayStorage(context)
    private val config = AmbientDisplayConfiguration(context)

    override fun dependencies(context: Context) = arrayOf(AmbientDisplayMainSwitchPreference.KEY)

    override fun isEnabled(context: Context): Boolean {
        // Enable only when AOD is available and not enabled (greyed out when AOD is on)
        return config.alwaysOnAvailableForUser(UserHandle.USER_CURRENT) &&
            !dozeAlwaysOnDataStore.getBoolean(DOZE_ALWAYS_ON)!!
    }

    override fun isAvailable(context: Context): Boolean {
        // Always show when AOD is available on device; visibility like AOD wallpaper
        return config.alwaysOnAvailableForUser(UserHandle.USER_CURRENT)
    }

    override fun storage(context: Context) = dataStore

    companion object {
        const val KEY = "doze_on_charge"

        private val Context.dataStore: KeyValueStore
            get() = SettingsSecureStore.get(this).apply { setDefaultValue(KEY, false) }
    }
}
