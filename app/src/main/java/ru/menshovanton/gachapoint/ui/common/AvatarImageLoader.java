package ru.menshovanton.gachapoint.ui.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import ru.menshovanton.gachapoint.R;

public final class AvatarImageLoader {

    private AvatarImageLoader() {}

    /**
     * Safely loads an avatar icon using Glide with graceful fallback to an uppercase letter badge.
     * Prevents empty/blank cards if image loading fails, is offline, or is missing.
     */
    public static void loadAvatar(
            @NonNull Context context,
            @Nullable String iconUrl,
            @NonNull String displayName,
            @Nullable ImageView ivAvatar,
            @Nullable TextView tvLetter
    ) {
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return;
            }
        }

        String cleanName = displayName.trim();
        String letter = !cleanName.isEmpty() ? String.valueOf(cleanName.charAt(0)).toUpperCase() : "•";

        if (tvLetter != null) {
            tvLetter.setText(letter);
            try {
                tvLetter.setTypeface(ResourcesCompat.getFont(context, R.font.genshin_font));
            } catch (Exception ignored) {}
        }

        if (ivAvatar == null) {
            if (tvLetter != null) {
                tvLetter.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (iconUrl == null || iconUrl.trim().isEmpty()) {
            Glide.with(context).clear(ivAvatar);
            ivAvatar.setImageDrawable(null);
            ivAvatar.setVisibility(View.GONE);
            if (tvLetter != null) {
                tvLetter.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Initially show letter badge while image is downloading or caching
        if (tvLetter != null) {
            tvLetter.setVisibility(View.VISIBLE);
        }
        ivAvatar.setVisibility(View.INVISIBLE);

        // Directly load URL with persistent disk caching
        String targetUrl = iconUrl.trim();

        Glide.with(context)
                .load(targetUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .format(com.bumptech.glide.load.DecodeFormat.PREFER_ARGB_8888)
                .override(256, 256)
                .circleCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(
                            @Nullable GlideException e,
                            Object model,
                            Target<Drawable> target,
                            boolean isFirstResource
                    ) {
                        ivAvatar.setImageDrawable(null);
                        ivAvatar.setVisibility(View.GONE);
                        if (tvLetter != null) {
                            tvLetter.setVisibility(View.VISIBLE);
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            Drawable resource,
                            Object model,
                            Target<Drawable> target,
                            DataSource dataSource,
                            boolean isFirstResource
                    ) {
                        ivAvatar.setVisibility(View.VISIBLE);
                        if (tvLetter != null) {
                            tvLetter.setVisibility(View.GONE);
                        }
                        return false;
                    }
                })
                .into(ivAvatar);
    }
}
