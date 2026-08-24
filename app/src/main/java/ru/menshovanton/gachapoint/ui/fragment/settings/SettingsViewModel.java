package ru.menshovanton.gachapoint.ui.fragment.settings;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.data.repository.DatabaseRepository;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;
import ru.menshovanton.gachapoint.worker.NotificationScheduler;

public class SettingsViewModel extends AndroidViewModel {

    private final Preferences preferences;

    private final MutableLiveData<Boolean> notificationsEnabled = new MutableLiveData<>();
    private final MutableLiveData<Integer> alarmHour = new MutableLiveData<>();
    private final MutableLiveData<Integer> alarmMinute = new MutableLiveData<>();

    private final SingleLiveEvent<Void> navigateToInfoEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> exportDbEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> toastMessageEvent = new SingleLiveEvent<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.preferences = new Preferences(application);
        loadSettings();
    }

    public LiveData<Boolean> getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public LiveData<Integer> getAlarmHour() {
        return alarmHour;
    }

    public LiveData<Integer> getAlarmMinute() {
        return alarmMinute;
    }

    public LiveData<Void> getNavigateToInfoEvent() {
        return navigateToInfoEvent;
    }

    public LiveData<Void> getExportDbEvent() {
        return exportDbEvent;
    }

    public LiveData<Integer> getToastMessageEvent() {
        return toastMessageEvent;
    }

    public void loadSettings() {
        notificationsEnabled.setValue(preferences.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS));
        alarmHour.setValue(preferences.getIntPreference(Preferences.ALARM_HOURS));
        alarmMinute.setValue(preferences.getIntPreference(Preferences.ALARM_MINUTES));
    }

    public void onNotificationsChanged(boolean allow) {
        preferences.saveBooleanPreference(Preferences.ALLOW_NOTIFICATIONS, allow);
        notificationsEnabled.setValue(allow);

        Context context = getApplication().getApplicationContext();
        if (allow) {
            NotificationScheduler.scheduleDailyNotification(context);
        } else {
            NotificationScheduler.cancelDailyNotification(context);
        }
    }

    public void onTimeSelected(int hour, int minute) {
        preferences.saveIntPreference(Preferences.ALARM_HOURS, hour);
        preferences.saveIntPreference(Preferences.ALARM_MINUTES, minute);

        alarmHour.setValue(hour);
        alarmMinute.setValue(minute);

        Context context = getApplication().getApplicationContext();
        NotificationScheduler.scheduleDailyNotification(context);
    }

    public void onInfoButtonClicked() {
        navigateToInfoEvent.call();
    }

    public void onExportDatabaseClicked() {
        exportDbEvent.call();
    }

    public void writeDatabaseToUri(Uri targetUri) {
        Context context = getApplication().getApplicationContext();

        AppDatabase.getExecutor().execute(() -> {
            try {
                AppDatabase.getInstance(context)
                        .getOpenHelper()
                        .getWritableDatabase()
                        .query("PRAGMA wal_checkpoint(TRUNCATE)")
                        .close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            File dbFile = context.getDatabasePath(DatabaseRepository.DATABASE_NAME);

            if (!dbFile.exists()) {
                toastMessageEvent.postValue(R.string.db_export_failed);
                return;
            }

            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = context.getContentResolver().openOutputStream(targetUri)) {

                if (out == null) {
                    toastMessageEvent.postValue(R.string.db_export_failed);
                    return;
                }

                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                out.flush();

                toastMessageEvent.postValue(R.string.db_export_successful);

            } catch (IOException e) {
                e.printStackTrace();
                toastMessageEvent.postValue(R.string.db_export_failed);
            }
        });
    }
}