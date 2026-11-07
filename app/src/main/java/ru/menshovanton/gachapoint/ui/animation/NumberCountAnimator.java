package ru.menshovanton.gachapoint.ui.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.text.NumberFormat;

import ru.menshovanton.gachapoint.R;

public final class NumberCountAnimator {

    private static final Interpolator NATURAL_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);

    private NumberCountAnimator() {}

    @FunctionalInterface
    public interface Formatter {
        String format(int value);
    }

    private static class PendingAnimation {
        final int targetValue;
        final Formatter formatter;

        PendingAnimation(int targetValue, Formatter formatter) {
            this.targetValue = targetValue;
            this.formatter = formatter;
        }
    }

    private static class AnimationState {
        ValueAnimator animator;
        Formatter formatter;
        int targetValue;
        int currentValue;

        AnimationState(int targetValue, Formatter formatter) {
            this.targetValue = targetValue;
            this.formatter = formatter;
            this.currentValue = targetValue;
        }
    }

    public static void setImmediate(@Nullable TextView textView, int value, @NonNull Formatter formatter) {
        if (textView == null) return;
        AnimationState state = (AnimationState) textView.getTag(R.id.tag_number_animator);
        if (state != null && state.animator != null && state.animator.isRunning()) {
            state.animator.cancel();
        }
        if (state == null) {
            state = new AnimationState(value, formatter);
            textView.setTag(R.id.tag_number_animator, state);
        } else {
            state.targetValue = value;
            state.currentValue = value;
            state.formatter = formatter;
        }
        textView.setText(formatter.format(value));
    }

    public static void setImmediate(@Nullable TextView textView, int value) {
        setImmediate(textView, value, String::valueOf);
    }

    public static void setImmediateFraction(@Nullable TextView textView, int currentVal, int maxVal) {
        if (textView == null) return;
        setImmediate(textView, currentVal, val -> textView.getContext().getString(R.string.fraction_format, val, maxVal));
    }

    public static void setImmediatePercent(@Nullable TextView textView, int targetPercent) {
        if (textView == null) return;
        setImmediate(textView, targetPercent, val -> textView.getContext().getString(R.string.percent_format, val));
    }

    public static void setImmediateFormatted(@Nullable TextView textView, long targetValue) {
        if (textView == null) return;
        NumberFormat nf = NumberFormat.getIntegerInstance();
        setImmediate(textView, (int) targetValue, nf::format);
    }

    public static void animateProgress(@Nullable ProgressBar progressBar, int targetProgress) {
        if (progressBar == null) return;
        ValueAnimator existing = (ValueAnimator) progressBar.getTag(R.id.tag_progress_animator);
        int currentProgress = progressBar.getProgress();
        if (existing != null && existing.isRunning()) {
            currentProgress = (int) existing.getAnimatedValue();
            existing.cancel();
        }
        if (currentProgress == targetProgress) {
            progressBar.setProgress(targetProgress);
            return;
        }
        long duration = calculateDuration(currentProgress, targetProgress);
        ValueAnimator animator = ValueAnimator.ofInt(currentProgress, targetProgress);
        animator.setDuration(duration);
        animator.setInterpolator(NATURAL_INTERPOLATOR);
        animator.addUpdateListener(anim -> {
            if (!progressBar.isAttachedToWindow()) {
                anim.cancel();
                return;
            }
            progressBar.setProgress((int) anim.getAnimatedValue());
        });
        progressBar.setTag(R.id.tag_progress_animator, animator);
        animator.start();
    }

    public static void animate(@Nullable TextView textView, int targetValue) {
        animate(textView, targetValue, String::valueOf);
    }

    public static void animateFormatted(@Nullable TextView textView, long targetValue) {
        if (textView == null) return;
        NumberFormat nf = NumberFormat.getIntegerInstance();
        animate(textView, (int) targetValue, nf::format);
    }

    public static void animateFraction(@Nullable TextView textView, int currentVal, int maxVal) {
        if (textView == null) return;
        animate(textView, currentVal, val -> textView.getContext().getString(R.string.fraction_format, val, maxVal));
    }

    public static void animatePercent(@Nullable TextView textView, int targetPercent) {
        if (textView == null) return;
        animate(textView, targetPercent, val -> textView.getContext().getString(R.string.percent_format, val));
    }

    public static void animateTemplate(@Nullable TextView textView, int targetValue, @StringRes int stringResId) {
        if (textView == null) return;
        animate(textView, targetValue, val -> textView.getContext().getString(stringResId, val));
    }

    private static long calculateDuration(int start, int target) {
        int delta = Math.abs(target - start);
        if (delta <= 1) {
            return 180L;
        } else if (delta <= 10) {
            return 260L;
        } else if (delta <= 100) {
            return 320L;
        } else {
            return 380L;
        }
    }

    public static void animate(@Nullable TextView textView, int targetValue, @NonNull Formatter formatter) {
        if (textView == null) return;

        if (!textView.isAttachedToWindow()) {
            boolean hasPending = textView.getTag(R.id.tag_number_animator_pending) != null;
            textView.setTag(R.id.tag_number_animator_pending, new PendingAnimation(targetValue, formatter));
            if (!hasPending) {
                textView.post(() -> {
                    Object pendingObj = textView.getTag(R.id.tag_number_animator_pending);
                    textView.setTag(R.id.tag_number_animator_pending, null);
                    if (pendingObj instanceof PendingAnimation) {
                        PendingAnimation pending = (PendingAnimation) pendingObj;
                        animate(textView, pending.targetValue, pending.formatter);
                    }
                });
            }

            AnimationState state = (AnimationState) textView.getTag(R.id.tag_number_animator);
            if (state == null) {
                textView.setText(formatter.format(targetValue));
            }
            return;
        }

        AnimationState state = (AnimationState) textView.getTag(R.id.tag_number_animator);

        if (state != null && state.animator != null && state.animator.isRunning()) {
            if (state.targetValue == targetValue) {
                state.formatter = formatter;
                return;
            }

            int currentVal = (int) state.animator.getAnimatedValue();
            state.animator.cancel();
            state.currentValue = currentVal;
            state.targetValue = targetValue;
            state.formatter = formatter;
            startAnimator(textView, state, currentVal, targetValue);
            return;
        }

        if (state == null) {
            state = new AnimationState(targetValue, formatter);
            state.currentValue = 0;
            textView.setTag(R.id.tag_number_animator, state);
            if (targetValue == 0) {
                textView.setText(formatter.format(0));
                return;
            }
            startAnimator(textView, state, 0, targetValue);
        } else {
            if (state.targetValue == targetValue && state.currentValue == targetValue) {
                textView.setText(formatter.format(targetValue));
                state.formatter = formatter;
                return;
            }
            int startVal = state.currentValue;
            state.targetValue = targetValue;
            state.formatter = formatter;
            startAnimator(textView, state, startVal, targetValue);
        }
    }

    private static void startAnimator(@NonNull TextView textView, @NonNull AnimationState state, int startValue, int targetValue) {
        if (startValue == targetValue) {
            state.currentValue = targetValue;
            textView.setText(state.formatter.format(targetValue));
            return;
        }

        long duration = calculateDuration(startValue, targetValue);
        textView.setText(state.formatter.format(startValue));

        ValueAnimator animator = ValueAnimator.ofInt(startValue, targetValue);
        animator.setDuration(duration);
        animator.setInterpolator(NATURAL_INTERPOLATOR);

        animator.addUpdateListener(animation -> {
            if (!textView.isAttachedToWindow()) {
                animator.cancel();
                return;
            }
            int current = (int) animation.getAnimatedValue();
            state.currentValue = current;
            textView.setText(state.formatter.format(current));
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (textView.isAttachedToWindow()) {
                    state.currentValue = targetValue;
                    textView.setText(state.formatter.format(targetValue));
                }
            }
        });

        state.animator = animator;
        animator.start();
    }
}
