package ru.menshovanton.gachapoint.ui.fragment.banners;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.List;

import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository;
import ru.menshovanton.gachapoint.data.repository.CatalogRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;
import ru.menshovanton.gachapoint.domain.models.CatalogItem;
import ru.menshovanton.gachapoint.domain.models.CharacterRerunInfo;

public class BannersViewModel extends AndroidViewModel {

    public enum DisplayMode {
        SCHEDULE,
        RERUNS
    }

    private final BannerScheduleRepository bannerRepo;
    private final CatalogRepository catalogRepo;

    private final MutableLiveData<GameType> selectedGameType = new MutableLiveData<>(GameType.GENSHIN);
    private final MutableLiveData<DisplayMode> displayMode = new MutableLiveData<>(DisplayMode.SCHEDULE);
    private final MutableLiveData<List<BannerScheduleItem>> bannersList = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<CharacterRerunInfo>> rerunList = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public BannersViewModel(@NonNull Application application) {
        super(application);
        this.bannerRepo = BannerScheduleRepository.getInstance(application);
        this.catalogRepo = CatalogRepository.getInstance(application);
        loadData(false);
    }

    public LiveData<GameType> getSelectedGameType() { return selectedGameType; }
    public LiveData<DisplayMode> getDisplayMode() { return displayMode; }
    public LiveData<List<BannerScheduleItem>> getBannersList() { return bannersList; }
    public LiveData<List<CharacterRerunInfo>> getRerunList() { return rerunList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void selectGameType(GameType gameType) {
        if (selectedGameType.getValue() != gameType) {
            selectedGameType.setValue(gameType);
            loadData(false);
        }
    }

    public void setDisplayMode(DisplayMode mode) {
        if (displayMode.getValue() != mode) {
            displayMode.setValue(mode);
        }
    }

    public void loadData(boolean forceRemote) {
        isLoading.setValue(true);
        GameType game = selectedGameType.getValue();
        AppDatabase.getExecutor().execute(() -> {
            if (forceRemote) {
                bannerRepo.reload(true);
                catalogRepo.reload(true);
            }
            List<BannerScheduleItem> banners = bannerRepo.getBanners(game);
            List<CatalogItem> items = catalogRepo.getItemsForGame(game);
            List<CharacterRerunInfo> reruns = bannerRepo.getCharacterRerunHistory(game, items);

            AppDatabase.postToMain(() -> {
                bannersList.setValue(banners);
                rerunList.setValue(reruns);
                isLoading.setValue(false);
            });
        });
    }
}
