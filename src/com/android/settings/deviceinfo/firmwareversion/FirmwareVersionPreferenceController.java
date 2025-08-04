/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import android.view.View;
import android.widget.TextView;
import android.content.Context;
import android.os.Build;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

public class FirmwareVersionPreferenceController extends BasePreferenceController {

    public FirmwareVersionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY;
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
