/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.system;

import android.content.ComponentName;
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
 * Controller for displaying settings suggestions at the bottom of system settings.
 */
public class SystemSuggestionsController extends BasePreferenceController {

    private static final String KEY_SUGGESTIONS = "settings_suggestions";
    private SettingsSuggestionsPreference mPreference;

    public SystemSuggestionsController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    public SystemSuggestionsController(Context context) {
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

        // Add gamespace suggestion
        Intent gamespaceIntent = createGamespaceIntent(context);
        if (gamespaceIntent != null) {
            suggestions.add(new SettingsSuggestionsPreference.SuggestionItem(
                context.getString(R.string.gamespace_title),
                gamespaceIntent
            ));
        }

        // Add spoofing suggestion
        Intent spoofingIntent = createSpoofingIntent(context);
        if (spoofingIntent != null) {
            // Get spoofing title from DerpFestCustomizations package
            String spoofingTitle = getStringFromPackage(context, "org.derpfest.customizations", 
                    "spoofing_title");
            if (spoofingTitle == null) {
                spoofingTitle = "Spoofing"; // Fallback
            }
            suggestions.add(new SettingsSuggestionsPreference.SuggestionItem(
                spoofingTitle,
                spoofingIntent
            ));
        }

        // Add button settings suggestion
        Intent buttonSettingsIntent = createButtonSettingsIntent(context);
        if (buttonSettingsIntent != null) {
            suggestions.add(new SettingsSuggestionsPreference.SuggestionItem(
                context.getString(R.string.button_settings_title),
                buttonSettingsIntent
            ));
        }

        mPreference.setSuggestions(suggestions);
    }

    private Intent createGamespaceIntent(Context context) {
        try {
            Intent intent = new Intent();
            ComponentName component = new ComponentName("io.chaldeaprjkt.gamespace",
                    "io.chaldeaprjkt.gamespace.settings.SettingsActivity");
            intent.setComponent(component);
            // Verify the intent can be resolved
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                return intent;
            }
        } catch (Exception e) {
            // Activity not available
        }
        return null;
    }

    private Intent createSpoofingIntent(Context context) {
        try {
            // Use SubSettingLauncher to create an Intent that loads spoofing fragment
            Intent intent = new SubSettingLauncher(context)
                    .setDestination("org.derpfest.customizations.fragment.Spoofing")
                    .setTitleRes("org.derpfest.customizations", 
                            context.getResources().getIdentifier("spoofing_title", "string", 
                                    "org.derpfest.customizations"))
                    .setSourceMetricsCategory(0)
                    .toIntent();
            // Verify the intent can be resolved
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                return intent;
            }
        } catch (Exception e) {
            // Fragment not available
        }
        return null;
    }

    private Intent createButtonSettingsIntent(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage("org.lineageos.lineageparts");
            intent.setClassName("org.lineageos.lineageparts",
                    "org.lineageos.lineageparts.input.ButtonSettings");
            // Verify the intent can be resolved
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                return intent;
            }
        } catch (Exception e) {
            // Activity not available
        }
        return null;
    }

    private String getStringFromPackage(Context context, String packageName, String stringName) {
        try {
            Context packageContext = context.createPackageContext(packageName, 0);
            int resId = packageContext.getResources().getIdentifier(stringName, "string", packageName);
            if (resId != 0) {
                return packageContext.getString(resId);
            }
        } catch (Exception e) {
            // Package or string not found
        }
        return null;
    }
}
