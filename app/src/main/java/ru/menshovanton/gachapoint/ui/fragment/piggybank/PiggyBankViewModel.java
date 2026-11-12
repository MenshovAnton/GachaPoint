package ru.menshovanton.gachapoint.ui.fragment.piggybank;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ru.menshovanton.gachapoint.data.repository.PiggyBankRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class PiggyBankViewModel extends AndroidViewModel {

    private final PiggyBankRepository repository;
    private final ru.menshovanton.gachapoint.data.repository.CalendarRepository calendarRepository;
    private final MutableLiveData<PiggyBankState> stateLiveData = new MutableLiveData<>();
    private final SingleLiveEvent<Void> goalExceededEvent = new SingleLiveEvent<>();

    private GameType currentGameType = GameType.GENSHIN;

    public PiggyBankViewModel(@NonNull Application application) {
        super(application);
        ru.menshovanton.gachapoint.di.AppContainer container = ((ru.menshovanton.gachapoint.App) application).getAppContainer();
        this.repository = container.getPiggyBankRepository();
        this.calendarRepository = container.getCalendarRepository();
    }

    public LiveData<PiggyBankState> getStateLiveData() { return stateLiveData; }
    public SingleLiveEvent<Void> getGoalExceededEvent() { return goalExceededEvent; }

    public GameType getCurrentGameType() {
        return currentGameType;
    }

    public void setGameType(GameType gameType) {
        if (gameType != null) {
            this.currentGameType = gameType;
            refreshData();
            calendarRepository.syncDailiesProgress(gameType, () ->
                    calendarRepository.syncSubsProgress(gameType, this::refreshData));
        }
    }

    public void syncAndRefresh() {
        calendarRepository.syncDailiesProgress(currentGameType, () ->
                calendarRepository.syncSubsProgress(currentGameType, this::refreshData));
    }

    public void refreshData() {
        int totalProgress = repository.getTotalProgress(currentGameType);
        int target = repository.getTarget(currentGameType);
        int remainderGems = repository.getEffectiveRemainderGems(currentGameType);
        int totalGems = repository.getTotalGems(currentGameType);

        PiggyBankState currentState = stateLiveData.getValue();
        if (currentState == null
                || currentState.getProgress() != totalProgress
                || currentState.getTarget() != target
                || currentState.getRemainderGems() != remainderGems
                || currentState.getTotalGems() != totalGems) {
            stateLiveData.setValue(new PiggyBankState(totalProgress, target, remainderGems, totalGems));
        }
    }

    public void addProgress(int wishes) {
        if (wishes <= 0) return;
        PiggyBankRepository.ManualAddResult result = PiggyBankRepository.calculateManualAddByPulls(
                repository.getManualProgress(currentGameType),
                repository.getManualGems(currentGameType),
                repository.getTotalGems(currentGameType),
                repository.getTarget(currentGameType),
                currentGameType.getPullCost(),
                wishes
        );

        if (result.goalAlreadyReached) {
            goalExceededEvent.call();
            return;
        }

        repository.saveManual(currentGameType, result.manualPulls, result.manualGems);
        refreshData();
    }

    public void removeProgress(int wishes) {
        if (wishes <= 0) return;
        int totalProgress = repository.getTotalProgress(currentGameType);
        if (totalProgress <= 0) return;

        int toRemovePulls = Math.min(wishes, totalProgress);
        deductGemsInternal((long) toRemovePulls * currentGameType.getPullCost());
    }

    public void addGems(int gems) {
        if (gems <= 0) return;
        int pullCost = currentGameType.getPullCost();
        long totalGems = repository.getTotalGems(currentGameType);
        int target = repository.getTarget(currentGameType);
        long targetGems = (long) target * pullCost;

        if (target > 0 && totalGems >= targetGems) {
            goalExceededEvent.call();
            return;
        }

        long effectiveGems = (target > 0) ? Math.min(gems, targetGems - totalGems) : gems;
        int currentManualGems = repository.getManualGems(currentGameType);
        int currentManualPulls = repository.getManualProgress(currentGameType);
        long totalNewManualGems = (long) currentManualGems + effectiveGems;
        int additionalPulls = (int) (totalNewManualGems / pullCost);
        int remainingGems = (int) (totalNewManualGems % pullCost);

        repository.saveManual(currentGameType, currentManualPulls + additionalPulls, remainingGems);
        refreshData();
    }

    public void removeGems(int gems) {
        if (gems <= 0) return;
        deductGemsInternal(gems);
    }

    private void deductGemsInternal(long gems) {
        if (gems <= 0) return;
        PiggyBankRepository.DeductionResult result = PiggyBankRepository.calculateDeduction(
                repository.getManualProgress(currentGameType),
                repository.getManualGems(currentGameType),
                repository.getSubsProgress(currentGameType),
                repository.getAutoSubsRemainderGems(currentGameType),
                repository.getDailiesProgress(currentGameType),
                repository.getAutoDailiesRemainderGems(currentGameType),
                currentGameType.getPullCost(),
                gems
        );

        if (result.spentFromSubsGems > 0) {
            calendarRepository.recordSpentSubsGems(currentGameType, result.spentFromSubsGems);
        }
        if (result.spentFromDailiesGems > 0) {
            calendarRepository.recordSpentDailiesGems(currentGameType, result.spentFromDailiesGems);
        }

        repository.saveAutoRemainders(currentGameType, result.subsRemainder, result.dailiesRemainder);
        repository.saveAllProgress(
                currentGameType,
                result.manualPulls,
                result.subsPulls,
                result.dailiesPulls,
                result.manualGems
        );
        refreshData();
    }

    public void clearAutoProgress() {
        repository.clearAutoProgress(currentGameType);
        refreshData();
        calendarRepository.clearAutoProgress(currentGameType, this::refreshData);
    }

    public void setTarget(int target) {
        repository.saveTarget(currentGameType, Math.max(0, target));
        refreshData();
    }

    public void addTargetMultiplier(int multiplier) {
        int currentTarget = repository.getTarget(currentGameType);
        int newTarget = currentTarget + (currentGameType.getDefaultPity() * multiplier);
        repository.saveTarget(currentGameType, newTarget);
        refreshData();
    }

    public void resetTarget() {
        repository.reset(currentGameType);
        refreshData();
        calendarRepository.clearAutoProgress(currentGameType, this::refreshData);
    }
}
