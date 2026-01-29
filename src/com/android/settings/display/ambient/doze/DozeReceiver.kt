/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.ambient.doze

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (DEBUG) Log.d(TAG, "Starting service")
            DozeUtils.enableService(context)
        }
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "DerpFestDoze"
    }
}
