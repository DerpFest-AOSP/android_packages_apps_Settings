/*
 * Copyright (C) 2026 Project ASCP
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

package com.android.settings.wifi.tether;

import android.content.Context;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class HotspotDataLimitActionController extends BasePreferenceController {

    private static final String KEY_ACTION = "hotspot_data_limit_action";
    private static final int ACTION_TURN_OFF_NOTIFY = 0;
    private static final int ACTION_NOTIFY_ONLY = 1;

    private Preference mPreference;

    public HotspotDataLimitActionController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateSummary();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        showActionDialog();
        return true;
    }

    private void updateSummary() {
        if (mPreference == null) {
            return;
        }
        final int action = Settings.Global.getInt(
                mContext.getContentResolver(), KEY_ACTION, ACTION_TURN_OFF_NOTIFY);
        mPreference.setSummary(action == ACTION_NOTIFY_ONLY
                ? R.string.one_time_data_limit_notify_only
                : R.string.one_time_data_limit_turn_off_notify);
    }

    private void showActionDialog() {
        final String[] options = {
                mContext.getString(R.string.one_time_data_limit_turn_off_notify),
                mContext.getString(R.string.one_time_data_limit_notify_only)
        };
        int current = Settings.Global.getInt(
                mContext.getContentResolver(), KEY_ACTION, ACTION_TURN_OFF_NOTIFY);
        if (current != ACTION_TURN_OFF_NOTIFY && current != ACTION_NOTIFY_ONLY) {
            current = ACTION_TURN_OFF_NOTIFY;
        }

        new AlertDialog.Builder(mContext)
                .setTitle(R.string.one_time_data_limit_when_exceeded)
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    Settings.Global.putInt(mContext.getContentResolver(), KEY_ACTION, which);
                    updateSummary();
                    dialog.dismiss();
                })
                .show();
    }
}
