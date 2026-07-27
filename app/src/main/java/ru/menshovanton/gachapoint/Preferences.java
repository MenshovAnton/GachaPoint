package ru.menshovanton.gachapoint;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    private static final String PREF_FILE = "Settings";
    public static final String ALARM_HOURS = "Alarm Hours";
    public static final String ALARM_MINUTES = "Alarm Minutes";
    public static final String ALLOW_NOTIFICATIONS = "Enable notifications";
    public static final String CALENDAR_SIZE = "Calendar size";
    public static final String SUB_TYPE = "Selected sub type";

    private final SharedPreferences settings;

    Preferences(SplashScreen splashScreen) {
        settings = splashScreen.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    Preferences(MainActivity mainActivity) {
        settings = mainActivity.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
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
