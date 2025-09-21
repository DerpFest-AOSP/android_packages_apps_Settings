package com.google.android.settings.notification;

import android.app.settings.SettingsEnums;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;

import com.android.settings.R;

public class ClearCallingSettings extends DashboardFragment {
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.clear_calling_settings);

    @Override
    protected String getLogTag() {
        return "ClearCallingSettings";
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CLEAR_CALLING;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.clear_calling_settings;
    }
}
