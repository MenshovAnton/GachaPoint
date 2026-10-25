package ru.menshovanton.gachapoint.worker;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.Preferences;

public class NotificationScheduler {

    public static void scheduleDailyNotification(Context context) {
        scheduleDailyNotification(context, false);
    }

    public static void scheduleDailyNotification(Context context, boolean forceReschedule) {
        try {
            Preferences preferences = new Preferences(context);
            boolean enabled = preferences.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS);

            if (!enabled) {
                cancelDailyNotification(context);
                return;
            }

            int targetHour = preferences.getIntPreference(Preferences.ALARM_HOURS);
            int targetMinute = preferences.getIntPreference(Preferences.ALARM_MINUTES);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime targetTime = now.withHour(targetHour).withMinute(targetMinute).withSecond(0).withNano(0);

            if (!targetTime.isAfter(now)) {
                targetTime = targetTime.plusDays(1);
            }

            long initialDelayMinutes = Duration.between(now, targetTime).toMinutes();

            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    DailyNotificationWorker.class, 24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                    .addTag(DailyNotificationWorker.WORK_TAG)
                    .build();

            ExistingPeriodicWorkPolicy policy = forceReschedule
                    ? ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                    : ExistingPeriodicWorkPolicy.KEEP;

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    DailyNotificationWorker.WORK_TAG,
                    policy,
                    workRequest
            );
            AppLogger.i("NotificationScheduler", "Daily notification scheduled. Target " + targetHour + ":" + targetMinute
                    + ", initial delay: " + initialDelayMinutes + " minutes.");
        } catch (Throwable t) {
            AppLogger.e("NotificationScheduler", "Failed to schedule daily notification", t);
        }
    }

    public static void cancelDailyNotification(Context context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(DailyNotificationWorker.WORK_TAG);
            AppLogger.i("NotificationScheduler", "Daily notification cancelled.");
        } catch (Throwable t) {
            AppLogger.e("NotificationScheduler", "Failed to cancel daily notification", t);
        }
    }
}
