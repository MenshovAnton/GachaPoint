package ru.menshovanton.gachapoint.data.repository;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.domain.enums.DayState;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Statistic;
import ru.menshovanton.gachapoint.testutil.InMemoryTrackerTestHarness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CalendarRepositoryTest {

    private InMemoryTrackerTestHarness harness;
    private CalendarRepository repo;
    private PiggyBankRepository piggy;

    @Before
    public void setUp() {
        harness = new InMemoryTrackerTestHarness();
        repo = harness.calendarRepository;
        piggy = harness.piggyBankRepository;
    }

    @Test
    public void addSubscriptionRejectsInvalidInputsAndRespectsMaxLimit() {
        AtomicBoolean res = new AtomicBoolean(true);
        repo.addSubscription(GameType.GENSHIN, 0, LocalDate.now(), res::set);
        assertFalse(res.get());

        res.set(true);
        repo.addSubscription(null, 1, LocalDate.now(), res::set);
        assertFalse(res.get());

        repo.addSubscription(GameType.GENSHIN, 6, LocalDate.now(), res::set);
        assertTrue(res.get());
        assertEquals(6, repo.getSubsCount());

        repo.addSubscription(GameType.GENSHIN, 1, LocalDate.now(), res::set);
        assertFalse(res.get());
        assertEquals(6, repo.getSubsCount());
    }

    @Test
    public void addAndExtendSubscriptionSetsCorrectDurationAndWaitGems() {
        LocalDate today = LocalDate.now();
        AtomicBoolean res = new AtomicBoolean(false);

        repo.addSubscription(GameType.GENSHIN, 1, today, res::set);
        assertTrue(res.get());
        assertEquals(1, repo.getSubsCount());

        AtomicReference<SubscriptionEntity> activeRef = new AtomicReference<>();
        harness.databaseRepository.getActiveSubscription(GameType.GENSHIN, today.toEpochDay(), activeRef::set);
        SubscriptionEntity sub = activeRef.get();
        assertNotNull(sub);
        assertEquals(today.toEpochDay(), sub.startDate);
        assertEquals(today.plusDays(29).toEpochDay(), sub.endDate);
        assertEquals(2700, sub.wait);

        repo.addSubscription(GameType.GENSHIN, 2, today, res::set);
        assertTrue(res.get());
        assertEquals(3, repo.getSubsCount());

        harness.databaseRepository.getActiveSubscription(GameType.GENSHIN, today.toEpochDay(), activeRef::set);
        sub = activeRef.get();
        assertEquals(3, sub.count);
        assertEquals(today.plusDays(89).toEpochDay(), sub.endDate);
        assertEquals(8100, sub.wait);
    }

    @Test
    public void deleteSubscriptionDecrementsCountAndClearsRemovedTailStatuses() {
        LocalDate today = LocalDate.now();
        repo.addSubscription(GameType.GENSHIN, 2, today, null);
        LocalDate secondMonthDay = today.plusDays(35);
        repo.setDayStatus(secondMonthDay.getYear(), secondMonthDay.getDayOfYear(), GameType.GENSHIN, DayState.STATUS_RECEIVED, null);

        AtomicReference<Integer> statusBefore = new AtomicReference<>(-1);
        repo.getDayStatus(secondMonthDay.getYear(), secondMonthDay.getDayOfYear(), GameType.GENSHIN, statusBefore::set);
        assertEquals(DayState.STATUS_RECEIVED, (int) statusBefore.get());

        AtomicBoolean deleted = new AtomicBoolean(false);
        repo.deleteSubscription(GameType.GENSHIN, deleted::set);
        assertTrue(deleted.get());
        assertEquals(1, repo.getSubsCount());

        AtomicReference<Integer> statusAfter = new AtomicReference<>(-1);
        repo.getDayStatus(secondMonthDay.getYear(), secondMonthDay.getDayOfYear(), GameType.GENSHIN, statusAfter::set);
        assertEquals(DayState.STATUS_MISSED, (int) statusAfter.get());

        repo.deleteSubscription(GameType.GENSHIN, deleted::set);
        assertTrue(deleted.get());
        assertEquals(0, repo.getSubsCount());

        deleted.set(true);
        repo.deleteSubscription(GameType.GENSHIN, deleted::set);
        assertFalse(deleted.get());
    }

    @Test
    public void claimsAndMissesStatisticCountsPastMissesAndTodayClaimCorrectly() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(3);
        repo.addSubscription(GameType.GENSHIN, 1, start, null);

        LocalDate dayMinus2 = today.minusDays(2);
        repo.setDayStatus(dayMinus2.getYear(), dayMinus2.getDayOfYear(), GameType.GENSHIN, DayState.STATUS_RECEIVED, null);
        repo.setDayStatus(today.getYear(), today.getDayOfYear(), GameType.GENSHIN, DayState.STATUS_RECEIVED, null);

        AtomicReference<Statistic> statRef = new AtomicReference<>();
        repo.getStatistic(GameType.GENSHIN, today.getYear(), statRef::set);
        Statistic stat = statRef.get();
        assertNotNull(stat);

        assertEquals(2 * 90, stat.claimGems);
        assertEquals(2 * 90, stat.missedGems);
        assertEquals(2700 - 180 - 180, stat.laterGems);
        assertEquals(1, stat.claimWishes);
        assertEquals(1, stat.missedWishes);
        assertEquals(2340 / 160, stat.laterWishes);
    }

    @Test
    public void upcomingSubscriptionStatisticHasZeroClaimedAndZeroMissed() {
        LocalDate futureStart = LocalDate.now().plusDays(5);
        repo.addSubscription(GameType.HSR, 1, futureStart, null);

        AtomicReference<Statistic> statRef = new AtomicReference<>();
        repo.getStatistic(GameType.HSR, futureStart.getYear(), statRef::set);
        Statistic stat = statRef.get();
        assertNotNull(stat);

        assertEquals(0, stat.claimGems);
        assertEquals(0, stat.missedGems);
        assertEquals(2700, stat.laterGems);
        assertEquals(2700 / 160, stat.laterWishes);
    }

    @Test
    public void dailiesAndSubscriptionSyncAutoProgressIntoPiggyBank() {
        LocalDate today = LocalDate.now();
        repo.addSubscription(GameType.GENSHIN, 1, today, null);
        repo.setDayStatus(today.getYear(), today.getDayOfYear(), GameType.GENSHIN, DayState.STATUS_RECEIVED, null);

        List<LocalDate> dailyDates = Arrays.asList(today.minusDays(2), today.minusDays(1), today);
        repo.setDaysDailyStatusBatch(dailyDates, GameType.GENSHIN, 1, null);

        assertEquals(1, piggy.getDailiesProgress(GameType.GENSHIN));
        assertEquals(20, piggy.getAutoDailiesRemainderGems(GameType.GENSHIN));
        assertEquals(0, piggy.getSubsProgress(GameType.GENSHIN));
        assertEquals(90, piggy.getAutoSubsRemainderGems(GameType.GENSHIN));
        assertEquals(110, piggy.getAutoRemainderGems(GameType.GENSHIN));
        assertEquals(1, piggy.getTotalProgress(GameType.GENSHIN));
        assertEquals(270, piggy.getTotalGems(GameType.GENSHIN));
    }

    @Test
    public void spendingFromAutoProgressAndResyncingPreservesDeductedBalance() {
        LocalDate today = LocalDate.now();
        repo.addSubscription(GameType.GENSHIN, 1, today.minusDays(1), null);
        repo.setDayStatus(today.minusDays(1).getYear(), today.minusDays(1).getDayOfYear(), GameType.GENSHIN, DayState.STATUS_RECEIVED, null);
        repo.setDayStatus(today.getYear(), today.getDayOfYear(), GameType.GENSHIN, DayState.STATUS_RECEIVED, null);
        repo.setDayDailyStatus(today.getYear(), today.getDayOfYear(), GameType.GENSHIN, 1, null);

        assertEquals(240, piggy.getTotalGems(GameType.GENSHIN));

        PiggyBankRepository.DeductionResult deduction = PiggyBankRepository.calculateDeduction(
                piggy.getManualProgress(GameType.GENSHIN),
                piggy.getManualGems(GameType.GENSHIN),
                piggy.getSubsProgress(GameType.GENSHIN),
                piggy.getAutoSubsRemainderGems(GameType.GENSHIN),
                piggy.getDailiesProgress(GameType.GENSHIN),
                piggy.getAutoDailiesRemainderGems(GameType.GENSHIN),
                GameType.GENSHIN.getPullCost(),
                100
        );
        repo.recordSpentSubsGems(GameType.GENSHIN, deduction.spentFromSubsGems);
        repo.recordSpentDailiesGems(GameType.GENSHIN, deduction.spentFromDailiesGems);
        piggy.saveAutoRemainders(GameType.GENSHIN, deduction.subsRemainder, deduction.dailiesRemainder);
        piggy.saveAllProgress(GameType.GENSHIN, deduction.manualPulls, deduction.subsPulls, deduction.dailiesPulls, deduction.manualGems);

        assertEquals(140, piggy.getTotalGems(GameType.GENSHIN));

        repo.syncAutoProgress(GameType.GENSHIN, null);
        assertEquals(140, piggy.getTotalGems(GameType.GENSHIN));
    }

    @Test
    public void clearAutoProgressPreventsAlreadyEarnedGemsFromReturningOnSync() {
        LocalDate today = LocalDate.now();
        repo.addSubscription(GameType.HI3, 1, today, null);
        repo.setDayStatus(today.getYear(), today.getDayOfYear(), GameType.HI3, DayState.STATUS_RECEIVED, null);
        repo.setDaysDailyStatusBatch(Arrays.asList(today.minusDays(1), today), GameType.HI3, 1, null);

        assertEquals(140, piggy.getTotalGems(GameType.HI3));

        repo.clearAutoProgress(GameType.HI3);
        piggy.clearAutoProgress(GameType.HI3);
        assertEquals(0, piggy.getTotalGems(GameType.HI3));

        repo.syncAutoProgress(GameType.HI3, null);
        assertEquals(0, piggy.getTotalGems(GameType.HI3));

        repo.setDayDailyStatus(today.minusDays(2).getYear(), today.minusDays(2).getDayOfYear(), GameType.HI3, 1, null);
        assertEquals(40, piggy.getTotalGems(GameType.HI3));
    }

    @Test
    public void getMonthDatesPopulatesSubDaysRemainingForActiveRange() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);
        repo.addSubscription(GameType.GENSHIN, 1, start, null);

        AtomicReference<List<Date>> monthRef = new AtomicReference<>();
        repo.getMonthDates(today.getYear(), today.getMonthValue(), GameType.GENSHIN, monthRef::set);
        List<Date> dates = monthRef.get();
        assertNotNull(dates);
        assertFalse(dates.isEmpty());
        assertEquals(30, dates.get(0).subDaysRemaining);
    }

    @Test
    public void getDailiesStatisticCountsCompletedMissedAndLaterDays() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        if (today.getDayOfMonth() > 1) {
            repo.setDayDailyStatus(firstOfMonth.getYear(), firstOfMonth.getDayOfYear(), GameType.GENSHIN, 1, null);
        }
        repo.setDayDailyStatus(today.getYear(), today.getDayOfYear(), GameType.GENSHIN, 1, null);

        AtomicReference<Statistic> statRef = new AtomicReference<>();
        repo.getDailiesStatistic(GameType.GENSHIN, today.getYear(), today.getMonthValue(), statRef::set);
        Statistic stat = statRef.get();
        assertNotNull(stat);

        int expectedCompleted = today.getDayOfMonth() > 1 ? 2 : 1;
        int expectedMissed = Math.max(0, today.getDayOfMonth() - 2);
        int expectedLater = today.lengthOfMonth() - today.getDayOfMonth();

        assertEquals(expectedCompleted * 60, stat.claimGems);
        assertEquals(expectedMissed * 60, stat.missedGems);
        assertEquals(expectedLater * 60, stat.laterGems);
    }

    @Test
    public void addSubscriptionRejectsOverlappingPastSubAndRecalculatesSeparatePastSub() {
        LocalDate today = LocalDate.now();
        repo.addSubscription(GameType.GENSHIN, 1, today, null);

        AtomicBoolean overlappingRes = new AtomicBoolean(true);
        repo.addSubscription(GameType.GENSHIN, 1, today.minusDays(10), overlappingRes::set);
        assertFalse(overlappingRes.get());
        assertEquals(1, repo.getSubsCount(GameType.GENSHIN));

        LocalDate pastStart = today.minusDays(40);
        repo.setDayStatus(pastStart.getYear(), pastStart.getDayOfYear(), GameType.GENSHIN, DayState.STATUS_RECEIVED, null);

        AtomicBoolean separateRes = new AtomicBoolean(false);
        repo.addSubscription(GameType.GENSHIN, 1, pastStart, separateRes::set);
        assertTrue(separateRes.get());
        assertEquals(1, repo.getSubsCount(GameType.GENSHIN));

        AtomicReference<List<SubscriptionEntity>> allSubsRef = new AtomicReference<>();
        harness.databaseRepository.getAllSubscriptions(GameType.GENSHIN, allSubsRef::set);
        List<SubscriptionEntity> subs = allSubsRef.get();
        assertNotNull(subs);
        assertEquals(2, subs.size());
        SubscriptionEntity pastSub = subs.get(1);
        assertEquals(90, pastSub.claimed);
        assertEquals(29 * 90, pastSub.missed);
        assertEquals(0, pastSub.wait);
    }

    @Test
    public void deleteSubscriptionUpdatesCountAndStatsWhenRemainingEndIsInPast() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(40);
        repo.addSubscription(GameType.GENSHIN, 2, start, null);
        assertEquals(2, repo.getSubsCount(GameType.GENSHIN));

        AtomicBoolean deleted = new AtomicBoolean(false);
        repo.deleteSubscription(GameType.GENSHIN, deleted::set);
        assertTrue(deleted.get());
        assertEquals(0, repo.getSubsCount(GameType.GENSHIN));

        AtomicReference<List<SubscriptionEntity>> allSubsRef = new AtomicReference<>();
        harness.databaseRepository.getAllSubscriptions(GameType.GENSHIN, allSubsRef::set);
        SubscriptionEntity remaining = allSubsRef.get().get(0);
        assertEquals(1, remaining.count);
        assertEquals(0, remaining.wait);
        assertEquals(30 * 90, remaining.missed);
    }

    @Test
    public void subsCountIsTrackedIndependentlyPerGameType() {
        LocalDate today = LocalDate.now();
        repo.addSubscription(GameType.GENSHIN, 2, today, null);
        repo.addSubscription(GameType.HSR, 4, today, null);

        assertEquals(2, repo.getSubsCount(GameType.GENSHIN));
        assertEquals(4, repo.getSubsCount(GameType.HSR));
        assertEquals(0, repo.getSubsCount(GameType.ZZZ));
    }

    @Test
    public void clearAutoProgressInvokesOnCompleteAfterSavingSpentGems() {
        LocalDate today = LocalDate.now();
        repo.addSubscription(GameType.GENSHIN, 1, today, null);
        repo.setDayStatus(today.getYear(), today.getDayOfYear(), GameType.GENSHIN, DayState.STATUS_RECEIVED, null);
        repo.setDayDailyStatus(today.getYear(), today.getDayOfYear(), GameType.GENSHIN, 1, null);

        AtomicBoolean completed = new AtomicBoolean(false);
        repo.clearAutoProgress(GameType.GENSHIN, () -> completed.set(true));
        assertTrue(completed.get());
        assertEquals(90, harness.preferences.getPiggyBankSpentSubsGems(GameType.GENSHIN));
        assertEquals(60, harness.preferences.getPiggyBankSpentDailiesGems(GameType.GENSHIN));
    }
}
