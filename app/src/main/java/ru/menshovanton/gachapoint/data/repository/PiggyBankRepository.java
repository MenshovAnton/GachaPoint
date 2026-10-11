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

    public int getManualProgress(GameType gameType) {
        return preferences.getPiggyBankManualProgress(gameType);
    }

    public void saveManualProgress(GameType gameType, int progress) {
        preferences.savePiggyBankManualProgress(gameType, progress);
    }

    public int getSubsProgress(GameType gameType) {
        return preferences.getPiggyBankSubsProgress(gameType);
    }

    public void saveSubsProgress(GameType gameType, int progress) {
        preferences.savePiggyBankSubsProgress(gameType, progress);
    }

    public void addSubsProgress(GameType gameType, int wishes) {
        if (wishes <= 0) return;
        int current = getSubsProgress(gameType);
        saveSubsProgress(gameType, current + wishes);
    }

    public int getTarget(GameType gameType) {
        return preferences.getPiggyBankTarget(gameType);
    }

    public void saveTarget(GameType gameType, int target) {
        preferences.savePiggyBankTarget(gameType, target);
    }

    public void reset(GameType gameType) {
        saveTarget(gameType, 0);
        saveManualProgress(gameType, 0);
    }
}