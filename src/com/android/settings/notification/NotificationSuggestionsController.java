/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.notification;

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
 * Controller for displaying settings suggestions at the bottom of notification settings.
 */
public class NotificationSuggestionsController extends BasePreferenceController {

    private static final String KEY_SUGGESTIONS = "notification_suggestions";
    private SettingsSuggestionsPreference mPreference;

    public NotificationSuggestionsController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    public NotificationSuggestionsController(Context context) {
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

        // Add notifications suggestion (DerpFestCustomizations)
        Intent notificationsIntent = createNotificationsIntent(context);
        if (notificationsIntent != null) {
            // Get notifications title from DerpFestCustomizations package
            String notificationsTitle = getStringFromPackage(context, "org.derpfest.customizations", 
                    "notifications_title");
            if (notificationsTitle == null) {
                notificationsTitle = "Notifications"; // Fallback
            }
            suggestions.add(new SettingsSuggestionsPreference.SuggestionItem(
                notificationsTitle,
                notificationsIntent
            ));
        }

        // Add edge light suggestion
        Intent edgeLightIntent = createEdgeLightIntent(context);
        if (edgeLightIntent != null) {
            // Get edge light title from DerpFestCustomizations package
            String edgeLightTitle = getStringFromPackage(context, "org.derpfest.customizations",
                    "edge_light_title");
            if (edgeLightTitle == null) {
                edgeLightTitle = "Edge light"; // Fallback
            }
            suggestions.add(new SettingsSuggestionsPreference.SuggestionItem(
                edgeLightTitle,
                edgeLightIntent
            ));
        }

        // Add status bar settings suggestion
        Intent statusBarIntent = createStatusBarIntent(context);
        if (statusBarIntent != null) {
            // Get status bar title from LineageParts package
            String statusBarTitle = getStringFromPackage(context, "org.lineageos.lineageparts", 
                    "statusbar_title");
            if (statusBarTitle == null) {
                statusBarTitle = "Status Bar"; // Fallback
            }
            suggestions.add(new SettingsSuggestionsPreference.SuggestionItem(
                statusBarTitle,
                statusBarIntent
            ));
        }

        mPreference.setSuggestions(suggestions);
    }

    private Intent createNotificationsIntent(Context context) {
        try {
            // Use SubSettingLauncher to create an Intent that loads notifications fragment
            Intent intent = new SubSettingLauncher(context)
                    .setDestination("org.derpfest.customizations.fragment.Notifications")
                    .setTitleRes("org.derpfest.customizations", 
                            context.getResources().getIdentifier("notifications_title", "string", 
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

    private Intent createEdgeLightIntent(Context context) {
        try {
            // Use SubSettingLauncher to create an Intent that loads edge light fragment
            Intent intent = new SubSettingLauncher(context)
                    .setDestination("org.derpfest.customizations.fragment.EdgeLightSettings")
                    .setTitleRes("org.derpfest.customizations",
                            context.getResources().getIdentifier("edge_light_title", "string",
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

    private Intent createStatusBarIntent(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage("org.lineageos.lineageparts");
            intent.setClassName("org.lineageos.lineageparts",
                    "org.lineageos.lineageparts.statusbar.StatusBarSettings");
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
