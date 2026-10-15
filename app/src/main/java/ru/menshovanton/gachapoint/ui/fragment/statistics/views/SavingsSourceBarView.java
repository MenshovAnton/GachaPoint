package ru.menshovanton.gachapoint.ui.fragment.statistics.views;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ru.menshovanton.gachapoint.R;

public class SavingsSourceBarView extends View {

    private final Paint paintManual = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSubs = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDailies = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTrack = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF rectBounds = new RectF();

    private float manualVal = 0;
    private float subsVal = 0;
    private float dailiesVal = 0;
    private float totalVal = 0;

    private float animationProgress = 1f;
    private ValueAnimator animator;

    public SavingsSourceBarView(Context context) {
        super(context);
        init(context);
    }

    public SavingsSourceBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SavingsSourceBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paintManual.setColor(ContextCompat.getColor(context, R.color.check));
        paintSubs.setColor(ContextCompat.getColor(context, R.color.rare_legendary));
        paintDailies.setColor(ContextCompat.getColor(context, R.color.checked));
        paintTrack.setColor(ContextCompat.getColor(context, R.color.divider_color));
    }

    public void setSourceData(int manual, int subs, int dailies, boolean animate) {
        this.manualVal = manual;
        this.subsVal = subs;
        this.dailiesVal = dailies;
        this.totalVal = manual + subs + dailies;

        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        if (animate && totalVal > 0) {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(850);
            animator.setInterpolator(new android.view.animation.PathInterpolator(0.20f, 0.05f, 0.12f, 1.0f));
            animator.addUpdateListener(anim -> {
                animationProgress = (float) anim.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            animationProgress = 1f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cornerRadius = h / 2f;

        rectBounds.set(0, 0, w, h);

        if (totalVal <= 0) {
            canvas.drawRoundRect(rectBounds, cornerRadius, cornerRadius, paintTrack);
            return;
        }

        canvas.drawRoundRect(rectBounds, cornerRadius, cornerRadius, paintTrack);

        float mWidth = (manualVal / totalVal) * w * animationProgress;
        float sWidth = (subsVal / totalVal) * w * animationProgress;
        float dWidth = (dailiesVal / totalVal) * w * animationProgress;

        int saveCount = canvas.save();
        @SuppressLint("DrawAllocation") android.graphics.Path clipPath = new android.graphics.Path();
        clipPath.addRoundRect(rectBounds, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW);
        canvas.clipPath(clipPath);

        float currentX = 0f;

        if (mWidth > 0) {
            canvas.drawRect(currentX, 0, currentX + mWidth, h, paintManual);
            currentX += mWidth;
        }

        if (sWidth > 0) {
            canvas.drawRect(currentX, 0, currentX + sWidth, h, paintSubs);
            currentX += sWidth;
        }

        if (dWidth > 0) {
            canvas.drawRect(currentX, 0, currentX + dWidth, h, paintDailies);
        }

        canvas.restoreToCount(saveCount);
    }
}
