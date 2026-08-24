package ru.menshovanton.gachapoint.ui.fragment.journal.pullscounter;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.repository.DatabaseRepository;
import ru.menshovanton.gachapoint.domain.enums.BannerType;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Pull;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class PullsCounterViewModel extends AndroidViewModel {

    private final DatabaseRepository databaseRepository;

    private final MutableLiveData<List<Pull>> wishesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentPityLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<String> currentBannerTypeLiveData = new MutableLiveData<>(BannerType.EVENT.getDbKey());

    private final SingleLiveEvent<Void> openDialogEvent = new SingleLiveEvent<>();

    private GameType currentGameType = GameType.GENSHIN;

    public PullsCounterViewModel(@NonNull Application application) {
        super(application);
        this.databaseRepository = new DatabaseRepository(application);
    }

    public LiveData<List<Pull>> getWishesLiveData() { return wishesLiveData; }
    public LiveData<Integer> getCurrentPityLiveData() { return currentPityLiveData; }
    public LiveData<String> getCurrentBannerTypeLiveData() { return currentBannerTypeLiveData; }
    public SingleLiveEvent<Void> getOpenDialogEvent() { return openDialogEvent; }

    public void setGameType(GameType gameType) {
        if (gameType != null) {
            this.currentGameType = gameType;
            refreshData();
        }
    }

    public void setBannerType(String bannerTypeKey) {
        if (bannerTypeKey != null && !bannerTypeKey.equals(currentBannerTypeLiveData.getValue())) {
            currentBannerTypeLiveData.setValue(bannerTypeKey);
            refreshData();
        }
    }

    public String getCurrentBannerType() {
        String key = currentBannerTypeLiveData.getValue();
        return key != null ? key : BannerType.EVENT.getDbKey();
    }

    public void refreshData() {
        databaseRepository.getPullsByBanner(currentGameType, getCurrentBannerType(), rawWishes -> {
            List<Pull> processedList = processWishesAndCalculatePity(rawWishes);
            wishesLiveData.setValue(processedList);
        });
    }

    private List<Pull> processWishesAndCalculatePity(List<Pull> pull) {
        if (pull == null || pull.isEmpty()) {
            currentPityLiveData.setValue(0);
            return new ArrayList<>();
        }

        List<Pull> listToProcess = new ArrayList<>(pull);
        Collections.reverse(listToProcess);

        int counter = 0;
        for (Pull wish : listToProcess) {
            counter++;
            wish.setPityNumber(counter);
            if (wish.isResetPity()) {
                counter = 0;
            }
        }

        Collections.reverse(listToProcess);
        currentPityLiveData.setValue(counter);
        return listToProcess;
    }

    public void addOneAttempt() {
        String threeStar = getApplication().getString(R.string.three_star);
        String defaultContent = getApplication().getString(R.string.default_pull_content);

        databaseRepository.addPulls(
                LocalDate.now().toString(),
                defaultContent,
                threeStar,
                1,
                currentGameType,
                getCurrentBannerType(),
                false,
                this::refreshData
        );
    }

    public void addTenAttempts() {
        String threeStar = getApplication().getString(R.string.three_star);
        String defaultContent = getApplication().getString(R.string.default_pull_content);

        databaseRepository.addPulls(
                LocalDate.now().toString(),
                defaultContent,
                threeStar,
                9,
                currentGameType,
                getCurrentBannerType(),
                false,
                () -> {
                    refreshData();
                    openDialogEvent.call();
                }
        );
    }

    public void saveWishFromUi(@Nullable Pull wishToEdit, @NonNull LocalDate selectedDate,
                               @Nullable String dropType, @NonNull String dropRare, boolean isResetPity) {

        String finalDropType = TextUtils.isEmpty(dropType) ? getApplication().getString(R.string.default_pull_content) : dropType;
        String dateForDb = selectedDate.toString();

        if (wishToEdit != null) {
            databaseRepository.updatePull(
                    wishToEdit.getId(),
                    dateForDb,
                    finalDropType,
                    dropRare,
                    currentGameType,
                    getCurrentBannerType(),
                    isResetPity,
                    this::refreshData
            );
        } else {
            databaseRepository.addPulls(
                    dateForDb,
                    finalDropType,
                    dropRare,
                    1,
                    currentGameType,
                    getCurrentBannerType(),
                    isResetPity,
                    this::refreshData
            );
        }
    }
}