/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.system;

import android.content.Context;
import android.content.Intent;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.widget.SettingsSuggestionsPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggestions at the bottom of the single/dual shade panels page.
 */
public class ShadePanelsSuggestionsController extends BasePreferenceController {

    public static final String KEY_SUGGESTIONS = "shade_panels_suggestions";
    private SettingsSuggestionsPreference mPreference;

    public ShadePanelsSuggestionsController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    public ShadePanelsSuggestionsController(Context context) {
        super(context, KEY_SUGGESTIONS);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            updateSuggestions();
        }
    }

    private void updateSuggestions() {
        if (mPreference == null) {
            return;
        }

        Context context = mContext;
        mPreference.setTitle(context.getString(R.string.settings_suggestions_title));

        List<SettingsSuggestionsPreference.SuggestionItem> suggestions = new ArrayList<>();
        Intent qsIntent = createQsIntent(context);
        if (qsIntent != null) {
            suggestions.add(new SettingsSuggestionsPreference.SuggestionItem(
                    context.getString(R.string.qs_title),
                    qsIntent
            ));
        }
        mPreference.setVisible(!suggestions.isEmpty());
        mPreference.setSuggestions(suggestions);
    }

    private Intent createQsIntent(Context context) {
        try {
            Intent intent = new SubSettingLauncher(context)
                    .setDestination("org.derpfest.customizations.fragment.QS")
                    .setTitleRes(R.string.qs_title)
                    .setSourceMetricsCategory(0)
                    .toIntent();
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                return intent;
            }
        } catch (Exception e) {
            // Fragment not available
        }
        return null;
    }
}
