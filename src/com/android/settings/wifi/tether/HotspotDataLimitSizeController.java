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
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.Locale;

public class HotspotDataLimitSizeController extends BasePreferenceController {

    private static final String KEY_LIMIT_BYTES = "hotspot_data_limit_bytes";
    private static final long MB = 1024L * 1024L;
    private static final long DEFAULT_LIMIT_BYTES = 50L * MB;
    private static final float MAX_LIMIT_MB = 1024f * 1024f;

    private Preference mPreference;

    public HotspotDataLimitSizeController(Context context, String key) {
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
        showSizeDialog();
        return true;
    }

    private void updateSummary() {
        if (mPreference == null) {
            return;
        }
        mPreference.setSummary(mContext.getString(
                R.string.one_time_data_limit_size_value, readLimitMb()));
    }

    private float readLimitMb() {
        final long bytes = Settings.Global.getLong(
                mContext.getContentResolver(), KEY_LIMIT_BYTES, DEFAULT_LIMIT_BYTES);
        return Math.max(bytes, 0) / (float) MB;
    }

    private void showSizeDialog() {
        final float currentMb = readLimitMb();
        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(currentMb == Math.rint(currentMb)
                ? String.valueOf((long) currentMb)
                : String.format(Locale.US, "%.1f", currentMb));
        input.setSelection(input.getText().length());
        final int pad = (int) (20 * mContext.getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(mContext)
                .setTitle(R.string.one_time_data_limit_size)
                .setMessage(R.string.one_time_data_limit_size_dialog_message)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final String raw = input.getText().toString().trim().replace(',', '.');
                    try {
                        final float mb = Float.parseFloat(raw);
                        if (mb <= 0f || mb > MAX_LIMIT_MB || Float.isNaN(mb)) {
                            return;
                        }
                        Settings.Global.putLong(mContext.getContentResolver(), KEY_LIMIT_BYTES,
                                (long) (mb * MB));
                        updateSummary();
                    } catch (NumberFormatException ignored) {
                        // Leave the stored limit unchanged.
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
