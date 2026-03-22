/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import org.derpfest.support.preferences.ProperSeekBarPreference;

public class ColorPictureBrightnessPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private final ColorDisplayManager mColorDisplayManager;

    public ColorPictureBrightnessPreferenceController(Context context, String key) {
        super(context, key);
        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isColorTransformAccelerated(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return mColorDisplayManager.setColorBrightness((int) newValue);
    }

    @Override
    public void updateState(Preference preference) {
        ((ProperSeekBarPreference) preference).setValue(
                mColorDisplayManager.getColorBrightness());
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
