package ru.menshovanton.gachapoint.ui.fragment.statistics.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class OverallStatisticsData {

    private final GameType gameType;

    private final int totalPulls;
    private final long totalCurrencySpent;
    private final int fiveStarCount;
    private final int fourStarCount;
    private final int threeStarCount;
    private final float fiveStarPercent;
    private final float fourStarPercent;
    private final float threeStarPercent;
    private final int win5050Count;
    private final int loss5050Count;
    private final int guaranteedCount;
    private final float win5050Rate;
    private final float avg5StarPity;
    private final int min5StarPity;
    private final int max5StarPity;
    private final Map<String, Integer> bannerPullsCount;
    private final Map<String, Integer> currentBannerPities;

    private final int totalSubsPurchased;
    private final int totalSubDays;
    private final int totalSubClaimedCurrency;
    private final int totalSubMissedCurrency;
    private final int totalSubWaitingCurrency;
    private final float subAttendanceRate;
    private final List<SubscriptionEntity> subscriptionHistory;

    private final int targetPulls;
    private final int totalProgressPulls;
    private final int manualPulls;
    private final int subsPulls;
    private final int dailiesPulls;
    private final int completedDailiesCount;
    private final int dailiesCurrencyEarned;

    public OverallStatisticsData(
            GameType gameType,
            int totalPulls,
            long totalCurrencySpent,
            int fiveStarCount,
            int fourStarCount,
            int threeStarCount,
            float fiveStarPercent,
            float fourStarPercent,
            float threeStarPercent,
            int win5050Count,
            int loss5050Count,
            int guaranteedCount,
            float win5050Rate,
            float avg5StarPity,
            int min5StarPity,
            int max5StarPity,
            Map<String, Integer> bannerPullsCount,
            Map<String, Integer> currentBannerPities,
            int totalSubsPurchased,
            int totalSubDays,
            int totalSubClaimedCurrency,
            int totalSubMissedCurrency,
            int totalSubWaitingCurrency,
            float subAttendanceRate,
            List<SubscriptionEntity> subscriptionHistory,
            int targetPulls,
            int totalProgressPulls,
            int manualPulls,
            int subsPulls,
            int dailiesPulls,
            int completedDailiesCount,
            int dailiesCurrencyEarned
    ) {
        this.gameType = gameType;
        this.totalPulls = totalPulls;
        this.totalCurrencySpent = totalCurrencySpent;
        this.fiveStarCount = fiveStarCount;
        this.fourStarCount = fourStarCount;
        this.threeStarCount = threeStarCount;
        this.fiveStarPercent = fiveStarPercent;
        this.fourStarPercent = fourStarPercent;
        this.threeStarPercent = threeStarPercent;
        this.win5050Count = win5050Count;
        this.loss5050Count = loss5050Count;
        this.guaranteedCount = guaranteedCount;
        this.win5050Rate = win5050Rate;
        this.avg5StarPity = avg5StarPity;
        this.min5StarPity = min5StarPity;
        this.max5StarPity = max5StarPity;
        this.bannerPullsCount = bannerPullsCount != null ? bannerPullsCount : Collections.emptyMap();
        this.currentBannerPities = currentBannerPities != null ? currentBannerPities : Collections.emptyMap();
        this.totalSubsPurchased = totalSubsPurchased;
        this.totalSubDays = totalSubDays;
        this.totalSubClaimedCurrency = totalSubClaimedCurrency;
        this.totalSubMissedCurrency = totalSubMissedCurrency;
        this.totalSubWaitingCurrency = totalSubWaitingCurrency;
        this.subAttendanceRate = subAttendanceRate;
        this.subscriptionHistory = subscriptionHistory != null ? subscriptionHistory : Collections.emptyList();
        this.targetPulls = targetPulls;
        this.totalProgressPulls = totalProgressPulls;
        this.manualPulls = manualPulls;
        this.subsPulls = subsPulls;
        this.dailiesPulls = dailiesPulls;
        this.completedDailiesCount = completedDailiesCount;
        this.dailiesCurrencyEarned = dailiesCurrencyEarned;
    }

    public GameType getGameType() { return gameType; }
    public int getTotalPulls() { return totalPulls; }
    public long getTotalCurrencySpent() { return totalCurrencySpent; }
    public int getFiveStarCount() { return fiveStarCount; }
    public int getFourStarCount() { return fourStarCount; }
    public int getThreeStarCount() { return threeStarCount; }
    public float getFiveStarPercent() { return fiveStarPercent; }
    public float getFourStarPercent() { return fourStarPercent; }
    public float getThreeStarPercent() { return threeStarPercent; }
    public int getWin5050Count() { return win5050Count; }
    public int getLoss5050Count() { return loss5050Count; }
    public int getGuaranteedCount() { return guaranteedCount; }
    public float getWin5050Rate() { return win5050Rate; }
    public float getAvg5StarPity() { return avg5StarPity; }
    public int getMin5StarPity() { return min5StarPity; }
    public int getMax5StarPity() { return max5StarPity; }
    public Map<String, Integer> getBannerPullsCount() { return bannerPullsCount; }
    public Map<String, Integer> getCurrentBannerPities() { return currentBannerPities; }

    public int getTotalSubsPurchased() { return totalSubsPurchased; }
    public int getTotalSubDays() { return totalSubDays; }
    public int getTotalSubClaimedCurrency() { return totalSubClaimedCurrency; }
    public int getTotalSubMissedCurrency() { return totalSubMissedCurrency; }
    public int getTotalSubWaitingCurrency() { return totalSubWaitingCurrency; }
    public float getSubAttendanceRate() { return subAttendanceRate; }
    public List<SubscriptionEntity> getSubscriptionHistory() { return subscriptionHistory; }

    public int getTargetPulls() { return targetPulls; }
    public int getTotalProgressPulls() { return totalProgressPulls; }
    public int getManualPulls() { return manualPulls; }
    public int getSubsPulls() { return subsPulls; }
    public int getDailiesPulls() { return dailiesPulls; }
    public int getCompletedDailiesCount() { return completedDailiesCount; }
    public int getDailiesCurrencyEarned() { return dailiesCurrencyEarned; }

    public long getTotalTrackedCurrency() {
        return totalCurrencySpent + ((long) totalProgressPulls * gameType.getPullCost());
    }

    public float getAveragePullsPerFiveStar() {
        return fiveStarCount > 0 ? (float) totalPulls / fiveStarCount : 0f;
    }

    public float getAveragePullsPerFourStar() {
        return fourStarCount > 0 ? (float) totalPulls / fourStarCount : 0f;
    }
}
