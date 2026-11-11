package ru.menshovanton.gachapoint.data.repository;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PiggyBankCalculatorTest {

    @Test
    public void deductFromAutoRemainderDoesNotDoubleCountOrIncreaseBalance() {
        PiggyBankRepository.DeductionResult result = PiggyBankRepository.calculateDeduction(
                0, 0,
                0, 90,
                0, 60,
                160,
                60
        );

        assertEquals(0, result.manualPulls);
        assertEquals(0, result.manualGems);
        assertEquals(0, result.subsPulls);
        assertEquals(30, result.subsRemainder);
        assertEquals(0, result.dailiesPulls);
        assertEquals(60, result.dailiesRemainder);
        assertEquals(60, result.spentFromSubsGems);
        assertEquals(0, result.spentFromDailiesGems);

        int totalGems = PiggyBankRepository.calculateTotalGems(
                result.manualPulls,
                result.manualGems,
                result.subsPulls,
                result.dailiesPulls,
                result.subsRemainder + result.dailiesRemainder,
                160
        );
        assertEquals(90, totalGems);

        PiggyBankRepository.AutoProgressResult synced = PiggyBankRepository.calculateAutoProgress(
                90, 60,
                result.spentFromSubsGems, result.spentFromDailiesGems,
                160
        );
        int syncedTotalGems = PiggyBankRepository.calculateTotalGems(
                0, 0,
                synced.subsPulls, synced.dailiesPulls,
                synced.subsRemainder + synced.dailiesRemainder,
                160
        );
        assertEquals(90, syncedTotalGems);
    }

    @Test
    public void deductFromManualWhenAutoRemainderPresentPreservesExactTotal() {
        PiggyBankRepository.DeductionResult result = PiggyBankRepository.calculateDeduction(
                1, 0,
                0, 0,
                0, 60,
                160,
                60
        );

        assertEquals(0, result.manualPulls);
        assertEquals(100, result.manualGems);
        assertEquals(0, result.subsPulls);
        assertEquals(0, result.subsRemainder);
        assertEquals(0, result.dailiesPulls);
        assertEquals(60, result.dailiesRemainder);
        assertEquals(0, result.spentFromSubsGems);
        assertEquals(0, result.spentFromDailiesGems);

        int autoRem = result.subsRemainder + result.dailiesRemainder;
        assertEquals(1, PiggyBankRepository.calculateTotalProgress(
                result.manualPulls, result.manualGems, result.subsPulls, result.dailiesPulls, autoRem, 160));
        assertEquals(0, PiggyBankRepository.calculateEffectiveRemainderGems(result.manualGems, autoRem, 160));
        assertEquals(160, PiggyBankRepository.calculateTotalGems(
                result.manualPulls, result.manualGems, result.subsPulls, result.dailiesPulls, autoRem, 160));
    }

    @Test
    public void deductFromCombinedManualAndAutoExtraPullDoesNotChargeSubs() {
        PiggyBankRepository.DeductionResult result = PiggyBankRepository.calculateDeduction(
                0, 100,
                0, 100,
                0, 0,
                160,
                60
        );

        assertEquals(0, result.manualPulls);
        assertEquals(40, result.manualGems);
        assertEquals(0, result.subsPulls);
        assertEquals(100, result.subsRemainder);
        assertEquals(0, result.spentFromSubsGems);
        assertEquals(0, result.spentFromDailiesGems);

        int autoRem = result.subsRemainder + result.dailiesRemainder;
        assertEquals(140, PiggyBankRepository.calculateTotalGems(
                result.manualPulls, result.manualGems, result.subsPulls, result.dailiesPulls, autoRem, 160));
    }

    @Test
    public void deductPullCombinedFromSubsAndDailiesRemaindersStaysDeductedAfterSync() {
        PiggyBankRepository.DeductionResult result = PiggyBankRepository.calculateDeduction(
                0, 0,
                0, 90,
                0, 100,
                160,
                160
        );

        assertEquals(0, result.subsPulls);
        assertEquals(0, result.subsRemainder);
        assertEquals(0, result.dailiesPulls);
        assertEquals(30, result.dailiesRemainder);
        assertEquals(90, result.spentFromSubsGems);
        assertEquals(70, result.spentFromDailiesGems);

        PiggyBankRepository.AutoProgressResult synced = PiggyBankRepository.calculateAutoProgress(
                90, 100,
                result.spentFromSubsGems, result.spentFromDailiesGems,
                160
        );
        assertEquals(0, synced.subsPulls);
        assertEquals(0, synced.subsRemainder);
        assertEquals(0, synced.dailiesPulls);
        assertEquals(30, synced.dailiesRemainder);
        assertEquals(90, synced.clampedSpentSubsGems);
        assertEquals(70, synced.clampedSpentDailiesGems);
    }

    @Test
    public void deductAcrossAllBucketsAndClampAtZero() {
        PiggyBankRepository.DeductionResult result = PiggyBankRepository.calculateDeduction(
                1, 20,
                1, 90,
                1, 60,
                160,
                1000
        );

        assertEquals(0, result.manualPulls);
        assertEquals(0, result.manualGems);
        assertEquals(0, result.subsPulls);
        assertEquals(0, result.subsRemainder);
        assertEquals(0, result.dailiesPulls);
        assertEquals(0, result.dailiesRemainder);
        assertEquals(250, result.spentFromSubsGems);
        assertEquals(220, result.spentFromDailiesGems);
        assertEquals(0, PiggyBankRepository.calculateTotalGems(0, 0, 0, 0, 0, 160));
    }

    @Test
    public void sequentialDeductionsInHi3MatchExactGemBalance() {
        int pullCost = 280;
        PiggyBankRepository.DeductionResult first = PiggyBankRepository.calculateDeduction(
                0, 40,
                0, 120,
                0, 80,
                pullCost,
                60
        );

        assertEquals(0, first.manualGems);
        assertEquals(100, first.subsRemainder);
        assertEquals(80, first.dailiesRemainder);
        assertEquals(20, first.spentFromSubsGems);
        assertEquals(0, first.spentFromDailiesGems);

        PiggyBankRepository.DeductionResult second = PiggyBankRepository.calculateDeduction(
                first.manualPulls, first.manualGems,
                first.subsPulls, first.subsRemainder,
                first.dailiesPulls, first.dailiesRemainder,
                pullCost,
                140
        );

        assertEquals(0, second.subsRemainder);
        assertEquals(40, second.dailiesRemainder);
        assertEquals(100, second.spentFromSubsGems);
        assertEquals(40, second.spentFromDailiesGems);

        PiggyBankRepository.AutoProgressResult synced = PiggyBankRepository.calculateAutoProgress(
                120, 80,
                first.spentFromSubsGems + second.spentFromSubsGems,
                first.spentFromDailiesGems + second.spentFromDailiesGems,
                pullCost
        );
        assertEquals(0, synced.subsRemainder);
        assertEquals(40, synced.dailiesRemainder);
    }
}
