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

    private static final String PREFIX_PIGGY_TARGET = "Piggy Bank target ";
    private static final String PREFIX_PIGGY_MANUAL = "Piggy Bank manual progress ";
    private static final String PREFIX_PIGGY_SUBS = "Piggy Bank subs progress ";
    private static final String SUFFIX_NEED_MIGRATION = " need migration";

    private final SharedPreferences settings;

    public Preferences(Context context) {
        settings = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public static String getPiggyBankTargetKey(GameType gameType) {
        if (gameType == GameType.GENSHIN) {
            return PIGGY_BANK_TARGET_GENSHIN;
        }
        return PREFIX_PIGGY_TARGET + gameType.getName();
    }

    public static String getPiggyBankManualProgressKey(GameType gameType) {
        if (gameType == GameType.GENSHIN) {
            return PIGGY_BANK_MANUAL_PROGRESS_GENSHIN;
        }
        return PREFIX_PIGGY_MANUAL + gameType.getName();
    }

    public static String getPiggyBankSubsProgressKey(GameType gameType) {
        if (gameType == GameType.GENSHIN) {
            return PIGGY_BANK_SUBS_PROGRESS_GENSHIN;
        }
        return PREFIX_PIGGY_SUBS + gameType.getName();
    }

    public static String getNeedMigrationKey(GameType gameType) {
        return gameType.getName() + SUFFIX_NEED_MIGRATION;
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
        try {
            return settings.getBoolean(key, true);
        } catch (Exception e) {
            return true;
        }
    }

    public void saveStringPreference(String key, String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getStringPreference(String key, String defValue) {
        try {
            return settings.getString(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }
}
