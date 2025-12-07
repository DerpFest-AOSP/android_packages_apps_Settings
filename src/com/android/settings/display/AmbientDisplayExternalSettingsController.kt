/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.Secure.DOZE_ENABLED
import com.android.settings.core.TogglePreferenceController

import com.android.settings.R

class AmbientDisplayExternalSettingsController(
    context: Context,
    key: String
) : TogglePreferenceController(context, key) {

    private val AOD_SETTINGS_KEY = "ambient_display_always_on_screen"
    private val DOZE_SETTINGS = "org.lineageos.settings.device.DOZE_SETTINGS"

    override fun getAvailabilityStatus(): Int =
        AVAILABLE.takeIf  {
            mContext.packageManager.queryIntentActivities(
                Intent(DOZE_SETTINGS), 0
            ).isNotEmpty()
        } ?: UNSUPPORTED_ON_DEVICE

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_display

    override fun isChecked(): Boolean =
        Settings.Secure.getInt(mContext.contentResolver, DOZE_ENABLED, 1) != 0

    override fun setChecked(isChecked: Boolean): Boolean =
        Settings.Secure.putInt(mContext.contentResolver, DOZE_ENABLED, if (isChecked) 1 else 0)
}

