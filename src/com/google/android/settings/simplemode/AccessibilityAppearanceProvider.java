package com.google.android.settings.simplemode;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;

import com.android.settings.R;
import com.google.common.base.Strings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class AccessibilityAppearanceProvider extends ContentProvider {
    private static final String TAG = "AccessibilityAppearanceProvider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        String callingPackage = getCallingPackage();
        Log.d(TAG, "getCallingPackage():" + callingPackage);
        if (!Objects.equals(callingPackage, "com.google.android.pixel.setupwizard")
                && !Objects.equals(callingPackage, "com.android.settings")
                && !Objects.equals(
                        callingPackage, "com.google.android.settings.simplemode.tests.unit")) {
            return null;
        }
        Log.d(TAG, "Method: " + method);
        Bundle bundle = new Bundle();
        switch (method) {
            case "METHOD_SET_ACCESSIBILITY_APPEARANCE_CONFIGS":
                SimpleModeUtils.overrideDataStore(
                        getContext(),
                        extras.getString(
                                "MATCHA_OVERRIDE_PREVIOUS_ACCESSIBILITY_APPEARANCE_SETTINGS"));
                return bundle;
            case "METHOD_GET_PREVIOUS_DEVICES_MATCHA_STATUS":
                String previousDeviceSettings =
                        SimpleModeUtils.getPreviousDeviceSettings(getContext());
                if (Strings.isNullOrEmpty(previousDeviceSettings)) {
                    Log.d(TAG, "no any backup data");
                    return bundle;
                }
                bundle.putBoolean(
                        "MATCHA_IS_PREVIOUSLY_ENABLED",
                        Boolean.valueOf(previousDeviceSettings.split(",")[0]).booleanValue());
                return bundle;
            case "METHOD_GET_PREVIOUS_ACCESSIBILITY_APPEARANCE_CONFIGS":
                boolean isSimpleViewEnabled = SimpleModeUtils.isSimpleViewEnabled(getContext());
                bundle.putString(
                        "MATCHA_BACKUP_FEATURE_SWITCH_STATUS", String.valueOf(isSimpleViewEnabled));
                if (isSimpleViewEnabled) {
                    RxDataStore<Preferences> rxDataStore =
                            new RxPreferenceDataStoreBuilder(
                                            getContext(),
                                            "simple_mode_data_store_data_for_feature_toggle")
                                    .build();
                    Preferences preferences = rxDataStore.data().blockingFirst();
                    int displaySize = preferences.get(SimpleModeUtils.KEY_USER_DISPLAY_SIZE);
                    float fontSize = preferences.get(SimpleModeUtils.KEY_USER_FONT_SIZE);
                    String navMode = preferences.get(SimpleModeUtils.KEY_USER_NAVIGATION_MODE);
                    int navModeConfig =
                            preferences.get(SimpleModeUtils.KEY_USER_NAVIGATION_MODE_CONFIG);
                    int touchTime = preferences.get(SimpleModeUtils.KEY_USER_TOUCH_TIME);
                    String gridOption = preferences.get(SimpleModeUtils.KEY_USER_GRID_OPTION);
                    long initTime = preferences.get(SimpleModeUtils.KEY_INIT_TIME);
                    bundle.putString("MATCHA_BACKUP_DISPLAY_SIZE", String.valueOf(displaySize));
                    bundle.putString("MATCHA_BACKUP_FONT_SIZE", String.valueOf(fontSize));
                    bundle.putString("MATCHA_BACKUP_NAVIGATION_MODE", String.valueOf(navMode));
                    bundle.putString(
                            "MATCHA_BACKUP_NAVIGATION_MODE_CONFIG", String.valueOf(navModeConfig));
                    bundle.putString("MATCHA_BACKUP_TOUCH_TIME", String.valueOf(touchTime));
                    bundle.putString("MATCHA_BACKUP_GRID_OPTION", String.valueOf(gridOption));
                    bundle.putString("MATCHA_BACKUP_INIT_TIME", String.valueOf(initTime));
                    rxDataStore.dispose();
                    rxDataStore.shutdownComplete().blockingAwait();
                }
                return bundle;
            case "METHOD_GET_CURRENT_ACCESSIBILITY_APPEARANCE_CONFIGS":
                bundle.putString(
                        "MATCHA_BACKUP_CURRENT_ACCESSIBILITY_APPEARANCE_CONFIGS",
                        SimpleModeUtils.getCurrentAccessibilityAppearanceConfigs(getContext()));
                return bundle;
            case "METHOD_IS_MATCHA_SUPPORTED":
                boolean isSupportedDevice = SimpleModeUtils.isSupportedDevice(getContext());
                Log.d(TAG, "isSupported:" + isSupportedDevice);
                bundle.putBoolean("MATCHA_IS_FEATURE_SUPPORTED", isSupportedDevice);
                return bundle;
            case "METHOD_RESTORE_ACCESSIBILITY_APPEARANCE_CONFIGS":
                SimpleModeUtils.restoreCurrentAccessibilityAppearance(
                        getContext(),
                        extras.getString(
                                "MATCHA_RESTORE_CURRENT_ACCESSIBILITY_APPEARANCE_SETTINGS"));
                return bundle;
            default:
                return null;
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!SimpleModeUtils.isSimpleViewEnabled(getContext())
                || !Objects.equals(getCallingPackage(), "com.google.android.apps.nexuslauncher")) {
            return null;
        }
        SimpleModeUtils.setWallpaper(getContext());
        InputStream inputStream =
                getContext().getResources().openRawResource(R.raw.simple_mode_launcher_layout);
        try {
            File file = new File(getContext().getCacheDir(), "launcher_layout");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                FileUtils.copy(inputStream, fos);
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Resources.NotFoundException | IOException e) {
            Log.e(TAG, e.toString());
            throw new FileNotFoundException();
        }
    }
}
