package ru.menshovanton.gachapoint.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static final String ENABLED_GAMES = "enabled_games";
    public static final String DEFAULT_GAME = "default_game";

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

    public List<GameType> getEnabledGames() {
        String raw = settings.getString(ENABLED_GAMES, null);
        List<GameType> list = new ArrayList<>();
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split(",");
            for (String p : parts) {
                try {
                    int code = Integer.parseInt(p.trim());
                    GameType type = GameType.fromCode(code);
                    if (type != null && !list.contains(type)) {
                        list.add(type);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (list.isEmpty()) {
            list.addAll(Arrays.asList(GameType.values()));
        }
        return list;
    }

    public void setEnabledGames(List<GameType> games) {
        if (games == null || games.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < games.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(games.get(i).getCode());
        }
        settings.edit().putString(ENABLED_GAMES, sb.toString()).apply();
    }

    public boolean isGameEnabled(GameType gameType) {
        if (gameType == null) return false;
        return getEnabledGames().contains(gameType);
    }

    public void setGameEnabled(GameType gameType, boolean enabled) {
        if (gameType == null) return;
        List<GameType> current = new ArrayList<>(getEnabledGames());
        if (enabled) {
            if (!current.contains(gameType)) {
                List<GameType> ordered = new ArrayList<>();
                for (GameType g : GameType.values()) {
                    if (current.contains(g) || g == gameType) {
                        ordered.add(g);
                    }
                }
                setEnabledGames(ordered);
            }
        } else {
            if (current.size() <= 1) {
                return;
            }
            current.remove(gameType);
            setEnabledGames(current);

            if (getDefaultGame() == gameType) {
                setDefaultGame(current.get(0));
            }
        }
    }

    public GameType getDefaultGame() {
        int code = settings.getInt(DEFAULT_GAME, GameType.GENSHIN.getCode());
        GameType type = GameType.fromCode(code);
        List<GameType> enabled = getEnabledGames();
        if (type != null && enabled.contains(type)) {
            return type;
        }
        return !enabled.isEmpty() ? enabled.get(0) : GameType.GENSHIN;
    }

    public void setDefaultGame(GameType gameType) {
        if (gameType == null) return;
        settings.edit().putInt(DEFAULT_GAME, gameType.getCode()).apply();
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

    private static final String PREFIX_PIGGY_SPENT_SUBS = "piggy_bank_spent_subs_";
    private static final String PREFIX_PIGGY_SPENT_DAILIES = "piggy_bank_spent_dailies_";

    public int getPiggyBankSpentSubs(GameType game) {
        if (game == null) return 0;
        return settings.getInt(PREFIX_PIGGY_SPENT_SUBS + game.name(), 0);
    }

    public void savePiggyBankSpentSubs(GameType game, int spent) {
        if (game == null) return;
        settings.edit().putInt(PREFIX_PIGGY_SPENT_SUBS + game.name(), Math.max(0, spent)).apply();
    }

    public int getPiggyBankSpentDailies(GameType game) {
        if (game == null) return 0;
        return settings.getInt(PREFIX_PIGGY_SPENT_DAILIES + game.name(), 0);
    }

    public void savePiggyBankSpentDailies(GameType game, int spent) {
        if (game == null) return;
        settings.edit().putInt(PREFIX_PIGGY_SPENT_DAILIES + game.name(), Math.max(0, spent)).apply();
    }
}
