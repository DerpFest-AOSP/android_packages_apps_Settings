/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.utils.getLocale
import com.android.settingslib.DeviceInfoUtils
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class SecurityPatchLevelPreference :
    PersistentPreference<String>,
    PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceBinding,
    PreferenceLifecycleProvider {

    private var currentPatch: String? = null

    override val key: String
        get() = "security_key"

    override val purpose: Int
        get() = R.string.security_key_purpose

    override val title: Int
        get() = R.string.security_patch

    override fun intent(context: Context): android.content.Intent? = null

    override val availabilityDescription =
        "The device must have a security patch level."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = context.getPatch().isNotEmpty()

    override val supportsWrite = false

    override val valueType = String::class.javaObjectType

    override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

    override fun getSummary(context: Context) = context.getPatch()

    private fun Context.getPatch(): String =
        currentPatch
            ?: (DeviceInfoUtils.getSecurityPatch(getLocale()) ?: "").also { currentPatch = it }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        val preference = context.requirePreference<Preference>(key)
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            SecurityDialogFragment.show(context.childFragmentManager)
            true
        }
    }
}
// LINT.ThenChange(SecurityPatchLevelPreferenceController.java)
