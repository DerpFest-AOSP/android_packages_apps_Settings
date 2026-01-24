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
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
    private static final String PREFS_NAME = "derpfest_suggestions_dismissed";
    private static final String KEY_LAST_SHOWN = "last_shown";
    private static final String KEY_SHOWN_LAST_LAUNCH = "shown_last_launch";
    private static final String KEY_FORCE_NEXT_SHOW = "force_next_show";
    private static final String ID_WELCOME = "derpfest_welcome";
    private static final float SHOW_PROBABILITY = 0.4f;

    /**
     * Gets a single DerpFest suggestion for this homepage visit, or none.
     * A welcome card is shown until the user dismisses it. Other cards appear
     * only on some launches, never two visits in a row, and dismissed
     * suggestions are never shown again. After the welcome card is dismissed,
     * the next eligible card is forced once so random suggestions start.
     */
    @NonNull
    public static List<DerpFestSuggestion> getSuggestions(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        DerpFestSuggestion welcome = getWelcomeSuggestion(context, prefs);
        if (welcome != null) {
            return Collections.singletonList(welcome);
        }

        List<DerpFestSuggestion> available = getAvailableSuggestions(context);
        if (available.isEmpty()) {
            return Collections.emptyList();
        }

        List<DerpFestSuggestion> remaining = new ArrayList<>();
        for (DerpFestSuggestion suggestion : available) {
            if (!prefs.getBoolean(suggestion.id, false)) {
                remaining.add(suggestion);
            }
        }
        if (remaining.isEmpty() || !shouldShowThisVisit(prefs)) {
            return Collections.emptyList();
        }

        Collections.shuffle(remaining);
        String lastShown = prefs.getString(KEY_LAST_SHOWN, null);
        if (remaining.size() > 1 && remaining.get(0).id.equals(lastShown)) {
            Collections.swap(remaining, 0, remaining.size() - 1);
        }

        DerpFestSuggestion chosen = remaining.get(0);
        prefs.edit()
                .putString(KEY_LAST_SHOWN, chosen.id)
                .putBoolean(KEY_SHOWN_LAST_LAUNCH, true)
                .putBoolean(KEY_FORCE_NEXT_SHOW, false)
                .apply();
        return Collections.singletonList(chosen);
    }

    @Nullable
    private static DerpFestSuggestion getWelcomeSuggestion(@NonNull Context context,
            @NonNull SharedPreferences prefs) {
        if (prefs.getBoolean(ID_WELCOME, false)) {
            return null;
        }
        Intent intent = createWelcomeIntent(context);
        if (intent == null) {
            return null;
        }
        return new DerpFestSuggestion(
                ID_WELCOME,
                context.getString(R.string.derpfest_suggestion_welcome_title),
                context.getString(R.string.derpfest_suggestion_welcome_summary),
                Icon.createWithResource(context, R.drawable.ic_derpfest_suggestions_logo),
                intent,
                FLAG_IS_DISMISSIBLE
        );
    }

    private static Intent createWelcomeIntent(Context context) {
        try {
            Intent intent = new SubSettingLauncher(context)
                    .setDestination("org.derpfest.customizations.DerpFestCustomizations")
                    .setTitleRes(R.string.derpfest_suggestion_customizations_title)
                    .setSourceMetricsCategory(0)
                    .toIntent();
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                return intent;
            }
        } catch (Exception e) {
            // Customizations screen not available
        }
        return createCustomizationsIntent(context);
    }

    private static boolean shouldShowThisVisit(SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_FORCE_NEXT_SHOW, false)) {
            return true;
        }
        boolean shownLastLaunch = prefs.getBoolean(KEY_SHOWN_LAST_LAUNCH, false);
        boolean showNow = !shownLastLaunch && new Random().nextFloat() < SHOW_PROBABILITY;
        prefs.edit().putBoolean(KEY_SHOWN_LAST_LAUNCH, showNow).apply();
        return showNow;
    }

    public static void dismissSuggestion(@NonNull Context context, @NonNull String suggestionId) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(suggestionId, true);
        // After welcome, guarantee the next eligible card can appear.
        if (ID_WELCOME.equals(suggestionId)) {
            editor.putBoolean(KEY_FORCE_NEXT_SHOW, true)
                    .putBoolean(KEY_SHOWN_LAST_LAUNCH, false);
        }
        editor.apply();
    }

    @NonNull
    private static List<DerpFestSuggestion> getAvailableSuggestions(@NonNull Context context) {
        List<DerpFestSuggestion> suggestions = new ArrayList<>();

        Intent gamespaceIntent = createGamespaceIntent(context);
        if (gamespaceIntent != null) {
            suggestions.add(new DerpFestSuggestion(
                    "derpfest_gamespace",
                    context.getString(R.string.derpfest_suggestion_gamespace_title),
                    context.getString(R.string.derpfest_suggestion_gamespace_summary),
                    Icon.createWithResource(context, R.drawable.ic_derpfest_suggestions_logo),
                    gamespaceIntent,
                    FLAG_IS_DISMISSIBLE
            ));
        }

        Intent customizationsIntent = createCustomizationsIntent(context);
        if (customizationsIntent != null) {
            suggestions.add(new DerpFestSuggestion(
                    "derpfest_customizations",
                    context.getString(R.string.derpfest_suggestion_customizations_title),
                    context.getString(R.string.derpfest_suggestion_customizations_summary),
                    Icon.createWithResource(context, R.drawable.ic_derpfest_suggestions_logo),
                    customizationsIntent,
                    FLAG_IS_DISMISSIBLE
            ));
        }

        Intent spoofingIntent = createSpoofingIntent(context);
        if (spoofingIntent != null) {
            suggestions.add(new DerpFestSuggestion(
                    "derpfest_spoofing",
                    context.getString(R.string.derpfest_suggestion_spoofing_title),
                    context.getString(R.string.derpfest_suggestion_spoofing_summary),
                    Icon.createWithResource(context, R.drawable.ic_derpfest_suggestions_logo),
                    spoofingIntent,
                    FLAG_IS_DISMISSIBLE
            ));
        }

        Intent buttonSettingsIntent = createButtonSettingsIntent(context);
        if (buttonSettingsIntent != null) {
            suggestions.add(new DerpFestSuggestion(
                    "derpfest_button_settings",
                    context.getString(R.string.derpfest_suggestion_button_settings_title),
                    context.getString(R.string.derpfest_suggestion_button_settings_summary),
                    Icon.createWithResource(context, R.drawable.ic_derpfest_suggestions_logo),
                    buttonSettingsIntent,
                    FLAG_IS_DISMISSIBLE
            ));
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
            Intent intent = new SubSettingLauncher(context)
                    .setDestination("org.derpfest.customizations.DerpFestCustomizations")
                    .setTitleRes(R.string.derpfest_suggestion_customizations_title)
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

    private static Intent createSpoofingIntent(Context context) {
        try {
            Intent intent = new SubSettingLauncher(context)
                    .setDestination("org.derpfest.customizations.fragment.Spoofing")
                    .setTitleRes(R.string.derpfest_suggestion_spoofing_title)
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
}
