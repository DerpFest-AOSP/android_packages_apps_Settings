/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.darkmode

import android.content.ContentResolver
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.BaseAppListSettingsFragment
/**
 * Per-app extended dark theme settings.
 *
 * Shows a list of apps. Checked = app uses **standard** dark (excluded from extended dark).
 * Unchecked = app uses extended dark when the global extended dark setting is on.
 *
 * Persists to [Settings.System.ACCESSIBILITY_FORCE_INVERT_COLOR_OVERRIDE_PACKAGES_TO_DISABLE].
 */
class ExtendedDarkThemePerAppSettings : BaseAppListSettingsFragment() {

    override fun getTitleResId(): Int = R.string.extended_dark_theme_per_app_title

    override fun getInitialCheckedList(): List<String> = getPackagesToDisable(requireContext().contentResolver)

    override fun onListUpdate(packageName: String, isChecked: Boolean) {
        val cr = requireContext().contentResolver
        val current = getPackagesToDisable(cr).toMutableSet()
        if (isChecked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        setPackagesToDisable(cr, current)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    companion object {
        private const val TAG = "ExtendedDarkThemePerApp"

        private fun getPackagesToDisable(cr: ContentResolver): List<String> {
            val csv = Settings.System.getStringForUser(
                cr,
                Settings.System.ACCESSIBILITY_FORCE_INVERT_COLOR_OVERRIDE_PACKAGES_TO_DISABLE,
                UserHandle.myUserId()
            ) ?: return emptyList()
            return csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        private fun setPackagesToDisable(cr: ContentResolver, packages: Set<String>) {
            val value = packages.joinToString(",")
            Settings.System.putStringForUser(
                cr,
                Settings.System.ACCESSIBILITY_FORCE_INVERT_COLOR_OVERRIDE_PACKAGES_TO_DISABLE,
                value,
                UserHandle.myUserId()
            )
        }
    }
}
