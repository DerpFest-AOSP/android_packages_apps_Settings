/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.ambient.doze

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.UserHandle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ProximitySensor(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor?
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    private var sawNear: Boolean = false
    private var inPocketTime: Long = 0

    private val vibrator: Vibrator? = run {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (v?.hasVibrator() == true) v else null
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "ProximitySensor"
        // Maximum time for the hand to cover the sensor: 1s
        private const val HANDWAVE_MAX_DELTA_NS = 1000L * 1000 * 1000
        // Minimum time until the device is considered to have been in the pocket: 2s
        private const val POCKET_MIN_DELTA_NS = 2000L * 1000 * 1000
    }

    init {
        val wakeup = try {
            context.resources.getBoolean(com.android.internal.R.bool.config_deviceHaveWakeUpProximity)
        } catch (e: Exception) {
            false
        }
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY, wakeup)
    }

    private fun submit(runnable: Runnable): Future<*> {
        return executorService.submit(runnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val isNear = event.values[0] < (sensor?.maximumRange ?: 0f)
        if (sawNear && !isNear) {
            if (shouldPulse(event.timestamp)) {
                DozeUtils.launchDozePulse(context)
                doHapticFeedback()
            }
        } else {
            inPocketTime = event.timestamp
        }
        sawNear = isNear
    }

    private fun shouldPulse(timestamp: Long): Boolean {
        val delta = timestamp - inPocketTime

        return when {
            DozeUtils.handwaveGestureEnabled(context) && DozeUtils.pocketGestureEnabled(context) -> true
            DozeUtils.handwaveGestureEnabled(context) -> delta < HANDWAVE_MAX_DELTA_NS
            DozeUtils.pocketGestureEnabled(context) -> delta >= POCKET_MIN_DELTA_NS
            else -> false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Empty
    }

    fun enable() {
        if (DEBUG) Log.d(TAG, "Enabling")
        sensor?.let {
            submit {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    fun disable() {
        if (DEBUG) Log.d(TAG, "Disabling")
        sensor?.let {
            submit {
                sensorManager.unregisterListener(this, it)
            }
        }
    }

    private fun doHapticFeedback() {
        vibrator ?: return
        val valMs = Settings.Secure.getIntForUser(
            context.contentResolver,
            "doze_gesture_vibrate",
            0,
            UserHandle.USER_CURRENT
        )
        if (valMs > 0) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    valMs.toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }
}
