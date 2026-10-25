package ru.menshovanton.gachapoint.worker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.menshovanton.gachapoint.data.local.AppLogger;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppLogger.i("BootReceiver", "Boot or system time change received. Rescheduling notifications.");
        NotificationScheduler.scheduleDailyNotification(context, true);
    }
}
