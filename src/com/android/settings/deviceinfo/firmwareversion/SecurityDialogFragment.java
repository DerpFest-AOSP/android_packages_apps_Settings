/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.settings.R;
import com.android.settingslib.DeviceInfoUtils;

public class SecurityDialogFragment extends DialogFragment {

    public static final String TAG = "SecurityDialogFragment";

    private static final String KEY_AOSP_VENDOR_SECURITY_PATCH =
            "ro.vendor.build.security_patch";

    private static final String KEY_LINEAGE_VENDOR_SECURITY_PATCH =
            "ro.lineage.build.vendor_security_patch";

    public static SecurityDialogFragment newInstance() {
        return new SecurityDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View content = LayoutInflater.from(getContext())
                .inflate(R.layout.security_dialog_layout, null);

        final TextView securityPatchLevelText = content.findViewById(R.id.security_patch_level_text);
        securityPatchLevelText.setText(DeviceInfoUtils.getSecurityPatch());

        final TextView vendorPatchText = content.findViewById(R.id.vendor_patch_level_text);
        final TextView vendorPatchSummary = content.findViewById(R.id.vendor_patch_level_summary);
        if (vendorPatchSummary != null && vendorPatchText != null) {
            String vendorPatchLevel = SystemProperties.get(KEY_AOSP_VENDOR_SECURITY_PATCH);
            if (vendorPatchLevel.isEmpty()) {
                vendorPatchLevel = SystemProperties.get(KEY_LINEAGE_VENDOR_SECURITY_PATCH);
            }
            if (vendorPatchLevel.isEmpty()) {
                vendorPatchLevel = "unknown";
            }
            
            // Format vendor patch level to match Android security patch format
            if (!"unknown".equals(vendorPatchLevel)) {
                try {
                    java.text.SimpleDateFormat template = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    java.util.Date patchLevelDate = template.parse(vendorPatchLevel);
                    String format = DateFormat.getBestDateTimePattern(
                            java.util.Locale.getDefault(), "dMMMMyyyy");
                    vendorPatchLevel = DateFormat.format(format, patchLevelDate).toString();
                } catch (java.text.ParseException e) {
                    // If parsing fails, use the raw string
                }
            }
            
            vendorPatchSummary.setText(vendorPatchLevel);
            if ("unknown".equals(vendorPatchLevel)) {
                vendorPatchText.setVisibility(View.GONE);
                vendorPatchSummary.setVisibility(View.GONE);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        
        // Apply custom background immediately after creation to prevent stuttering
        try {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.security_dialog_corners);
            }
        } catch (Exception e) {
            // Ignore if custom background fails
        }
        
        return dialog;
    }

    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_SETTINGS_HARDWARE_INFO;
    }
} 
