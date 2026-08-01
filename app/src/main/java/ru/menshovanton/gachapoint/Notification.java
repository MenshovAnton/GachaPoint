package ru.menshovanton.gachapoint;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Objects;

import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.helpers.AlarmHelper;

public class Notification extends BroadcastReceiver {
    private static final int NOTIFY_ID = 101;
    private static final String CHANNEL_ID = "Оповещения";

    public static int subsCount = 0;
    public static boolean allowNotifications = true;

    private final MainActivity MAIN_ACTIVITY = MainActivity.mainActivity;
    Preferences preferences = new Preferences(MAIN_ACTIVITY);

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @SuppressLint("ScheduleExactAlarm")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (subsCount > 0 && allowNotifications) {
            allowNotifications = preferences.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS);

            CharSequence name = "Оповещения";
            String description = "Высылает напоминания о сборе наград.";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager nm = getSystemService(context, NotificationManager.class);
            Objects.requireNonNull(nm).createNotificationChannel(channel);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Напоминание")
                    .setContentText("Не забудь собрать награды в гачах!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(1, builder.build());

            AlarmHelper.setDailyAlarm(MainActivity.context);
        }
    }

}