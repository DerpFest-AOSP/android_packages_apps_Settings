/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.aboutphone;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.firmwareversion.SecurityDialogFragment;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.widget.LayoutPreference;

public class AboutDeviceSecurityPatchLevelPreferenceController extends BasePreferenceController {

    private Fragment mHost;

    public AboutDeviceSecurityPatchLevelPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void setHost(Fragment host) {
        mHost = host;
    }

    @Override
    public int getAvailabilityStatus() {
        return !DeviceInfoUtils.getSecurityPatch().isEmpty()
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return DeviceInfoUtils.getSecurityPatch();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }

        // Show security dialog
        if (mHost != null) {
            FragmentManager fragmentManager = mHost.getChildFragmentManager();
            SecurityDialogFragment.show(fragmentManager);
            return true;
        }
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
                titleView.setText(mContext.getString(R.string.security_patch));
            }
            if (summaryView != null) {
                summaryView.setText(getSummary());
            }
        }
    }
}
