package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.menshovanton.gachapoint.data.db.entities.SavingsEntity;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class PiggyBankRepository {

    private final Preferences preferences;
    private final DatabaseRepository databaseRepository;
    private final Map<GameType, SavingsEntity> cache = new ConcurrentHashMap<>();

    public PiggyBankRepository(@NonNull Context context) {
        this(context, new DatabaseRepository(context));
    }

    public PiggyBankRepository(@NonNull Context context, @NonNull DatabaseRepository databaseRepository) {
        this.preferences = new Preferences(context);
        this.databaseRepository = databaseRepository;
        initCache();
    }

    private void initCache() {
        for (GameType game : GameType.values()) {
            cache.put(game, new SavingsEntity(
                    game.getCode(),
                    preferences.getPiggyBankTarget(game),
                    preferences.getPiggyBankManualProgress(game),
                    preferences.getPiggyBankSubsProgress(game),
                    preferences.getPiggyBankDailiesProgress(game)
            ));
        }
        loadFromDatabase(null);
    }

    public void loadFromDatabase(Runnable onComplete) {
        databaseRepository.getAllSavings(list -> {
            if (list != null) {
                for (SavingsEntity entity : list) {
                    GameType game = entity.getGameTypeEnum();
                    cache.put(game, entity);
                    preferences.savePiggyBankTarget(game, entity.target);
                    preferences.savePiggyBankManualProgress(game, entity.manualProgress);
                    preferences.savePiggyBankSubsProgress(game, entity.subsProgress);
                    preferences.savePiggyBankDailiesProgress(game, entity.dailiesProgress);
                }
            }
            if (onComplete != null) onComplete.run();
        });
    }

    public int getManualProgress(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        return s != null ? s.manualProgress : preferences.getPiggyBankManualProgress(gameType);
    }

    public void saveManualProgress(GameType gameType, int progress) {
        SavingsEntity s = cache.get(gameType);
        if (s != null) s.manualProgress = progress;
        preferences.savePiggyBankManualProgress(gameType, progress);
        databaseRepository.saveSavingsManualProgress(gameType, progress, null);
    }

    public int getSubsProgress(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        return s != null ? s.subsProgress : preferences.getPiggyBankSubsProgress(gameType);
    }

    public void saveSubsProgress(GameType gameType, int progress) {
        SavingsEntity s = cache.get(gameType);
        if (s != null) s.subsProgress = progress;
        preferences.savePiggyBankSubsProgress(gameType, progress);
        databaseRepository.saveSavingsSubsProgress(gameType, progress, null);
    }

    public void addSubsProgress(GameType gameType, int wishes) {
        if (wishes <= 0) return;
        int current = getSubsProgress(gameType);
        saveSubsProgress(gameType, current + wishes);
    }

    public int getDailiesProgress(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        return s != null ? s.dailiesProgress : preferences.getPiggyBankDailiesProgress(gameType);
    }

    public void saveDailiesProgress(GameType gameType, int progress) {
        SavingsEntity s = cache.get(gameType);
        if (s != null) s.dailiesProgress = progress;
        preferences.savePiggyBankDailiesProgress(gameType, progress);
        databaseRepository.saveSavingsDailiesProgress(gameType, progress, null);
    }

    public int getTotalProgress(GameType gameType) {
        return getManualProgress(gameType) + getSubsProgress(gameType) + getDailiesProgress(gameType);
    }

    public int getTarget(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        return s != null ? s.target : preferences.getPiggyBankTarget(gameType);
    }

    public void saveTarget(GameType gameType, int target) {
        SavingsEntity s = cache.get(gameType);
        if (s != null) s.target = target;
        preferences.savePiggyBankTarget(gameType, target);
        databaseRepository.saveSavingsTarget(gameType, target, null);
    }

    public void reset(GameType gameType) {
        SavingsEntity s = cache.get(gameType);
        if (s != null) {
            s.target = 0;
            s.manualProgress = 0;
        }
        preferences.savePiggyBankTarget(gameType, 0);
        preferences.savePiggyBankManualProgress(gameType, 0);
        databaseRepository.resetSavings(gameType, null);
    }
}