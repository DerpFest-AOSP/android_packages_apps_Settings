/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display.ambient

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.preference.Preference
import androidx.fragment.app.Fragment
import com.android.settings.CatalystFragment
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.display.ambient.doze.DozeUtils
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceCategory as Category
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(AmbientSensorSettingsScreen.KEY)
class AmbientSensorSettingsScreen(context: Context) :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {

    private val ambientTiltGesturePreference = AmbientTiltGesturePreference(context)
    private val ambientPickupGesturePreference = AmbientPickupGesturePreference(context)
    private val ambientHandwaveGesturePreference = AmbientHandwaveGesturePreference(context)
    private val ambientPocketGesturePreference = AmbientPocketGesturePreference(context)
    private val ambientRaiseToWakePreference = AmbientRaiseToWakePreference(context)
    private val ambientDozeVibratePreference = AmbientDozeVibratePreference(context)
    private lateinit var sensorObserver: KeyedObserver<String>

    override val title: Int
        get() = R.string.doze_additional_settings_title

    override val key: String
        get() = KEY

    override val indexable
        get() = true

    override fun getMetricsCategory() = SettingsEnums.AMBIENT_DISPLAY_ALWAYS_ON

    override val highlightMenuKey: Int
        get() = R.string.menu_key_display

    override fun isAvailable(context: Context): Boolean {
        return DozeUtils.isDozeAdditionalSettingsAvailable(context) &&
            (DozeUtils.getTiltSensor(context) ||
                DozeUtils.getPickupSensor(context) ||
                DozeUtils.getProximitySensor(context))
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            // Observe sensor preference changes to enable/disable service
            sensorObserver = KeyedObserver { key, _ ->
                DozeUtils.enableService(context)
                if (key == AmbientRaiseToWakePreference.KEY) {
                    updateVibratePreferenceEnabled(context)
                }
            }
            val storage = SettingsSecureStore.get(context)
            if (ambientTiltGesturePreference.isAvailable(context)) {
                storage.addObserver(AmbientTiltGesturePreference.KEY, sensorObserver, HandlerExecutor.main)
            }
            if (ambientPickupGesturePreference.isAvailable(context)) {
                storage.addObserver(AmbientPickupGesturePreference.KEY, sensorObserver, HandlerExecutor.main)
            }
            if (ambientHandwaveGesturePreference.isAvailable(context)) {
                storage.addObserver(AmbientHandwaveGesturePreference.KEY, sensorObserver, HandlerExecutor.main)
            }
            if (ambientPocketGesturePreference.isAvailable(context)) {
                storage.addObserver(AmbientPocketGesturePreference.KEY, sensorObserver, HandlerExecutor.main)
            }
            if (ambientRaiseToWakePreference.isAvailable(context)) {
                storage.addObserver(AmbientRaiseToWakePreference.KEY, sensorObserver, HandlerExecutor.main)
            }
            if (ambientDozeVibratePreference.isAvailable(context)) {
                updateVibratePreferenceEnabled(context)
            }
        }
    }

    private fun updateVibratePreferenceEnabled(context: PreferenceLifecycleContext) {
        if (!ambientDozeVibratePreference.isAvailable(context)) return
        val pref = context.findPreference<Preference>(AmbientDozeVibratePreference.KEY) ?: return
        pref.isEnabled = !DozeUtils.isRaiseToWakeEnabled(context)
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            val storage = SettingsSecureStore.get(context)
            if (ambientTiltGesturePreference.isAvailable(context)) {
                storage.removeObserver(AmbientTiltGesturePreference.KEY, sensorObserver)
            }
            if (ambientPickupGesturePreference.isAvailable(context)) {
                storage.removeObserver(AmbientPickupGesturePreference.KEY, sensorObserver)
            }
            if (ambientHandwaveGesturePreference.isAvailable(context)) {
                storage.removeObserver(AmbientHandwaveGesturePreference.KEY, sensorObserver)
            }
            if (ambientPocketGesturePreference.isAvailable(context)) {
                storage.removeObserver(AmbientPocketGesturePreference.KEY, sensorObserver)
            }
            if (ambientRaiseToWakePreference.isAvailable(context)) {
                storage.removeObserver(AmbientRaiseToWakePreference.KEY, sensorObserver)
            }
        }
    }

    override fun fragmentClass(): Class<out Fragment>? = AmbientSensorSettingsFragment::class.java

    override fun hasCompleteHierarchy() = true

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, AmbientSensorSettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +Category("ambient_sensorGroup", R.string.doze_sensor_title) += {
                if (ambientTiltGesturePreference.isAvailable(context)) {
                    +ambientTiltGesturePreference
                }
                if (ambientPickupGesturePreference.isAvailable(context)) {
                    +ambientPickupGesturePreference
                }
                if (ambientHandwaveGesturePreference.isAvailable(context)) {
                    +ambientHandwaveGesturePreference
                }
                if (ambientPocketGesturePreference.isAvailable(context)) {
                    +ambientPocketGesturePreference
                }
                if (ambientRaiseToWakePreference.isAvailable(context)) {
                    +ambientRaiseToWakePreference
                }
                if (ambientDozeVibratePreference.isAvailable(context)) {
                    +ambientDozeVibratePreference
                }
            }
        }

    companion object {
        const val KEY = "ambient_sensor_settings"
    }
}

class AmbientSensorSettingsActivity :
    CatalystSettingsActivity(
        AmbientSensorSettingsScreen.KEY,
        AmbientSensorSettingsFragment::class.java,
    )

class AmbientSensorSettingsFragment : CatalystFragment() {
    override fun getPreferenceScreenBindingKey(context: Context): String {
        return AmbientSensorSettingsScreen.KEY
    }
}
