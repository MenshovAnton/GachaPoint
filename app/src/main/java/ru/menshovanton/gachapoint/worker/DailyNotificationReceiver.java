package ru.menshovanton.gachapoint.worker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.Preferences;

public class DailyNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppLogger.i("DailyNotificationReceiver", "Daily notification alarm triggered.");

        Preferences preferences = new Preferences(context);
        boolean enabled = preferences.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS);
        if (enabled) {
            DailyNotificationWorker.dispatchNotification(context);
            NotificationScheduler.scheduleDailyNotification(context, true);
        } else {
            NotificationScheduler.cancelDailyNotification(context);
        }
    }
}
