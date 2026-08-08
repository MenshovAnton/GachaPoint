package ru.menshovanton.gachapoint.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {
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

    private final SharedPreferences settings;

    public PreferencesHelper(Context context) {
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

    public boolean getBooleanPreference(String key) {
        try {
            return settings.getBoolean(key, true);
        } catch (Exception e) {
            return true;
        }
    }
}
