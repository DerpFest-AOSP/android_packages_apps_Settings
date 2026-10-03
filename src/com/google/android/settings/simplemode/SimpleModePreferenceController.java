package com.google.android.settings.simplemode;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class SimpleModePreferenceController extends BasePreferenceController {
    private boolean mIsSupportedDevice;

    public SimpleModePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mIsSupportedDevice = SimpleModeUtils.isSupportedDevice(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mIsSupportedDevice ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        boolean isSimpleViewEnabled = SimpleModeUtils.isSimpleViewEnabled(mContext);
        if (isSimpleViewEnabled) {
            return mContext.getString(R.string.simple_mode_on_summary);
        }
        return mContext.getString(R.string.simple_mode_off_summary);
    }
}
