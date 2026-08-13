package ru.menshovanton.gachapoint.ui.journal.wishescounter;

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
import ru.menshovanton.gachapoint.domain.models.Wish;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class WishesCounterViewModel extends AndroidViewModel {

    private final DatabaseRepository databaseRepository;

    private final MutableLiveData<List<Wish>> wishesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentPityLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<String> currentBannerTypeLiveData = new MutableLiveData<>(BannerType.EVENT.getDbKey());

    private final SingleLiveEvent<Void> openDialogEvent = new SingleLiveEvent<>();

    private GameType currentGameType = GameType.GENSHIN;

    public WishesCounterViewModel(@NonNull Application application) {
        super(application);
        this.databaseRepository = new DatabaseRepository(application);
    }

    public LiveData<List<Wish>> getWishesLiveData() { return wishesLiveData; }
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
        databaseRepository.getWishesByBanner(currentGameType, getCurrentBannerType(), rawWishes -> {
            List<Wish> processedList = processWishesAndCalculatePity(rawWishes);
            wishesLiveData.setValue(processedList);
        });
    }

    private List<Wish> processWishesAndCalculatePity(List<Wish> wishes) {
        if (wishes == null || wishes.isEmpty()) {
            currentPityLiveData.setValue(0);
            return new ArrayList<>();
        }

        List<Wish> listToProcess = new ArrayList<>(wishes);
        Collections.reverse(listToProcess);

        int counter = 0;
        for (Wish wish : listToProcess) {
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
        String defaultContent = getApplication().getString(R.string.default_wish_content);

        databaseRepository.addWishes(
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
        String defaultContent = getApplication().getString(R.string.default_wish_content);

        databaseRepository.addWishes(
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

    public void saveWishFromUi(@Nullable Wish wishToEdit, @NonNull LocalDate selectedDate,
                               @Nullable String dropType, @NonNull String dropRare, boolean isResetPity) {

        String finalDropType = TextUtils.isEmpty(dropType) ? getApplication().getString(R.string.default_wish_content) : dropType;
        String dateForDb = selectedDate.toString();

        if (wishToEdit != null) {
            databaseRepository.updateWish(
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
            databaseRepository.addWishes(
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