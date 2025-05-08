/**
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2020 The "Best Improved Cherry Picked Rom" Project
 * Copyright (C) 2020 Project Fluid
 * Copyright (C) 2021 ShapeShiftOS
 * Copyright (C) 2021 NezukoOS
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

import android.os.SystemProperties;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.view.Display;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.graphics.Point;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.android.internal.os.PowerProfile;
import com.android.internal.util.MemInfoReader;

import java.io.File;

import com.android.settings.R;

public class SpecUtils {

    private static final long BYTES_PER_GB = 1024 * 1024 * 1024; // 1GB in bytes
    private static final int[] KNOWN_RAM_SIZES = {1, 2, 3, 4, 6, 8, 10, 12, 16, 32, 48, 64}; // Known RAM sizes in GB

    private static int roundToNearestKnownRamSize(double memoryGB) {
        if (memoryGB <= 0) return 1;
        for (int size : KNOWN_RAM_SIZES) {
            if (memoryGB <= size) return size;
        }
        return KNOWN_RAM_SIZES[KNOWN_RAM_SIZES.length - 1];
    }

    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        double total = (totalBlocks * blockSize) / 1073741824;
        int lastval = (int) Math.round(total);
        if (lastval > 0  && lastval <= 16)
            return "16";
        else if (lastval > 16 && lastval <=32)
            return "32";
        else if (lastval > 32 && lastval <=64)
            return "64";
        else if (lastval > 64 && lastval <=128)
            return "128";
        else if (lastval > 128 && lastval <= 256)
            return "256";
        else if (lastval > 256 && lastval <= 512)
            return "512";
        else if (lastval > 512)
            return "512+";
        else return "null";
    }

    public static int getTotalRAM() {
        try {
            MemInfoReader memReader = new MemInfoReader();
            memReader.readMemInfo();
            double totalMemB = memReader.getTotalSize();
            if (totalMemB <= 0) {
                return 0;
            }
            double totalMemGB = totalMemB / BYTES_PER_GB;
            return roundToNearestKnownRamSize(totalMemGB);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getScreenRes(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y + getNavigationBarHeight(windowManager);
        return screenWidth + " x " + screenHeight;
    }

    private static int getNavigationBarHeight(WindowManager wm) {
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight)
            return realHeight - usableHeight;
        else
            return 0;
    }

    public static int getBatteryCapacity(Context context) {
        // Try BatteryManager first
        Intent batteryIntent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            final int designCapacityUah = batteryIntent.getIntExtra(
                    BatteryManager.EXTRA_DESIGN_CAPACITY, -1);
            if (designCapacityUah > 0) {
                return designCapacityUah / 1000; // Convert from uAh to mAh
            }
        }

        // Fallback to PowerProfile
        try {
            PowerProfile powerProfile = new PowerProfile(context);
            double batteryCapacity = powerProfile.getBatteryCapacity();
            return (int) batteryCapacity;
        } catch (Exception e) {
            return 0;
        }
    }
}
