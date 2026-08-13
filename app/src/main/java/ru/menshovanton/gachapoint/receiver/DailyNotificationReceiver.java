package ru.menshovanton.gachapoint.receiver;


import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.service.AlarmService;
import ru.menshovanton.gachapoint.data.local.Preferences;

public class DailyNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "Notifications";

    public static int subsCount = 0;
    public static boolean allowNotifications = true;

    Preferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        Preferences preferences = new Preferences(context);

        boolean allowNotifications = preferences.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS);

        if (subsCount > 0 && allowNotifications) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    AlarmService.setDailyAlarm(context);
                    return;
                }
            }

            CharSequence name = context.getString(R.string.notifications_name);
            String description = context.getString(R.string.notifications_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(context.getString(R.string.notifications_title))
                    .setContentText(context.getString(R.string.notifications_text))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            try {
                notificationManager.notify(1, builder.build());
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            AlarmService.setDailyAlarm(context);
        }
    }

}