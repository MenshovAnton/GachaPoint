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

import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.data.repository.PromocodeRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.PromoCode;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;

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
                Preferences prefs = new Preferences(context);
                boolean notificationsAllowed = prefs.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS, true);
                if (!notificationsAllowed) return;

                PromocodeRepository repository = PromocodeRepository.getInstance(context);
                List<PromoCode> unnotified = repository.getUnnotifiedStreamCodes();

                if (unnotified == null || unnotified.isEmpty()) {
                    return;
                }

                createNotificationChannel(context);

                for (PromoCode code : unnotified) {
                    showStreamNotification(context, code);
                    repository.markAsNotified(code.getId());
                }
            } catch (Exception e) {
                AppLogger.e("PromocodeSyncTask", "Error checking stream promocodes in background", e);
            }
        });
    }

    private static void showStreamNotification(@NonNull Context context, @NonNull PromoCode code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

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

        String title = context.getString(R.string.promocode_stream_push_title, gameTitle);
        String text = context.getString(R.string.promocode_stream_push_text, code.getCode(), code.getRewardsDescription());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PROMOCODES)
                .setSmallIcon(R.drawable.icon_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManagerCompat.from(context).notify(code.getId().hashCode(), builder.build());
    }
}
