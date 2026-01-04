/*
 * Copyright (C) 2020 Wave-OS
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

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.os.SystemProperties;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.deviceinfo.aboutphone.SpecUtils;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.tether.WifiDeviceNameTextValidator;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.LayoutPreference;

public class DerpFestInfoPreferenceController extends AbstractPreferenceController {

    private static final String KEY_DERPFEST_INFO = "about_phone_info_header";
    private static final String KEY_DEVICE_NAME = "device_name_in_card";

    private static final String PROP_DERPFEST_DEVICE = "ro.product.model";
    
    private Fragment mFragment;
    private TextView mDeviceNameView;
    private ValidatedEditTextPreference mDeviceNamePreference;

    public DerpFestInfoPreferenceController(Context context) {
        super(context);
    }

    public void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference derpfestInfoPreference = screen.findPreference(KEY_DERPFEST_INFO);
        final TextView device = (TextView) derpfestInfoPreference.findViewById(R.id.device_summary);
        final TextView storage = (TextView) derpfestInfoPreference.findViewById(R.id.memory_storage_summary);
        final TextView battery = (TextView) derpfestInfoPreference.findViewById(R.id.battery_size_summary);
        final TextView infoScreen = (TextView) derpfestInfoPreference.findViewById(R.id.screen_res_summary);
        
        // Display device name (editable) instead of model
        mDeviceNameView = device;
        updateDeviceName();
        
        // Make device name clickable to edit
        if (mFragment != null && mFragment.getContext() != null && 
            mFragment.getContext().getResources().getBoolean(R.bool.config_show_device_name)) {
            device.setClickable(true);
            device.setFocusable(true);
            device.setOnClickListener(v -> showDeviceNameDialog());
        }
        
        storage.setText(String.valueOf(SpecUtils.getTotalInternalMemorySize()) + "GB ROM + " + String.valueOf(SpecUtils.getTotalRAM()) + "GB RAM");
        battery.setText(SpecUtils.getBatteryCapacity(mContext) + " mAh");
        infoScreen.setText(SpecUtils.getScreenRes(mContext));
    }

    private void updateDeviceName() {
        if (mDeviceNameView == null) {
            return;
        }
        String deviceName = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.DEVICE_NAME);
        if (deviceName == null) {
            deviceName = Build.MODEL;
        }
        mDeviceNameView.setText(deviceName);
    }

    private void showDeviceNameDialog() {
        if (mFragment == null || !(mFragment instanceof AboutDevice)) {
            return;
        }
        
        final AboutDevice host = (AboutDevice) mFragment;
        
        // Create a temporary preference to show the dialog
        if (mDeviceNamePreference == null) {
            mDeviceNamePreference = new ValidatedEditTextPreference(mContext);
            mDeviceNamePreference.setKey(KEY_DEVICE_NAME);
            mDeviceNamePreference.setTitle(R.string.my_device_info_device_name_preference_title);
            mDeviceNamePreference.setValidator(new WifiDeviceNameTextValidator());
            mDeviceNamePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                // Show warning dialog with the new device name
                String newDeviceName = (String) newValue;
                host.showDeviceNameWarningDialog(newDeviceName);
                return true;
            });
        }
        
        // Set current device name
        String currentName = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.DEVICE_NAME);
        if (currentName == null) {
            currentName = Build.MODEL;
        }
        mDeviceNamePreference.setText(currentName);
        mDeviceNamePreference.setSummary(currentName);
        
        // Add preference to screen temporarily so dialog can find it
        // Hide it so it doesn't show up in the UI
        PreferenceScreen screen = host.getPreferenceScreen();
        if (screen != null) {
            Preference existingPref = screen.findPreference(KEY_DEVICE_NAME);
            if (existingPref == null) {
                screen.addPreference(mDeviceNamePreference);
            }
            // Hide the preference so it doesn't appear in the list
            mDeviceNamePreference.setVisible(false);
        }
        
        // Show the dialog
        if (host instanceof com.android.settings.SettingsPreferenceFragment) {
            ((com.android.settings.SettingsPreferenceFragment) host)
                    .onDisplayPreferenceDialog(mDeviceNamePreference);
        }
    }

    public void updateDeviceNameDisplay() {
        updateDeviceName();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DERPFEST_INFO;
    }
}
