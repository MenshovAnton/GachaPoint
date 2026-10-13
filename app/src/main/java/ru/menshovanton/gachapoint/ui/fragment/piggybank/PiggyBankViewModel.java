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
    private final MutableLiveData<PiggyBankState> stateLiveData = new MutableLiveData<>(new PiggyBankState(0, 0));
    private final SingleLiveEvent<Void> goalExceededEvent = new SingleLiveEvent<>();

    private GameType currentGameType = GameType.GENSHIN;
    private static final int PITY = 77;

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

        stateLiveData.setValue(new PiggyBankState(totalProgress, target));
    }

    public void addProgress(int wishes) {
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
        int totalProgress = repository.getTotalProgress(currentGameType);
        if (totalProgress <= 0) return;
        int diff = Math.min(wishes, totalProgress);
        int manualProgress = repository.getManualProgress(currentGameType);
        repository.saveManualProgress(currentGameType, manualProgress - diff);
        refreshData();
    }

    public void setTarget(int target) {
        repository.saveTarget(currentGameType, Math.max(0, target));
        refreshData();
    }

    public void addTargetMultiplier(int multiplier) {
        int currentTarget = repository.getTarget(currentGameType);
        int newTarget = currentTarget + (PITY * multiplier);
        repository.saveTarget(currentGameType, newTarget);
        refreshData();
    }

    public void resetTarget() {
        repository.reset(currentGameType);
        refreshData();
    }
}