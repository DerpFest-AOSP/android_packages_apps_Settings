package com.google.android.settings.simplemode;

import android.app.Activity;
import android.app.WallpaperManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.window.ConfigurationChangeSetting;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import io.reactivex.Single;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class SimpleModeUtils {
    private static final String TAG = "SimpleModeUtils";
    private static final Executor sExecutor = Executors.newSingleThreadExecutor();
    private static final ImmutableList<String> ONE_GRID_OPTION =
            ImmutableList.of("small", "medium", "large", "xl");
    protected static final Preferences.Key<Integer> KEY_USER_DISPLAY_SIZE =
            PreferencesKeys.intKey("user_display_size");
    protected static final Preferences.Key<Float> KEY_USER_FONT_SIZE =
            PreferencesKeys.floatKey("user_font_size");
    protected static final Preferences.Key<String> KEY_USER_NAVIGATION_MODE =
            PreferencesKeys.stringKey("user_navigation_mode");
    protected static final Preferences.Key<Integer> KEY_USER_NAVIGATION_MODE_CONFIG =
            PreferencesKeys.intKey("user_navigation_mode_config");
    protected static final Preferences.Key<Integer> KEY_USER_TOUCH_TIME =
            PreferencesKeys.intKey("user_touch_time");
    protected static final Preferences.Key<String> KEY_USER_GRID_OPTION =
            PreferencesKeys.stringKey("user_grid_option");
    protected static final Preferences.Key<String> KEY_PREVIOUS_DEVICE_SETTINGS =
            PreferencesKeys.stringKey("previous_device_settings");
    protected static final Preferences.Key<Long> KEY_INIT_TIME =
            PreferencesKeys.longKey("init_time");
    private static final Preferences.Key<String> KEY_DEVICE_LAUNCHER_AUTHORITY =
            PreferencesKeys.stringKey("device_launcher_authority");
    private static final long FIVE_MINUTES = Duration.ofMinutes(5).getSeconds();
    private static final long ONE_HOUR = Duration.ofHours(1).getSeconds();
    private static final long ONE_DAY = Duration.ofDays(1).getSeconds();
    private static final long ONE_WEEK = Duration.ofDays(7).getSeconds();

    private static int getSimpleViewDisplaySizeIndex() {
        return 2;
    }

    private static int getSimpleViewNavigationModeConfig() {
        return 0;
    }

    private static boolean isFoldable(Context context) {
        return context.getResources()
                        .getIntArray(com.android.internal.R.array.config_foldedDeviceStates)
                        .length
                > 0;
    }

    private static boolean isTablet() {
        return Arrays.asList(SystemProperties.get("ro.build.characteristics").split(","))
                .contains("tablet");
    }

    private static boolean isDesktop() {
        return Arrays.asList(SystemProperties.get("ro.build.characteristics").split(","))
                .contains("desktop");
    }

    private static int getNavigationModeConfig(Context context) {
        return context.getResources()
                .getInteger(com.android.internal.R.integer.config_navBarInteractionMode);
    }

    private static String getNavigationMode(Context context) {
        if (getNavigationModeConfig(context) == 0) {
            return "com.android.internal.systemui.navbar.threebutton";
        }
        return "com.android.internal.systemui.navbar.gestural";
    }

    private static Uri getUriForGridOption(String path) {
        return new Uri.Builder()
                .scheme("content")
                .authority("com.google.android.apps.nexuslauncher.grid_control")
                .appendPath(path)
                .build();
    }

    private static String getSimpleViewGridOption() {
        return "medium";
    }

    private static String getSimpleViewNavigationMode() {
        return "com.android.internal.systemui.navbar.threebutton";
    }

    private static float getSimpleViewFontSize(Context context) {
        return Float.valueOf(
                        context.getResources()
                                .getStringArray(
                                        com.android.settingslib.R.array.entryvalues_font_size)[2])
                .floatValue();
    }

    private static int getSimpleViewTouchTime(Context context) {
        return Integer.parseInt(
                context.getResources()
                        .getStringArray(R.array.long_press_timeout_selector_values)[1]);
    }

    private static Uri getUriForSetDefaultGrid() {
        return getUriForGridOption("default_grid");
    }

    protected static void setFeatureToggleStatus(Context context, int status) {
        Settings.Secure.putInt(context.getContentResolver(), "matcha_enable", status);
    }

    private static String getGridOption(Context context) {
        String str = "small";
        try {
            Cursor cursor =
                    context.getContentResolver()
                            .query(getUriForGridOption("list_options"), null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    if (Boolean.parseBoolean(
                            cursor.getString(cursor.getColumnIndex("is_default")))) {
                        str = name;
                        break;
                    }
                }
                cursor.close();
            }
            return str;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get list options", e);
            return "small";
        }
    }

    private static ContentValues getGridOptionContentValue(String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", str);
        contentValues.put("enable_apply_button", Boolean.FALSE);
        return contentValues;
    }

    protected static void setSimpleViewConfigsWithoutGridSize(Context context) {
        setupAccessibilityAppearance(
                context,
                getSimpleViewDisplaySizeIndex(),
                getSimpleViewFontSize(context),
                null,
                -1,
                getSimpleViewTouchTime(context),
                null);
    }

    private static void setSimpleViewConfigs(Context context) {
        setupAccessibilityAppearance(
                context,
                getSimpleViewDisplaySizeIndex(),
                getSimpleViewFontSize(context),
                getSimpleViewNavigationMode(),
                getSimpleViewNavigationModeConfig(),
                getSimpleViewTouchTime(context),
                getSimpleViewGridOption());
    }

    protected static String getDataForFeatureToggle(Context context) {
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(
                                context, "simple_mode_data_store_data_for_feature_toggle")
                        .build();
        Preferences preferences = rxDataStore.data().blockingFirst();
        int displaySize = preferences.get(KEY_USER_DISPLAY_SIZE);
        float fontSize = preferences.get(KEY_USER_FONT_SIZE);
        String navMode = preferences.get(KEY_USER_NAVIGATION_MODE);
        int navModeConfig = preferences.get(KEY_USER_NAVIGATION_MODE_CONFIG);
        int touchTime = preferences.get(KEY_USER_TOUCH_TIME);
        String gridOption = preferences.get(KEY_USER_GRID_OPTION);
        long initTime = preferences.get(KEY_INIT_TIME);
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
        return String.valueOf(displaySize)
                + ","
                + String.valueOf(fontSize)
                + ","
                + navMode
                + ","
                + String.valueOf(navModeConfig)
                + ","
                + String.valueOf(touchTime)
                + ","
                + gridOption
                + ","
                + String.valueOf(initTime);
    }

    private static void restoreUserConfigs(Context context, String[] strArr) {
        setupAccessibilityAppearance(
                context,
                Integer.valueOf(strArr[0]).intValue(),
                Float.valueOf(strArr[1]).floatValue(),
                strArr[2],
                Integer.valueOf(strArr[3]).intValue(),
                Integer.valueOf(strArr[4]).intValue(),
                strArr[5]);
    }

    private static void backupDeviceLauncherAuthority(Context context, final String str) {
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(
                                context, "simple_mode_data_store_data_for_feature_toggle")
                        .build();
        rxDataStore
                .updateDataAsync(
                        preferences -> {
                            MutablePreferences mutablePreferences =
                                    preferences.toMutablePreferences();
                            mutablePreferences.set(KEY_DEVICE_LAUNCHER_AUTHORITY, str);
                            return Single.just(mutablePreferences);
                        })
                .blockingGet();
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
    }

    private static void backupUserConfigs(Context context) {
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(
                                context, "simple_mode_data_store_data_for_feature_toggle")
                        .build();
        com.android.settingslib.display.DisplayDensityUtils displayDensityUtils =
                new com.android.settingslib.display.DisplayDensityUtils(context);
        ContentResolver contentResolver = context.getContentResolver();
        String[] fontSizeArray =
                context.getResources()
                        .getStringArray(com.android.settingslib.R.array.entryvalues_font_size);
        String[] touchTimeArray =
                context.getResources().getStringArray(R.array.long_press_timeout_selector_values);
        final int displaySize = displayDensityUtils.getCurrentIndex();
        final float fontSize =
                Settings.System.getFloat(
                        contentResolver,
                        "font_scale",
                        Float.valueOf(fontSizeArray[1]).floatValue());
        final String navMode = getNavigationMode(context);
        final int navModeConfig = getNavigationModeConfig(context);
        final int touchTime =
                Settings.Secure.getInt(
                        contentResolver, "long_press_timeout", Integer.parseInt(touchTimeArray[0]));
        final String gridOption = getGridOption(context);
        final long initTime = System.currentTimeMillis();
        rxDataStore
                .updateDataAsync(
                        preferences -> {
                            MutablePreferences mutablePreferences =
                                    preferences.toMutablePreferences();
                            mutablePreferences.set(
                                    KEY_USER_DISPLAY_SIZE, Integer.valueOf(displaySize));
                            mutablePreferences.set(KEY_USER_FONT_SIZE, Float.valueOf(fontSize));
                            mutablePreferences.set(KEY_USER_NAVIGATION_MODE, navMode);
                            mutablePreferences.set(
                                    KEY_USER_NAVIGATION_MODE_CONFIG,
                                    Integer.valueOf(navModeConfig));
                            mutablePreferences.set(KEY_USER_TOUCH_TIME, Integer.valueOf(touchTime));
                            mutablePreferences.set(KEY_USER_GRID_OPTION, gridOption);
                            mutablePreferences.set(KEY_INIT_TIME, Long.valueOf(initTime));
                            return Single.just(mutablePreferences);
                        })
                .blockingGet();
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
    }

    protected static boolean isSupportedDevice(Context context) {
        return !isFoldable(context) && !isTablet() && !isDesktop();
    }

    protected static void enableSimpleView(final Context context, final boolean isFromSuw) {
        setFeatureToggleStatus(context, 1);
        sExecutor.execute(
                () -> {
                    Trace.beginSection("enableSimpleView");
                    backupUserConfigs(context);
                    setSimpleViewConfigs(context);
                    if (isFromSuw) {
                        setComponentsStatusForSuw(context, false);
                        String string =
                                Settings.Secure.getString(
                                        context.getContentResolver(), "launcher3.layout.provider");
                        if (!"com.android.settings.matcha.accessibilityappearanceprovider"
                                .equals(string)) {
                            backupDeviceLauncherAuthority(context, string);
                        }
                        setLauncherLayout(
                                context,
                                "com.android.settings.matcha.accessibilityappearanceprovider");
                    }
                    Trace.endSection();
                    Log.d(TAG, "enableSimpleViewFromSetupWizard:" + isFromSuw);
                    FeatureFactory.getFeatureFactory()
                            .getMetricsFeatureProvider()
                            .action(
                                    context,
                                    isFromSuw
                                            ? SettingsEnums.ACTION_EASY_MODE_CHANGED_VIA_SETUPWIZARD
                                            : SettingsEnums.ACTION_EASY_MODE_CHANGED,
                                    0);
                });
    }

    protected static void disableSimpleView(final Context context, final boolean isFromSuw) {
        setFeatureToggleStatus(context, 0);
        sExecutor.execute(
                () -> {
                    Trace.beginSection("disableSimpleView");
                    String[] strArrSplit = getDataForFeatureToggle(context).split(",");
                    restoreUserConfigs(context, strArrSplit);
                    long initTime = Long.parseLong(strArrSplit[6]);
                    if (isFromSuw) {
                        setComponentsStatusForSuw(context, true);
                        String string =
                                Settings.Secure.getString(
                                        context.getContentResolver(), "launcher3.layout.provider");
                        RxDataStore<Preferences> rxDataStore =
                                new RxPreferenceDataStoreBuilder(
                                                context,
                                                "simple_mode_data_store_data_for_feature_toggle")
                                        .build();
                        String launcherAuthority =
                                rxDataStore
                                        .data()
                                        .blockingFirst()
                                        .get(KEY_DEVICE_LAUNCHER_AUTHORITY);
                        if ("com.android.settings.matcha.accessibilityappearanceprovider"
                                .equals(string)) {
                            setLauncherLayout(context, launcherAuthority);
                        }
                        rxDataStore.dispose();
                        rxDataStore.shutdownComplete().blockingAwait();
                    }
                    Trace.endSection();
                    Log.d(TAG, "disableSimpleViewFromSetupWizard:" + isFromSuw);
                    FeatureFactory.getFeatureFactory()
                            .getMetricsFeatureProvider()
                            .action(
                                    context,
                                    isFromSuw
                                            ? SettingsEnums.ACTION_EASY_MODE_CHANGED_VIA_SETUPWIZARD
                                            : SettingsEnums.ACTION_EASY_MODE_CHANGED,
                                    isFromSuw ? 1 : getAccumulatedTimeLevel(initTime));
                });
    }

    protected static void setLauncherLayout(Context context, String str) {
        Settings.Secure.putString(context.getContentResolver(), "launcher3.layout.provider", str);
    }

    protected static boolean isFontOrDisplaySmaller(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        return new com.android.settingslib.display.DisplayDensityUtils(context).getCurrentIndex()
                        > getSimpleViewDisplaySizeIndex()
                || Settings.System.getFloat(
                                contentResolver,
                                "font_scale",
                                Float.valueOf(
                                                context.getResources()
                                                        .getStringArray(
                                                                com.android.settingslib.R.array
                                                                        .entryvalues_font_size)[1])
                                        .floatValue())
                        > getSimpleViewFontSize(context);
    }

    protected static boolean isSimpleViewEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "matcha_enable", 0) == 1;
    }

    protected static boolean isCallingFromSUWEntryPoint(Activity activity) {
        if (activity == null || activity.getCallingPackage() == null) {
            return false;
        }
        return activity.getCallingPackage().equals("com.google.android.pixel.setupwizard");
    }

    protected static void setComponentsStatusForSuw(Context context, boolean enabled) {
        setTextReadingPackageComponent(context, enabled);
        setWallpaperSuggestionPackageComponent(context, enabled);
    }

    private static void setTextReadingPackageComponent(Context context, boolean enabled) {
        setComponentEnabledSetting(
                context,
                new ComponentName(
                        "com.android.settings",
                        "com.android.settings.TextReadingForSetupWizardActivity"),
                enabled);
    }

    private static void setWallpaperSuggestionPackageComponent(Context context, boolean enabled) {
        setComponentEnabledSetting(
                context,
                new ComponentName(
                        "com.android.settings",
                        "com.android.settings.wallpaper.WallpaperSuggestionActivity"),
                enabled);
    }

    private static void setComponentEnabledSetting(
            Context context, ComponentName componentName, boolean enabled) {
        context.getPackageManager().setComponentEnabledSetting(componentName, enabled ? 1 : 2, 1);
    }

    protected static void setWallpaper(Context context) {
        try {
            WallpaperManager.getInstance(context)
                    .setResource(com.android.settings.R.raw.simple_mode_wallpaper);
        } catch (IOException e) {
            Log.e(TAG, "exception: ", e);
        }
    }

    protected static void overrideDataStore(Context context, String str) {
        String[] strArrSplit = str.split(",");
        if (strArrSplit.length == 8 && strArrSplit[0].equals("true")) {
            Log.d(TAG, "overrideDataStore");
            RxDataStore<Preferences> rxDataStore =
                    new RxPreferenceDataStoreBuilder(
                                    context, "simple_mode_data_store_data_for_feature_toggle")
                            .build();
            final int displaySize = Integer.valueOf(strArrSplit[1]).intValue();
            final float fontSize = Float.valueOf(strArrSplit[2]).floatValue();
            final String navMode = strArrSplit[3];
            final int navModeConfig = Integer.valueOf(strArrSplit[4]).intValue();
            final int touchTime = Integer.valueOf(strArrSplit[5]).intValue();
            final String gridOption = strArrSplit[6];
            final long initTime = Long.parseLong(strArrSplit[7]);
            Log.d(TAG, "overrideDataStore-initTimeFromBackup:" + initTime);
            rxDataStore
                    .updateDataAsync(
                            preferences -> {
                                MutablePreferences mutablePreferences =
                                        preferences.toMutablePreferences();
                                mutablePreferences.set(
                                        KEY_USER_DISPLAY_SIZE, Integer.valueOf(displaySize));
                                mutablePreferences.set(KEY_USER_FONT_SIZE, Float.valueOf(fontSize));
                                mutablePreferences.set(KEY_USER_NAVIGATION_MODE, navMode);
                                mutablePreferences.set(
                                        KEY_USER_NAVIGATION_MODE_CONFIG,
                                        Integer.valueOf(navModeConfig));
                                mutablePreferences.set(
                                        KEY_USER_TOUCH_TIME, Integer.valueOf(touchTime));
                                mutablePreferences.set(KEY_USER_GRID_OPTION, gridOption);
                                mutablePreferences.set(KEY_INIT_TIME, Long.valueOf(initTime));
                                return Single.just(mutablePreferences);
                            })
                    .blockingGet();
            rxDataStore.dispose();
            rxDataStore.shutdownComplete().blockingAwait();
        }
    }

    protected static String getCurrentAccessibilityAppearanceConfigs(Context context) {
        com.android.settingslib.display.DisplayDensityUtils displayDensityUtils =
                new com.android.settingslib.display.DisplayDensityUtils(context);
        ContentResolver contentResolver = context.getContentResolver();
        String[] fontSizeArray =
                context.getResources()
                        .getStringArray(com.android.settingslib.R.array.entryvalues_font_size);
        String[] touchTimeArray =
                context.getResources().getStringArray(R.array.long_press_timeout_selector_values);
        boolean isSimpleViewEnabled = isSimpleViewEnabled(context);
        int displaySize = displayDensityUtils.getCurrentIndex();
        float fontSize =
                Settings.System.getFloat(
                        contentResolver,
                        "font_scale",
                        Float.valueOf(fontSizeArray[1]).floatValue());
        String navMode = getNavigationMode(context);
        int navModeConfig = getNavigationModeConfig(context);
        int touchTime =
                Settings.Secure.getInt(
                        contentResolver, "long_press_timeout", Integer.parseInt(touchTimeArray[0]));
        return String.valueOf(isSimpleViewEnabled)
                + ","
                + String.valueOf(displaySize)
                + ","
                + String.valueOf(fontSize)
                + ","
                + navMode
                + ","
                + String.valueOf(navModeConfig)
                + ","
                + String.valueOf(touchTime)
                + ","
                + getGridOption(context);
    }

    protected static void restoreCurrentAccessibilityAppearance(Context context, final String str) {
        if (Strings.isNullOrEmpty(str)) {
            Log.d(TAG, "Skipping restore: no any backup data");
            return;
        }
        String[] strArrSplit = str.split(",");
        if (strArrSplit.length != 7) {
            Log.d(TAG, "Skipping restore: data loss");
            return;
        }
        FeatureFactory.getFeatureFactory()
                .getMetricsFeatureProvider()
                .action(
                        context,
                        SettingsEnums.ACTION_RESTORE_SIMPLE_VIEW_RESULT_IN_SUW,
                        Boolean.parseBoolean(strArrSplit[0]));
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(
                                context, "simple_mode_data_store_data_for_backup_restore")
                        .build();
        rxDataStore
                .updateDataAsync(
                        preferences -> {
                            MutablePreferences mutablePreferences =
                                    preferences.toMutablePreferences();
                            mutablePreferences.set(KEY_PREVIOUS_DEVICE_SETTINGS, str);
                            return Single.just(mutablePreferences);
                        })
                .blockingGet();
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
    }

    protected static void setupAccessibilityAppearance(
            Context context,
            int displaySize,
            float fontScale,
            String navMode,
            int navModeConfig,
            int touchTime,
            String gridOption) {
        com.android.settingslib.display.DisplayDensityUtils displayDensityUtils =
                new com.android.settingslib.display.DisplayDensityUtils(context);
        ContentResolver contentResolver = context.getContentResolver();
        IOverlayManager overlayManager =
                IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay"));
        List<ConfigurationChangeSetting> forcedDisplayDensitySetting =
                displayDensityUtils.getForcedDisplayDensitySetting(displaySize);
        forcedDisplayDensitySetting.add(new ConfigurationChangeSetting.FontScaleSetting(fontScale));
        try {
            WindowManagerGlobal.getWindowManagerService()
                    .setConfigurationChangeSettingsForUser(
                            forcedDisplayDensitySetting, UserHandle.myUserId());
        } catch (RemoteException unused) {
            Log.w(TAG, "Unable to save forced display density and font size settings");
        }
        if (navMode != null && navModeConfig != -1) {
            try {
                overlayManager.setEnabledExclusiveInCategory(navMode, UserHandle.USER_CURRENT);
                Settings.Secure.putInt(contentResolver, "navigation_mode", navModeConfig);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Settings.Secure.putInt(contentResolver, "long_press_timeout", touchTime);
        if (gridOption != null) {
            if (!isOneGridOption(gridOption)) {
                gridOption = remapToOneGridOption(gridOption);
                Log.d(TAG, "remapToOneGridOption: " + gridOption);
            }
            contentResolver.update(
                    getUriForSetDefaultGrid(), getGridOptionContentValue(gridOption), null, null);
        }
    }

    protected static String getPreviousDeviceSettings(Context context) {
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(
                                context, "simple_mode_data_store_data_for_backup_restore")
                        .build();
        String str = rxDataStore.data().blockingFirst().get(KEY_PREVIOUS_DEVICE_SETTINGS);
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
        return str;
    }

    protected static int getAccumulatedTimeLevel(long initTime) {
        long duration = (System.currentTimeMillis() - initTime) / 1000;
        Log.d(TAG, "getAccumulatedTime:" + duration);
        if (duration > ONE_WEEK) {
            return 5;
        }
        if (duration > ONE_DAY) {
            return 4;
        }
        if (duration > ONE_HOUR) {
            return 3;
        }
        return duration > FIVE_MINUTES ? 2 : 1;
    }

    private static boolean isOneGridOption(String str) {
        return ONE_GRID_OPTION.contains(str);
    }

    private static String remapToOneGridOption(String str) {
        switch (str.hashCode()) {
            case -1039745817:
                str.equals("normal");
                return "small";
            case -621369835:
                return str.equals("practical") ? "medium" : "small";
            case 97536:
                return str.equals("big") ? "large" : "small";
            case 723019166:
                return str.equals("reasonable") ? "medium" : "small";
            case 2063491154:
                return str.equals("crazy_big") ? "xl" : "small";
            default:
                return "small";
        }
    }
}
