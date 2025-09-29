/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.development

import android.os.Bundle
import android.util.Log
import com.android.settings.core.BaseAppListSettingsFragment
import com.android.internal.util.HideDeveloperStatusUtils
import com.android.settings.R

class HideDeveloperStatusSettings : BaseAppListSettingsFragment() {

    private lateinit var utils: HideDeveloperStatusUtils

    override fun getTitleResId() = R.string.hide_developer_status_title

    override fun getInitialCheckedList() = utils.apps.toList()

    override fun onListUpdate(packageName: String, isChecked: Boolean) {
        if (isChecked) {
            utils.addApp(packageName)
        } else {
            utils.removeApp(packageName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        utils = HideDeveloperStatusUtils(requireContext())
    }

    companion object {
        private const val TAG = "HideDeveloperStatusSettings"
    }
}
