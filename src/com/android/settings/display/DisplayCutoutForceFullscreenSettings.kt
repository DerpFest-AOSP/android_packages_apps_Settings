/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display

import android.os.Bundle
import android.util.Log
import com.android.settings.core.BaseAppListSettingsFragment
import com.android.internal.util.CutoutFullscreenController
import com.android.settings.R

class DisplayCutoutForceFullscreenSettings : BaseAppListSettingsFragment() {

    private lateinit var cutoutForceFullscreenSettings: CutoutFullscreenController

    override fun getTitleResId() = R.string.display_cutout_force_fullscreen_title

    override fun getInitialCheckedList() = cutoutForceFullscreenSettings.apps.toList()

    override fun onListUpdate(packageName: String, isChecked: Boolean) {
        if (isChecked) {
            cutoutForceFullscreenSettings.addApp(packageName)
        } else {
            cutoutForceFullscreenSettings.removeApp(packageName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cutoutForceFullscreenSettings = CutoutFullscreenController(requireContext())
    }

    companion object {
        private const val TAG = "DisplayCutoutForceFullscreenSettings"
    }
}
