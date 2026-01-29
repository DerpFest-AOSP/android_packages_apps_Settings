/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display.ambient

import android.content.Context
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settingslib.core.AbstractPreferenceController

class DozeVibrateFragment : DashboardFragment() {

    override fun getPreferenceScreenResId(): Int = R.xml.doze_gesture_vibrate_settings

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.DERPFEST

    override fun getLogTag(): String = "DozeVibrateFragment"

    override fun createPreferenceControllers(context: Context): MutableList<AbstractPreferenceController> =
        mutableListOf()
}
