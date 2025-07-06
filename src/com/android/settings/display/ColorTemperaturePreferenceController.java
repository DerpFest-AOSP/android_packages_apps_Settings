/*
 * SPDX-FileCopyrightText: 2018 The Android Open Source Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.SliceData;

import org.derpfest.support.preferences.ProperSeekBarPreference;

public class ColorTemperaturePreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private final ColorDisplayManager mColorDisplayManager;

    public ColorTemperaturePreferenceController(Context context, String key) {
        super(context, key);
        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isColorTransformAccelerated(mContext) ?
                AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return mColorDisplayManager.setColorTemperature((int) newValue);
    }

    @Override
    public void updateState(Preference preference) {
        ((ProperSeekBarPreference) preference).setValue(mColorDisplayManager.getColorTemperature());
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }
}
