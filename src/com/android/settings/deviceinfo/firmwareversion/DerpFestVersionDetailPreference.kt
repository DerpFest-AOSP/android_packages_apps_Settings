/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.os.SystemProperties
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class DerpFestVersionDetailPreference :
    PreferenceMetadata, PreferenceSummaryProvider, PreferenceAvailabilityProvider,
    PreferenceBinding {

    override val key: String
        get() = "derpfest_version"

    override val purpose: Int
        get() = R.string.os_firmware_version_purpose

    override val title: Int
        get() = R.string.derpfest_version

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_PREFERENCE)

    override val availabilityDescription =
        "The device must have a DerpFest version property set."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) =
        SystemProperties.get(DERPFEST_VERSION_PROPERTY).isNotEmpty()

    override fun bind(preference: androidx.preference.Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun getSummary(context: Context): CharSequence =
        SystemProperties.get(
            DERPFEST_VERSION_PROPERTY,
            context.getString(R.string.device_info_default)
        )

    companion object {
        const val DERPFEST_VERSION_PROPERTY: String = "ro.derpfest.version"
    }
}
// LINT.ThenChange(DerpFestVersionPreferenceController.java)
