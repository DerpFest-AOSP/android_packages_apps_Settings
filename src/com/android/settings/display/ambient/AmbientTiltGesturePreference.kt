/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display.ambient

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import com.android.settings.R
import com.android.settings.display.ambient.doze.DozeUtils
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.SwitchPreference

class AmbientTiltGesturePreference(context: Context) :
    SwitchPreference(
        KEY,
        R.string.tilt_title,
        R.string.tilt_summary,
    ),
    PreferenceAvailabilityProvider {

    private val dataStore = context.dataStore

    override fun dependencies(context: Context) = arrayOf(AmbientDisplayMainSwitchPreference.KEY)

    override fun isAvailable(context: Context): Boolean {
        return DozeUtils.getTiltSensor(context)
    }

    override fun storage(context: Context): KeyValueStore = dataStore

    companion object {
        const val KEY = "doze_tilt_gesture"

        private val Context.dataStore: KeyValueStore
            get() = SettingsSecureStore.get(this).apply { setDefaultValue(KEY, false) }
    }
}
