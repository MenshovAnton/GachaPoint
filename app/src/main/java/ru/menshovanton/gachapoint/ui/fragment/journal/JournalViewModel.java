package ru.menshovanton.gachapoint.ui.fragment.journal;

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
import ru.menshovanton.gachapoint.domain.enums.PityStatus;
import ru.menshovanton.gachapoint.domain.enums.RareType;
import ru.menshovanton.gachapoint.domain.models.BannerInfo;
import ru.menshovanton.gachapoint.domain.models.Pull;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class JournalViewModel extends AndroidViewModel {

    private final DatabaseRepository databaseRepository;

    private final MutableLiveData<List<Pull>> wishesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> pity5StarLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> pity4StarLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isGuaranteedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentBannerTypeLiveData = new MutableLiveData<>(BannerType.EVENT.getDbKey());

    private final SingleLiveEvent<Void> openDialogEvent = new SingleLiveEvent<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);

    private GameType currentGameType = GameType.GENSHIN;

    public JournalViewModel(@NonNull Application application) {
        super(application);
        ru.menshovanton.gachapoint.di.AppContainer container = ((ru.menshovanton.gachapoint.App) application).getAppContainer();
        this.databaseRepository = container.getDatabaseRepository();
    }

    public LiveData<List<Pull>> getWishesLiveData() { return wishesLiveData; }
    public LiveData<Integer> getPity5StarLiveData() { return pity5StarLiveData; }
    public LiveData<Integer> getPity4StarLiveData() { return pity4StarLiveData; }
    public LiveData<Boolean> getIsGuaranteedLiveData() { return isGuaranteedLiveData; }
    public LiveData<String> getCurrentBannerTypeLiveData() { return currentBannerTypeLiveData; }
    public SingleLiveEvent<Void> getOpenDialogEvent() { return openDialogEvent; }
    public LiveData<Boolean> getIsLoadingLiveData() { return isLoadingLiveData; }

    public void setGameType(GameType gameType) {
        if (gameType != null) {
            this.currentGameType = gameType;
            List<BannerInfo> bannersInfo = gameType.getBanners();
            boolean banners = false;
            for (BannerInfo banner : bannersInfo) {
                if (banner.getDbKey().equals(getCurrentBannerType())) {
                    banners = true;
                    break;
                }
            }
            if (!banners && !bannersInfo.isEmpty()) {
                currentBannerTypeLiveData.setValue(bannersInfo.get(0).getDbKey());
            }
            refreshData();
        }
    }

    public GameType getCurrentGameType() {
        return currentGameType;
    }

    public BannerInfo getCurrentBannerInfo() {
        return currentGameType.getBannerInfo(getCurrentBannerType());
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
        refreshData(true);
    }

    public void refreshData(boolean showLoading) {
        if (showLoading) {
            isLoadingLiveData.setValue(true);
        }
        databaseRepository.getPullsByBanner(currentGameType, getCurrentBannerType(), rawWishes -> {
            List<Pull> processedList = processWishesAndCalculatePity(rawWishes);
            if (showLoading) {
                isLoadingLiveData.setValue(false);
            }
            wishesLiveData.setValue(processedList);
        });
    }

    private List<Pull> processWishesAndCalculatePity(List<Pull> rawPulls) {
        if (rawPulls == null || rawPulls.isEmpty()) {
            pity5StarLiveData.setValue(0);
            pity4StarLiveData.setValue(0);
            isGuaranteedLiveData.setValue(false);
            return new ArrayList<>();
        }

        List<Pull> chronological = new ArrayList<>(rawPulls);
        Collections.reverse(chronological);

        int counter5 = 0;
        int counter4 = 0;
        boolean guaranteed = false;

        BannerInfo bannerInfo = getCurrentBannerInfo();
        boolean bannerHas5050 = bannerInfo != null && bannerInfo.hasFiftyFifty();

        for (Pull pull : chronological) {
            counter5++;
            counter4++;

            RareType rareType = RareType.from(pull.getDropRare(), getApplication());

            if (rareType == RareType.LEGENDARY) {
                pull.setPityNumber(counter5);
                pull.setPity4StarNumber(counter4);

                if (bannerHas5050) {
                    PityStatus status = pull.getPityStatus();
                    if (status == PityStatus.LOSS_50_50) {
                        guaranteed = true;
                    } else if (status == PityStatus.WIN_50_50 || status == PityStatus.GUARANTEED) {
                        guaranteed = false;
                    }
                }

                counter5 = 0;
                counter4 = 0;
            } else if (rareType == RareType.EPIC) {
                pull.setPityNumber(counter5);
                pull.setPity4StarNumber(counter4);
                counter4 = 0;
            } else {
                pull.setPityNumber(counter5);
                pull.setPity4StarNumber(counter4);
            }
        }

        Collections.reverse(chronological);

        pity5StarLiveData.setValue(counter5);
        pity4StarLiveData.setValue(counter4);
        isGuaranteedLiveData.setValue(bannerHas5050 && guaranteed);

        return chronological;
    }

    public void saveOneWish() {
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
                PityStatus.NONE,
                () -> refreshData(false)
        );
    }

    public void saveWishFromUi(@Nullable Pull wishToEdit, @NonNull LocalDate selectedDate,
                               @Nullable String dropType, @NonNull String dropRare,
                               @NonNull PityStatus pityStatus, boolean isBatch) {

        String finalDropType = TextUtils.isEmpty(dropType) ? getApplication().getString(R.string.default_pull_content) : dropType;
        String dateForDb = selectedDate.toString();

        String threeStar = getApplication().getString(R.string.three_star);
        String defaultContent = getApplication().getString(R.string.default_pull_content);

        boolean isResetPity = (RareType.from(dropRare, getApplication()) == RareType.LEGENDARY);

        if (wishToEdit != null) {
            databaseRepository.updatePull(
                    wishToEdit.getId(),
                    dateForDb,
                    finalDropType,
                    dropRare,
                    currentGameType,
                    getCurrentBannerType(),
                    isResetPity,
                    pityStatus,
                    () -> refreshData(false)
            );
        } else {
            if (isBatch) {
                databaseRepository.addPulls(
                    dateForDb,
                    defaultContent,
                    threeStar,
                    9,
                    currentGameType,
                    getCurrentBannerType(),
                    false,
                    PityStatus.NONE,
                    () -> refreshData(false)
                );
            }
            databaseRepository.addPulls(
                    dateForDb,
                    finalDropType,
                    dropRare,
                    1,
                    currentGameType,
                    getCurrentBannerType(),
                    isResetPity,
                    pityStatus,
                    () -> refreshData(false)
            );
        }
    }

    public void deleteWish(int id) {
        databaseRepository.deletePull(id, () -> refreshData(false));
    }

    public void deleteWishesBatch(List<Integer> ids) {
        databaseRepository.deletePullsBatch(ids, () -> refreshData(false));
    }
}
