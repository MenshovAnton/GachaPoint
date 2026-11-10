package ru.menshovanton.gachapoint.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.AppLogger;

public final class PromocodeSyncTask {

    public static final String CHANNEL_PROMOCODES = "channel_promocodes";
    public static final int NOTIFICATION_ID_PROMOCODES = 1004;

    private PromocodeSyncTask() {}

    public static void createNotificationChannel(@NonNull Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_PROMOCODES,
                    context.getString(R.string.promocodes_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.promocodes_notification_channel_desc));
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }
    }

    public static void checkStreamPromocodesAsync(@NonNull Context context) {
        ru.menshovanton.gachapoint.data.local.NetworkExecutor.execute(() -> {
            try {
                DataSyncWorker.checkStreamPromocodesSync(context);
            } catch (Exception e) {
                AppLogger.e("PromocodeSyncTask", "Error checking stream promocodes in background", e);
            }
        });
    }
}
