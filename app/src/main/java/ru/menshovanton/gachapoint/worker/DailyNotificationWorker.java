package ru.menshovanton.gachapoint.worker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.data.db.entities.SavingsEntity;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.DayState;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;

public final class DailyNotificationWorker {

    public static final String CHANNEL_SUBSCRIPTIONS = "channel_subscriptions";
    public static final String CHANNEL_DAILIES = "channel_dailies";
    public static final String CHANNEL_EVENTS = "channel_events";

    public static final int NOTIFICATION_ID_SUBSCRIPTIONS = 1001;
    public static final int NOTIFICATION_ID_DAILIES = 1002;
    public static final int NOTIFICATION_ID_EVENTS = 1003;
    public static final String NOTIFICATION_GROUP_DAILY = "ru.menshovanton.gachapoint.DAILY_REMINDERS";

    private DailyNotificationWorker() {}

    public static void createNotificationChannels(Context context) {
        if (context == null) {
            return;
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        try {
            nm.deleteNotificationChannel("Notifications");
        } catch (Throwable ignored) {}

        List<NotificationChannel> channels = new ArrayList<>();

        NotificationChannel subChannel = new NotificationChannel(
                CHANNEL_SUBSCRIPTIONS,
                context.getString(R.string.channel_subscriptions_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        subChannel.setDescription(context.getString(R.string.channel_subscriptions_desc));
        subChannel.enableVibration(true);
        channels.add(subChannel);

        NotificationChannel dailyChannel = new NotificationChannel(
                CHANNEL_DAILIES,
                context.getString(R.string.channel_dailies_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        dailyChannel.setDescription(context.getString(R.string.channel_dailies_desc));
        dailyChannel.enableVibration(true);
        channels.add(dailyChannel);

        NotificationChannel eventsChannel = new NotificationChannel(
                CHANNEL_EVENTS,
                context.getString(R.string.channel_events_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        eventsChannel.setDescription(context.getString(R.string.channel_events_desc));
        eventsChannel.enableVibration(true);
        channels.add(eventsChannel);

        nm.createNotificationChannels(channels);
    }

    public static void dispatchNotification(Context context) {
        if (context == null) return;
        Preferences preferences = new Preferences(context);

        boolean allowNotifications = preferences.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS);
        if (!allowNotifications) {
            AppLogger.i("DailyNotificationWorker", "Notifications are disabled in preferences. Skipping.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                AppLogger.w("DailyNotificationWorker", "POST_NOTIFICATIONS permission is not granted. Skipping notification.");
                return;
            }
        }

        createNotificationChannels(context);

        if (Looper.myLooper() == Looper.getMainLooper()) {
            AppDatabase.getExecutor().execute(() -> doDispatchNotification(context));
        } else {
            doDispatchNotification(context);
        }
    }

    private static void doDispatchNotification(Context context) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            Preferences preferences = new Preferences(context);
            LocalDate today = LocalDate.now();
            int year = today.getYear();
            int dayOfYear = today.getDayOfYear();
            long epochDay = today.toEpochDay();

            List<GameType> pendingSubGames = new ArrayList<>();
            List<GameType> pendingDailyGames = new ArrayList<>();
            boolean hasAnyTrackedGame = false;

            List<GameType> enabledGames = preferences.getEnabledGames();

            for (GameType game : enabledGames) {
                SubscriptionEntity activeSub = db.subscriptionDao().getActiveSubscription(game.getCode(), epochDay);
                boolean hasActiveSub = (activeSub != null);

                boolean isTracked = hasActiveSub
                        || db.subscriptionDao().getSubscriptionsCountForGame(game.getCode()) > 0
                        || db.calendarDao().getCompletedDailiesCount(game.getCode()) > 0
                        || !db.pullDao().getAllPullsForGame(game.getCode()).isEmpty();

                if (!isTracked) {
                    SavingsEntity savings = db.savingsDao().getSavings(game.getCode());
                    if (savings != null && (savings.target > 0 || savings.manualProgress > 0 || savings.subsProgress > 0 || savings.dailiesProgress > 0)) {
                        isTracked = true;
                    }
                }

                if (isTracked) {
                    hasAnyTrackedGame = true;
                }

                CalendarGameStatusEntity status = db.calendarDao().getGameStatus(year, dayOfYear, game.getCode());

                if (hasActiveSub) {
                    boolean subClaimed = status != null && (status.status == DayState.STATUS_RECEIVED || status.status == DayState.STATUS_OLD_RECEIVED);
                    if (!subClaimed) {
                        pendingSubGames.add(game);
                    }
                }

                if (isTracked) {
                    boolean dailyDone = status != null && status.dailyStatus == 1;
                    if (!dailyDone) {
                        pendingDailyGames.add(game);
                    }
                }
            }

            if (!hasAnyTrackedGame && !enabledGames.isEmpty()) {
                GameType defaultGame = preferences.getDefaultGame();
                if (!enabledGames.contains(defaultGame)) {
                    defaultGame = enabledGames.get(0);
                }
                CalendarGameStatusEntity status = db.calendarDao().getGameStatus(year, dayOfYear, defaultGame.getCode());
                if (status == null || status.dailyStatus != 1) {
                    pendingDailyGames.add(defaultGame);
                }
            }

            if (pendingSubGames.isEmpty() && pendingDailyGames.isEmpty()) {
                AppLogger.i("DailyNotificationWorker", "Smart Silence: all daily tasks and subscriptions for today are already completed. Suppressing notification.");
                return;
            }

            AppLogger.i("DailyNotificationWorker", "Pending tasks found. Subscriptions: " + pendingSubGames.size() + ", Dailies: " + pendingDailyGames.size());

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (!pendingSubGames.isEmpty()) {
                sendSubscriptionNotification(context, notificationManager, pendingSubGames);
            }

            if (!pendingDailyGames.isEmpty()) {
                sendDailyTasksNotification(context, notificationManager, pendingDailyGames);
            }

            AppLogger.i("DailyNotificationWorker", "Daily notifications dispatched successfully.");
        } catch (Throwable t) {
            AppLogger.e("DailyNotificationWorker", "Error evaluating or sending notifications", t);
        }
    }

    private static void sendSubscriptionNotification(Context context, NotificationManagerCompat nm, List<GameType> games) {
        String title = context.getString(R.string.notification_sub_title);
        String text;
        if (games.size() == 1) {
            String gameName = context.getString(games.get(0).getNameResId());
            text = context.getString(R.string.notification_sub_text_game, gameName);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < games.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(context.getString(games.get(i).getNameResId()));
            }
            text = context.getString(R.string.notification_sub_text_games, sb.toString());
        }

        Intent intent = new Intent(context, MainActivityView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_SUBSCRIPTIONS,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SUBSCRIPTIONS)
                .setSmallIcon(R.drawable.icon_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(NOTIFICATION_GROUP_DAILY)
                .setAutoCancel(true);

        try {
            nm.notify(NOTIFICATION_ID_SUBSCRIPTIONS, builder.build());
        } catch (SecurityException e) {
            AppLogger.e("DailyNotificationWorker", "SecurityException posting subscription notification", e);
        }
    }

    private static void sendDailyTasksNotification(Context context, NotificationManagerCompat nm, List<GameType> games) {
        String title = context.getString(R.string.notification_dailies_title);
        String text;
        if (games.size() == 1) {
            String gameName = context.getString(games.get(0).getNameResId());
            text = context.getString(R.string.notification_dailies_text_game, gameName);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < games.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(context.getString(games.get(i).getNameResId()));
            }
            text = context.getString(R.string.notification_dailies_text_games, sb.toString());
        }

        Intent intent = new Intent(context, MainActivityView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_DAILIES,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_DAILIES)
                .setSmallIcon(R.drawable.icon_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(NOTIFICATION_GROUP_DAILY)
                .setAutoCancel(true);

        try {
            nm.notify(NOTIFICATION_ID_DAILIES, builder.build());
        } catch (SecurityException e) {
            AppLogger.e("DailyNotificationWorker", "SecurityException posting dailies notification", e);
        }
    }

    public static void sendEventNotification(Context context, String title, String text) {
        if (context == null) return;
        createNotificationChannels(context);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        Intent intent = new Intent(context, MainActivityView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_EVENTS,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_EVENTS)
                .setSmallIcon(R.drawable.icon_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        try {
            nm.notify(NOTIFICATION_ID_EVENTS, builder.build());
        } catch (SecurityException e) {
            AppLogger.e("DailyNotificationWorker", "SecurityException posting event notification", e);
        }
    }
}
