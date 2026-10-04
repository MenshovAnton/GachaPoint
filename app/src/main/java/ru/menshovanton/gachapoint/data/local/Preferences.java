package ru.menshovanton.gachapoint.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
    public static final String PIGGY_BANK_TARGET_HSR = "Piggy Bank target HSR";
    public static final String PIGGY_BANK_TARGET_ZZZ = "Piggy Bank target ZZZ";

    public static final String PIGGY_BANK_MANUAL_PROGRESS_GENSHIN = "Piggy Bank manual progress Genshin Impact";
    public static final String PIGGY_BANK_MANUAL_PROGRESS_HSR = "Piggy Bank manual progress HSR";
    public static final String PIGGY_BANK_MANUAL_PROGRESS_ZZZ = "Piggy Bank manual progress ZZZ";

    public static final String PIGGY_BANK_SUBS_PROGRESS_GENSHIN = "Piggy Bank subs progress Genshin Impact";
    public static final String PIGGY_BANK_SUBS_PROGRESS_HSR = "Piggy Bank subs progress HSR";
    public static final String PIGGY_BANK_SUBS_PROGRESS_ZZZ = "Piggy Bank subs progress ZZZ";

    public static final String GENSHIN_NEED_MIGRATION = "Genshin need migration";
    public static final String HSR_NEED_MIGRATION = "HSR need migration";
    public static final String ZZZ_NEED_MIGRATION = "ZZZ need migration";

    private final SharedPreferences settings;

    public Preferences(Context context) {
        settings = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
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
        Log.d("Preferences", "saveBooleanPreference: " + key + " = " + value);
    }

    public boolean saveBooleanPreferenceSync(String key, boolean value) {
        return settings.edit()
                .putBoolean(key, value)
                .commit();
    }

    public boolean getBooleanPreference(String key) {
        try {
            boolean value = settings.getBoolean(key, true);
            Log.d("Preferences", "getBooleanPreference: " + key + " = " + value);
            return value;
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
