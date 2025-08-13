/*
 * Copyright (C) 2025 The LineageOS Project
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

package com.android.settings.fuelgauge;

import android.content.Context;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import lineageos.health.HealthInterface;

/**
 * Controller to change and update the fast charging settings
 */
public class HealthFastChargingPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_HEALTH_FAST_CHARGING = "health_fast_charging";
    private static final String TAG = "HealthFastChargingPreferenceController";

    private HealthInterface mHealthInterface;
    private ListPreference mListPreference;

    public HealthFastChargingPreferenceController(Context context) {
        super(context, KEY_HEALTH_FAST_CHARGING);

        mHealthInterface = HealthInterface.getInstance(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        String[] fastChargeEntries = mContext.getResources().getStringArray(
                org.lineageos.platform.internal.R.array.fast_charge_entries);

        int[] supportedFastChargeModes = mHealthInterface.getSupportedFastChargeModes();
        String[] supportedFastChargeModesEntries = new String[supportedFastChargeModes.length];
        String[] supportedFastChargeModesValues = new String[supportedFastChargeModes.length];

        for (int i = 0; i < supportedFastChargeModes.length; i++) {
            supportedFastChargeModesEntries[i] = fastChargeEntries[supportedFastChargeModes[i]];
            supportedFastChargeModesValues[i] = String.valueOf(supportedFastChargeModes[i]);
        }

        mListPreference = screen.findPreference(getPreferenceKey());
        mListPreference.setEntries(supportedFastChargeModesEntries);
        mListPreference.setEntryValues(supportedFastChargeModesValues);
    }

    @Override
    public int getAvailabilityStatus() {
        return mHealthInterface != null && mHealthInterface.isFastChargeSupported()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        String fastChargeMode = String.valueOf(mHealthInterface.getFastChargeMode());
        int index = mListPreference.findIndexOfValue(fastChargeMode);
        if (index < 0) index = 0;
        mListPreference.setValueIndex(index);
        mListPreference.setSummary(mListPreference.getEntries()[index]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return mHealthInterface.setFastChargeMode(Integer.parseInt((String) newValue));
    }
}
