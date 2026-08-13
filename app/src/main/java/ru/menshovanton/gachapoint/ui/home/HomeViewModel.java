package ru.menshovanton.gachapoint.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;

import ru.menshovanton.gachapoint.data.repository.CalendarRepository;
import ru.menshovanton.gachapoint.data.repository.PiggyBankRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Statistic;

public class HomeViewModel extends AndroidViewModel {

    private final CalendarRepository calendarRepository;
    private final PiggyBankRepository piggyBankRepository;

    private final MutableLiveData<GameType> gameTypeLiveData = new MutableLiveData<>(GameType.GENSHIN);
    private final MutableLiveData<Integer> subsCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Statistic> statisticLiveData = new MutableLiveData<>();

    private final MutableLiveData<Integer> piggyProgressLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> piggyTargetLiveData = new MutableLiveData<>(100);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        calendarRepository = new CalendarRepository(application);
        piggyBankRepository = new PiggyBankRepository(application);

        loadData();
    }

    public void setGameType(GameType gameType) {
        gameTypeLiveData.setValue(gameType);
        loadData();
    }

    public void loadData() {
        GameType gameType = gameTypeLiveData.getValue();
        if (gameType == null) gameType = GameType.GENSHIN;
        int currentYear = LocalDate.now().getYear();

        calendarRepository.init(gameType, currentYear, () -> subsCountLiveData.setValue(calendarRepository.getSubsCount()));

        calendarRepository.getStatistic(gameType, currentYear, statisticLiveData::setValue);

        int manualProgress = piggyBankRepository.getManualProgress(gameType);
        int subsProgress = piggyBankRepository.getSubsProgress(gameType);
        int totalProgress = manualProgress + subsProgress;
        int target = piggyBankRepository.getTarget(gameType);

        piggyProgressLiveData.setValue(totalProgress);
        piggyTargetLiveData.setValue(target);
    }

    public LiveData<GameType> getGameTypeLiveData() { return gameTypeLiveData; }
    public LiveData<Integer> getSubsCountLiveData() { return subsCountLiveData; }
    public LiveData<Statistic> getStatisticLiveData() { return statisticLiveData; }
    public LiveData<Integer> getPiggyProgressLiveData() { return piggyProgressLiveData; }
    public LiveData<Integer> getPiggyTargetLiveData() { return piggyTargetLiveData; }
}