/*
 * Copyright (C) 2020 The ConquerOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.aboutphone;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.view.View;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.DeviceNamePreferenceController;
import com.android.settings.deviceinfo.BuildNumberPreferenceController;
import com.android.settings.deviceinfo.aboutphone.AboutDeviceSecurityPatchLevelPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import androidx.preference.PreferenceScreen;

import android.bluetooth.BluetoothAdapter;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.SpannedString;
import com.android.settings.bluetooth.BluetoothLengthDeviceNameFilter;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class AboutDevice extends DashboardFragment 
        implements DeviceNamePreferenceController.DeviceNamePreferenceHost {

    private static final String LOG_TAG = "DerpFestAboutDevice";

    private BuildNumberPreferenceController mBuildNumberPreferenceController;
    private DerpFestInfoPreferenceController mDerpFestInfoPreferenceController;
    private DeviceNamePreferenceController mDeviceNamePreferenceController;
    private AboutDeviceSecurityPatchLevelPreferenceController mSecurityPatchLevelPreferenceController;
    private String mPendingDeviceName;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.derpfest_about_device;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mDeviceNamePreferenceController != null) {
            mDeviceNamePreferenceController.setHost(this /* parent */);
        }
        mBuildNumberPreferenceController = use(BuildNumberPreferenceController.class);
        mBuildNumberPreferenceController.setHost(this /* parent */);
        mSecurityPatchLevelPreferenceController = use(AboutDeviceSecurityPatchLevelPreferenceController.class);
        if (mSecurityPatchLevelPreferenceController != null) {
            mSecurityPatchLevelPreferenceController.setHost(this /* parent */);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FIRMWARE_VERSION;
    }

    @Override
    public void showDeviceNameWarningDialog(String deviceName) {
        mPendingDeviceName = deviceName;
        DeviceNameWarningDialog.show(this);
    }

    public void onSetDeviceNameConfirm(boolean confirm) {
        if (confirm && mPendingDeviceName != null) {
            // Update device name directly since we don't have a preference
            String deviceName = mPendingDeviceName;
            // Update Settings.Global
            Settings.Global.putString(getContext().getContentResolver(), 
                    Settings.Global.DEVICE_NAME, deviceName);
            // Update Bluetooth name
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                bluetoothAdapter.setName(
                        DeviceNamePreferenceController.getFilteredBluetoothString(deviceName));
            }
            // Update WiFi tether SSID
            WifiManager wifiManager = (WifiManager) getContext().getSystemService(
                    Context.WIFI_SERVICE);
            if (wifiManager != null) {
                SoftApConfiguration config = wifiManager.getSoftApConfiguration();
                wifiManager.setSoftApConfiguration(
                        new SoftApConfiguration.Builder(config).setSsid(deviceName).build());
            }
        }
        // Update device name display in the card
        if (mDerpFestInfoPreferenceController != null) {
            mDerpFestInfoPreferenceController.updateDeviceNameDisplay();
        }
        mPendingDeviceName = null;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        mDerpFestInfoPreferenceController = new DerpFestInfoPreferenceController(context);
        mDerpFestInfoPreferenceController.setFragment(this);
        controllers.add(mDerpFestInfoPreferenceController);
        // Create DeviceNamePreferenceController manually since we removed it from XML
        // Use a dummy key since we won't be using the preference
        mDeviceNamePreferenceController = new DeviceNamePreferenceController(context, "device_name") {
            @Override
            public void displayPreference(PreferenceScreen screen) {
                // Override to prevent crash when preference doesn't exist in screen
                // We handle device name editing in the card instead
                // Only call super if preference exists, otherwise skip
                if (screen.findPreference(getPreferenceKey()) != null) {
                    super.displayPreference(screen);
                }
            }
        };
        controllers.add(mDeviceNamePreferenceController);
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBuildNumberPreferenceController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.derpfest_about_device;
                    result.add(sir);
                    return result;
                }

            };
}
