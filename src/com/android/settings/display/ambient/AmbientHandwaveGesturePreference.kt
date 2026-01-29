/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display.ambient

import android.content.Context
import com.android.settings.R
import com.android.settings.display.ambient.doze.DozeUtils
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

class AmbientHandwaveGesturePreference(context: Context) :
    SwitchPreference(
        key = KEY,
        purpose = R.string.handwave_gesture_purpose,
        title = R.string.handwave_title,
        summary = R.string.handwave_summary,
    ),
    PreferenceAvailabilityProvider {

    private val dataStore = context.dataStore

    override fun dependencies(context: Context) = arrayOf(AmbientDisplayMainSwitchPreference.KEY)

    override val availabilityDescription =
        "The device must have a proximity sensor for ambient display gestures."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        return DozeUtils.getProximitySensor(context)
    }

    override fun storage(context: Context): KeyValueStore = dataStore

    companion object {
        const val KEY = "doze_handwave_gesture"

        private val Context.dataStore: KeyValueStore
            get() = SettingsSecureStore.get(this).apply { setDefaultValue(KEY, false) }
    }
}
