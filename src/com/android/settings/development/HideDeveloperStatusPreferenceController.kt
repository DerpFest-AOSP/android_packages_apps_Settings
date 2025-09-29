/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.development

import android.content.Context
import com.android.settings.core.PreferenceControllerMixin
import com.android.settingslib.development.DeveloperOptionsPreferenceController

class HideDeveloperStatusPreferenceController(context: Context) :
        DeveloperOptionsPreferenceController(context),
        PreferenceControllerMixin {

    override fun getPreferenceKey(): String = PREF_KEY

    companion object {
        private const val TAG = "HideDeveloperStatusPreferenceController"
        private const val PREF_KEY = "hide_developer_status_settings"
    }
}
