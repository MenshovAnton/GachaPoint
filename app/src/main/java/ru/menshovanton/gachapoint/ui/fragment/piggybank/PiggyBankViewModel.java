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
            calendarRepository.syncDailiesProgress(gameType, this::refreshData);
        }
    }

    public void syncAndRefresh() {
        calendarRepository.syncDailiesProgress(currentGameType, this::refreshData);
    }

    public void refreshData() {
        int totalProgress = repository.getTotalProgress(currentGameType);
        int target = repository.getTarget(currentGameType);
        int remainderGems = repository.getManualGems(currentGameType);
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
        int totalProgress = repository.getTotalProgress(currentGameType);
        int target = repository.getTarget(currentGameType);

        if (target > 0 && totalProgress + wishes > target) {
            goalExceededEvent.call();
            return;
        }

        int manualProgress = repository.getManualProgress(currentGameType);
        repository.saveManualProgress(currentGameType, manualProgress + wishes);
        refreshData();
    }

    public void removeProgress(int wishes) {
        if (wishes <= 0) return;
        int manualProgress = repository.getManualProgress(currentGameType);
        if (manualProgress <= 0) return;
        int diff = Math.min(wishes, manualProgress);
        repository.saveManualProgress(currentGameType, manualProgress - diff);
        refreshData();
    }

    public void addGems(int gems) {
        if (gems <= 0) return;
        int pullCost = currentGameType.getPullCost();
        int currentManualGems = repository.getManualGems(currentGameType);
        int currentManualPulls = repository.getManualProgress(currentGameType);
        int totalNewManualGems = currentManualGems + gems;
        int additionalPulls = totalNewManualGems / pullCost;
        int remainingGems = totalNewManualGems % pullCost;

        int totalProgress = repository.getTotalProgress(currentGameType);
        int target = repository.getTarget(currentGameType);

        if (target > 0 && (totalProgress + additionalPulls > target ||
                (totalProgress + additionalPulls == target && remainingGems > 0))) {
            goalExceededEvent.call();
            return;
        }

        repository.saveManual(currentGameType, currentManualPulls + additionalPulls, remainingGems);
        refreshData();
    }

    public void removeGems(int gems) {
        if (gems <= 0) return;
        int pullCost = currentGameType.getPullCost();
        int currentManualGems = repository.getManualGems(currentGameType);
        int currentManualPulls = repository.getManualProgress(currentGameType);
        int totalManualValue = currentManualPulls * pullCost + currentManualGems;

        if (totalManualValue <= 0) return;
        int remainingValue = Math.max(0, totalManualValue - gems);
        int newPulls = remainingValue / pullCost;
        int newGems = remainingValue % pullCost;

        repository.saveManual(currentGameType, newPulls, newGems);
        refreshData();
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
    }
}
