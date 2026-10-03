package com.google.android.settings.simplemode;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class SimpleModeTogglePreferenceController extends TogglePreferenceController {
    private static final String KEY_TAG_DIALOG_WITH_MODE = "dialog_with_mode_tag";
    static final String REQUEST_KEY = "simple_view";
    private final boolean mIsSupportedDevice;
    private Fragment mParent;
    private Preference mPreference;

    public SimpleModeTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mIsSupportedDevice = SimpleModeUtils.isSupportedDevice(context);
    }

    public void setFragment(Fragment fragment) {
        mParent = fragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return mIsSupportedDevice ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return SimpleModeUtils.isSimpleViewEnabled(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    private void showDialogWithMode(int mode) {
        Bundle bundle = new Bundle();
        bundle.putInt("dialog_mode", mode);
        SimpleViewDialogFragment simpleViewDialogFragment = new SimpleViewDialogFragment();
        simpleViewDialogFragment.setArguments(bundle);
        FragmentManager parentFragmentManager = mParent.getParentFragmentManager();
        parentFragmentManager.setFragmentResultListener(
                REQUEST_KEY,
                mParent.getViewLifecycleOwner(),
                (requestKey, result) -> updateState(mPreference));
        simpleViewDialogFragment.show(parentFragmentManager, KEY_TAG_DIALOG_WITH_MODE);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            if (SimpleModeUtils.isFontOrDisplaySmaller(mContext)) {
                showDialogWithMode(0);
            } else {
                SimpleModeUtils.enableSimpleView(
                        mContext,
                        SimpleModeUtils.isCallingFromSUWEntryPoint(mParent.requireActivity()));
            }
        } else {
            showDialogWithMode(1);
        }
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
