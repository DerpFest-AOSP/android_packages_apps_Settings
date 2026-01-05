/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * A preference widget that displays settings suggestions in a rounded card at the bottom
 * of settings screens, similar to Samsung OneUI and Nothing OS.
 */
public class SettingsSuggestionsPreference extends Preference {

    private String mTitle;
    private List<SuggestionItem> mSuggestions = new ArrayList<>();

    public SettingsSuggestionsPreference(Context context) {
        super(context);
        init();
    }

    public SettingsSuggestionsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SettingsSuggestionsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.settings_suggestions_preference);
        setSelectable(false);
    }

    public void setTitle(String title) {
        mTitle = title;
        notifyChanged();
    }

    public void setSuggestions(List<SuggestionItem> suggestions) {
        mSuggestions = suggestions != null ? suggestions : new ArrayList<>();
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        
        View rootView = holder.itemView;
        Context context = getContext();
        
        // The root view IS the MaterialCardView (it's the root of our layout)
        MaterialCardView cardView = (MaterialCardView) rootView;
        
        // Get padding values for alignment
        int paddingStart = getPaddingStart(context);
        int paddingEnd = getPaddingEnd(context);
        
        // Get corner radius
        float cornerRadius = context.getResources().getDimension(R.dimen.derpfest_card_radius);
        
        // Apply card styling
        cardView.setCardElevation(0f);
        cardView.setMaxCardElevation(0f);
        cardView.setUseCompatPadding(false);
        cardView.setPreventCornerOverlap(false);
        cardView.setRadius(cornerRadius);
        cardView.setStrokeWidth(0);
        
        // Set background drawable
        cardView.setBackgroundResource(R.drawable.settings_suggestions_card_background);
        
        // Get background color for card background color (theme-aware)
        TypedArray bgArray = context.obtainStyledAttributes(new int[]{android.R.attr.colorBackground});
        int backgroundColor = bgArray.getColor(0, 0);
        bgArray.recycle();
        cardView.setCardBackgroundColor(backgroundColor);
        
        // Set content padding to 0 since we handle padding on inner LinearLayouts
        cardView.setContentPadding(0, 0, 0, 0);
        
        // Ensure proper margins
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cardView.getLayoutParams();
        if (params != null) {
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                params.setMarginStart(0);
                params.setMarginEnd(0);
            } else {
                params.setMarginStart(paddingStart);
                params.setMarginEnd(paddingEnd);
            }
            cardView.setLayoutParams(params);
        }
        
        // Find the suggestions container by ID
        LinearLayout suggestionsContainer = rootView.findViewById(R.id.suggestions_container);
        
        ImageView logoView = rootView.findViewById(R.id.suggestions_logo);
        TextView titleView = rootView.findViewById(R.id.suggestions_title);

        if (logoView != null) {
            logoView.setVisibility(View.VISIBLE);
            // Set tint to match title text color
            TypedArray titleColorArray = context.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
            int titleColor = titleColorArray.getColor(0, 0);
            titleColorArray.recycle();
            logoView.setColorFilter(titleColor);
        }

        if (titleView != null) {
            if (mTitle != null && !mTitle.isEmpty()) {
                titleView.setText(mTitle);
                titleView.setVisibility(View.VISIBLE);
                
                // Align logo vertically with the center of the title
                if (logoView != null) {
                    titleView.post(() -> {
                        int titleHeight = titleView.getHeight();
                        int logoHeight = logoView.getHeight();
                        if (titleHeight > 0 && logoHeight > 0) {
                            // Calculate margin top to center logo with title
                            // Title starts at top, so we need to shift logo down by half the difference
                            int marginTop = (titleHeight - logoHeight) / 2;
                            ViewGroup.MarginLayoutParams logoParams = 
                                    (ViewGroup.MarginLayoutParams) logoView.getLayoutParams();
                            if (logoParams != null) {
                                logoParams.topMargin = Math.max(0, marginTop);
                                logoView.setLayoutParams(logoParams);
                            }
                        }
                    });
                }
            } else {
                titleView.setVisibility(View.GONE);
            }
        }

                if (suggestionsContainer != null) {
                    suggestionsContainer.removeAllViews();

                    if (mSuggestions != null && !mSuggestions.isEmpty()) {
                        int suggestionCount = mSuggestions.size();
                        for (int i = 0; i < suggestionCount; i++) {
                            SuggestionItem suggestion = mSuggestions.get(i);
                            View suggestionView = createSuggestionView(suggestion, i == suggestionCount - 1);
                            suggestionsContainer.addView(suggestionView);
                        }
                        suggestionsContainer.setVisibility(View.VISIBLE);
                    } else {
                        suggestionsContainer.setVisibility(View.GONE);
                    }
                }
    }

    private View createSuggestionView(SuggestionItem suggestion, boolean isLast) {
        Context context = getContext();
        TextView textView = new TextView(context);
        textView.setTextAppearance(context, R.style.TextAppearance_SettingsSuggestions_Item);
        textView.setText(suggestion.getTitle());
        // No horizontal padding to align with title text start
        // Add vertical padding for spacing
        int verticalPadding = context.getResources().getDimensionPixelSize(R.dimen.suggestions_item_padding_vertical);
        textView.setPadding(0, verticalPadding, 0, verticalPadding);
        // Remove font padding to ensure proper baseline alignment
        textView.setIncludeFontPadding(false);
        textView.setBackgroundResource(R.drawable.settings_suggestions_item_background);
        
        // Add bottom margin for spacing between items (except last one)
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
            ViewGroup.MarginLayoutParams.MATCH_PARENT,
            ViewGroup.MarginLayoutParams.WRAP_CONTENT
        );
        if (!isLast) {
            params.bottomMargin = verticalPadding;
        }
        textView.setLayoutParams(params);
        
        textView.setOnClickListener(v -> {
            if (suggestion.getIntent() != null) {
                context.startActivity(suggestion.getIntent());
            }
        });
        return textView;
    }

    public static class SuggestionItem {
        private String mTitle;
        private Intent mIntent;

        public SuggestionItem(String title, Intent intent) {
            mTitle = title;
            mIntent = intent;
        }

        public String getTitle() {
            return mTitle;
        }

        public Intent getIntent() {
            return mIntent;
        }
    }

    private int getPaddingStart(Context context) {
        TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.listPreferredItemPaddingStart});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();
        return padding;
    }

    private int getPaddingEnd(Context context) {
        TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.listPreferredItemPaddingEnd});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();
        return padding;
    }
}
