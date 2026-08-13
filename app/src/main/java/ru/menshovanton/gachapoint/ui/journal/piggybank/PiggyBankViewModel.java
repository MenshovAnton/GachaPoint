package ru.menshovanton.gachapoint.ui.journal.piggybank;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ru.menshovanton.gachapoint.data.repository.PiggyBankRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.common.SingleLiveEvent;

public class PiggyBankViewModel extends AndroidViewModel {

    private final PiggyBankRepository repository;
    private final MutableLiveData<PiggyBankState> stateLiveData = new MutableLiveData<>(new PiggyBankState(0, 0));
    private final SingleLiveEvent<Void> goalExceededEvent = new SingleLiveEvent<>();

    private GameType currentGameType = GameType.GENSHIN;
    private static final int PITY = 77;

    public PiggyBankViewModel(@NonNull Application application) {
        super(application);
        this.repository = new PiggyBankRepository(application);
    }

    public LiveData<PiggyBankState> getStateLiveData() { return stateLiveData; }
    public SingleLiveEvent<Void> getGoalExceededEvent() { return goalExceededEvent; }

    public void setGameType(GameType gameType) {
        if (gameType != null) {
            this.currentGameType = gameType;
            refreshData();
        }
    }

    public void refreshData() {
        int manualProgress = repository.getManualProgress(currentGameType);
        int subsProgress = repository.getSubsProgress(currentGameType);
        int totalProgress = manualProgress + subsProgress;
        int target = repository.getTarget(currentGameType);

        stateLiveData.setValue(new PiggyBankState(totalProgress, target));
    }

    public void addProgress(int wishes) {
        int manualProgress = repository.getManualProgress(currentGameType);
        int subsProgress = repository.getSubsProgress(currentGameType);
        int target = repository.getTarget(currentGameType);

        if (manualProgress + subsProgress + wishes <= target) {
            int newManualProgress = manualProgress + wishes;
            repository.saveManualProgress(currentGameType, newManualProgress);
            refreshData();
        } else {
            goalExceededEvent.call();
        }
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