package ru.menshovanton.gachapoint.ui.fragment.banners;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    public enum ScheduleFilter {
        ALL,
        CURRENT,
        FUTURE,
        PAST
    }

    public enum RerunFilter {
        ALL,
        ACTIVE,
        OVERDUE,
        NEVER
    }

    private final BannerScheduleRepository bannerRepo;
    private final CatalogRepository catalogRepo;

    private final MutableLiveData<GameType> selectedGameType = new MutableLiveData<>(GameType.GENSHIN);
    private final MutableLiveData<DisplayMode> displayMode = new MutableLiveData<>(DisplayMode.SCHEDULE);
    private final MutableLiveData<List<BannerScheduleItem>> bannersList = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<CharacterRerunInfo>> rerunList = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private List<BannerScheduleItem> allBanners = Collections.emptyList();
    private List<CharacterRerunInfo> allReruns = Collections.emptyList();
    private String currentSearchQuery = "";
    private ScheduleFilter scheduleFilter = ScheduleFilter.ALL;
    private RerunFilter rerunFilter = RerunFilter.ALL;

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
    public String getSearchQuery() { return currentSearchQuery; }
    public ScheduleFilter getScheduleFilter() { return scheduleFilter; }
    public RerunFilter getRerunFilter() { return rerunFilter; }

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

    public void setScheduleFilter(ScheduleFilter filter) {
        ScheduleFilter target = filter != null ? filter : ScheduleFilter.ALL;
        if (this.scheduleFilter != target) {
            this.scheduleFilter = target;
            applyFilter();
        }
    }

    public void setRerunFilter(RerunFilter filter) {
        RerunFilter target = filter != null ? filter : RerunFilter.ALL;
        if (this.rerunFilter != target) {
            this.rerunFilter = target;
            applyFilter();
        }
    }

    public void setSearchQuery(String query) {
        this.currentSearchQuery = query != null ? query.trim() : "";
        applyFilter();
    }

    private void applyFilter() {
        String query = currentSearchQuery.toLowerCase(Locale.ROOT);

        List<BannerScheduleItem> filteredBanners = new ArrayList<>();
        for (BannerScheduleItem item : allBanners) {
            if (!matchesScheduleFilter(item, scheduleFilter)) continue;
            if (!query.isEmpty() && !matchesBanner(item, query)) continue;
            filteredBanners.add(item);
        }

        List<CharacterRerunInfo> filteredReruns = new ArrayList<>();
        for (CharacterRerunInfo item : allReruns) {
            if (!matchesRerunFilter(item, rerunFilter)) continue;
            if (!query.isEmpty() && !matchesRerun(item, query)) continue;
            filteredReruns.add(item);
        }

        bannersList.setValue(filteredBanners);
        rerunList.setValue(filteredReruns);
    }

    private boolean matchesScheduleFilter(BannerScheduleItem banner, ScheduleFilter filter) {
        if (filter == ScheduleFilter.ALL) return true;
        try {
            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
            LocalDate start = LocalDate.parse(banner.getStartDate(), fmt);
            LocalDate end = LocalDate.parse(banner.getEndDate(), fmt);
            String bannerStatus = banner.getStatus();
            boolean isCurrent = "CURRENT".equalsIgnoreCase(bannerStatus) || (!today.isBefore(start) && !today.isAfter(end));
            boolean isAnnounced = !isCurrent && ("ANNOUNCED".equalsIgnoreCase(bannerStatus) || today.isBefore(start));
            boolean isPast = !isCurrent && !isAnnounced;

            if (filter == ScheduleFilter.CURRENT) return isCurrent;
            if (filter == ScheduleFilter.FUTURE) return isAnnounced;
            if (filter == ScheduleFilter.PAST) return isPast;
        } catch (Exception e) {
            String bannerStatus = banner.getStatus();
            if (filter == ScheduleFilter.CURRENT) return "CURRENT".equalsIgnoreCase(bannerStatus);
            if (filter == ScheduleFilter.FUTURE) return "ANNOUNCED".equalsIgnoreCase(bannerStatus);
            if (filter == ScheduleFilter.PAST) return "PAST".equalsIgnoreCase(bannerStatus);
        }
        return true;
    }

    private boolean matchesRerunFilter(CharacterRerunInfo rerun, RerunFilter filter) {
        if (filter == RerunFilter.ALL) return true;
        int days = rerun.getDaysSinceLastRerun();
        boolean isNever = (days == 999 || rerun.getLastVersion() == null);
        boolean isActive = !isNever && days <= 0;
        boolean isOverdue = !isNever && days > 180;

        if (filter == RerunFilter.ACTIVE) return isActive;
        if (filter == RerunFilter.OVERDUE) return isOverdue;
        if (filter == RerunFilter.NEVER) return isNever;
        return true;
    }

    private boolean matchesBanner(BannerScheduleItem item, String query) {
        if (item.getVersion().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        for (String name : item.getFeaturedFiveStarsRu()) {
            if (name.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        for (String name : item.getFeaturedFiveStarsEn()) {
            if (name.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        for (String name : item.getFeaturedFourStarsRu()) {
            if (name.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        for (String name : item.getFeaturedFourStarsEn()) {
            if (name.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    private boolean matchesRerun(CharacterRerunInfo item, String query) {
        if (item.getCharacterNameRu().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        if (item.getCharacterNameEn().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        return item.getLastVersion() != null && item.getLastVersion().toLowerCase(Locale.ROOT).contains(query);
    }

    public void loadData(boolean forceRemote) {
        isLoading.setValue(true);
        GameType game = selectedGameType.getValue();
        ru.menshovanton.gachapoint.data.local.NetworkExecutor.execute(() -> {
            if (forceRemote) {
                bannerRepo.reload(true);
                catalogRepo.reload(true);
            }
            List<BannerScheduleItem> banners = bannerRepo.getBanners(game);
            List<CatalogItem> items = catalogRepo.getItemsForGame(game);
            List<CharacterRerunInfo> reruns = bannerRepo.getCharacterRerunHistory(game, items);

            AppDatabase.postToMain(() -> {
                allBanners = banners;
                allReruns = reruns;
                applyFilter();
                isLoading.setValue(false);
            });
        });
    }
}
