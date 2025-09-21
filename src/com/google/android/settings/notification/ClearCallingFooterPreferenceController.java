package com.google.android.settings.notification;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

import com.android.settings.R;

public class ClearCallingFooterPreferenceController extends BasePreferenceController {
    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    public ClearCallingFooterPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        FooterPreference footerPreference =
                (FooterPreference) preferenceScreen.findPreference(getPreferenceKey());
        final String string = mContext.getString(R.string.clear_calling_footer_learn_more_link);
        if (footerPreference == null || TextUtils.isEmpty(string)) {
            return;
        }
        footerPreference.setLearnMoreAction(
                new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        mContext.startActivity(
                                HelpUtils.getHelpIntent(mContext, mPreferenceKey, ""));
                    }
                });
    }
}
