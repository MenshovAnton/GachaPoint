package ru.menshovanton.gachapoint.ui.fragment.statistics.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

import ru.menshovanton.gachapoint.R;

public class BannerDistributionBarView extends LinearLayout {

    public static class BannerItem {
        public final String title;
        public final int count;
        public final float percentage;

        public BannerItem(String title, int count, float percentage) {
            this.title = title;
            this.count = count;
            this.percentage = percentage;
        }
    }

    public BannerDistributionBarView(Context context) {
        super(context);
        init();
    }

    public BannerDistributionBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BannerDistributionBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    public void setBannersData(List<BannerItem> items) {
        removeAllViews();
        if (items == null || items.isEmpty()) {
            return;
        }

        Context context = getContext();
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < items.size(); i++) {
            BannerItem item = items.get(i);

            LinearLayout headerRow = new LinearLayout(context);
            headerRow.setOrientation(HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            LayoutParams headerLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                headerLp.topMargin = (int) (14 * density);
            }
            headerRow.setLayoutParams(headerLp);

            android.graphics.Typeface genshinFont = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.genshin_font);

            TextView tvTitle = new TextView(context);
            LayoutParams titleLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            tvTitle.setLayoutParams(titleLp);
            tvTitle.setText(item.title);
            tvTitle.setTextSize(13f);
            tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text));
            if (genshinFont != null) {
                tvTitle.setTypeface(genshinFont, android.graphics.Typeface.BOLD);
            } else {
                tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
            }
            headerRow.addView(tvTitle);

            TextView tvCount = new TextView(context);
            tvCount.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            tvCount.setTextSize(12f);
            tvCount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            if (genshinFont != null) {
                tvCount.setTypeface(genshinFont);
            }
            ru.menshovanton.gachapoint.ui.animation.NumberCountAnimator.animate(tvCount, item.count, countVal -> {
                float curPct = (item.count > 0 && item.percentage > 0) ? (countVal * item.percentage / item.count) : 0f;
                return String.format(Locale.getDefault(), "%d (%.1f%%)", countVal, curPct);
            });
            headerRow.addView(tvCount);

            addView(headerRow);

            LinearProgressIndicator progress = new LinearProgressIndicator(context);
            LayoutParams progressLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            progressLp.topMargin = (int) (6 * density);
            progress.setLayoutParams(progressLp);
            progress.setTrackThickness((int) (8 * density));
            progress.setTrackCornerRadius((int) (4 * density));
            progress.setTrackColor(ContextCompat.getColor(context, R.color.divider_color));
            progress.setIndicatorColor(ContextCompat.getColor(context, R.color.check));
            progress.setProgress(0);

            int targetProgress = Math.round(item.percentage);
            progress.post(() -> {
                if (progress.isAttachedToWindow()) {
                    progress.setProgressCompat(targetProgress, true);
                }
            });

            addView(progress);
        }
    }
}
