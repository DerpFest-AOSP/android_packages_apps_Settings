/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.aboutphone;

import android.view.View;
import android.widget.TextView;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

public class AboutDeviceFirmwareVersionPreferenceController extends BasePreferenceController {

    public AboutDeviceFirmwareVersionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(com.android.settings.R.string.about_device_firmware_version);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        
        LayoutPreference preference = (LayoutPreference) screen.findPreference(getPreferenceKey());
        if (preference != null) {
            // Get the root view of your custom layout
            View widgetFrame = preference.findViewById(android.R.id.widget_frame);
            if (widgetFrame != null && widgetFrame.getParent() instanceof View) {
                ((View) widgetFrame.getParent()).setBackground(null);
            }
            
            // Populate the TextViews manually
            TextView titleView = preference.findViewById(android.R.id.title);
            TextView summaryView = preference.findViewById(android.R.id.summary);
            
            if (titleView != null) {
                titleView.setText(mContext.getString(R.string.firmware_version));
            }
            if (summaryView != null) {
                summaryView.setText(getSummary());
            }
        }
    }
}
