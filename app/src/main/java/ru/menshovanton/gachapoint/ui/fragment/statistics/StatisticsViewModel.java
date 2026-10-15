package ru.menshovanton.gachapoint.ui.fragment.statistics;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.menshovanton.gachapoint.App;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.data.repository.DatabaseRepository;
import ru.menshovanton.gachapoint.data.repository.PiggyBankRepository;
import ru.menshovanton.gachapoint.di.AppContainer;
import ru.menshovanton.gachapoint.domain.enums.BannerType;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.enums.PityStatus;
import ru.menshovanton.gachapoint.domain.enums.RareType;
import ru.menshovanton.gachapoint.domain.models.BannerInfo;
import ru.menshovanton.gachapoint.domain.models.Pull;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;
import ru.menshovanton.gachapoint.ui.fragment.statistics.model.OverallStatisticsData;
import ru.menshovanton.gachapoint.ui.fragment.statistics.share.StatsReportGenerator;

public class StatisticsViewModel extends AndroidViewModel {

    private final DatabaseRepository databaseRepository;
    private final PiggyBankRepository piggyBankRepository;

    private final MutableLiveData<OverallStatisticsData> statisticsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<GameType> currentGameTypeLiveData = new MutableLiveData<>(GameType.GENSHIN);
    private final SingleLiveEvent<Intent> shareIntentEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> saveToGallerySuccessEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> errorEvent = new SingleLiveEvent<>();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        AppContainer container = ((App) application).getAppContainer();
        this.databaseRepository = container.getDatabaseRepository();
        this.piggyBankRepository = container.getPiggyBankRepository();
    }

    public LiveData<OverallStatisticsData> getStatisticsLiveData() { return statisticsLiveData; }
    public LiveData<Boolean> getIsLoadingLiveData() { return isLoadingLiveData; }
    public LiveData<GameType> getCurrentGameTypeLiveData() { return currentGameTypeLiveData; }
    public SingleLiveEvent<Intent> getShareIntentEvent() { return shareIntentEvent; }
    public SingleLiveEvent<Void> getSaveToGallerySuccessEvent() { return saveToGallerySuccessEvent; }
    public SingleLiveEvent<String> getErrorEvent() { return errorEvent; }

    public void setGameType(GameType gameType) {
        if (gameType != null && gameType != currentGameTypeLiveData.getValue()) {
            currentGameTypeLiveData.setValue(gameType);
            loadStatistics(gameType);
        }
    }

    public void loadStatistics() {
        GameType current = currentGameTypeLiveData.getValue();
        loadStatistics(current != null ? current : GameType.GENSHIN);
    }

    public void loadStatistics(GameType gameType) {
        isLoadingLiveData.setValue(true);

        databaseRepository.getAllPullsForGame(gameType, pulls ->
                databaseRepository.getAllSubscriptions(gameType, subs ->
                        databaseRepository.getCompletedDailiesCount(gameType, dailiesCount -> {
                            OverallStatisticsData stats = processData(gameType, pulls, subs, dailiesCount != null ? dailiesCount : 0);
                            isLoadingLiveData.setValue(false);
                            statisticsLiveData.setValue(stats);
                        })
                )
        );
    }

    private OverallStatisticsData processData(
            GameType gameType,
            List<Pull> pulls,
            List<SubscriptionEntity> subs,
            int completedDailiesCount
    ) {
        Context context = getApplication();
        int totalPulls = (pulls != null) ? pulls.size() : 0;
        long totalCurrencySpent = (long) totalPulls * gameType.getPullCost();

        int c5 = 0;
        int c4 = 0;
        int c3 = 0;
        int win5050 = 0;
        int loss5050 = 0;
        int guaranteed = 0;

        Map<String, Integer> bannerCounts = new HashMap<>();
        Map<String, Integer> currentPities = new HashMap<>();

        // Initialize banner maps
        for (BannerInfo banner : gameType.getBanners()) {
            bannerCounts.put(banner.getDbKey(), 0);
            currentPities.put(banner.getDbKey(), 0);
        }

        List<Integer> fiveStarPities = new ArrayList<>();
        Map<String, Integer> bannerRunningCounters = new HashMap<>();

        if (pulls != null) {
            for (Pull pull : pulls) {
                RareType rare = RareType.from(pull.getDropRare(), context);
                String bKey = pull.getBannerType() != null ? pull.getBannerType() : BannerType.EVENT.getDbKey();

                Integer bCountObj = bannerCounts.get(bKey);
                int bCount = bCountObj != null ? bCountObj : 0;
                bannerCounts.put(bKey, bCount + 1);

                Integer runningPityObj = bannerRunningCounters.get(bKey);
                int runningPity = runningPityObj != null ? runningPityObj : 0;
                runningPity++;

                if (rare == RareType.LEGENDARY) {
                    c5++;
                    fiveStarPities.add(runningPity);
                    runningPity = 0;

                    PityStatus status = pull.getPityStatus();
                    if (status == PityStatus.WIN_50_50) {
                        win5050++;
                    } else if (status == PityStatus.LOSS_50_50) {
                        loss5050++;
                    } else if (status == PityStatus.GUARANTEED) {
                        guaranteed++;
                    }
                } else if (rare == RareType.EPIC) {
                    c4++;
                } else {
                    c3++;
                }

                bannerRunningCounters.put(bKey, runningPity);
            }
        }

        currentPities.putAll(bannerRunningCounters);

        float p5 = (totalPulls > 0) ? (c5 * 100f / totalPulls) : 0f;
        float p4 = (totalPulls > 0) ? (c4 * 100f / totalPulls) : 0f;
        float p3 = (totalPulls > 0) ? (c3 * 100f / totalPulls) : 0f;

        int total5050Decisions = win5050 + loss5050;
        float winrate = (total5050Decisions > 0) ? (win5050 * 100f / total5050Decisions) : 0f;

        float avgPity = 0f;
        int minPity = 0;
        int maxPity = 0;

        if (!fiveStarPities.isEmpty()) {
            int sum = 0;
            minPity = fiveStarPities.get(0);
            maxPity = fiveStarPities.get(0);
            for (int p : fiveStarPities) {
                sum += p;
                if (p < minPity) minPity = p;
                if (p > maxPity) maxPity = p;
            }
            avgPity = (float) sum / fiveStarPities.size();
        }

        int totalSubs = 0;
        int totalSubDays = 0;
        int subClaimed = 0;
        int subMissed = 0;
        int subWaiting = 0;

        if (subs != null) {
            for (SubscriptionEntity s : subs) {
                totalSubs += s.count;
                totalSubDays = (int) (totalSubDays + (s.endDate - s.startDate + 1));
                subClaimed += s.claimed;
                subMissed += s.missed;
                subWaiting += s.wait;
            }
        }

        int totalSubActivity = subClaimed + subMissed;
        float attendanceRate = (totalSubActivity > 0) ? (subClaimed * 100f / totalSubActivity) : 100f;

        int target = piggyBankRepository.getTarget(gameType);
        int totalProgress = piggyBankRepository.getTotalProgress(gameType);
        int manualProgress = piggyBankRepository.getManualProgress(gameType);
        int subsProgress = piggyBankRepository.getSubsProgress(gameType);
        int dailiesProgress = piggyBankRepository.getDailiesProgress(gameType);
        int dailiesCurrency = completedDailiesCount * gameType.getDailyReward();

        return new OverallStatisticsData(
                gameType,
                totalPulls,
                totalCurrencySpent,
                c5, c4, c3,
                p5, p4, p3,
                win5050, loss5050, guaranteed,
                winrate,
                avgPity, minPity, maxPity,
                bannerCounts,
                currentPities,
                totalSubs,
                totalSubDays,
                subClaimed,
                subMissed,
                subWaiting,
                attendanceRate,
                subs != null ? subs : Collections.emptyList(),
                target,
                totalProgress,
                manualProgress,
                subsProgress,
                dailiesProgress,
                completedDailiesCount,
                dailiesCurrency
        );
    }

    public void shareReport(Context context) {
        OverallStatisticsData currentData = statisticsLiveData.getValue();
        if (currentData == null) return;

        StatsReportGenerator.generateAndShare(context, currentData, new StatsReportGenerator.ShareCallback() {
            @Override
            public void onSuccess(Intent shareIntent) {
                shareIntentEvent.setValue(shareIntent);
            }

            @Override
            public void onError(Exception e) {
                errorEvent.setValue(e != null ? e.getMessage() : "Error generating report");
            }
        });
    }

    public void saveReportToGallery(Context context) {
        OverallStatisticsData currentData = statisticsLiveData.getValue();
        if (currentData == null) return;

        StatsReportGenerator.saveToGallery(context, currentData, new StatsReportGenerator.SaveGalleryCallback() {
            @Override
            public void onSuccess() {
                saveToGallerySuccessEvent.call();
            }

            @Override
            public void onError(Exception e) {
                errorEvent.setValue(e != null ? e.getMessage() : "Error saving report to gallery");
            }
        });
    }
}
