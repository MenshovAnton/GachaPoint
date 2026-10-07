package ru.menshovanton.gachapoint.ui.fragment.settings;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.db.DatabaseExporter;
import ru.menshovanton.gachapoint.data.db.DatabaseImporter;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;
import ru.menshovanton.gachapoint.worker.NotificationScheduler;

public class SettingsViewModel extends AndroidViewModel {

    private final Preferences preferences;

    private final MutableLiveData<Integer> selectedTheme = new MutableLiveData<>();
    private final MutableLiveData<String> selectedLanguage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> vibrationMode = new MutableLiveData<>();

    private final MutableLiveData<Boolean> notificationsEnabled = new MutableLiveData<>();
    private final MutableLiveData<Integer> alarmHour = new MutableLiveData<>();
    private final MutableLiveData<Integer> alarmMinute = new MutableLiveData<>();

    private final SingleLiveEvent<Void> navigateToSettingsEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> exportDbEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> importDbEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> restartAppEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> importErrorEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> toastMessageEvent = new SingleLiveEvent<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.preferences = new Preferences(application);
        loadSettings();
    }

    public LiveData<Integer> getSelectedTheme() {
        return selectedTheme;
    }

    public LiveData<String> getSelectedLanguage() {
        return selectedLanguage;
    }

    public LiveData<Boolean> getVibrationMode() {
        return vibrationMode;
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

    public LiveData<Void> getNavigateToMenuEvent() {
        return navigateToSettingsEvent;
    }
    public LiveData<Void> getExportDbEvent() {
        return exportDbEvent;
    }
    public LiveData<Void> getImportDbEvent() {
        return importDbEvent;
    }

    public LiveData<Void> getRestartAppEvent() {
        return restartAppEvent;
    }

    public LiveData<String> getImportErrorEvent() {
        return importErrorEvent;
    }

    public LiveData<Integer> getToastMessageEvent() {
        return toastMessageEvent;
    }

    public void loadSettings() {
        notificationsEnabled.setValue(preferences.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS));
        alarmHour.setValue(preferences.getIntPreference(Preferences.ALARM_HOURS));
        alarmMinute.setValue(preferences.getIntPreference(Preferences.ALARM_MINUTES));
        selectedTheme.setValue(preferences.getIntPreference(Preferences.APP_THEME));
        selectedLanguage.setValue(preferences.getStringPreference(Preferences.APP_LANGUAGE, "sys"));
        vibrationMode.setValue(preferences.getBooleanPreference(Preferences.VIBRATION_MODE));
    }

    public void onThemeSelected(int position) {
        int mode;
        switch (position) {
            case 1:
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case 2:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case 0:
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }

        if (preferences.getIntPreference(Preferences.APP_THEME) != mode) {
            preferences.saveIntPreference(Preferences.APP_THEME, mode);
            selectedTheme.setValue(mode);
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    public void onLanguageSelected(int position) {
        String langCode;
        switch (position) {
            case 1:
                langCode = "en";
                break;
            case 2:
                langCode = "ru";
                break;
            case 0:
            default:
                langCode = "sys";
                break;
        }

        String currentLang = preferences.getStringPreference(Preferences.APP_LANGUAGE, "sys");
        if (!currentLang.equals(langCode)) {
            preferences.saveStringPreference(Preferences.APP_LANGUAGE, langCode);
            selectedLanguage.setValue(langCode);

            if ("sys".equals(langCode)) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode));
            }
        }
    }

    public void onVibrationModeChanged(boolean mode) {
        preferences.saveBooleanPreference(Preferences.VIBRATION_MODE, mode);
        vibrationMode.setValue(mode);
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

    public void onBackToMenuClicked() {
        navigateToSettingsEvent.call();
    }

    public void onExportDatabaseClicked() {
        exportDbEvent.call();
    }
    public void onImportDatabaseClicked() {
        importDbEvent.call();
    }

    public void writeDatabaseToUri(Uri targetUri) {
        Context context = getApplication().getApplicationContext();
        AppDatabase.getExecutor().execute(() -> {
            boolean ok = DatabaseExporter.export(context, targetUri);
            toastMessageEvent.postValue(ok
                    ? R.string.db_export_successful
                    : R.string.db_export_failed);
        });
    }

    public void importDatabaseFromUri(Uri fileUri) {
        Context context = getApplication().getApplicationContext();
        DatabaseImporter.importDatabase(context, fileUri, new DatabaseImporter.ImportCallback() {
            @Override
            public void onSuccess() {
                toastMessageEvent.setValue(R.string.db_import_successful);

                preferences.saveBooleanPreference(Preferences.GENSHIN_NEED_MIGRATION, true);
                preferences.saveBooleanPreference(Preferences.HSR_NEED_MIGRATION, true);
                preferences.saveBooleanPreference(Preferences.ZZZ_NEED_MIGRATION, true);

                restartAppEvent.call();
            }

            @Override
            public void onError(Exception e) {
                String error = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getMessage();
                importErrorEvent.setValue(error);
            }
        });
    }
}