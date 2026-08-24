package ru.menshovanton.gachapoint.worker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.Preferences;

public class DailyNotificationWorker extends Worker {

    private static final String CHANNEL_ID = "Notifications";
    public static final String WORK_TAG = "daily_notification_work";

    public DailyNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Preferences preferences = new Preferences(context);

        boolean allowNotifications = preferences.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS);
        if (!allowNotifications) {
            return Result.success();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return Result.failure();
            }
        }

        sendNotification(context);
        return Result.success();
    }

    private void sendNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            CharSequence name = context.getString(R.string.notifications_name);
            String description = context.getString(R.string.notifications_description);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
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
    }
}
