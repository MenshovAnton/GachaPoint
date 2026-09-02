package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class PiggyBankRepository {

    private final Preferences preferences;

    public PiggyBankRepository(@NonNull Context context) {
        this.preferences = new Preferences(context);
    }

    private String getManualProgressPrefTag(GameType gameType) {
        switch (gameType) {
            case HSR:
                return Preferences.PIGGY_BANK_MANUAL_PROGRESS_HSR;
            case ZZZ:
                return Preferences.PIGGY_BANK_MANUAL_PROGRESS_ZZZ;
            case GENSHIN:
            default:
                return Preferences.PIGGY_BANK_MANUAL_PROGRESS_GENSHIN;
        }
    }

    private String getSubsProgressPrefTag(GameType gameType) {
        switch (gameType) {
            case HSR:
                return Preferences.PIGGY_BANK_SUBS_PROGRESS_HSR;
            case ZZZ:
                return Preferences.PIGGY_BANK_SUBS_PROGRESS_ZZZ;
            case GENSHIN:
            default:
                return Preferences.PIGGY_BANK_SUBS_PROGRESS_GENSHIN;
        }
    }

    private String getTargetPrefTag(GameType gameType) {
        switch (gameType) {
            case HSR:
                return Preferences.PIGGY_BANK_TARGET_HSR;
            case ZZZ:
                return Preferences.PIGGY_BANK_TARGET_ZZZ;
            case GENSHIN:
            default:
                return Preferences.PIGGY_BANK_TARGET_GENSHIN;
        }
    }

    public int getManualProgress(GameType gameType) {
        return preferences.getIntPreference(getManualProgressPrefTag(gameType));
    }

    public void saveManualProgress(GameType gameType, int progress) {
        preferences.saveIntPreference(getManualProgressPrefTag(gameType), progress);
    }

    public int getSubsProgress(GameType gameType) {
        return preferences.getIntPreference(getSubsProgressPrefTag(gameType));
    }

    public void saveSubsProgress(GameType gameType, int progress) {
        preferences.saveIntPreference(getSubsProgressPrefTag(gameType), progress);
    }

    public void addSubsProgress(GameType gameType, int wishes) {
        if (wishes <= 0) return;
        int current = getSubsProgress(gameType);
        saveSubsProgress(gameType, current + wishes);
    }

    public int getTarget(GameType gameType) {
        return preferences.getIntPreference(getTargetPrefTag(gameType));
    }

    public void saveTarget(GameType gameType, int target) {
        preferences.saveIntPreference(getTargetPrefTag(gameType), target);
    }

    public void reset(GameType gameType) {
        saveTarget(gameType, 0);
        saveManualProgress(gameType, 0);
    }
}