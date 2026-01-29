/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.ambient.doze

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log

object DozeUtils {
    private const val TAG = "DozeUtils"
    private const val DEBUG = false
    private var mServiceEnabled = false

    const val DOZE_INTENT = "com.android.systemui.doze.pulse"

    private fun startService(context: Context) {
        if (DEBUG) Log.d(TAG, "Starting service")
        context.startServiceAsUser(
            Intent(context, DozeService::class.java),
            UserHandle.CURRENT
        )
        mServiceEnabled = true
    }

    private fun stopService(context: Context) {
        if (DEBUG) Log.d(TAG, "Stopping service")
        mServiceEnabled = false
        context.stopServiceAsUser(
            Intent(context, DozeService::class.java),
            UserHandle.CURRENT
        )
    }

    fun getTiltSensor(context: Context): Boolean {
        return try {
            context.resources.getBoolean(
                com.android.internal.R.bool.config_dozePulseTilt
            )
        } catch (e: Exception) {
            false
        }
    }

    fun getPickupSensor(context: Context): Boolean {
        return try {
            context.resources.getBoolean(
                com.android.internal.R.bool.config_dozePulsePickup
            )
        } catch (e: Exception) {
            false
        }
    }

    fun getProximitySensor(context: Context): Boolean {
        return getProxCheckBeforePulse(context) && try {
            context.resources.getBoolean(
                com.android.internal.R.bool.config_dozePulseProximity
            )
        } catch (e: Exception) {
            false
        }
    }

    private fun getProxCheckBeforePulse(context: Context): Boolean {
        return try {
            val con = context.createPackageContext("com.android.systemui", 0)
            val id = con.resources.getIdentifier(
                "doze_proximity_check_before_pulse",
                "bool",
                "com.android.systemui"
            )
            if (id != 0) {
                con.resources.getBoolean(id)
            } else {
                false
            }
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isDozeEnabled(context: Context): Boolean {
        return Settings.Secure.getIntForUser(
            context.contentResolver,
            Settings.Secure.DOZE_ENABLED,
            1,
            UserHandle.USER_CURRENT
        ) != 0
    }

    /**
     * Whether sensor-based "Additional settings" are allowed on this device.
     * When false, the Additional settings entry and sensor screen are hidden.
     * See config_doze_additional_settings_available.
     */
    fun isDozeAdditionalSettingsAvailable(context: Context): Boolean {
        return try {
            context.resources.getBoolean(
                com.android.internal.R.bool.config_doze_additional_settings_available
            )
        } catch (e: Exception) {
            false
        }
    }

    fun isDozeAlwaysOnEnabled(context: Context): Boolean {
        return Settings.Secure.getIntForUser(
            context.contentResolver,
            Settings.Secure.DOZE_ALWAYS_ON,
            context.resources.getBoolean(
                com.android.internal.R.bool.config_dozeAlwaysOnEnabled
            ).let { if (it) 1 else 0 },
            UserHandle.USER_CURRENT
        ) != 0
    }

    private const val RAISE_TO_WAKE_GESTURE = "raise_to_wake_gesture"

    fun isRaiseToWakeEnabled(context: Context): Boolean {
        return Settings.Secure.getIntForUser(
            context.contentResolver,
            RAISE_TO_WAKE_GESTURE,
            0,
            UserHandle.USER_CURRENT
        ) == 1
    }

    fun enableService(context: Context) {
        if (!isDozeAdditionalSettingsAvailable(context)) {
            if (mServiceEnabled) stopService(context)
            return
        }
        if (!getTiltSensor(context) && !getPickupSensor(context) && !getProximitySensor(context)) {
            return
        }
        val alwaysOnEnabled = isDozeAlwaysOnEnabled(context)
        val sensorsOn = sensorsEnabled(context) || isRaiseToWakeEnabled(context)
        if (sensorsOn && !alwaysOnEnabled && !mServiceEnabled) {
            startService(context)
            showSensorWarningIfFirstTime(context)
        } else if ((!sensorsOn || alwaysOnEnabled) && mServiceEnabled) {
            stopService(context)
        }
    }

    /**
     * Show a one-time warning when enabling any sensor-based gesture.
     * Only shown when context is an Activity (e.g. from settings UI), not on boot.
     */
    fun showSensorWarningIfFirstTime(context: Context) {
        if (context !is Activity) return
        val prefs = context.getSharedPreferences("doze_sensor_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("sensor_warning_shown", false)) return
        prefs.edit().putBoolean("sensor_warning_shown", true).apply()
        android.app.AlertDialog.Builder(context)
            .setTitle(context.getString(com.android.settings.R.string.caution))
            .setMessage(context.getString(com.android.settings.R.string.sensor_warning_message))
            .setNegativeButton(android.R.string.ok, null)
            .show()
    }

    fun launchDozePulse(context: Context) {
        if (DEBUG) Log.d(TAG, "Launch doze pulse")
        context.sendBroadcastAsUser(
            Intent(DOZE_INTENT),
            UserHandle.CURRENT
        )
    }

    private const val DOZE_TILT_GESTURE = "doze_tilt_gesture"
    private const val DOZE_HANDWAVE_GESTURE = "doze_handwave_gesture"
    private const val DOZE_POCKET_GESTURE = "doze_pocket_gesture"

    fun tiltEnabled(context: Context): Boolean {
        return Settings.Secure.getIntForUser(
            context.contentResolver,
            DOZE_TILT_GESTURE,
            0,
            UserHandle.USER_CURRENT
        ) == 1
    }

    fun pickUpEnabled(context: Context): Boolean {
        return Settings.Secure.getIntForUser(
            context.contentResolver,
            Settings.Secure.DOZE_PICK_UP_GESTURE,
            0,
            UserHandle.USER_CURRENT
        ) == 1
    }

    fun handwaveGestureEnabled(context: Context): Boolean {
        return Settings.Secure.getIntForUser(
            context.contentResolver,
            DOZE_HANDWAVE_GESTURE,
            0,
            UserHandle.USER_CURRENT
        ) == 1
    }

    fun pocketGestureEnabled(context: Context): Boolean {
        return Settings.Secure.getIntForUser(
            context.contentResolver,
            DOZE_POCKET_GESTURE,
            0,
            UserHandle.USER_CURRENT
        ) == 1
    }

    fun sensorsEnabled(context: Context): Boolean {
        return tiltEnabled(context) || pickUpEnabled(context) ||
            handwaveGestureEnabled(context) || pocketGestureEnabled(context)
    }
}
