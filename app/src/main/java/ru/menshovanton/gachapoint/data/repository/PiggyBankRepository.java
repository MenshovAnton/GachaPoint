package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.menshovanton.gachapoint.data.db.entities.SavingsEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class PiggyBankRepository {

    private final DatabaseRepository databaseRepository;
    private final Map<GameType, SavingsEntity> cache = new ConcurrentHashMap<>();

    public PiggyBankRepository(@NonNull Context context) {
        this(context, new DatabaseRepository(context));
    }

    public PiggyBankRepository(@NonNull Context context, @NonNull DatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
        initCache();
    }

    private void initCache() {
        for (GameType game : GameType.values()) {
            cache.put(game, new SavingsEntity(game.getCode(), 0, 0, 0, 0, 0));
        }
        loadFromDatabase(null);
    }

    public void loadFromDatabase(Runnable onComplete) {
        databaseRepository.getAllSavings(list -> {
            if (list != null) {
                for (SavingsEntity entity : list) {
                    GameType game = entity.getGameTypeEnum();
                    cache.put(game, entity);
                }
            }
            if (onComplete != null) onComplete.run();
        });
    }

    public int getManualProgress(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        return s != null ? s.manualProgress : 0;
    }

    public void saveManualProgress(GameType gameType, int progress) {
        int safeProgress = Math.max(0, progress);
        SavingsEntity s = cache.get(gameType);
        if (s != null) s.manualProgress = safeProgress;
        databaseRepository.saveSavingsManualProgress(gameType, safeProgress, null);
    }

    public int getManualGems(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        return s != null ? s.manualGems : 0;
    }

    public void saveManualGems(GameType gameType, int gems) {
        int safeGems = Math.max(0, gems);
        SavingsEntity s = cache.get(gameType);
        if (s != null) s.manualGems = safeGems;
        databaseRepository.saveSavingsManualGems(gameType, safeGems, null);
    }

    public void saveManual(GameType gameType, int progress, int gems) {
        int safeProgress = Math.max(0, progress);
        int safeGems = Math.max(0, gems);
        SavingsEntity s = cache.get(gameType);
        if (s != null) {
            s.manualProgress = safeProgress;
            s.manualGems = safeGems;
        }
        databaseRepository.saveSavingsManual(gameType, safeProgress, safeGems, null);
    }

    public int getSubsProgress(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        return s != null ? s.subsProgress : 0;
    }

    public void saveSubsProgress(GameType gameType, int progress) {
        int safeProgress = Math.max(0, progress);
        SavingsEntity s = cache.get(gameType);
        if (s != null) s.subsProgress = safeProgress;
        databaseRepository.saveSavingsSubsProgress(gameType, safeProgress, null);
    }

    public void addSubsProgress(GameType gameType, int wishes) {
        if (wishes <= 0) return;
        int current = getSubsProgress(gameType);
        saveSubsProgress(gameType, current + wishes);
    }

    public int getDailiesProgress(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        return s != null ? s.dailiesProgress : 0;
    }

    public void saveDailiesProgress(GameType gameType, int progress) {
        int safeProgress = Math.max(0, progress);
        SavingsEntity s = cache.get(gameType);
        if (s != null) s.dailiesProgress = safeProgress;
        databaseRepository.saveSavingsDailiesProgress(gameType, safeProgress, null);
    }

    public int getTotalProgress(GameType gameType) {
        return getManualProgress(gameType) + getSubsProgress(gameType) + getDailiesProgress(gameType);
    }

    public int getTotalGems(GameType gameType) {
        if (gameType == null) return 0;
        return getTotalProgress(gameType) * gameType.getPullCost() + getManualGems(gameType);
    }

    public int getTarget(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        return s != null ? s.target : 0;
    }

    public void saveTarget(GameType gameType, int target) {
        int safeTarget = Math.max(0, target);
        SavingsEntity s = cache.get(gameType);
        if (s != null) s.target = safeTarget;
        databaseRepository.saveSavingsTarget(gameType, safeTarget, null);
    }

    public void saveAllProgress(GameType gameType, int manual, int subs, int dailies, int manualGems) {
        int safeManual = Math.max(0, manual);
        int safeSubs = Math.max(0, subs);
        int safeDailies = Math.max(0, dailies);
        int safeGems = Math.max(0, manualGems);
        SavingsEntity s = cache.get(gameType);
        if (s != null) {
            s.manualProgress = safeManual;
            s.subsProgress = safeSubs;
            s.dailiesProgress = safeDailies;
            s.manualGems = safeGems;
        }
        databaseRepository.saveAllSavingsProgress(gameType, safeManual, safeSubs, safeDailies, safeGems, null);
    }

    public void clearAutoProgress(GameType gameType) {
        saveAllProgress(gameType, getManualProgress(gameType), 0, 0, getManualGems(gameType));
    }

    public void reset(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        if (s != null) {
            s.target = 0;
            s.manualProgress = 0;
            s.manualGems = 0;
            s.subsProgress = 0;
            s.dailiesProgress = 0;
        }
        databaseRepository.resetSavings(gameType, null);
    }
}