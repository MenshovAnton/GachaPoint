package ru.menshovanton.gachapoint.data.repository;

import org.junit.Before;
import org.junit.Test;

import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.testutil.InMemoryTrackerTestHarness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PiggyBankRepositoryTest {

    private InMemoryTrackerTestHarness harness;
    private PiggyBankRepository repo;

    @Before
    public void setUp() {
        harness = new InMemoryTrackerTestHarness();
        repo = harness.piggyBankRepository;
    }

    @Test
    public void manualProgressAndGemsAreClampedAtZeroAndSummedWithAutoRemainder() {
        repo.saveManual(GameType.GENSHIN, -5, -10);
        assertEquals(0, repo.getManualProgress(GameType.GENSHIN));
        assertEquals(0, repo.getManualGems(GameType.GENSHIN));

        repo.saveManual(GameType.GENSHIN, 2, 100);
        repo.saveAutoRemainders(GameType.GENSHIN, 90, 60);

        assertEquals(150, repo.getAutoRemainderGems(GameType.GENSHIN));
        assertEquals(1, repo.getExtraPullsFromGems(GameType.GENSHIN));
        assertEquals(90, repo.getEffectiveRemainderGems(GameType.GENSHIN));
        assertEquals(3, repo.getTotalProgress(GameType.GENSHIN));
        assertEquals(3 * 160 + 90, repo.getTotalGems(GameType.GENSHIN));
    }

    @Test
    public void hi3Uses280PullCostAcrossTotalAndRemainderCalculations() {
        repo.saveTarget(GameType.HI3, 10);
        repo.saveManual(GameType.HI3, 1, 200);
        repo.saveAutoRemainders(GameType.HI3, 60, 40);

        assertEquals(10, repo.getTarget(GameType.HI3));
        assertEquals(100, repo.getAutoRemainderGems(GameType.HI3));
        assertEquals(1, repo.getExtraPullsFromGems(GameType.HI3));
        assertEquals(20, repo.getEffectiveRemainderGems(GameType.HI3));
        assertEquals(2, repo.getTotalProgress(GameType.HI3));
        assertEquals(2 * 280 + 20, repo.getTotalGems(GameType.HI3));
    }

    @Test
    public void legacyAutoRemainderIsUsedWhenSplitRemaindersAbsent() {
        harness.preferences.savePiggyBankAutoRemainder(GameType.HSR, 120);

        assertEquals(0, repo.getAutoSubsRemainderGems(GameType.HSR));
        assertEquals(120, repo.getAutoDailiesRemainderGems(GameType.HSR));
        assertEquals(120, repo.getAutoRemainderGems(GameType.HSR));
        assertEquals(120, repo.getTotalGems(GameType.HSR));
    }

    @Test
    public void addSubsProgressIgnoresNonPositiveAndAccumulatesPositive() {
        repo.addSubsProgress(GameType.ZZZ, 0);
        repo.addSubsProgress(GameType.ZZZ, -3);
        assertEquals(0, repo.getSubsProgress(GameType.ZZZ));

        repo.addSubsProgress(GameType.ZZZ, 2);
        repo.addSubsProgress(GameType.ZZZ, 3);
        assertEquals(5, repo.getSubsProgress(GameType.ZZZ));
    }

    @Test
    public void clearAutoProgressPreservesManualProgressAndTarget() {
        repo.saveTarget(GameType.GENSHIN, 50);
        repo.saveAllProgress(GameType.GENSHIN, 10, 4, 3, 80);
        repo.saveAutoRemainders(GameType.GENSHIN, 90, 60);

        repo.clearAutoProgress(GameType.GENSHIN);

        assertEquals(50, repo.getTarget(GameType.GENSHIN));
        assertEquals(10, repo.getManualProgress(GameType.GENSHIN));
        assertEquals(80, repo.getManualGems(GameType.GENSHIN));
        assertEquals(0, repo.getSubsProgress(GameType.GENSHIN));
        assertEquals(0, repo.getDailiesProgress(GameType.GENSHIN));
        assertEquals(0, repo.getAutoRemainderGems(GameType.GENSHIN));
        assertEquals(10, repo.getTotalProgress(GameType.GENSHIN));
        assertEquals(10 * 160 + 80, repo.getTotalGems(GameType.GENSHIN));
    }

    @Test
    public void resetClearsAllFieldsIncludingTargetAndManual() {
        repo.saveTarget(GameType.GENSHIN, 90);
        repo.saveAllProgress(GameType.GENSHIN, 10, 4, 3, 80);
        repo.saveAutoRemainders(GameType.GENSHIN, 90, 60);

        repo.reset(GameType.GENSHIN);

        assertEquals(0, repo.getTarget(GameType.GENSHIN));
        assertEquals(0, repo.getManualProgress(GameType.GENSHIN));
        assertEquals(0, repo.getManualGems(GameType.GENSHIN));
        assertEquals(0, repo.getSubsProgress(GameType.GENSHIN));
        assertEquals(0, repo.getDailiesProgress(GameType.GENSHIN));
        assertEquals(0, repo.getAutoRemainderGems(GameType.GENSHIN));
        assertEquals(0, repo.getTotalProgress(GameType.GENSHIN));
        assertEquals(0, repo.getTotalGems(GameType.GENSHIN));
    }

    @Test
    public void calculateAutoProgressHandlesNegativeAndZeroInputsSafely() {
        PiggyBankRepository.AutoProgressResult res = PiggyBankRepository.calculateAutoProgress(
                -100, -50, -10, 200, 0
        );
        assertEquals(0, res.subsPulls);
        assertEquals(0, res.subsRemainder);
        assertEquals(0, res.dailiesPulls);
        assertEquals(0, res.dailiesRemainder);
        assertEquals(0, res.clampedSpentSubsGems);
        assertEquals(0, res.clampedSpentDailiesGems);
    }

    @Test
    public void calculateDeductionWithZeroOrNegativeRemovalLeavesBucketsIntact() {
        PiggyBankRepository.DeductionResult res = PiggyBankRepository.calculateDeduction(
                2, 50,
                1, 90,
                1, 60,
                160,
                0
        );
        assertEquals(2, res.manualPulls);
        assertEquals(50, res.manualGems);
        assertEquals(1, res.subsPulls);
        assertEquals(90, res.subsRemainder);
        assertEquals(1, res.dailiesPulls);
        assertEquals(60, res.dailiesRemainder);
        assertEquals(0, res.spentFromSubsGems);
        assertEquals(0, res.spentFromDailiesGems);
    }

    @Test
    public void calculateManualAddByPullsClampsToTargetGemsWhenRemainderPresent() {
        PiggyBankRepository.ManualAddResult clamped = PiggyBankRepository.calculateManualAddByPulls(
                9, 0,
                9 * 160 + 120,
                10,
                160,
                1
        );
        assertFalse(clamped.goalAlreadyReached);
        assertEquals(9, clamped.manualPulls);
        assertEquals(40, clamped.manualGems);

        PiggyBankRepository.ManualAddResult alreadyReached = PiggyBankRepository.calculateManualAddByPulls(
                10, 0,
                10 * 160,
                10,
                160,
                1
        );
        assertTrue(alreadyReached.goalAlreadyReached);

        PiggyBankRepository.ManualAddResult noTarget = PiggyBankRepository.calculateManualAddByPulls(
                3, 50,
                3 * 160 + 50,
                0,
                160,
                2
        );
        assertFalse(noTarget.goalAlreadyReached);
        assertEquals(5, noTarget.manualPulls);
        assertEquals(50, noTarget.manualGems);
    }
}
