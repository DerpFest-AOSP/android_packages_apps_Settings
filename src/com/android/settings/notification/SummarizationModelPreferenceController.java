/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.android.settings.core.BasePreferenceController;

/** Shows the on-device summary model entry only when DerpSummarizer is installed. */
public class SummarizationModelPreferenceController extends BasePreferenceController {

    private static final String ACTION_MODEL_SETTINGS =
            "org.derpfest.summarizer.action.MODEL_SETTINGS";
    private static final String SUMMARIZER_PACKAGE = "org.derpfest.summarizer";

    public SummarizationModelPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        Intent intent = new Intent(ACTION_MODEL_SETTINGS).setPackage(SUMMARIZER_PACKAGE);
        return mContext.getPackageManager().resolveActivity(intent, 0) != null
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
