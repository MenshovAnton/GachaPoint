package ru.menshovanton.gachapoint.worker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.menshovanton.gachapoint.data.local.AppLogger;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            AppLogger.i("BootReceiver", "Boot or system time change received. Rescheduling notifications.");
            NotificationScheduler.scheduleDailyNotification(context, true);
        }
    }
}
