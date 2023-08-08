/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controls availability of {@link Settings.System#SCROLL_FLING_HAPTIC_FEEDBACK}, which is only
 * meaningful when touch haptic feedback is enabled.
 */
public class ScrollFlingHapticFeedbackPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final HapticFeedbackIntensityPreferenceController.HapticFeedbackVibrationPreferenceConfig
            mTouchHapticConfig;
    private final VibrationPreferenceConfig.SettingObserver mTouchHapticObserver;

    public ScrollFlingHapticFeedbackPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mTouchHapticConfig =
                new HapticFeedbackIntensityPreferenceController.HapticFeedbackVibrationPreferenceConfig(
                        context);
        mTouchHapticObserver = new VibrationPreferenceConfig.SettingObserver(mTouchHapticConfig);
    }

    @Override
    public int getAvailabilityStatus() {
        final Vibrator vibrator = mContext.getSystemService(Vibrator.class);
        return vibrator != null && vibrator.hasVibrator()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        mTouchHapticObserver.onDisplayPreference(this, preference);
        updateEnabledState(preference);
    }

    @Override
    public void updateState(Preference preference) {
        updateEnabledState(preference);
    }

    @Override
    public void onStart() {
        mTouchHapticObserver.register(mContext);
    }

    @Override
    public void onStop() {
        mTouchHapticObserver.unregister(mContext);
    }

    private void updateEnabledState(Preference preference) {
        if (preference == null) {
            return;
        }
        final boolean touchHapticEnabled =
                Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.HAPTIC_FEEDBACK_ENABLED, ON) != OFF
                        && mTouchHapticConfig.readIntensity() != Vibrator.VIBRATION_INTENSITY_OFF;
        preference.setEnabled(touchHapticEnabled && mTouchHapticConfig.isPreferenceEnabled());
    }
}
