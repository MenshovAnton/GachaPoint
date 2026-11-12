package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.menshovanton.gachapoint.data.db.entities.SavingsEntity;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class PiggyBankRepository {

    private final DatabaseRepository databaseRepository;
    private final Preferences preferences;
    private final Map<GameType, SavingsEntity> cache = new ConcurrentHashMap<>();

    public PiggyBankRepository(@NonNull Context context) {
        this(context, new DatabaseRepository(context), new Preferences(context));
    }

    public PiggyBankRepository(@NonNull Context context, @NonNull DatabaseRepository databaseRepository) {
        this(context, databaseRepository, new Preferences(context));
    }

    public PiggyBankRepository(@NonNull Context context, @NonNull DatabaseRepository databaseRepository, @NonNull Preferences preferences) {
        this.databaseRepository = databaseRepository;
        this.preferences = preferences;
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

    public int getAutoSubsRemainderGems(GameType gameType) {
        return preferences != null ? preferences.getPiggyBankAutoSubsRemainder(gameType) : 0;
    }

    public int getAutoDailiesRemainderGems(GameType gameType) {
        return preferences != null ? preferences.getPiggyBankAutoDailiesRemainder(gameType) : 0;
    }

    public int getAutoRemainderGems(GameType gameType) {
        if (preferences == null || gameType == null) return 0;
        if (preferences.hasPiggyBankSplitAutoRemainder(gameType)) {
            return preferences.getPiggyBankAutoSubsRemainder(gameType)
                    + preferences.getPiggyBankAutoDailiesRemainder(gameType);
        }
        return preferences.getPiggyBankAutoRemainder(gameType);
    }

    public void saveAutoRemainders(GameType gameType, int subsRemainder, int dailiesRemainder) {
        if (preferences == null || gameType == null) return;
        int safeSubs = Math.max(0, subsRemainder);
        int safeDailies = Math.max(0, dailiesRemainder);
        preferences.savePiggyBankAutoSubsRemainder(gameType, safeSubs);
        preferences.savePiggyBankAutoDailiesRemainder(gameType, safeDailies);
        preferences.savePiggyBankAutoRemainder(gameType, (safeSubs + safeDailies) % gameType.getPullCost());
    }

    public int getEffectiveRemainderGems(GameType gameType) {
        if (gameType == null) return 0;
        return calculateEffectiveRemainderGems(getManualGems(gameType), getAutoRemainderGems(gameType), gameType.getPullCost());
    }

    public int getExtraPullsFromGems(GameType gameType) {
        if (gameType == null) return 0;
        int totalRem = Math.max(0, getManualGems(gameType)) + Math.max(0, getAutoRemainderGems(gameType));
        return totalRem / gameType.getPullCost();
    }

    public int getTotalProgress(GameType gameType) {
        if (gameType == null) return 0;
        return calculateTotalProgress(
                getManualProgress(gameType),
                getManualGems(gameType),
                getSubsProgress(gameType),
                getDailiesProgress(gameType),
                getAutoRemainderGems(gameType),
                gameType.getPullCost()
        );
    }

    public int getTotalGems(GameType gameType) {
        if (gameType == null) return 0;
        return getTotalProgress(gameType) * gameType.getPullCost() + getEffectiveRemainderGems(gameType);
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
        saveAutoRemainders(gameType, 0, 0);
        saveAllProgress(gameType, getManualProgress(gameType), 0, 0, getManualGems(gameType));
    }

    public void reset(GameType gameType) {
        saveAutoRemainders(gameType, 0, 0);
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

    public static int calculateEffectiveRemainderGems(int manualGems, int autoRemainderGems, int pullCost) {
        int safePullCost = Math.max(1, pullCost);
        return (Math.max(0, manualGems) + Math.max(0, autoRemainderGems)) % safePullCost;
    }

    public static int calculateTotalProgress(
            int manualPulls,
            int manualGems,
            int subsPulls,
            int dailiesPulls,
            int autoRemainderGems,
            int pullCost
    ) {
        int safePullCost = Math.max(1, pullCost);
        int extraPulls = (Math.max(0, manualGems) + Math.max(0, autoRemainderGems)) / safePullCost;
        return Math.max(0, manualPulls) + Math.max(0, subsPulls) + Math.max(0, dailiesPulls) + extraPulls;
    }

    public static int calculateTotalGems(
            int manualPulls,
            int manualGems,
            int subsPulls,
            int dailiesPulls,
            int autoRemainderGems,
            int pullCost
    ) {
        int safePullCost = Math.max(1, pullCost);
        int totalProgress = calculateTotalProgress(manualPulls, manualGems, subsPulls, dailiesPulls, autoRemainderGems, safePullCost);
        int rem = calculateEffectiveRemainderGems(manualGems, autoRemainderGems, safePullCost);
        return totalProgress * safePullCost + rem;
    }

    public static AutoProgressResult calculateAutoProgress(
            int totalSubsGems,
            int totalDailiesGems,
            int spentSubsGems,
            int spentDailiesGems,
            int pullCost
    ) {
        int safeTotalSubs = Math.max(0, totalSubsGems);
        int safeTotalDailies = Math.max(0, totalDailiesGems);
        int clampedSpentSubs = Math.min(Math.max(0, spentSubsGems), safeTotalSubs);
        int clampedSpentDailies = Math.min(Math.max(0, spentDailiesGems), safeTotalDailies);

        int availSubs = safeTotalSubs - clampedSpentSubs;
        int availDailies = safeTotalDailies - clampedSpentDailies;

        int safePullCost = Math.max(1, pullCost);
        return new AutoProgressResult(
                availSubs / safePullCost,
                availSubs % safePullCost,
                availDailies / safePullCost,
                availDailies % safePullCost,
                clampedSpentSubs,
                clampedSpentDailies
        );
    }

    public static DeductionResult calculateDeduction(
            int manualPulls,
            int manualGems,
            int subsPulls,
            int subsRemainder,
            int dailiesPulls,
            int dailiesRemainder,
            int pullCost,
            long gemsToRemove
    ) {
        int safePullCost = Math.max(1, pullCost);
        long manualTotal = Math.max(0L, (long) Math.max(0, manualPulls) * safePullCost + Math.max(0, manualGems));
        long subsTotal = Math.max(0L, (long) Math.max(0, subsPulls) * safePullCost + Math.max(0, subsRemainder));
        long dailiesTotal = Math.max(0L, (long) Math.max(0, dailiesPulls) * safePullCost + Math.max(0, dailiesRemainder));
        long total = manualTotal + subsTotal + dailiesTotal;

        if (gemsToRemove <= 0 || total <= 0) {
            return new DeductionResult(
                    (int) (manualTotal / safePullCost),
                    (int) (manualTotal % safePullCost),
                    (int) (subsTotal / safePullCost),
                    (int) (subsTotal % safePullCost),
                    (int) (dailiesTotal / safePullCost),
                    (int) (dailiesTotal % safePullCost),
                    0,
                    0
            );
        }

        long remaining = Math.min(gemsToRemove, total);

        long fromManual = Math.min(remaining, manualTotal);
        manualTotal -= fromManual;
        remaining -= fromManual;

        long fromSubs = Math.min(remaining, subsTotal);
        subsTotal -= fromSubs;
        remaining -= fromSubs;

        long fromDailies = Math.min(remaining, dailiesTotal);
        dailiesTotal -= fromDailies;

        return new DeductionResult(
                (int) (manualTotal / safePullCost),
                (int) (manualTotal % safePullCost),
                (int) (subsTotal / safePullCost),
                (int) (subsTotal % safePullCost),
                (int) (dailiesTotal / safePullCost),
                (int) (dailiesTotal % safePullCost),
                (int) fromSubs,
                (int) fromDailies
        );
    }

    public static ManualAddResult calculateManualAddByPulls(
            int manualPulls,
            int manualGems,
            long totalGems,
            int targetPulls,
            int pullCost,
            int wishesToAdd
    ) {
        int safePullCost = Math.max(1, pullCost);
        int safeManualPulls = Math.max(0, manualPulls);
        int safeManualGems = Math.max(0, manualGems);
        if (wishesToAdd <= 0) {
            return new ManualAddResult(safeManualPulls, safeManualGems, false);
        }

        if (targetPulls > 0) {
            long targetGems = (long) targetPulls * safePullCost;
            if (totalGems >= targetGems) {
                return new ManualAddResult(safeManualPulls, safeManualGems, true);
            }
            long maxAddableGems = targetGems - totalGems;
            long requestedGems = (long) wishesToAdd * safePullCost;
            if (requestedGems > maxAddableGems) {
                long newManualGemsTotal = (long) safeManualGems + maxAddableGems;
                int additionalPulls = (int) (newManualGemsTotal / safePullCost);
                int remainingGems = (int) (newManualGemsTotal % safePullCost);
                return new ManualAddResult(safeManualPulls + additionalPulls, remainingGems, false);
            }
        }

        return new ManualAddResult(safeManualPulls + wishesToAdd, safeManualGems, false);
    }

    public static final class ManualAddResult {
        public final int manualPulls;
        public final int manualGems;
        public final boolean goalAlreadyReached;

        public ManualAddResult(int manualPulls, int manualGems, boolean goalAlreadyReached) {
            this.manualPulls = manualPulls;
            this.manualGems = manualGems;
            this.goalAlreadyReached = goalAlreadyReached;
        }
    }

    public static final class AutoProgressResult {
        public final int subsPulls;
        public final int subsRemainder;
        public final int dailiesPulls;
        public final int dailiesRemainder;
        public final int clampedSpentSubsGems;
        public final int clampedSpentDailiesGems;

        public AutoProgressResult(
                int subsPulls,
                int subsRemainder,
                int dailiesPulls,
                int dailiesRemainder,
                int clampedSpentSubsGems,
                int clampedSpentDailiesGems
        ) {
            this.subsPulls = subsPulls;
            this.subsRemainder = subsRemainder;
            this.dailiesPulls = dailiesPulls;
            this.dailiesRemainder = dailiesRemainder;
            this.clampedSpentSubsGems = clampedSpentSubsGems;
            this.clampedSpentDailiesGems = clampedSpentDailiesGems;
        }
    }

    public static final class DeductionResult {
        public final int manualPulls;
        public final int manualGems;
        public final int subsPulls;
        public final int subsRemainder;
        public final int dailiesPulls;
        public final int dailiesRemainder;
        public final int spentFromSubsGems;
        public final int spentFromDailiesGems;

        public DeductionResult(
                int manualPulls,
                int manualGems,
                int subsPulls,
                int subsRemainder,
                int dailiesPulls,
                int dailiesRemainder,
                int spentFromSubsGems,
                int spentFromDailiesGems
        ) {
            this.manualPulls = manualPulls;
            this.manualGems = manualGems;
            this.subsPulls = subsPulls;
            this.subsRemainder = subsRemainder;
            this.dailiesPulls = dailiesPulls;
            this.dailiesRemainder = dailiesRemainder;
            this.spentFromSubsGems = spentFromSubsGems;
            this.spentFromDailiesGems = spentFromDailiesGems;
        }
    }
}