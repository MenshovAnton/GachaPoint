package ru.menshovanton.gachapoint.worker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDateTime;
import java.time.ZoneId;

import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.Preferences;

public class NotificationScheduler {

    public static final int ALARM_REQUEST_CODE = 1001;

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

            long triggerAtMillis = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                Intent intent = new Intent(context, DailyNotificationReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        ALARM_REQUEST_CODE,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                AppLogger.i("NotificationScheduler", "Daily notification scheduled via AlarmManager for " + targetTime);
            }

        } catch (Throwable t) {
            AppLogger.e("NotificationScheduler", "Failed to schedule daily notification", t);
        }
    }

    public static void cancelDailyNotification(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                Intent intent = new Intent(context, DailyNotificationReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        ALARM_REQUEST_CODE,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(pendingIntent);
            }

            AppLogger.i("NotificationScheduler", "Daily notification cancelled.");
        } catch (Throwable t) {
            AppLogger.e("NotificationScheduler", "Failed to cancel daily notification", t);
        }
    }
}
