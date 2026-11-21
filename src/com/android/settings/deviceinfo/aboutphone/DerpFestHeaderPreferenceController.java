/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.aboutphone;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.LayoutPreference;

public class DerpFestHeaderPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver {

    private static final String TAG = "DerpFestHeader";
    private static final int CAPTURE_ATTEMPTS = 20;
    private static final long CAPTURE_INTERVAL_MS = 150L;
    private static final String CHANGELOG_ACTIVITY = "org.derpfest.changelog.MainActivity";
    /** Product package first. The suffix is the sideloaded debug build. */
    private static final String[] CHANGELOG_PACKAGES = {
            "org.derpfest.changelog",
            "org.derpfest.changelog.dev",
    };

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final BroadcastReceiver mWallpaperReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshWallpaper();
        }
    };

    private LayoutPreference mPreference;
    private AboutHeaderWallpaper.Layer mWallpaperLayer;
    private ComponentName mChangelogActivity;
    private int mGeneration;
    private boolean mListening;

    public DerpFestHeaderPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        bindMaintainer();
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof LayoutPreference) {
            mPreference = (LayoutPreference) preference;
        }
        bindMaintainer();
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (!mListening) {
            mContext.registerReceiver(mWallpaperReceiver,
                    new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED);
            mListening = true;
        }
        refreshWallpaper();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        stopWallpaperUpdates();
    }

    private void bindMaintainer() {
        if (mPreference == null) {
            return;
        }
        TextView summary = mPreference.findViewById(R.id.status_summary);
        if (summary != null) {
            summary.setText(getMaintainerLabel());
        }
        bindChangelog();
    }

    private void bindChangelog() {
        View card = mPreference.findViewById(R.id.about_header_card);
        View chip = mPreference.findViewById(R.id.about_header_changelog);
        mChangelogActivity = findChangelogActivity();
        boolean available = mChangelogActivity != null;
        if (chip != null) {
            chip.setVisibility(available ? View.VISIBLE : View.GONE);
        }
        if (card == null) {
            return;
        }
        card.setClickable(available);
        card.setFocusable(available);
        if (!available) {
            card.setOnClickListener(null);
            card.setContentDescription(null);
            return;
        }
        card.setOnClickListener(v -> openChangelog());
        card.setContentDescription(mContext.getString(
                R.string.derpfest_about_header_content_description,
                getMaintainerLabel(),
                mContext.getString(R.string.derpfest_whats_new)));
    }

    private ComponentName findChangelogActivity() {
        PackageManager packageManager = mContext.getPackageManager();
        for (String packageName : CHANGELOG_PACKAGES) {
            ComponentName component = new ComponentName(packageName, CHANGELOG_ACTIVITY);
            try {
                packageManager.getActivityInfo(component, 0);
                return component;
            } catch (PackageManager.NameNotFoundException exception) {
                // This package is not installed. Try the next one.
            }
        }
        return null;
    }

    private void openChangelog() {
        if (mChangelogActivity == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setComponent(mChangelogActivity);
        Activity activity = AboutHeaderWallpaper.findActivity(mContext);
        if (activity != null) {
            activity.startActivity(intent);
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private String getMaintainerLabel() {
        String maintainer = mContext.getString(R.string.derpfest_maintainer);
        if (TextUtils.isEmpty(maintainer)) {
            maintainer = mContext.getString(R.string.derpfest_maintainer_unofficial);
        }
        return String.format(mContext.getString(R.string.derpfest_summary), maintainer);
    }

    private void refreshWallpaper() {
        final int generation = ++mGeneration;
        closeWallpaperLayer();
        mHandler.removeCallbacksAndMessages(null);
        ThreadUtils.postOnBackgroundThread(() -> {
            AboutHeaderWallpaper.Source source;
            try {
                source = AboutHeaderWallpaper.load(mContext);
            } catch (RuntimeException exception) {
                Log.w(TAG, "Unable to load the header wallpaper", exception);
                source = new AboutHeaderWallpaper.Source(false, null);
            }
            AboutHeaderWallpaper.Source loaded = source;
            ThreadUtils.postOnMainThread(() -> applySource(generation, loaded));
        });
    }

    private void applySource(int generation, AboutHeaderWallpaper.Source source) {
        if (generation != mGeneration) {
            return;
        }
        ImageView wallpaper = wallpaperView();
        if (wallpaper == null) {
            return;
        }
        wallpaper.setImageDrawable(source.drawable);
        applyWallpaperBlur(wallpaper, source.drawable != null);
        if (source.captureLiveFrame) {
            startLiveCapture(generation);
        }
    }

    private void startLiveCapture(int generation) {
        Activity activity = AboutHeaderWallpaper.findActivity(mContext);
        if (activity == null) {
            return;
        }
        mWallpaperLayer = new AboutHeaderWallpaper.Layer(activity.getWindow());
        scheduleCapture(generation, 0);
    }

    private void scheduleCapture(int generation, int attempt) {
        mHandler.postDelayed(() -> {
            if (generation != mGeneration) {
                return;
            }
            ThreadUtils.postOnBackgroundThread(() -> {
                Bitmap bitmap = AboutHeaderWallpaper.captureFrame();
                ThreadUtils.postOnMainThread(() -> onCapturedFrame(generation, attempt, bitmap));
            });
        }, CAPTURE_INTERVAL_MS);
    }

    private void onCapturedFrame(int generation, int attempt, Bitmap bitmap) {
        if (generation != mGeneration) {
            if (bitmap != null) {
                bitmap.recycle();
            }
            return;
        }
        if (bitmap != null) {
            ImageView wallpaper = wallpaperView();
            if (wallpaper != null) {
                wallpaper.setImageDrawable(new BitmapDrawable(mContext.getResources(), bitmap));
                applyWallpaperBlur(wallpaper, true);
            } else {
                bitmap.recycle();
            }
            closeWallpaperLayer();
            return;
        }
        if (attempt + 1 < CAPTURE_ATTEMPTS) {
            scheduleCapture(generation, attempt + 1);
        } else {
            closeWallpaperLayer();
        }
    }

    private void applyWallpaperBlur(ImageView wallpaper, boolean enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        if (!enabled) {
            wallpaper.setRenderEffect(null);
            return;
        }
        boolean night = (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, night ? 8f : 14f,
                mContext.getResources().getDisplayMetrics());
        wallpaper.setRenderEffect(RenderEffect.createBlurEffect(
                radius, radius, Shader.TileMode.CLAMP));
    }

    private ImageView wallpaperView() {
        if (mPreference == null) {
            return null;
        }
        return mPreference.findViewById(R.id.about_header_wallpaper);
    }

    private void stopWallpaperUpdates() {
        mGeneration++;
        mHandler.removeCallbacksAndMessages(null);
        closeWallpaperLayer();
        if (mListening) {
            mContext.unregisterReceiver(mWallpaperReceiver);
            mListening = false;
        }
    }

    private void closeWallpaperLayer() {
        if (mWallpaperLayer != null) {
            mWallpaperLayer.close();
            mWallpaperLayer = null;
        }
    }
}
