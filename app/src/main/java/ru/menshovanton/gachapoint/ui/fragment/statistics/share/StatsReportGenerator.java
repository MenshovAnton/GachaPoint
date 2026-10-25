package ru.menshovanton.gachapoint.ui.fragment.statistics.share;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.models.BannerInfo;
import ru.menshovanton.gachapoint.ui.fragment.statistics.model.OverallStatisticsData;

public class StatsReportGenerator {

    public interface ShareCallback {
        void onSuccess(Intent shareIntent);
        void onError(Exception e);
    }

    public interface SaveGalleryCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public static void saveToGallery(Context context, OverallStatisticsData stats, SaveGalleryCallback callback) {
        new Thread(() -> {
            try {
                Bitmap bitmap = createReportBitmap(context, stats);
                String fileName = "GachaPoint_" + stats.getGameType().name() + "_" + System.currentTimeMillis() + ".png";

                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/GachaPoint");
                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1);
                }

                android.content.ContentResolver resolver = context.getContentResolver();
                Uri uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri == null) {
                    throw new IOException("Failed to create MediaStore entry");
                }

                try (java.io.OutputStream out = resolver.openOutputStream(uri)) {
                    if (out == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        throw new IOException("Failed to save image bytes");
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(uri, values, null, null);
                }

                if (callback != null) {
                    ru.menshovanton.gachapoint.data.db.AppDatabase.postToMain(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    ru.menshovanton.gachapoint.data.db.AppDatabase.postToMain(() -> callback.onError(e));
                }
            }
        }).start();
    }

    public static void generateAndShare(Context context, OverallStatisticsData stats, ShareCallback callback) {
        new Thread(() -> {
            try {
                Bitmap bitmap = createReportBitmap(context, stats);
                File cachePath = new File(context.getCacheDir(), "shared_stats");
                if (!cachePath.exists()) {
                    cachePath.mkdirs();
                }
                File imageFile = new File(cachePath, "gachapoint_" + stats.getGameType().name().toLowerCase(Locale.ROOT) + "_stats.png");
                try (FileOutputStream stream = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                }

                Uri contentUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        imageFile
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                String chooserTitle = context.getString(R.string.stat_share_chooser_title);
                Intent chooser = Intent.createChooser(shareIntent, chooserTitle);
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (callback != null) {
                    ru.menshovanton.gachapoint.data.db.AppDatabase.postToMain(() -> callback.onSuccess(chooser));
                }
            } catch (Exception e) {
                if (callback != null) {
                    ru.menshovanton.gachapoint.data.db.AppDatabase.postToMain(() -> callback.onError(e));
                }
            }
        }).start();
    }

    private static Bitmap createReportBitmap(Context context, OverallStatisticsData stats) {
        Typeface genshinFont = ResourcesCompat.getFont(context, R.font.genshin_font);

        java.util.List<BannerInfo> allBanners = stats.getGameType().getBanners();
        Map<String, Integer> bannerCounts = stats.getBannerPullsCount();
        java.util.List<BannerInfo> displayBanners = new java.util.ArrayList<>();
        for (BannerInfo banner : allBanners) {
            Integer countObj = bannerCounts.get(banner.getDbKey());
            int count = countObj != null ? countObj : 0;
            if (count > 0 || displayBanners.size() < 3) {
                displayBanners.add(banner);
            }
        }
        if (displayBanners.isEmpty()) {
            displayBanners = allBanners;
        }

        int width = 1080;
        float padding = 54f;
        float contentWidth = width - 2 * padding;

        float headerH = 96f;
        float gapHeaderToGame = 24f;
        float gameCardH = 88f;

        float sectionGap = 44f;
        float titleGap = 32f;

        float card1Height = 282f;
        float card2Height = 156f;
        float card3Height = 28f + displayBanners.size() * 60f + 12f;
        float card4Height = 214f;
        float card5Height = 90f;

        float footerGap = 48f;
        float footerH = 26f;
        float bottomPad = 56f;

        float totalHeight = 56f
                + headerH + gapHeaderToGame + gameCardH
                + sectionGap + titleGap + card1Height
                + sectionGap + titleGap + card2Height
                + sectionGap + titleGap + card3Height
                + sectionGap + titleGap + card4Height
                + sectionGap + titleGap + card5Height
                + footerGap + footerH + bottomPad;

        int height = Math.round(totalHeight);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(
                0, 0, 0, height,
                Color.parseColor("#0C111A"),
                Color.parseColor("#141D29"),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, width, height, bgPaint);

        Paint decorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        decorPaint.setColor(Color.parseColor("#09537497"));
        canvas.drawCircle(width - 50, 160, 300, decorPaint);
        canvas.drawCircle(80, height - 250, 360, decorPaint);

        float currentY = 56f;
        Bitmap logoBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_app_logo);
        if (logoBitmap != null) {
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 92, 92, true);
            int saveLogo = canvas.save();
            Path logoClip = new Path();
            logoClip.addRoundRect(new RectF(padding, currentY, padding + 92f, currentY + 92f), 20f, 20f, Path.Direction.CW);
            canvas.clipPath(logoClip);
            canvas.drawBitmap(scaledLogo, padding, currentY, null);
            canvas.restoreToCount(saveLogo);
        }

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (genshinFont != null) textPaint.setTypeface(genshinFont);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setFakeBoldText(true);
        canvas.drawText("GachaPoint", padding + 112f, currentY + 38f, textPaint);

        String subTitle = context.getString(R.string.stat_share_subtitle);
        textPaint.setColor(Color.parseColor("#7EA1C7"));
        textPaint.setTextSize(17f);
        textPaint.setFakeBoldText(true);
        float subTitleWidth = textPaint.measureText(subTitle);
        float badgePadH = 14f;
        float badgeLeft = padding + 112f;
        float badgeTop = currentY + 50f;
        float badgeRight = badgeLeft + subTitleWidth + 2 * badgePadH;
        float badgeBottom = currentY + 84f;
        RectF badgeRect = new RectF(badgeLeft, badgeTop, badgeRight, badgeBottom);
        drawRoundedCard(canvas, badgeRect, Color.parseColor("#1B2A3D"), 0, 10f);

        Paint.FontMetrics badgeFm = textPaint.getFontMetrics();
        float badgeTextY = (badgeTop + badgeBottom) / 2f - (badgeFm.ascent + badgeFm.descent) / 2f;
        canvas.drawText(subTitle, badgeLeft + badgePadH, badgeTextY, textPaint);

        currentY += headerH + gapHeaderToGame;
        RectF gameCardRect = new RectF(padding, currentY, padding + contentWidth, currentY + gameCardH);
        drawRoundedCard(canvas, gameCardRect, Color.parseColor("#032D54"), Color.parseColor("#537497"), 24f);

        Bitmap gameIcon = BitmapFactory.decodeResource(context.getResources(), stats.getGameType().getCurrencyIconResId());
        if (gameIcon != null) {
            Bitmap scaledGameIcon = Bitmap.createScaledBitmap(gameIcon, 56, 56, true);
            canvas.drawBitmap(scaledGameIcon, padding + 20f, currentY + 16f, null);
        }

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(context.getString(stats.getGameType().getNameResId()), padding + 92f, currentY + 55f, textPaint);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault()));
        textPaint.setColor(Color.parseColor("#9EB1C7"));
        textPaint.setTextSize(20f);
        textPaint.setFakeBoldText(false);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(dateStr, padding + contentWidth - 24f, currentY + 54f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);

        currentY += gameCardH + sectionGap;
        drawSectionTitle(canvas, context.getString(R.string.stat_section_pulls_title), padding, currentY, genshinFont);

        currentY += titleGap;
        RectF pullsCardRect = new RectF(padding, currentY, padding + contentWidth, currentY + card1Height);
        drawRoundedCard(canvas, pullsCardRect, Color.parseColor("#151D29"), Color.parseColor("#28364A"), 26f);

        float col2X = padding + contentWidth / 2f + 16f;

        textPaint.setColor(Color.parseColor("#8E9FBA"));
        textPaint.setTextSize(22f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(context.getString(R.string.stat_card_total_pulls), padding + 28f, currentY + 42f, textPaint);
        canvas.drawText(context.getString(R.string.stat_card_currency_spent), col2X, currentY + 42f, textPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(46f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(String.format(Locale.getDefault(), "%,d", stats.getTotalPulls()).replace(',', ' '), padding + 28f, currentY + 100f, textPaint);

        textPaint.setColor(Color.parseColor("#E1A142"));
        canvas.drawText(String.format(Locale.getDefault(), "%,d", stats.getTotalCurrencySpent()).replace(',', ' '), col2X, currentY + 100f, textPaint);

        Paint divPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        divPaint.setColor(Color.parseColor("#253142"));
        divPaint.setStrokeWidth(2f);
        canvas.drawLine(padding + 28f, currentY + 126f, padding + contentWidth - 28f, currentY + 126f, divPaint);

        float badgeGap = 16f;
        float badgeWidth = (contentWidth - 56f - badgeGap) / 2f;
        float badgeY = currentY + 144f;
        float badgeHeight = 118f;

        RectF box5 = new RectF(padding + 28f, badgeY, padding + 28f + badgeWidth, badgeY + badgeHeight);
        drawRoundedCard(canvas, box5, Color.parseColor("#2B2113"), Color.parseColor("#E1A142"), 16f);

        textPaint.setColor(Color.parseColor("#E1A142"));
        textPaint.setTextSize(20f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(context.getString(R.string.stat_badge_5star_title), padding + 44f, badgeY + 32f, textPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", stats.getFiveStarPercent()), padding + 44f, badgeY + 68f, textPaint);

        String sub5Text = (stats.getFiveStarCount() > 0)
                ? String.format(Locale.getDefault(), context.getString(R.string.stat_badge_sub_count_freq), stats.getFiveStarCount(), context.getString(R.string.stat_subs_unit), stats.getAveragePullsPerFiveStar())
                : String.format(Locale.getDefault(), context.getString(R.string.stat_badge_sub_empty_count), context.getString(R.string.stat_subs_unit));
        textPaint.setColor(Color.parseColor("#D4B375"));
        textPaint.setTextSize(16f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(sub5Text, padding + 44f, badgeY + 100f, textPaint);

        float box4X = padding + 28f + badgeWidth + badgeGap;
        RectF box4 = new RectF(box4X, badgeY, box4X + badgeWidth, badgeY + badgeHeight);
        drawRoundedCard(canvas, box4, Color.parseColor("#23172E"), Color.parseColor("#6E488E"), 16f);

        textPaint.setColor(Color.parseColor("#B088E0"));
        textPaint.setTextSize(20f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(context.getString(R.string.stat_badge_4star_title), box4X + 16f, badgeY + 32f, textPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", stats.getFourStarPercent()), box4X + 16f, badgeY + 68f, textPaint);

        String sub4Text = (stats.getFourStarCount() > 0)
                ? String.format(Locale.getDefault(), context.getString(R.string.stat_badge_sub_count_freq), stats.getFourStarCount(), context.getString(R.string.stat_subs_unit), stats.getAveragePullsPerFourStar())
                : String.format(Locale.getDefault(), context.getString(R.string.stat_badge_sub_empty_count), context.getString(R.string.stat_subs_unit));
        textPaint.setColor(Color.parseColor("#C5AEE4"));
        textPaint.setTextSize(16f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(sub4Text, box4X + 16f, badgeY + 100f, textPaint);

        currentY += card1Height + sectionGap;
        drawSectionTitle(canvas, context.getString(R.string.stat_section_luck_title), padding, currentY, genshinFont);

        currentY += titleGap;
        RectF luckCardRect = new RectF(padding, currentY, padding + contentWidth, currentY + card2Height);
        drawRoundedCard(canvas, luckCardRect, Color.parseColor("#151D29"), Color.parseColor("#28364A"), 26f);

        textPaint.setColor(Color.parseColor("#8E9FBA"));
        textPaint.setTextSize(22f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(context.getString(R.string.stat_winrate_5050_label), padding + 28f, currentY + 42f, textPaint);

        String winrateStr = (stats.getWin5050Count() + stats.getLoss5050Count() > 0)
                ? String.format(Locale.getDefault(), "%.1f%%", stats.getWin5050Rate())
                : "—";
        textPaint.setColor(Color.parseColor("#4CAF50"));
        textPaint.setTextSize(42f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(winrateStr, padding + 28f, currentY + 96f, textPaint);

        textPaint.setColor(Color.parseColor("#8E9FBA"));
        textPaint.setTextSize(19f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(String.format(Locale.getDefault(), "%dW / %dL", stats.getWin5050Count(), stats.getLoss5050Count()), padding + 28f, currentY + 128f, textPaint);

        textPaint.setColor(Color.parseColor("#8E9FBA"));
        textPaint.setTextSize(22f);
        canvas.drawText(context.getString(R.string.stat_avg_pity_label), col2X, currentY + 42f, textPaint);

        String avgPityStr = (stats.getAvg5StarPity() > 0)
                ? String.format(Locale.getDefault(), "%.1f", stats.getAvg5StarPity())
                : "—";
        textPaint.setColor(Color.parseColor("#E1A142"));
        textPaint.setTextSize(42f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(avgPityStr, col2X, currentY + 96f, textPaint);

        textPaint.setColor(Color.parseColor("#8E9FBA"));
        textPaint.setTextSize(19f);
        textPaint.setFakeBoldText(false);
        String rangeStr = (stats.getAvg5StarPity() > 0)
                ? String.format(Locale.getDefault(), context.getString(R.string.stat_pity_range), stats.getMin5StarPity(), stats.getMax5StarPity())
                : "—";
        canvas.drawText(rangeStr, col2X, currentY + 128f, textPaint);

        currentY += card2Height + sectionGap;
        drawSectionTitle(canvas, context.getString(R.string.stat_section_banners_title), padding, currentY, genshinFont);

        currentY += titleGap;
        RectF bannerCardRect = new RectF(padding, currentY, padding + contentWidth, currentY + card3Height);
        drawRoundedCard(canvas, bannerCardRect, Color.parseColor("#151D29"), Color.parseColor("#28364A"), 26f);

        float itemY = currentY + 36f;
        int totalP = stats.getTotalPulls();

        for (BannerInfo banner : displayBanners) {
            Integer countObj = bannerCounts.get(banner.getDbKey());
            int count = countObj != null ? countObj : 0;
            float pct = (totalP > 0) ? (count * 100f / totalP) : 0f;

            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(20f);
            textPaint.setFakeBoldText(true);
            canvas.drawText(context.getString(banner.getLabelResId()), padding + 28f, itemY, textPaint);

            textPaint.setColor(Color.parseColor("#8E9FBA"));
            textPaint.setTextSize(19f);
            textPaint.setFakeBoldText(false);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.format(Locale.getDefault(), "%d (%.1f%%)", count, pct), padding + contentWidth - 28f, itemY, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);

            float barY = itemY + 14f;
            float barW = contentWidth - 56f;
            RectF trackRect = new RectF(padding + 28f, barY, padding + 28f + barW, barY + 10f);
            drawRoundedCard(canvas, trackRect, Color.parseColor("#243042"), 0, 5f);

            if (pct > 0) {
                float fillW = (pct / 100f) * barW;
                RectF fillRect = new RectF(padding + 28f, barY, padding + 28f + fillW, barY + 10f);
                drawRoundedCard(canvas, fillRect, Color.parseColor("#537497"), 0, 5f);
            }

            itemY += 60f;
        }

        currentY += card3Height + sectionGap;
        drawSectionTitle(canvas, context.getString(R.string.stat_section_subs_and_savings_title), padding, currentY, genshinFont);

        currentY += titleGap;
        RectF subsSavingsCardRect = new RectF(padding, currentY, padding + contentWidth, currentY + card4Height);
        drawRoundedCard(canvas, subsSavingsCardRect, Color.parseColor("#151D29"), Color.parseColor("#28364A"), 26f);

        textPaint.setColor(Color.parseColor("#8E9FBA"));
        textPaint.setTextSize(20f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(context.getString(R.string.stat_subs_overview_label), padding + 28f, currentY + 38f, textPaint);
        canvas.drawText(context.getString(R.string.title_bank), col2X, currentY + 38f, textPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(25f);
        textPaint.setFakeBoldText(true);
        String subsShort = String.format(Locale.getDefault(), "%d %s (%d %s)",
                stats.getTotalSubsPurchased(), context.getString(R.string.stat_subs_unit),
                stats.getTotalSubDays(), context.getString(R.string.stat_days_unit));
        canvas.drawText(subsShort, padding + 28f, currentY + 74f, textPaint);

        String bankShort = String.format(Locale.getDefault(), "%d / %d %s",
                stats.getTotalProgressPulls(), stats.getTargetPulls(), context.getString(R.string.pulls).toLowerCase(Locale.ROOT));
        canvas.drawText(bankShort, col2X, currentY + 74f, textPaint);

        textPaint.setColor(Color.parseColor("#4CAF50"));
        textPaint.setTextSize(18f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(String.format(Locale.getDefault(), "%s: %.1f%%", context.getString(R.string.stat_subs_attendance), stats.getSubAttendanceRate()), padding + 28f, currentY + 104f, textPaint);

        int bankPct = (stats.getTargetPulls() > 0) ? (int) ((stats.getTotalProgressPulls() * 100f) / stats.getTargetPulls()) : 0;
        textPaint.setColor(Color.parseColor("#8E9FBA"));
        String rawTargetLabel = context.getString(R.string.savings_target_label);
        String cleanTargetLabel = rawTargetLabel.endsWith(":") ? rawTargetLabel.substring(0, rawTargetLabel.length() - 1).trim() : rawTargetLabel;
        canvas.drawText(String.format(Locale.getDefault(), "%s: %d%%", cleanTargetLabel, bankPct), col2X, currentY + 104f, textPaint);

        float sBarY = currentY + 128f;
        float sBarW = contentWidth - 56f;
        RectF sTrackRect = new RectF(padding + 28f, sBarY, padding + 28f + sBarW, sBarY + 14f);
        drawRoundedCard(canvas, sTrackRect, Color.parseColor("#243042"), 0, 7f);

        float sTotal = stats.getManualPulls() + stats.getSubsPulls() + stats.getDailiesPulls();
        if (sTotal > 0) {
            int saveClip = canvas.save();
            Path barClip = new Path();
            barClip.addRoundRect(sTrackRect, 7f, 7f, Path.Direction.CW);
            canvas.clipPath(barClip);

            float curX = padding + 28f;
            Paint segPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            float mW = (stats.getManualPulls() / sTotal) * sBarW;
            if (mW > 0) {
                segPaint.setColor(Color.parseColor("#537497"));
                canvas.drawRect(curX, sBarY, curX + mW, sBarY + 14f, segPaint);
                curX += mW;
            }
            float sW = (stats.getSubsPulls() / sTotal) * sBarW;
            if (sW > 0) {
                segPaint.setColor(Color.parseColor("#E1A142"));
                canvas.drawRect(curX, sBarY, curX + sW, sBarY + 14f, segPaint);
                curX += sW;
            }
            float dW = (stats.getDailiesPulls() / sTotal) * sBarW;
            if (dW > 0) {
                segPaint.setColor(Color.parseColor("#4CAF50"));
                canvas.drawRect(curX, sBarY, curX + dW, sBarY + 14f, segPaint);
            }
            canvas.restoreToCount(saveClip);
        }

        float legY = currentY + 180f;
        float colW = (contentWidth - 56f) / 3f;
        float legX1 = padding + 28f;
        float legX2 = legX1 + colW;
        float legX3 = legX1 + 2 * colW;

        drawLegendBadge(canvas, legX1, legY, Color.parseColor("#537497"),
                String.format(Locale.getDefault(), "%s: %d", context.getString(R.string.stat_source_manual), stats.getManualPulls()), genshinFont);
        drawLegendBadge(canvas, legX2, legY, Color.parseColor("#E1A142"),
                String.format(Locale.getDefault(), "%s: %d", context.getString(R.string.stat_source_subs), stats.getSubsPulls()), genshinFont);
        drawLegendBadge(canvas, legX3, legY, Color.parseColor("#4CAF50"),
                String.format(Locale.getDefault(), "%s: %d", context.getString(R.string.stat_source_dailies), stats.getDailiesPulls()), genshinFont);

        currentY += card4Height + sectionGap;
        drawSectionTitle(canvas, context.getString(R.string.stat_total_value_title), padding, currentY, genshinFont);

        currentY += titleGap;
        RectF wealthCardRect = new RectF(padding, currentY, padding + contentWidth, currentY + card5Height);
        drawRoundedCard(canvas, wealthCardRect, Color.parseColor("#151D29"), Color.parseColor("#28364A"), 26f);

        textPaint.setColor(Color.parseColor("#8E9FBA"));
        textPaint.setTextSize(20f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(context.getString(R.string.stat_total_value_desc), padding + 28f, currentY + 54f, textPaint);

        textPaint.setColor(Color.parseColor("#E1A142"));
        textPaint.setTextSize(34f);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(Locale.getDefault(), "%,d", stats.getTotalTrackedCurrency()).replace(',', ' '), padding + contentWidth - 28f, currentY + 58f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);

        currentY += card5Height + footerGap;
        textPaint.setColor(Color.parseColor("#657991"));
        textPaint.setTextSize(19f);
        textPaint.setFakeBoldText(false);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(context.getString(R.string.stat_watermark_text), width / 2f, currentY, textPaint);

        return bitmap;
    }

    private static void drawSectionTitle(Canvas canvas, String title, float x, float y, Typeface font) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (font != null) paint.setTypeface(font);
        paint.setColor(Color.parseColor("#537497"));
        paint.setTextSize(22f);
        paint.setFakeBoldText(true);
        canvas.drawText(title.toUpperCase(Locale.getDefault()), x + 4f, y, paint);
    }

    private static void drawRoundedCard(Canvas canvas, RectF rect, int bgColor, int strokeColor, float radius) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        canvas.drawRoundRect(rect, radius, radius, paint);

        if (strokeColor != 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(strokeColor);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
    }

    private static void drawLegendBadge(Canvas canvas, float x, float y, int dotColor, String label, Typeface font) {
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(dotColor);
        canvas.drawCircle(x + 10f, y - 6f, 7f, dotPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (font != null) textPaint.setTypeface(font);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(label, x + 26f, y, textPaint);
    }
}
