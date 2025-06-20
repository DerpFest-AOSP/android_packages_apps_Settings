/*
 * Copyright (C) 2019 The Android Open Source Project
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

// LINT.IfChange
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.firmwareversion.SecurityDialogFragment;
import com.android.settingslib.DeviceInfoUtils;

public class SecurityPatchLevelPreferenceController extends BasePreferenceController {

    private static final String TAG = "SecurityPatchCtrl";
    private static final Uri INTENT_URI_DATA = Uri.parse(
            "https://source.android.com/docs/security/bulletin/");

    private final PackageManager mPackageManager;
    private final String mCurrentPatch;
    private Fragment mHost;

    public SecurityPatchLevelPreferenceController(Context context, String key) {
        super(context, key);
        mPackageManager = mContext.getPackageManager();
        mCurrentPatch = DeviceInfoUtils.getSecurityPatch();
    }

    public void setFragment(Fragment host) {
        mHost = host;
    }

    @Override
    public int getAvailabilityStatus() {
        return !TextUtils.isEmpty(mCurrentPatch)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mCurrentPatch;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        // Show security dialog instead of opening web page
        if (mHost != null) {
            SecurityDialogFragment fragment = SecurityDialogFragment.newInstance();
            FragmentManager fragmentManager = mHost.getChildFragmentManager();
            fragment.show(fragmentManager, SecurityDialogFragment.TAG);
            return true;
        }
        return false;
    }
}
// LINT.ThenChange(SecurityPatchLevelPreference.kt)
