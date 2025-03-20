/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import androidx.annotation.NonNull;

import com.android.settings.core.TogglePreferenceController;

import lineageos.providers.LineageSettings;

/**
 * Configures behaviour of Taskbar setting.
 */
public class TaskbarSettingsController extends TogglePreferenceController {

    public TaskbarSettingsController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        boolean isTablet = mContext.getResources().getConfiguration().smallestScreenWidthDp >= 600;
        return LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.ENABLE_TASKBAR, isTablet ? 1 : 0)
                == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return LineageSettings.System.putInt(mContext.getContentResolver(),
                LineageSettings.System.ENABLE_TASKBAR, isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        return hasNavigationBar() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }
    
    private static boolean hasNavigationBar() {
        boolean hasNavigationBar = false;
        try {
            IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
            hasNavigationBar = windowManager.hasNavigationBar(Display.DEFAULT_DISPLAY);
        } catch (RemoteException e) {}
        return hasNavigationBar;
    }
}
