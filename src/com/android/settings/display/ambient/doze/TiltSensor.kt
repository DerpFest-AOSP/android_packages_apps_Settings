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
import android.os.SystemClock
import android.os.UserHandle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class TiltSensor(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_TILT_DETECTOR)
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var entryTimestamp: Long = 0

    private val vibrator: Vibrator? = run {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (v?.hasVibrator() == true) v else null
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "TiltSensor"
        private const val BATCH_LATENCY_IN_MS = 100
        private const val MIN_PULSE_INTERVAL_MS = 2500L
    }

    private fun submit(runnable: Runnable): Future<*> {
        return executorService.submit(runnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (DEBUG) Log.d(TAG, "Got sensor event: ${event.values[0]}")

        val delta = SystemClock.elapsedRealtime() - entryTimestamp
        if (delta < MIN_PULSE_INTERVAL_MS) {
            return
        } else {
            entryTimestamp = SystemClock.elapsedRealtime()
        }

        if (event.values[0] == 1f) {
            DozeUtils.launchDozePulse(context)
            doHapticFeedback()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Empty
    }

    fun enable() {
        if (DEBUG) Log.d(TAG, "Enabling")
        sensor?.let {
            submit {
                entryTimestamp = SystemClock.elapsedRealtime()
                sensorManager.registerListener(
                    this,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    BATCH_LATENCY_IN_MS * 1000
                )
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
