/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.aboutphone;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;

import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Loads the image shown behind the About phone logo header.
 *
 * Static wallpapers are read directly. Emoji wallpapers come from their config provider, because
 * the service thumbnail is a single glyph. Other live wallpapers are captured from the wallpaper
 * layer, since that thumbnail is only a preview icon.
 */
public final class AboutHeaderWallpaper {

    private static final String TAG = "AboutHeaderWallpaper";
    private static final String EMOJI_WALLPAPER_PACKAGE = "com.google.android.apps.emojiwallpaper";
    private static final String EMOJI_AUTHORITY = "com.google.android.apps.emojiwallpaper.config";
    private static final String CONFIG_PREVIEW = "wallpaper_config_preview_uri";
    private static final String DESTINATION = "destination";
    private static final int MAX_IMAGE_SIZE = 1024;

    public static final class Source {
        public final boolean captureLiveFrame;
        @Nullable public final Drawable drawable;

        public Source(boolean captureLiveFrame, @Nullable Drawable drawable) {
            this.captureLiveFrame = captureLiveFrame;
            this.drawable = drawable;
        }
    }

    private AboutHeaderWallpaper() {}

    /** Returns the current system wallpaper, or a color stand-in when a live frame is required. */
    public static Source load(Context context) {
        WallpaperManager manager = WallpaperManager.getInstance(context);
        WallpaperInfo info;
        try {
            info = manager.getWallpaperInfo(WallpaperManager.FLAG_SYSTEM);
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to identify the header wallpaper", exception);
            info = null;
        }
        if (info == null) {
            return new Source(false, manager.getDrawable(WallpaperManager.FLAG_SYSTEM));
        }
        int color = Color.TRANSPARENT;
        try {
            if (manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) != null) {
                color = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                        .getPrimaryColor().toArgb();
            }
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to read header wallpaper colors", exception);
        }
        if (EMOJI_WALLPAPER_PACKAGE.equals(info.getPackageName())) {
            Drawable emoji = loadEmojiWallpaper(context);
            return new Source(false, emoji != null ? emoji : new ColorDrawable(color));
        }
        return new Source(true, new ColorDrawable(color));
    }

    /** Captures only the wallpaper layer. Returns null until the engine has produced a frame. */
    @Nullable
    public static Bitmap captureFrame() {
        try {
            if (WindowManagerGlobal.getWindowManagerService() == null) {
                return null;
            }
            return WindowManagerGlobal.getWindowManagerService().screenshotWallpaper();
        } catch (RemoteException | RuntimeException exception) {
            Log.w(TAG, "Unable to capture the live wallpaper layer", exception);
            return null;
        }
    }

    /**
     * Reveals the wallpaper layer while a frame is captured, then restores flags the caller owned.
     */
    public static final class Layer implements AutoCloseable {
        private final Window mWindow;
        private final boolean mAdded;

        public Layer(Window window) {
            mWindow = window;
            boolean alreadyShown = (window.getAttributes().flags & LayoutParams.FLAG_SHOW_WALLPAPER)
                    != 0;
            mAdded = !alreadyShown;
            if (mAdded) {
                window.addFlags(LayoutParams.FLAG_SHOW_WALLPAPER);
            }
        }

        @Override
        public void close() {
            if (mAdded) {
                mWindow.clearFlags(LayoutParams.FLAG_SHOW_WALLPAPER);
            }
        }
    }

    @Nullable
    public static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Nullable
    private static Drawable loadEmojiWallpaper(Context context) {
        try {
            Uri current = Uri.parse("content://" + EMOJI_AUTHORITY + "/currentwallpapers");
            try (Cursor cursor = context.getContentResolver()
                    .query(current, null, null, null, null)) {
                Uri imageUri = emojiWallpaperImageUri(cursor);
                if (imageUri == null) {
                    return null;
                }
                // The Emoji provider has no MIME type, so typed image/* requests cannot open it.
                ImageDecoder.Source source = ImageDecoder.createSource(
                        (Callable<AssetFileDescriptor>) () -> {
                            AssetFileDescriptor descriptor = context.getContentResolver()
                                    .openAssetFileDescriptor(imageUri, "r");
                            if (descriptor == null) {
                                throw new FileNotFoundException(
                                        "No image for the current Emoji wallpaper");
                            }
                            return descriptor;
                        });
                Bitmap bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, unused) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    int width = info.getSize().getWidth();
                    int height = info.getSize().getHeight();
                    int largest = Math.max(width, height);
                    if (largest > MAX_IMAGE_SIZE) {
                        float scale = MAX_IMAGE_SIZE / (float) largest;
                        decoder.setTargetSize(
                                Math.max(1, Math.round(width * scale)),
                                Math.max(1, Math.round(height * scale)));
                    }
                });
                return new BitmapDrawable(context.getResources(), bitmap);
            }
        } catch (IOException | RuntimeException exception) {
            Log.w(TAG, "Unable to read the Emoji wallpaper image", exception);
            return null;
        }
    }

    @Nullable
    static Uri emojiWallpaperImageUri(@Nullable Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        int destination = cursor.getColumnIndex(DESTINATION);
        int preview = cursor.getColumnIndex(CONFIG_PREVIEW);
        if (destination < 0 || preview < 0) {
            return null;
        }
        while (cursor.moveToNext()) {
            if (!"home".equals(cursor.getString(destination)) || cursor.isNull(preview)) {
                continue;
            }
            Uri config = Uri.parse(cursor.getString(preview));
            if (!"content".equals(config.getScheme())
                    || !EMOJI_AUTHORITY.equals(config.getAuthority())) {
                return null;
            }
            String assetId = config.getQueryParameter("config_preview");
            if (assetId == null || assetId.isEmpty()) {
                return null;
            }
            return new Uri.Builder()
                    .scheme("content")
                    .authority(EMOJI_AUTHORITY)
                    .appendQueryParameter("wallpaper_thumbnail", assetId)
                    .build();
        }
        return null;
    }
}
