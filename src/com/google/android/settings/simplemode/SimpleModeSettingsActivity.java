package com.google.android.settings.simplemode;

import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;

import com.google.android.setupdesign.util.ThemeHelper;

public class SimpleModeSettingsActivity extends SettingsActivity {
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SimpleModeSettingsFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (SimpleModeUtils.isCallingFromSUWEntryPoint(this)) {
            applySuwTheme();
            findViewById(R.id.content_parent).setFitsSystemWindows(false);
        }
    }

    private void applySuwTheme() {
        if (ThemeHelper.shouldApplyGlifExpressiveStyle(this)) {
            setTheme(R.style.SettingsPreferenceTheme_SetupWizard);
            ThemeHelper.trySetSuwTheme(this);
        } else {
            setTheme(SetupWizardUtils.getTheme(this, getIntent()));
            setTheme(R.style.SettingsPreferenceTheme_SetupWizard);
            ThemeHelper.trySetDynamicColor(this);
        }
    }
}
