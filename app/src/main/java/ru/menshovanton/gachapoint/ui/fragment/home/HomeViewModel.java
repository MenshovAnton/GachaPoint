package ru.menshovanton.gachapoint.ui.fragment.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ru.menshovanton.gachapoint.domain.enums.DayState;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.data.repository.CalendarRepository;
import ru.menshovanton.gachapoint.data.repository.DatabaseRepository;
import ru.menshovanton.gachapoint.data.repository.PiggyBankRepository;
import ru.menshovanton.gachapoint.domain.enums.BannerType;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.enums.PityStatus;
import ru.menshovanton.gachapoint.domain.enums.RareType;
import ru.menshovanton.gachapoint.domain.models.BannerInfo;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Pull;
import ru.menshovanton.gachapoint.domain.models.Statistic;
import ru.menshovanton.gachapoint.ui.fragment.home.model.HomeWidgetItem;

public class HomeViewModel extends AndroidViewModel {

    public static class HomeSavingsState {
        public final int progress;
        public final int target;
        public final int totalGems;

        public HomeSavingsState(int progress, int target, int totalGems) {
            this.progress = progress;
            this.target = target;
            this.totalGems = totalGems;
        }
    }

    public static class HomePityState {
        public final int pity5Star;
        public final int maxPity;
        public final boolean isGuaranteed;

        public HomePityState(int pity5Star, int maxPity, boolean isGuaranteed) {
            this.pity5Star = pity5Star;
            this.maxPity = maxPity;
            this.isGuaranteed = isGuaranteed;
        }
    }

    private final CalendarRepository calendarRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final DatabaseRepository databaseRepository;
    private final Preferences preferences;

    private final MutableLiveData<GameType> gameTypeLiveData = new MutableLiveData<>(GameType.GENSHIN);
    private final MutableLiveData<Integer> subsCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Statistic> statisticLiveData = new MutableLiveData<>();

    private final MutableLiveData<HomeSavingsState> savingsStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<HomePityState> pityStateLiveData = new MutableLiveData<>();

    private final MutableLiveData<Integer> piggyProgressLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> piggyTargetLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> piggyGemsLiveData = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isDailyDoneTodayLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSubClaimedTodayLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> subDaysRemainingTodayLiveData = new MutableLiveData<>(0);

    private final MutableLiveData<int[]> dailiesMonthProgressLiveData = new MutableLiveData<>(new int[]{0, 30});

    private final MutableLiveData<Integer> pity5StarLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> maxPityLiveData = new MutableLiveData<>(90);
    private final MutableLiveData<Boolean> isGuaranteedLiveData = new MutableLiveData<>(false);

    private final MutableLiveData<List<HomeWidgetItem>> widgetsConfigLiveData = new MutableLiveData<>();

    private final Map<GameType, HomePityState> pityCache = new EnumMap<>(GameType.class);

    private long loadGeneration = 0;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        ru.menshovanton.gachapoint.di.AppContainer container = ((ru.menshovanton.gachapoint.App) application).getAppContainer();
        this.calendarRepository = container.getCalendarRepository();
        this.piggyBankRepository = container.getPiggyBankRepository();
        this.databaseRepository = container.getDatabaseRepository();
        this.preferences = container.getPreferences();

        loadWidgetsConfig();
        loadData();
    }

    public void setGameType(GameType gameType) {
        gameTypeLiveData.setValue(gameType);
        loadData();
    }

    public void loadWidgetsConfig() {
        String configStr = preferences.getHomeWidgetsConfig();
        widgetsConfigLiveData.setValue(HomeWidgetItem.parseConfig(configStr));
    }

    public void saveWidgetsConfig(List<HomeWidgetItem> items) {
        String configStr = HomeWidgetItem.toConfigString(items);
        preferences.saveHomeWidgetsConfig(configStr);
        widgetsConfigLiveData.setValue(items);
    }

    public void resetWidgetsConfig() {
        preferences.saveHomeWidgetsConfig(Preferences.DEFAULT_HOME_WIDGETS);
        widgetsConfigLiveData.setValue(HomeWidgetItem.parseConfig(Preferences.DEFAULT_HOME_WIDGETS));
    }

    public void loadData() {
        GameType gameType = gameTypeLiveData.getValue();
        if (gameType == null) gameType = GameType.GENSHIN;
        final GameType finalGameType = gameType;

        loadGeneration++;
        final long currentGen = loadGeneration;

        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();
        int dayOfYear = today.getDayOfYear();

        int totalProgress = piggyBankRepository.getTotalProgress(gameType);
        int target = piggyBankRepository.getTarget(gameType);
        int totalGems = piggyBankRepository.getTotalGems(gameType);
        int initialProgress = target > 0 ? totalProgress : 0;
        int initialGems = target > 0 ? totalGems : 0;
        savingsStateLiveData.setValue(new HomeSavingsState(initialProgress, target, initialGems));
        piggyTargetLiveData.setValue(target);
        piggyProgressLiveData.setValue(initialProgress);
        piggyGemsLiveData.setValue(initialGems);

        HomePityState cachedPity = pityCache.get(gameType);
        if (cachedPity != null) {
            pityStateLiveData.setValue(cachedPity);
            maxPityLiveData.setValue(cachedPity.maxPity);
            pity5StarLiveData.setValue(cachedPity.pity5Star);
            isGuaranteedLiveData.setValue(cachedPity.isGuaranteed);
        } else {
            BannerInfo bannerInfo = gameType.getBannerInfo(BannerType.EVENT.getDbKey());
            int defaultMaxPity = bannerInfo != null ? bannerInfo.getMaxPity5Star() : 90;
            HomePityState initialPity = new HomePityState(0, defaultMaxPity, false);
            pityStateLiveData.setValue(initialPity);
            maxPityLiveData.setValue(defaultMaxPity);
            pity5StarLiveData.setValue(0);
            isGuaranteedLiveData.setValue(false);
        }

        calendarRepository.init(gameType, currentYear, () -> {
            if (currentGen != loadGeneration || gameTypeLiveData.getValue() != finalGameType) {
                return;
            }
            subsCountLiveData.setValue(calendarRepository.getSubsCount());
            int total = piggyBankRepository.getTotalProgress(finalGameType);
            int updatedTarget = piggyBankRepository.getTarget(finalGameType);
            int updatedGems = piggyBankRepository.getTotalGems(finalGameType);
            int newProgress = updatedTarget > 0 ? total : 0;
            int newGems = updatedTarget > 0 ? updatedGems : 0;
            savingsStateLiveData.setValue(new HomeSavingsState(newProgress, updatedTarget, newGems));
            piggyTargetLiveData.setValue(updatedTarget);
            piggyProgressLiveData.setValue(newProgress);
            piggyGemsLiveData.setValue(newGems);
        });

        calendarRepository.getStatistic(gameType, currentYear, stat -> {
            if (currentGen == loadGeneration && gameTypeLiveData.getValue() == finalGameType) {
                statisticLiveData.setValue(stat);
            }
        });

        calendarRepository.getDayDailyStatus(currentYear, dayOfYear, gameType, status -> {
            if (currentGen == loadGeneration && gameTypeLiveData.getValue() == finalGameType) {
                isDailyDoneTodayLiveData.setValue(status != null && status == 1);
            }
        });

        calendarRepository.getDayStatus(currentYear, dayOfYear, gameType, status -> {
            if (currentGen == loadGeneration && gameTypeLiveData.getValue() == finalGameType) {
                boolean claimed = (status != null) && (status == DayState.STATUS_RECEIVED || status == DayState.STATUS_OLD_RECEIVED);
                isSubClaimedTodayLiveData.setValue(claimed);
            }
        });

        calendarRepository.getDaySubDaysRemaining(currentYear, dayOfYear, gameType, rem -> {
            if (currentGen == loadGeneration && gameTypeLiveData.getValue() == finalGameType) {
                subDaysRemainingTodayLiveData.setValue(rem != null ? rem : 0);
            }
        });

        calendarRepository.getMonthDates(currentYear, currentMonth, gameType, dates -> {
            if (currentGen != loadGeneration || gameTypeLiveData.getValue() != finalGameType) {
                return;
            }
            if (dates != null) {
                int total = dates.size();
                int completed = 0;
                for (Date d : dates) {
                    if (d.dailyStatus == 1) completed++;
                }
                dailiesMonthProgressLiveData.setValue(new int[]{completed, total});
            }
        });

        BannerInfo bannerInfo = gameType.getBannerInfo(BannerType.EVENT.getDbKey());
        int defaultMaxPity = bannerInfo != null ? bannerInfo.getMaxPity5Star() : 90;

        databaseRepository.getPullsByBanner(gameType, BannerType.EVENT.getDbKey(), rawPulls -> {
            if (currentGen != loadGeneration || gameTypeLiveData.getValue() != finalGameType) {
                return;
            }
            calculatePityForHome(rawPulls, finalGameType);
        });
    }

    private void calculatePityForHome(List<Pull> rawPulls, GameType gameType) {
        BannerInfo bannerInfo = gameType.getBannerInfo(BannerType.EVENT.getDbKey());
        int maxPity = bannerInfo != null ? bannerInfo.getMaxPity5Star() : 90;

        if (rawPulls == null || rawPulls.isEmpty()) {
            HomePityState emptyState = new HomePityState(0, maxPity, false);
            pityCache.put(gameType, emptyState);
            pityStateLiveData.setValue(emptyState);
            maxPityLiveData.setValue(maxPity);
            pity5StarLiveData.setValue(0);
            isGuaranteedLiveData.setValue(false);
            return;
        }

        List<Pull> chronological = new ArrayList<>(rawPulls);
        Collections.reverse(chronological);

        int counter5 = 0;
        boolean guaranteed = false;
        boolean bannerHas5050 = bannerInfo != null && bannerInfo.hasFiftyFifty();

        for (Pull pull : chronological) {
            counter5++;
            RareType rareType = RareType.from(pull.getDropRare(), getApplication());
            if (rareType == RareType.LEGENDARY) {
                if (bannerHas5050) {
                    PityStatus status = pull.getPityStatus();
                    if (status == null || status == PityStatus.NONE) {
                        ru.menshovanton.gachapoint.data.repository.CatalogRepository catalogRepo =
                                ru.menshovanton.gachapoint.data.repository.CatalogRepository.getInstance(getApplication());
                        ru.menshovanton.gachapoint.domain.models.CatalogItem item =
                                catalogRepo.findItemByName(gameType, pull.getDropType());
                        boolean isStd = (item != null && item.isStandard())
                                || catalogRepo.isStandardFiveStar(gameType, pull.getDropType());
                        status = isStd ? PityStatus.LOSS_50_50 : (guaranteed ? PityStatus.GUARANTEED : PityStatus.WIN_50_50);
                    }
                    if (status == PityStatus.LOSS_50_50) {
                        guaranteed = true;
                    } else if (status == PityStatus.WIN_50_50 || status == PityStatus.GUARANTEED) {
                        guaranteed = false;
                    }
                }
                counter5 = 0;
            }
        }

        HomePityState state = new HomePityState(counter5, maxPity, bannerHas5050 && guaranteed);
        pityCache.put(gameType, state);
        pityStateLiveData.setValue(state);
        maxPityLiveData.setValue(maxPity);
        pity5StarLiveData.setValue(counter5);
        isGuaranteedLiveData.setValue(bannerHas5050 && guaranteed);
    }

    public void markDailyToday(@Nullable java.util.function.Consumer<Boolean> onComplete) {
        LocalDate today = LocalDate.now();
        GameType gameType = gameTypeLiveData.getValue();
        if (gameType == null) gameType = GameType.GENSHIN;

        Boolean currentDone = isDailyDoneTodayLiveData.getValue();
        boolean newDone = !(currentDone != null && currentDone);
        int newStatus = newDone ? 1 : 0;
        isDailyDoneTodayLiveData.setValue(newDone);

        calendarRepository.setDayDailyStatus(today.getYear(), today.getDayOfYear(), gameType, newStatus, () -> {
            loadData();
            if (onComplete != null) onComplete.accept(newDone);
        });
    }

    public void markSubToday(@Nullable java.util.function.Consumer<Boolean> onComplete) {
        LocalDate today = LocalDate.now();
        GameType gameType = gameTypeLiveData.getValue();
        if (gameType == null) gameType = GameType.GENSHIN;

        Integer rem = subDaysRemainingTodayLiveData.getValue();
        if (rem == null || rem <= 0) {
            if (onComplete != null) onComplete.accept(null);
            return;
        }

        Boolean currentClaimed = isSubClaimedTodayLiveData.getValue();
        boolean newClaimed = !(currentClaimed != null && currentClaimed);
        int newStatus = newClaimed ? DayState.STATUS_RECEIVED : DayState.STATUS_MISSED;
        isSubClaimedTodayLiveData.setValue(newClaimed);

        calendarRepository.setDayStatus(today.getYear(), today.getDayOfYear(), gameType, newStatus, () -> {
            loadData();
            if (onComplete != null) onComplete.accept(newClaimed);
        });
    }

    public LiveData<GameType> getGameTypeLiveData() { return gameTypeLiveData; }
    public LiveData<Integer> getSubsCountLiveData() { return subsCountLiveData; }
    public LiveData<Statistic> getStatisticLiveData() { return statisticLiveData; }
    public LiveData<HomeSavingsState> getSavingsStateLiveData() { return savingsStateLiveData; }
    public LiveData<HomePityState> getPityStateLiveData() { return pityStateLiveData; }
    public LiveData<Integer> getPiggyProgressLiveData() { return piggyProgressLiveData; }
    public LiveData<Integer> getPiggyTargetLiveData() { return piggyTargetLiveData; }
    public LiveData<Integer> getPiggyGemsLiveData() { return piggyGemsLiveData; }

    public LiveData<Boolean> getIsDailyDoneTodayLiveData() { return isDailyDoneTodayLiveData; }
    public LiveData<Boolean> getIsSubClaimedTodayLiveData() { return isSubClaimedTodayLiveData; }
    public LiveData<Integer> getSubDaysRemainingTodayLiveData() { return subDaysRemainingTodayLiveData; }
    public LiveData<int[]> getDailiesMonthProgressLiveData() { return dailiesMonthProgressLiveData; }

    public LiveData<Integer> getPity5StarLiveData() { return pity5StarLiveData; }
    public LiveData<Integer> getMaxPityLiveData() { return maxPityLiveData; }
    public LiveData<Boolean> getIsGuaranteedLiveData() { return isGuaranteedLiveData; }

    public LiveData<List<HomeWidgetItem>> getWidgetsConfigLiveData() { return widgetsConfigLiveData; }
}