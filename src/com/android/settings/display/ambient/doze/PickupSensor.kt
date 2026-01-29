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
import android.os.PowerManager
import android.os.SystemClock
import android.os.UserHandle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import com.android.settings.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class PickupSensor(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val powerManager: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val sensorPickup: Sensor?
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val isCustomPickupSensor: Boolean
    private val minPulseIntervalMs: Int
    private val wakelockTimeoutMs: Long

    private var gravity: FloatArray? = null
    private var accelLast: Float = SensorManager.GRAVITY_EARTH
    private var accelCurrent: Float = SensorManager.GRAVITY_EARTH
    private var entryTimestamp: Long = 0

    private val vibrator: Vibrator? = run {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (v?.hasVibrator() == true) v else null
    }

    private val wakeLock: PowerManager.WakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK, "PickupSensor"
    )

    companion object {
        private const val DEBUG = false
        private const val TAG = "PickupSensor"
        private const val DEFAULT_MIN_PULSE_INTERVAL_MS = 2500
        private const val DEFAULT_WAKELOCK_TIMEOUT_MS = 300L
    }

    init {
        val pickupSensorName = try {
            context.resources.getString(R.string.pickup_sensor)
        } catch (e: Exception) {
            ""
        }
        isCustomPickupSensor = pickupSensorName.isNotEmpty()
        minPulseIntervalMs = try {
            context.resources.getInteger(R.integer.config_dozePulsePickup_MinPulseIntervalMs)
        } catch (e: Exception) {
            DEFAULT_MIN_PULSE_INTERVAL_MS
        }
        wakelockTimeoutMs = try {
            context.resources.getInteger(R.integer.config_dozePulsePickup_WakelockTimeoutMs).toLong()
        } catch (e: Exception) {
            DEFAULT_WAKELOCK_TIMEOUT_MS
        }

        sensorPickup = when {
            isCustomPickupSensor -> sensorManager.getSensorList(Sensor.TYPE_ALL)
                .firstOrNull { it.stringType == pickupSensorName }
            else -> sensorManager.getDefaultSensor(Sensor.TYPE_PICK_UP_GESTURE)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        if (DEBUG && sensorPickup != null) {
            Log.d(TAG, "Pickup sensor: ${sensorPickup.stringType}")
        }
        accelLast = SensorManager.GRAVITY_EARTH
        accelCurrent = SensorManager.GRAVITY_EARTH
    }

    private fun submit(runnable: Runnable): Future<*> {
        return executorService.submit(runnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (DEBUG) Log.d(TAG, "Got sensor event: ${event.values[0]}")

        val delta = SystemClock.elapsedRealtime() - entryTimestamp
        if (delta < minPulseIntervalMs) return
        entryTimestamp = SystemClock.elapsedRealtime()

        try {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                gravity = event.values.clone()
                val x = gravity!![0]
                val y = gravity!![1]
                val z = gravity!![2]
                accelLast = accelCurrent
                accelCurrent = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val accDelta = kotlin.math.abs(accelCurrent - accelLast)
                if (accDelta >= 0.1f && accDelta <= 1.5f) {
                    launchWakeOrPulse()
                }
            } else {
                if (event.values[0] == 1f) launchWakeOrPulse()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchWakeOrPulse() {
        if (DozeUtils.isRaiseToWakeEnabled(context)) {
            if (wakeLock.isHeld) wakeLock.release()
            wakeLock.acquire(wakelockTimeoutMs)
            powerManager.wakeUp(SystemClock.uptimeMillis(), PowerManager.WAKE_REASON_GESTURE, TAG)
        } else {
            DozeUtils.launchDozePulse(context)
            doHapticFeedback()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Empty
    }

    fun enable() {
        if (DEBUG) Log.d(TAG, "Enabling")
        sensorPickup?.let { s ->
            submit {
                entryTimestamp = SystemClock.elapsedRealtime()
                sensorManager.registerListener(
                    this,
                    s,
                    if (isCustomPickupSensor) {
                        SensorManager.SENSOR_DELAY_NORMAL
                    } else {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH
                    }
                )
            }
        }
    }

    fun disable() {
        if (DEBUG) Log.d(TAG, "Disabling")
        if (wakeLock.isHeld) wakeLock.release()
        sensorPickup?.let { s ->
            submit {
                sensorManager.unregisterListener(this, s)
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
