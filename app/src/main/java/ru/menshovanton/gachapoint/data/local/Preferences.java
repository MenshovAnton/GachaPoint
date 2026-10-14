package ru.menshovanton.gachapoint.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import ru.menshovanton.gachapoint.domain.enums.GameType;

public class Preferences {
    public static final String APP_THEME = "App Theme";
    public static final String APP_LANGUAGE = "App Language";
    public static final String VIBRATION_MODE = "Vibration Mode";

    private static final String PREF_FILE = "Settings";

    public static final String ALARM_HOURS = "Alarm Hours";
    public static final String ALARM_MINUTES = "Alarm Minutes";
    public static final String ALLOW_NOTIFICATIONS = "Enable notifications";

    public static final String CALENDAR_SIZE = "Calendar size";
    public static final String SUB_TYPE = "Selected sub type";

    public static final String PIGGY_BANK_TARGET_GENSHIN = "Piggy Bank target Genshin Impact";
    public static final String PIGGY_BANK_MANUAL_PROGRESS_GENSHIN = "Piggy Bank manual progress Genshin Impact";
    public static final String PIGGY_BANK_SUBS_PROGRESS_GENSHIN = "Piggy Bank subs progress Genshin Impact";
    public static final String PIGGY_BANK_DAILIES_PROGRESS_GENSHIN = "Piggy Bank dailies progress Genshin Impact";

    private static final String PREFIX_PIGGY_TARGET = "Piggy Bank target ";
    private static final String PREFIX_PIGGY_MANUAL = "Piggy Bank manual progress ";
    private static final String PREFIX_PIGGY_SUBS = "Piggy Bank subs progress ";
    private static final String PREFIX_PIGGY_DAILIES = "Piggy Bank dailies progress ";
    private static final String SUFFIX_NEED_MIGRATION = " need migration";

    public static final String HOME_WIDGETS_CONFIG = "home_widgets_config";
    public static final String DEFAULT_HOME_WIDGETS = "quick_actions:true,savings:true,statistics:true,pity:true";

    private final SharedPreferences settings;

    public Preferences(Context context) {
        settings = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public static String getPiggyBankTargetKey(GameType gameType) {
        return (gameType != null ? gameType : GameType.GENSHIN).getPiggyBankTargetKey();
    }

    public static String getPiggyBankManualProgressKey(GameType gameType) {
        return (gameType != null ? gameType : GameType.GENSHIN).getPiggyBankManualProgressKey();
    }

    public static String getPiggyBankSubsProgressKey(GameType gameType) {
        return (gameType != null ? gameType : GameType.GENSHIN).getPiggyBankSubsProgressKey();
    }

    public static String getPiggyBankDailiesProgressKey(GameType gameType) {
        return (gameType != null ? gameType : GameType.GENSHIN).getPiggyBankDailiesProgressKey();
    }

    public static String getNeedMigrationKey(GameType gameType) {
        return (gameType != null ? gameType : GameType.GENSHIN).getNeedMigrationKey();
    }

    public int getPiggyBankTarget(GameType gameType) {
        return getIntPreference(getPiggyBankTargetKey(gameType));
    }

    public void savePiggyBankTarget(GameType gameType, int value) {
        saveIntPreference(getPiggyBankTargetKey(gameType), value);
    }

    public int getPiggyBankManualProgress(GameType gameType) {
        return getIntPreference(getPiggyBankManualProgressKey(gameType));
    }

    public void savePiggyBankManualProgress(GameType gameType, int value) {
        saveIntPreference(getPiggyBankManualProgressKey(gameType), value);
    }

    public int getPiggyBankSubsProgress(GameType gameType) {
        return getIntPreference(getPiggyBankSubsProgressKey(gameType));
    }

    public void savePiggyBankSubsProgress(GameType gameType, int value) {
        saveIntPreference(getPiggyBankSubsProgressKey(gameType), value);
    }

    public int getPiggyBankDailiesProgress(GameType gameType) {
        return getIntPreference(getPiggyBankDailiesProgressKey(gameType));
    }

    public void savePiggyBankDailiesProgress(GameType gameType, int value) {
        saveIntPreference(getPiggyBankDailiesProgressKey(gameType), value);
    }

    public boolean getNeedMigration(GameType gameType) {
        return getBooleanPreference(getNeedMigrationKey(gameType));
    }

    public void saveNeedMigration(GameType gameType, boolean value) {
        saveBooleanPreference(getNeedMigrationKey(gameType), value);
    }

    public void saveIntPreference(String key, int value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public int getIntPreference(String key) {
        int defValue;
        if (key.equals(ALARM_HOURS)) {
            defValue = 12;
        } else if (key.equals(APP_THEME)) {
            defValue = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        } else {
            defValue = 0;
        }

        try {
            return settings.getInt(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    public void saveBooleanPreference(String key, boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean saveBooleanPreferenceSync(String key, boolean value) {
        return settings.edit()
                .putBoolean(key, value)
                .commit();
    }

    public boolean getBooleanPreference(String key) {
        return getBooleanPreference(key, true);
    }

    public boolean getBooleanPreference(String key, boolean defValue) {
        try {
            return settings.getBoolean(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    public void saveStringPreference(String key, String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getHomeWidgetsConfig() {
        return getStringPreference(HOME_WIDGETS_CONFIG, DEFAULT_HOME_WIDGETS);
    }

    public void saveHomeWidgetsConfig(String config) {
        saveStringPreference(HOME_WIDGETS_CONFIG, config);
    }

    public String getStringPreference(String key, String defValue) {
        try {
            return settings.getString(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }
}
