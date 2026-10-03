package com.google.android.settings.simplemode;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

public class SimpleModeCategoryController extends BasePreferenceController {
    private boolean mIsSupportedDevice;

    public SimpleModeCategoryController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mIsSupportedDevice = SimpleModeUtils.isSupportedDevice(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mIsSupportedDevice ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
