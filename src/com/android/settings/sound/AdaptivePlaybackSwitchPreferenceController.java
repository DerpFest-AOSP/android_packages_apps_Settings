/*
 * SPDX-FileCopyrightText: 2020-2022 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.sound;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.MainSwitchPreference;

public class AdaptivePlaybackSwitchPreferenceController extends
        TogglePreferenceController implements LifecycleObserver, OnStart, OnStop {

    private MainSwitchPreference mPreference;
    private @Nullable Preference mTogglePreference;
    private final SettingsObserver mSettingsObserver;

    public AdaptivePlaybackSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED, isChecked ? 1 : 0,
                UserHandle.USER_CURRENT);
        return true;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mTogglePreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mSettingsObserver.observe();
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ADAPTIVE_PLAYBACK = Settings.System.getUriFor(
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED);

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            mContext.getContentResolver().registerContentObserver(ADAPTIVE_PLAYBACK, false, this,
                    UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (ADAPTIVE_PLAYBACK.equals(uri)) {
                if (mTogglePreference != null) {
                    updateState(mTogglePreference);
                }
            }
        }
    }
}
