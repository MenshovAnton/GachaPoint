package ru.menshovanton.gachapoint.helpers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.time.LocalDateTime;
import java.time.ZoneId;

import ru.menshovanton.gachapoint.Notification;

public class AlarmHelper extends Service {
    private static final String TAG = "AlarmHelper";
    private static final int REQUEST_CODE = 100;

    public static int alarmHour = 12;
    public static int alarmMinute = 0;

    PreferencesHelper preferencesHelper;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        preferencesHelper = new PreferencesHelper(getApplicationContext());

        alarmHour = preferencesHelper.getIntPreference(PreferencesHelper.ALARM_HOURS);
        alarmMinute = preferencesHelper.getIntPreference(PreferencesHelper.ALARM_MINUTES);

        setDailyAlarm(getApplicationContext());
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAlarm(getApplicationContext());
    }

    public static void setDailyAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, Notification.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime alarm = LocalDateTime.of(
                now.getYear(), now.getMonth(), now.getDayOfMonth(),
                alarmHour, alarmMinute, 0, 0
        );

        if (!alarm.isAfter(now)) {
            alarm = alarm.plusDays(1);
        }

        long alarmMillis = alarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
    }

    public static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, Notification.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }
}
