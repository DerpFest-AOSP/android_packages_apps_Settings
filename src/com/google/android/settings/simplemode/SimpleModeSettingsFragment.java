package com.google.android.settings.simplemode;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilitySetupWizardUtils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.LottieColorUtils;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.widget.TopIntroPreference;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.util.DelightHelper;
import com.google.android.setupdesign.GlifPreferenceLayout;
import com.google.android.setupdesign.template.IconMixin;
import com.google.android.setupdesign.util.ThemeHelper;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class SimpleModeSettingsFragment extends DashboardFragment {
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.simple_mode_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return SimpleModeUtils.isSupportedDevice(context);
                }
            };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_EASY_PRESET;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.simple_mode_settings;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        TopIntroPreference topIntroPreference =
                (TopIntroPreference) findPreference("simple_mode_top_description");
        if (topIntroPreference != null) {
            if (SimpleModeUtils.isCallingFromSUWEntryPoint(getActivity())) {
                topIntroPreference.setTitle(
                        getContext().getString(R.string.simple_mode_suw_description));
            } else {
                topIntroPreference.setHyperlinkListener(v -> openLearnMorePage());
            }
        }
        IllustrationPreference illustrationPreference =
                (IllustrationPreference) findPreference("simple_mode_preview");
        if (illustrationPreference != null) {
            if (shouldEnableExpressiveStyleInSuw() || shouldEnableExpressiveStyleInSettings()) {
                illustrationPreference.applyDynamicColor();
                illustrationPreference.setLottieAnimationResId(R.raw.simple_mode_preview);
                illustrationPreference.setOnBindListener(
                        this::handleIllustrationAnimationForSetupWizard);
            } else {
                illustrationPreference.setLottieAnimationResId(
                        R.raw.simple_mode_pre_expressive_preview);
            }
        }
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

    private boolean shouldEnableExpressiveStyleInSuw() {
        return SimpleModeUtils.isCallingFromSUWEntryPoint(getActivity())
                && ThemeHelper.shouldApplyGlifExpressiveStyle(getContext());
    }

    private boolean shouldEnableExpressiveStyleInSettings() {
        return !SimpleModeUtils.isCallingFromSUWEntryPoint(getActivity())
                && SettingsThemeHelper.isExpressiveTheme(getContext());
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        if (view instanceof GlifPreferenceLayout) {
            GlifPreferenceLayout glifPreferenceLayout = (GlifPreferenceLayout) view;
            String string = getContext().getString(R.string.simple_mode_title);
            Drawable drawable;
            if (ThemeHelper.shouldApplyGlifExpressiveStyle(getContext())) {
                drawable = getContext().getDrawable(R.drawable.ic_expressive_simplemode_suw);
            } else {
                drawable = getContext().getDrawable(R.drawable.ic_simplemode_suw);
            }
            AccessibilitySetupWizardUtils.setPrimaryButton(
                    getContext(),
                    (FooterBarMixin) glifPreferenceLayout.getMixin(FooterBarMixin.class),
                    R.string.simple_mode_suw_done_button,
                    () -> {
                        setResult(0);
                        finish();
                    });
            AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(
                    getContext(), glifPreferenceLayout, string, null, drawable);
            if (DelightHelper.shouldApplyAnimatedIcon(getContext())) {
                IconMixin iconMixin = (IconMixin) glifPreferenceLayout.getMixin(IconMixin.class);
                iconMixin.setAnimatedIcon(R.raw.ic_animated_simplemode);
                iconMixin.setAnimatedIconDelayed(false);
            }
        }
    }

    private void openLearnMorePage() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(Uri.parse("https://support.google.com/pixelphone?p=pixel_simple_view"));
        startActivity(intent);
    }

    @Override
    public RecyclerView onCreateRecyclerView(
            LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        if (viewGroup instanceof GlifPreferenceLayout) {
            return ((GlifPreferenceLayout) viewGroup)
                    .onCreateRecyclerView(layoutInflater, viewGroup, bundle);
        }
        return super.onCreateRecyclerView(layoutInflater, viewGroup, bundle);
    }

    @Override
    protected String getLogTag() {
        return "SimpleModeSettingsFragment";
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((SimpleModeTogglePreferenceController) use(SimpleModeTogglePreferenceController.class))
                .setFragment(this);
    }

    @Override
    public String getPreferenceScreenBindingKey(Context context) {
        return "simple_mode_settings";
    }

    private void handleIllustrationAnimationForSetupWizard(
            LottieAnimationView lottieAnimationView) {
        Context context = lottieAnimationView.getContext();
        if (DelightHelper.shouldApplyAnimatedIcon(context)
                && SimpleModeUtils.isCallingFromSUWEntryPoint(getActivity())) {
            lottieAnimationView.cancelAnimation();
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                LottieColorUtils.applyMaterialColor(context, lottieAnimationView);
            }
            lottieAnimationView.postDelayed(
                    lottieAnimationView::playAnimation,
                    context.getResources()
                            .getInteger(
                                    com.google.android.setupdesign.R.integer
                                            .sud_lottie_animation_delay_ms));
        }
    }
}
