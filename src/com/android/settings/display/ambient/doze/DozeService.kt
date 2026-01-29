/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.ambient.doze

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

class DozeService : Service() {
    private var tiltSensor: TiltSensor? = null
    private var pickupSensor: PickupSensor? = null
    private var proximitySensor: ProximitySensor? = null

    private var tiltSensorAvailable: Boolean = false
    private var pickupSensorAvailable: Boolean = false
    private var proximitySensorAvailable: Boolean = false

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> onDisplayOn()
                Intent.ACTION_SCREEN_OFF -> onDisplayOff()
            }
        }
    }

    companion object {
        private const val TAG = "DerpFestDozeService"
        private const val DEBUG = false
    }

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        tiltSensorAvailable = DozeUtils.getTiltSensor(this)
        pickupSensorAvailable = DozeUtils.getPickupSensor(this)
        proximitySensorAvailable = DozeUtils.getProximitySensor(this)

        if (!tiltSensorAvailable && !pickupSensorAvailable && !proximitySensorAvailable) {
            return
        }

        if (tiltSensorAvailable) {
            tiltSensor = TiltSensor(this)
        }
        if (pickupSensorAvailable) {
            pickupSensor = PickupSensor(this)
        }
        if (proximitySensorAvailable) {
            proximitySensor = ProximitySensor(this)
        }

        val screenStateFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, screenStateFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        super.onDestroy()

        if (!tiltSensorAvailable && !pickupSensorAvailable && !proximitySensorAvailable) {
            return
        }

        unregisterReceiver(screenStateReceiver)
        tiltSensor?.disable()
        pickupSensor?.disable()
        proximitySensor?.disable()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun pickupEnabled(): Boolean =
        DozeUtils.pickUpEnabled(this) || DozeUtils.isRaiseToWakeEnabled(this)

    private fun onDisplayOn() {
        if (DEBUG) Log.d(TAG, "Display on")
        if (tiltSensorAvailable && DozeUtils.tiltEnabled(this)) {
            tiltSensor?.disable()
        }
        if (pickupSensorAvailable && pickupEnabled()) {
            pickupSensor?.disable()
        }
        if (proximitySensorAvailable && (DozeUtils.handwaveGestureEnabled(this) ||
                DozeUtils.pocketGestureEnabled(this))
        ) {
            proximitySensor?.disable()
        }
    }

    private fun onDisplayOff() {
        if (DEBUG) Log.d(TAG, "Display off")
        if (tiltSensorAvailable && DozeUtils.tiltEnabled(this)) {
            tiltSensor?.enable()
        }
        if (pickupSensorAvailable && pickupEnabled()) {
            pickupSensor?.enable()
        }
        if (proximitySensorAvailable && (DozeUtils.handwaveGestureEnabled(this) ||
                DozeUtils.pocketGestureEnabled(this))
        ) {
            proximitySensor?.enable()
        }
    }
}
