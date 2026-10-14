package ru.menshovanton.gachapoint.ui.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public final class TextAnimator {

    private static final long FADE_OUT_DURATION = 90L;
    private static final long FADE_IN_DURATION = 120L;
    private static final Interpolator FADE_OUT_INTERPOLATOR = new AccelerateInterpolator();
    private static final Interpolator FADE_IN_INTERPOLATOR = new DecelerateInterpolator();

    private TextAnimator() {}

    public static void crossfade(@Nullable TextView textView, @NonNull CharSequence newText) {
        crossfade(textView, newText, null);
    }

    public static void crossfade(@Nullable TextView textView, @StringRes int stringResId) {
        if (textView == null) return;
        crossfade(textView, textView.getContext().getString(stringResId), null);
    }

    public static void crossfade(@Nullable TextView textView, @NonNull CharSequence newText, @Nullable Runnable onMidpoint) {
        if (textView == null) return;

        if (TextUtils.equals(textView.getText(), newText)) {
            if (onMidpoint != null) onMidpoint.run();
            return;
        }

        if (!textView.isAttachedToWindow()) {
            textView.setText(newText);
            if (onMidpoint != null) onMidpoint.run();
            textView.setAlpha(1.0f);
            return;
        }

        textView.animate().cancel();
        textView.animate()
                .alpha(0.0f)
                .setDuration(FADE_OUT_DURATION)
                .setInterpolator(FADE_OUT_INTERPOLATOR)
                .setListener(new AnimatorListenerAdapter() {
                    private boolean isCancelled = false;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        isCancelled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isCancelled || !textView.isAttachedToWindow()) return;
                        textView.setText(newText);
                        if (onMidpoint != null) {
                            onMidpoint.run();
                        }
                        textView.animate()
                                .alpha(1.0f)
                                .setDuration(FADE_IN_DURATION)
                                .setInterpolator(FADE_IN_INTERPOLATOR)
                                .setListener(null)
                                .start();
                    }
                })
                .start();
    }

    public static void crossfadeTextAndColor(@Nullable TextView textView, @NonNull CharSequence newText, @ColorInt int newColor) {
        if (textView == null) return;
        if (TextUtils.equals(textView.getText(), newText) && textView.getCurrentTextColor() == newColor) {
            return;
        }
        crossfade(textView, newText, () -> textView.setTextColor(newColor));
    }

    public static void crossfadeTextAndColor(@Nullable TextView textView, @StringRes int stringResId, @ColorInt int newColor) {
        if (textView == null) return;
        crossfadeTextAndColor(textView, textView.getContext().getString(stringResId), newColor);
    }

    public static void crossfadeImage(@Nullable ImageView imageView, @DrawableRes int newDrawableRes, @Nullable ColorStateList newTint) {
        if (imageView == null) return;

        if (!imageView.isAttachedToWindow()) {
            imageView.setImageResource(newDrawableRes);
            if (newTint != null) imageView.setImageTintList(newTint);
            imageView.setAlpha(1.0f);
            return;
        }

        imageView.animate().cancel();
        imageView.animate()
                .alpha(0.0f)
                .setDuration(FADE_OUT_DURATION)
                .setInterpolator(FADE_OUT_INTERPOLATOR)
                .setListener(new AnimatorListenerAdapter() {
                    private boolean isCancelled = false;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        isCancelled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isCancelled || !imageView.isAttachedToWindow()) return;
                        imageView.setImageResource(newDrawableRes);
                        if (newTint != null) {
                            imageView.setImageTintList(newTint);
                        }
                        imageView.animate()
                                .alpha(1.0f)
                                .setDuration(FADE_IN_DURATION)
                                .setInterpolator(FADE_IN_INTERPOLATOR)
                                .setListener(null)
                                .start();
                    }
                })
                .start();
    }
}
