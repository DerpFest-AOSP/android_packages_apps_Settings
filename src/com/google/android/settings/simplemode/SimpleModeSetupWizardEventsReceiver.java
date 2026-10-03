package com.google.android.settings.simplemode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.common.base.Strings;

import java.util.Objects;

public final class SimpleModeSetupWizardEventsReceiver extends BroadcastReceiver {
    private static final String TAG = "SetupWizardEventsReceiver";

    private static int getRestoreOption(boolean isPreviouslyEnabled, boolean isSimpleViewEnabled) {
        if (isPreviouslyEnabled) {
            return 1;
        }
        return isSimpleViewEnabled ? 2 : 0;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(
                intent.getAction(), "com.google.android.setupwizard.SETUP_WIZARD_FINISHED")) {
            String previousDeviceSettings = SimpleModeUtils.getPreviousDeviceSettings(context);
            if (Strings.isNullOrEmpty(previousDeviceSettings)) {
                Log.d(TAG, "Skipping restore: no any backup data");
                return;
            }
            String[] strArrSplit = previousDeviceSettings.split(",");
            if (strArrSplit.length != 7) {
                Log.d(TAG, "Skipping restore: data loss");
                return;
            }
            boolean isSimpleViewEnabled = SimpleModeUtils.isSimpleViewEnabled(context);
            boolean isPreviouslyEnabled = Boolean.valueOf(strArrSplit[0]).booleanValue();
            int displaySize = Integer.valueOf(strArrSplit[1]).intValue();
            float fontSize = Float.valueOf(strArrSplit[2]).floatValue();
            String navMode = strArrSplit[3];
            int touchTime = Integer.valueOf(strArrSplit[5]).intValue();
            String gridOption = strArrSplit[6];
            int restoreOption = getRestoreOption(isPreviouslyEnabled, isSimpleViewEnabled);
            Log.d(TAG, "restoreOption: " + restoreOption);
            if (restoreOption == 1) {
                SimpleModeUtils.setFeatureToggleStatus(context, 1);
                SimpleModeUtils.setupAccessibilityAppearance(
                        context, displaySize, fontSize, null, -1, touchTime, gridOption);
            } else if (restoreOption == 2) {
                SimpleModeUtils.setSimpleViewConfigsWithoutGridSize(context);
            }
        }
    }
}
