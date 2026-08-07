/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: AxionOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display

import android.content.Context
import android.provider.Settings
import android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

class BlurSettingsPreferenceController(
    context: Context,
) : BasePreferenceController(context, KEY_BLUR_SETTINGS) {

    override fun getAvailabilityStatus(): Int {
        return if (CROSS_WINDOW_BLUR_SUPPORTED) AVAILABLE else UNSUPPORTED_ON_DEVICE
    }

    override fun getSummary(): CharSequence {
        val blursEnabled = Settings.Global.getInt(
            mContext.contentResolver,
            Settings.Global.DISABLE_WINDOW_BLURS,
            0,
        ) == 0

        if (blursEnabled) {
            val intensity = Settings.System.getInt(
                mContext.contentResolver,
                Settings.System.BLUR_INTENSITY,
                DEFAULT_BLUR_INTENSITY,
            ).coerceIn(MIN_BLUR_INTENSITY, MAX_BLUR_INTENSITY)
            return mContext.getString(R.string.blur_settings_summary_on, intensity)
        }
        return mContext.getString(R.string.blur_settings_summary_off)
    }

    companion object {
        const val KEY_BLUR_SETTINGS = "blur_settings"
        private const val MIN_BLUR_INTENSITY = 0
        private const val MAX_BLUR_INTENSITY = 200
        private const val DEFAULT_BLUR_INTENSITY = 100
    }
}
