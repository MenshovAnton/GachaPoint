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
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.util.Locale;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.db.DatabaseExporter;
import ru.menshovanton.gachapoint.data.db.DatabaseImporter;
import ru.menshovanton.gachapoint.data.db.MigrationHelper;
import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.JsonDataLoader;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository;
import ru.menshovanton.gachapoint.data.repository.CatalogRepository;
import ru.menshovanton.gachapoint.data.repository.PromocodeRepository;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;
import ru.menshovanton.gachapoint.worker.NotificationScheduler;

public class SettingsViewModel extends AndroidViewModel {

    private final Preferences preferences;

    private final MutableLiveData<Integer> selectedTheme = new MutableLiveData<>();
    private final MutableLiveData<String> selectedLanguage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> vibrationMode = new MutableLiveData<>();

    private final MutableLiveData<Boolean> notificationsEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notifySubscriptions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notifyDailies = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notifyPromocodes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notifyBanners = new MutableLiveData<>();
    private final MutableLiveData<Integer> alarmHour = new MutableLiveData<>();
    private final MutableLiveData<Integer> alarmMinute = new MutableLiveData<>();

    private final SingleLiveEvent<Void> navigateToSettingsEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> exportDbEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> importDbEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> restartAppEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> importErrorEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> toastMessageEvent = new SingleLiveEvent<>();
    private final MutableLiveData<String> cacheSizeText = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.preferences = new Preferences(application);
        loadSettings();
        calculateCacheSize();
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

    public LiveData<Boolean> getNotifySubscriptions() {
        return notifySubscriptions;
    }

    public LiveData<Boolean> getNotifyDailies() {
        return notifyDailies;
    }

    public LiveData<Boolean> getNotifyPromocodes() {
        return notifyPromocodes;
    }

    public LiveData<Boolean> getNotifyBanners() {
        return notifyBanners;
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

    public LiveData<String> getCacheSizeText() {
        return cacheSizeText;
    }

    public void loadSettings() {
        notificationsEnabled.setValue(preferences.getBooleanPreference(Preferences.ALLOW_NOTIFICATIONS));
        notifySubscriptions.setValue(preferences.isNotifySubscriptionsEnabled());
        notifyDailies.setValue(preferences.isNotifyDailiesEnabled());
        notifyPromocodes.setValue(preferences.isNotifyPromocodesEnabled());
        notifyBanners.setValue(preferences.isNotifyBannersEnabled());
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
        preferences.saveIntPreference(Preferences.APP_THEME, mode);
        selectedTheme.setValue(mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public void onLanguageSelected(int position) {
        String langTag;
        switch (position) {
            case 1:
                langTag = "en";
                break;
            case 2:
                langTag = "ru";
                break;
            case 0:
            default:
                langTag = "sys";
                break;
        }

        preferences.saveStringPreference(Preferences.APP_LANGUAGE, langTag);
        selectedLanguage.setValue(langTag);

        if ("sys".equals(langTag)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langTag));
        }
    }

    public void onNotificationsChanged(boolean isEnabled) {
        preferences.saveBooleanPreference(Preferences.ALLOW_NOTIFICATIONS, isEnabled);
        notificationsEnabled.setValue(isEnabled);
        Context context = getApplication().getApplicationContext();
        if (isEnabled) {
            NotificationScheduler.scheduleDailyNotification(context, true);
        } else {
            NotificationScheduler.cancelDailyNotification(context);
        }
    }

    public void onVibrationModeChanged(boolean isEnabled) {
        preferences.saveBooleanPreference(Preferences.VIBRATION_MODE, isEnabled);
        vibrationMode.setValue(isEnabled);
    }

    public void onAlarmTimeChanged(int hour, int minute) {
        preferences.saveIntPreference(Preferences.ALARM_HOURS, hour);
        preferences.saveIntPreference(Preferences.ALARM_MINUTES, minute);
        alarmHour.setValue(hour);
        alarmMinute.setValue(minute);
        Context context = getApplication().getApplicationContext();
        NotificationScheduler.scheduleDailyNotification(context, true);
    }

    public void onTimeSelected(int hour, int minute) {
        onAlarmTimeChanged(hour, minute);
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
                restartAppEvent.call();
            }

            @Override
            public void onError(Exception e) {
                String error = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getMessage();
                importErrorEvent.setValue(error);
            }
        });
    }

    public void calculateCacheSize() {
        Context context = getApplication().getApplicationContext();
        AppDatabase.getExecutor().execute(() -> {
            long jsonBytes = JsonDataLoader.getJsonCacheSize(context);
            long glideBytes = 0;
            File glideDir = new File(context.getCacheDir(), "image_manager_disk_cache");
            if (glideDir.exists() && glideDir.isDirectory()) {
                File[] files = glideDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) glideBytes += f.length();
                    }
                }
            }
            long total = jsonBytes + glideBytes;
            String text;
            if (total < 1024) {
                text = total + " B";
            } else if (total < 1024 * 1024) {
                text = String.format(Locale.getDefault(), "%.1f KB", total / 1024.0);
            } else {
                text = String.format(Locale.getDefault(), "%.1f MB", total / (1024.0 * 1024.0));
            }
            cacheSizeText.postValue(text);
        });
    }

    public void clearAppCache() {
        Context context = getApplication().getApplicationContext();
        AppDatabase.getExecutor().execute(() -> {
            try {
                JsonDataLoader.clearAllCache(context);
                com.bumptech.glide.Glide.get(context).clearDiskCache();
                PromocodeRepository.getInstance(context).getPromoCodes(null, false);
                BannerScheduleRepository.getInstance(context).reload(false);
                CatalogRepository.getInstance(context).reload(false);
                toastMessageEvent.postValue(R.string.settings_cache_cleared_toast);
                calculateCacheSize();
            } catch (Exception e) {
                AppLogger.e("SettingsViewModel", "Error clearing cache", e);
            }
        });
    }

    public void onNotifySubscriptionsChanged(boolean isEnabled) {
        preferences.setNotifySubscriptionsEnabled(isEnabled);
        notifySubscriptions.setValue(isEnabled);
    }

    public void onNotifyDailiesChanged(boolean isEnabled) {
        preferences.setNotifyDailiesEnabled(isEnabled);
        notifyDailies.setValue(isEnabled);
    }

    public void onNotifyPromocodesChanged(boolean isEnabled) {
        preferences.setNotifyPromocodesEnabled(isEnabled);
        notifyPromocodes.setValue(isEnabled);
    }

    public void onNotifyBannersChanged(boolean isEnabled) {
        preferences.setNotifyBannersEnabled(isEnabled);
        notifyBanners.setValue(isEnabled);
    }

    public void resetAllData() {
        Context context = getApplication().getApplicationContext();
        AppDatabase.getExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                SupportSQLiteDatabase rawDb = getRawDb(db);

                preferences.resetUserData();
                MigrationHelper.syncGamesRaw(rawDb);
                MigrationHelper.syncSavingsRaw(context, rawDb);
                MigrationHelper.syncSubscriptionsRaw(rawDb);

                ru.menshovanton.gachapoint.App app = getApplication();
                if (app.getAppContainer() != null) {
                    app.getAppContainer().reload(() -> {});
                }

                toastMessageEvent.postValue(R.string.settings_reset_success_toast);
                AppDatabase.postToMain(restartAppEvent::call);
            } catch (Exception e) {
                AppLogger.e("SettingsViewModel", "Error resetting data", e);
            }
        });
    }

    @NonNull
    private static SupportSQLiteDatabase getRawDb(AppDatabase db) {
        SupportSQLiteDatabase rawDb = db.getOpenHelper().getWritableDatabase();
        rawDb.beginTransaction();
        try {
            rawDb.execSQL("DELETE FROM `pulls`");
            rawDb.execSQL("DELETE FROM `calendar`");
            rawDb.execSQL("DELETE FROM `calendar_game_status`");
            rawDb.execSQL("DELETE FROM `subscriptions`");
            rawDb.execSQL("DELETE FROM `savings`");
            rawDb.execSQL("DELETE FROM `promocode_statuses`");
            rawDb.setTransactionSuccessful();
        } finally {
            rawDb.endTransaction();
        }
        return rawDb;
    }
}
