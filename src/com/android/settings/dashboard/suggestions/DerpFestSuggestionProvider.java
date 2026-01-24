/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.dashboard.suggestions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides DerpFest-specific suggestions for the homepage suggestion card.
 */
public class DerpFestSuggestionProvider {

    /**
     * Represents a DerpFest suggestion with title, summary, icon, and intent.
     */
    public static class DerpFestSuggestion implements Parcelable {
        @NonNull
        public final String id;
        @NonNull
        public final CharSequence title;
        @Nullable
        public final CharSequence summary;
        @Nullable
        public final Icon icon;
        @NonNull
        public final Intent intent;
        public final int flags;

        public DerpFestSuggestion(@NonNull String id, @NonNull CharSequence title,
                @Nullable CharSequence summary, @Nullable Icon icon, @NonNull Intent intent,
                int flags) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.icon = icon;
            this.intent = intent;
            this.flags = flags;
        }

        // Parcelable implementation
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id);
            dest.writeCharSequence(title);
            dest.writeCharSequence(summary);
            dest.writeParcelable(icon, flags);
            dest.writeParcelable(intent, flags);
            dest.writeInt(this.flags);
        }

        public static final Creator<DerpFestSuggestion> CREATOR = new Creator<DerpFestSuggestion>() {
            @Override
            public DerpFestSuggestion createFromParcel(Parcel in) {
                return new DerpFestSuggestion(
                        in.readString(),
                        in.readCharSequence(),
                        in.readCharSequence(),
                        in.readParcelable(Icon.class.getClassLoader()),
                        in.readParcelable(Intent.class.getClassLoader()),
                        in.readInt()
                );
            }

            @Override
            public DerpFestSuggestion[] newArray(int size) {
                return new DerpFestSuggestion[size];
            }
        };
    }

    private static final int FLAG_IS_DISMISSIBLE = 1 << 2;

    /**
     * Gets a list of available DerpFest suggestions.
     * Only returns suggestions for features that are available on the device.
     * Filters out dismissed suggestions.
     */
    @NonNull
    public static List<DerpFestSuggestion> getSuggestions(@NonNull Context context) {
        // Get dismissed suggestions
        SharedPreferences dismissedPrefs = context.getSharedPreferences(
                "derpfest_suggestions_dismissed", Context.MODE_PRIVATE);

        List<DerpFestSuggestion> suggestions = new ArrayList<>();

        // Add GameSpace suggestion
        if (!dismissedPrefs.getBoolean("derpfest_gamespace", false)) {
            Intent gamespaceIntent = createGamespaceIntent(context);
            if (gamespaceIntent != null) {
                suggestions.add(new DerpFestSuggestion(
                        "derpfest_gamespace",
                        context.getString(R.string.gamespace_title),
                        context.getString(R.string.derpfest_suggestion_gamespace_summary),
                        Icon.createWithResource(context, R.drawable.ic_derpfest_suggestions_logo),
                        gamespaceIntent,
                        FLAG_IS_DISMISSIBLE
                ));
            }
        }

        // Add DerpFest Customizations suggestion
        if (!dismissedPrefs.getBoolean("derpfest_customizations", false)) {
            Intent customizationsIntent = createCustomizationsIntent(context);
            if (customizationsIntent != null) {
                String customizationsTitle = getStringFromPackage(context,
                        "org.derpfest.customizations", "derpfest_customizations_title");
                if (customizationsTitle == null) {
                    customizationsTitle = context.getString(R.string.derpfest_suggestion_customizations_title);
                }
                suggestions.add(new DerpFestSuggestion(
                        "derpfest_customizations",
                        customizationsTitle,
                        context.getString(R.string.derpfest_suggestion_customizations_summary),
                        Icon.createWithResource(context, R.drawable.ic_derpfest_suggestions_logo),
                        customizationsIntent,
                        FLAG_IS_DISMISSIBLE
                ));
            }
        }

        // Add Spoofing suggestion
        if (!dismissedPrefs.getBoolean("derpfest_spoofing", false)) {
            Intent spoofingIntent = createSpoofingIntent(context);
            if (spoofingIntent != null) {
                String spoofingTitle = getStringFromPackage(context,
                        "org.derpfest.customizations", "spoofing_title");
                if (spoofingTitle == null) {
                    spoofingTitle = "Spoofing"; // Fallback
                }
                suggestions.add(new DerpFestSuggestion(
                        "derpfest_spoofing",
                        spoofingTitle,
                        context.getString(R.string.derpfest_suggestion_spoofing_summary),
                        Icon.createWithResource(context, R.drawable.ic_derpfest_suggestions_logo),
                        spoofingIntent,
                        FLAG_IS_DISMISSIBLE
                ));
            }
        }

        // Add Button Settings suggestion
        if (!dismissedPrefs.getBoolean("derpfest_button_settings", false)) {
            Intent buttonSettingsIntent = createButtonSettingsIntent(context);
            if (buttonSettingsIntent != null) {
                suggestions.add(new DerpFestSuggestion(
                        "derpfest_button_settings",
                        context.getString(R.string.button_settings_title),
                        context.getString(R.string.derpfest_suggestion_button_settings_summary),
                        Icon.createWithResource(context, R.drawable.ic_derpfest_suggestions_logo),
                        buttonSettingsIntent,
                        FLAG_IS_DISMISSIBLE
                ));
            }
        }

        return suggestions;
    }

    private static Intent createGamespaceIntent(Context context) {
        try {
            Intent intent = new Intent();
            ComponentName component = new ComponentName("io.chaldeaprjkt.gamespace",
                    "io.chaldeaprjkt.gamespace.settings.SettingsActivity");
            intent.setComponent(component);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                return intent;
            }
        } catch (Exception e) {
            // Activity not available
        }
        return null;
    }

    private static Intent createCustomizationsIntent(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage("org.derpfest.customizations");
            intent.setClassName("org.derpfest.customizations",
                    "org.derpfest.customizations.MainActivity");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                return intent;
            }
        } catch (Exception e) {
            // Activity not available
        }
        return null;
    }

    private static Intent createSpoofingIntent(Context context) {
        try {
            Intent intent = new SubSettingLauncher(context)
                    .setDestination("org.derpfest.customizations.fragment.Spoofing")
                    .setTitleRes("org.derpfest.customizations",
                            context.getResources().getIdentifier("spoofing_title", "string",
                                    "org.derpfest.customizations"))
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

    private static Intent createButtonSettingsIntent(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage("org.lineageos.lineageparts");
            intent.setClassName("org.lineageos.lineageparts",
                    "org.lineageos.lineageparts.input.ButtonSettings");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                return intent;
            }
        } catch (Exception e) {
            // Activity not available
        }
        return null;
    }

    private static String getStringFromPackage(Context context, String packageName,
            String stringName) {
        try {
            Context packageContext = context.createPackageContext(packageName, 0);
            int resId = packageContext.getResources().getIdentifier(stringName, "string",
                    packageName);
            if (resId != 0) {
                return packageContext.getString(resId);
            }
        } catch (Exception e) {
            // Package or string not found
        }
        return null;
    }
}
