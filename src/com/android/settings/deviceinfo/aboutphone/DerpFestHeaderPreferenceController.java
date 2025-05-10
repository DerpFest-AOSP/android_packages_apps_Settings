/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.aboutphone;

import android.content.Context;
import android.os.SystemProperties;
import android.widget.TextView;
import androidx.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

public class DerpFestHeaderPreferenceController extends BasePreferenceController {

    private static final String TAG = "DerpFestHeaderPreferenceController";

    public DerpFestHeaderPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        String summary = mContext.getString(R.string.derpfest_summary);
        String maintainer = mContext.getString(R.string.derpfest_maintainer);
        LayoutPreference layoutPreference = (LayoutPreference) preference;
        ((TextView) layoutPreference.findViewById(R.id.status_summary)).setText(String.format(summary, maintainer));
    }
}
