/*
 * SPDX-FileCopyrightText: 2020-2021 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.sound;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;

public class AdaptivePlaybackSoundSettings extends DashboardFragment {

    private static final String TAG = "AdaptivePlaybackSoundSettings";

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.adaptive_playback_sound_settings;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.adaptive_playback_sound_settings);
}
