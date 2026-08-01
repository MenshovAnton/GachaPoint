package ru.menshovanton.gachapoint.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.RequiresPermission;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import ru.menshovanton.gachapoint.Notification;
import ru.menshovanton.gachapoint.Preferences;
import ru.menshovanton.gachapoint.activities.MainActivity;

public class AlarmHelper extends Service {
    private static final String TAG = "AlarmHelper";
    private static final int REQUEST_CODE = 100;

    public static int alarmHour = 12;
    public static int alarmMinute = 0;

    public MainActivity mainActivity = MainActivity.mainActivity;
    Preferences preferences = new Preferences(mainActivity);

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        alarmHour = preferences.getIntPreference(Preferences.ALARM_HOURS);
        alarmMinute = preferences.getIntPreference(Preferences.ALARM_MINUTES);

        setDailyAlarm(MainActivity.context);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAlarm(MainActivity.context);
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
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

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
    }

    public static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, Notification.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }
}
