package com.google.android.settings.notification;

import android.content.Context;
import android.media.AudioManager;

import com.android.settings.core.BasePreferenceController;

import com.android.settings.R;

public class ClearCallingPreferenceController extends BasePreferenceController {
    private static final String CCA_ENABLED_FLAG = "CcaConfig__is_enabled";
    private static final String CCA_STATUS_OFF = "cca_pixel_enabled=false";
    private final AudioManager mAudioManager;

    public ClearCallingPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAudioManager = (AudioManager) context.getSystemService(AudioManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getResources().getBoolean(R.bool.config_clear_calling_enabled)) {
            return AVAILABLE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }
}
