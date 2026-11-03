package ru.menshovanton.gachapoint.worker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository;
import ru.menshovanton.gachapoint.data.repository.PromocodeRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;
import ru.menshovanton.gachapoint.domain.models.PromoCode;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;

public class DataSyncWorker extends Worker {

    public static final String WORK_NAME = "GachaPointDataSyncWorker";
    public static final String CHANNEL_BANNERS = "channel_banners";
    private static final String NOTIFICATION_GROUP_BANNERS = "ru.menshovanton.gachapoint.BANNERS";
    private static final String NOTIFICATION_GROUP_PROMOS = "ru.menshovanton.gachapoint.PROMOS";

    public DataSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void enqueuePeriodicWork(@NonNull Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    DataSyncWorker.class,
                    3, TimeUnit.HOURS,
                    30, TimeUnit.MINUTES
            )
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            );
            AppLogger.i("DataSyncWorker", "Periodic data sync work enqueued successfully (every 3h with network constraint).");
        } catch (Exception e) {
            AppLogger.e("DataSyncWorker", "Failed to enqueue periodic data sync work", e);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Preferences prefs = new Preferences(context);

        if (!prefs.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS, true)) {
            AppLogger.d("DataSyncWorker", "Notifications disabled in settings. Skipping background notifications.");
            return Result.success();
        }

        try {
            createChannels(context);

            // 1. Check Promocodes: Stream & Burning (<24h)
            checkPromocodes(context, prefs);

            // 2. Check Banners: Announced, Upcoming (24h before), Active
            checkBanners(context, prefs);

            return Result.success();
        } catch (Exception e) {
            AppLogger.e("DataSyncWorker", "Error during background sync work", e);
            return Result.retry();
        }
    }

    private void createChannels(Context context) {
        DailyNotificationWorker.createNotificationChannels(context);
        PromocodeSyncTask.createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel bannerChannel = new NotificationChannel(
                        CHANNEL_BANNERS,
                        context.getString(R.string.channel_banners_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                bannerChannel.setDescription(context.getString(R.string.channel_banners_desc));
                bannerChannel.enableVibration(true);
                nm.createNotificationChannel(bannerChannel);
            }
        }
    }

    private void checkPromocodes(Context context, Preferences prefs) {
        PromocodeRepository repo = PromocodeRepository.getInstance(context);
        List<PromoCode> unnotifiedStream = repo.getUnnotifiedStreamCodes();

        // Check stream codes
        if (unnotifiedStream != null) {
            for (PromoCode code : unnotifiedStream) {
                if (hasNotificationPermission(context)) {
                    showCodeNotification(context, code, true);
                }
                repo.markAsNotified(code.getId());
            }
        }

        // Check burning codes (expires within 24 hours)
        List<PromoCode> allCodes = repo.getPromoCodes(null, false);
        Instant now = Instant.now();

        for (PromoCode code : allCodes) {
            if (!code.isActive() || code.isRedeemed() || code.isExpiredByUser() || code.isPermanent()) {
                continue;
            }

            String expiresAtStr = code.getExpiresAt();
            if (expiresAtStr == null || expiresAtStr.isEmpty()) {
                continue;
            }

            try {
                Instant expiry = Instant.parse(expiresAtStr);
                Duration diff = Duration.between(now, expiry);
                long hours = diff.toHours();

                if (hours > 0 && hours <= 24) {
                    String notifyKey = "notified_expiring_" + code.getId();
                    if (!prefs.getStringPreference(notifyKey, "").equals(expiresAtStr)) {
                        if (hasNotificationPermission(context)) {
                            showCodeNotification(context, code, false);
                        }
                        prefs.saveStringPreference(notifyKey, expiresAtStr);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void checkBanners(Context context, Preferences prefs) {
        BannerScheduleRepository bannerRepo = BannerScheduleRepository.getInstance(context);
        bannerRepo.reload(true);

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (GameType game : prefs.getEnabledGames()) {
            List<BannerScheduleItem> banners = bannerRepo.getBanners(game);
            if (banners == null) continue;

            for (BannerScheduleItem banner : banners) {
                String bannerId = banner.getId();
                LocalDate start = null;
                LocalDate end = null;
                try {
                    start = LocalDate.parse(banner.getStartDate(), fmt);
                    end = LocalDate.parse(banner.getEndDate(), fmt);
                } catch (Exception ignored) {}

                if (start == null || end == null) continue;

                String gameTitle = context.getString(game.getDisplayNameResId());
                String heroes = String.join(", ", banner.getFeaturedFiveStars(context));

                // 1. Анонс (Announced)
                if ("ANNOUNCED".equalsIgnoreCase(banner.getStatus()) && today.isBefore(start)) {
                    if (!prefs.isBannerNotifiedAnnounced(bannerId)) {
                        if (hasNotificationPermission(context)) {
                            String title = context.getString(R.string.notification_banner_announced_title, gameTitle);
                            String text = "v" + banner.getVersion() + " (" + context.getString(R.string.three_star).replace("3", String.valueOf(banner.getPhase())) + "): " + heroes;
                            showBannerNotification(context, banner, title, text, bannerId.hashCode() + 1);
                        }
                        prefs.setBannerNotifiedAnnounced(bannerId);
                    }
                }

                // 2. Скоро (Завтра старт)
                long daysUntil = ChronoUnit.DAYS.between(today, start);
                if (daysUntil == 1) {
                    if (!prefs.isBannerNotifiedUpcoming(bannerId)) {
                        if (hasNotificationPermission(context)) {
                            String title = context.getString(R.string.notification_banner_upcoming_title, gameTitle);
                            String text = heroes + " (" + banner.getStartDate() + " — " + banner.getEndDate() + ")";
                            showBannerNotification(context, banner, title, text, bannerId.hashCode() + 2);
                        }
                        prefs.setBannerNotifiedUpcoming(bannerId);
                    }
                }

                // 3. Доступность (Уже доступен)
                boolean isLive = !today.isBefore(start) && !today.isAfter(end);
                if (isLive && "CURRENT".equalsIgnoreCase(banner.getStatus())) {
                    if (!prefs.isBannerNotifiedActive(bannerId)) {
                        if (hasNotificationPermission(context)) {
                            String title = context.getString(R.string.notification_banner_active_title, gameTitle);
                            String text = heroes + " — испытайте удачу!";
                            showBannerNotification(context, banner, title, text, bannerId.hashCode() + 3);
                        }
                        prefs.setBannerNotifiedActive(bannerId);
                    }
                }
            }
        }
    }

    private void showCodeNotification(Context context, PromoCode code, boolean isStream) {
        GameType game = GameType.fromId(code.getGameType());
        String gameTitle = game != null ? context.getString(game.getDisplayNameResId()) : "";

        Intent intent = new Intent(context, MainActivityView.class);
        intent.putExtra("navigate_to", "promocodes");
        intent.putExtra("game_type", code.getGameType());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                code.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = isStream
                ? context.getString(R.string.promocode_stream_push_title, gameTitle)
                : context.getString(R.string.notification_hot_code_title, gameTitle, code.getCode());

        String content = isStream
                ? context.getString(R.string.promocode_stream_push_text, code.getCode(), code.getRewardsDescription())
                : context.getString(R.string.notification_hot_code_desc);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PromocodeSyncTask.CHANNEL_PROMOCODES)
                .setSmallIcon(R.drawable.icon_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setGroup(NOTIFICATION_GROUP_PROMOS);

        NotificationManagerCompat.from(context).notify(code.getId().hashCode(), builder.build());
    }

    private void showBannerNotification(Context context, BannerScheduleItem banner, String title, String text, int notifId) {
        Intent intent = new Intent(context, MainActivityView.class);
        intent.putExtra("navigate_to", "banners");
        intent.putExtra("game_type", banner.getGameType());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_BANNERS)
                .setSmallIcon(R.drawable.icon_star)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setGroup(NOTIFICATION_GROUP_BANNERS);

        NotificationManagerCompat.from(context).notify(notifId, builder.build());
    }

    private boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
