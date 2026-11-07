package ru.menshovanton.gachapoint.ui.fragment.promocodes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.repository.PromocodeRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.PromoCode;

public class PromocodesViewModel extends AndroidViewModel {

    public enum FilterMode {
        ACTIVE,
        STREAM,
        PERMANENT,
        EXPIRED,
        ALL,
        REDEEMED
    }

    public static class FilterCounts {
        public final int allCount;
        public final int activeCount;
        public final int streamCount;
        public final int permanentCount;
        public final int expiredCount;
        public final int redeemedCount;

        public FilterCounts(int allCount, int activeCount, int streamCount, int permanentCount, int expiredCount, int redeemedCount) {
            this.allCount = allCount;
            this.activeCount = activeCount;
            this.streamCount = streamCount;
            this.permanentCount = permanentCount;
            this.expiredCount = expiredCount;
            this.redeemedCount = redeemedCount;
        }
    }

    private final PromocodeRepository repository;
    private final MutableLiveData<GameType> selectedGameType = new MutableLiveData<>(GameType.GENSHIN);
    private final MutableLiveData<FilterMode> filterMode = new MutableLiveData<>(FilterMode.ACTIVE);
    private final MutableLiveData<List<PromoCode>> displayedCodes = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<FilterCounts> filterCounts = new MutableLiveData<>(new FilterCounts(0, 0, 0, 0, 0, 0));
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private List<PromoCode> rawCodes = new ArrayList<>();

    private final ru.menshovanton.gachapoint.data.local.JsonDataLoader.OnDataUpdatedListener dataUpdatedListener = cacheFileName -> {
        if (ru.menshovanton.gachapoint.data.local.DataSyncConfig.CACHE_PROMOCODES.equals(cacheFileName)) {
            loadPromocodes(false);
        }
    };

    public PromocodesViewModel(@NonNull Application application) {
        super(application);
        this.repository = PromocodeRepository.getInstance(application);
        ru.menshovanton.gachapoint.data.local.JsonDataLoader.addDataUpdatedListener(dataUpdatedListener);
        loadPromocodes(false);
    }

    @Override
    protected void onCleared() {
        ru.menshovanton.gachapoint.data.local.JsonDataLoader.removeDataUpdatedListener(dataUpdatedListener);
    }

    public LiveData<GameType> getSelectedGameType() { return selectedGameType; }
    public LiveData<FilterMode> getFilterMode() { return filterMode; }
    public LiveData<List<PromoCode>> getDisplayedCodes() { return displayedCodes; }
    public LiveData<FilterCounts> getFilterCounts() { return filterCounts; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void selectGameType(GameType gameType) {
        if (selectedGameType.getValue() != gameType) {
            selectedGameType.setValue(gameType);
            loadPromocodes(false);
        }
    }

    public void setFilterMode(FilterMode mode) {
        if (filterMode.getValue() != mode) {
            filterMode.setValue(mode);
            applyFilter();
        }
    }

    public void loadPromocodes(boolean forceRemote) {
        isLoading.setValue(true);
        GameType game = selectedGameType.getValue();
        ru.menshovanton.gachapoint.data.local.NetworkExecutor.execute(() -> {
            List<PromoCode> list = repository.getPromoCodes(game, forceRemote);
            AppDatabase.postToMain(() -> {
                rawCodes = list != null ? list : Collections.emptyList();
                applyFilter();
                isLoading.setValue(false);
            });
        });
    }

    public void setRedeemed(PromoCode promoCode, boolean isRedeemed) {
        promoCode.setRedeemed(isRedeemed);
        repository.setRedeemed(promoCode.getId(), isRedeemed);
        applyFilter();
    }

    public void setExpiredByUser(PromoCode promoCode, boolean isExpiredByUser) {
        promoCode.setExpiredByUser(isExpiredByUser);
        repository.setExpiredByUser(promoCode.getId(), isExpiredByUser);
        applyFilter();
    }

    private void applyFilter() {
        FilterMode mode = filterMode.getValue();
        if (mode == null) mode = FilterMode.ACTIVE;

        int allCount = rawCodes.size();
        int activeCount = 0;
        int streamCount = 0;
        int permanentCount = 0;
        int expiredCount = 0;
        int redeemedCount = 0;

        for (PromoCode code : rawCodes) {
            boolean isExp = code.isExpired();
            boolean isRed = code.isRedeemed();
            boolean isStr = code.isStream() || "STREAM".equalsIgnoreCase(code.getStatus());
            boolean isPerm = code.isPermanent();

            if (!isExp && !isRed) {
                activeCount++;
            }
            if (isStr && !isExp && !isRed) {
                streamCount++;
            }
            if (isPerm && !isExp && !isRed) {
                permanentCount++;
            }
            if (isExp) {
                expiredCount++;
            }
            if (isRed) {
                redeemedCount++;
            }
        }
        filterCounts.setValue(new FilterCounts(allCount, activeCount, streamCount, permanentCount, expiredCount, redeemedCount));

        List<PromoCode> filtered = new ArrayList<>();
        for (PromoCode code : rawCodes) {
            boolean isExp = code.isExpired();
            boolean isRed = code.isRedeemed();
            boolean isStr = code.isStream() || "STREAM".equalsIgnoreCase(code.getStatus());
            boolean isPerm = code.isPermanent();

            if (mode == FilterMode.ACTIVE) {
                // ACTIVE tab displays all currently actionable working promo codes
                if (isExp || isRed) continue;
            } else if (mode == FilterMode.STREAM) {
                if (!isStr || isExp || isRed) continue;
            } else if (mode == FilterMode.PERMANENT) {
                if (!isPerm || isExp || isRed) continue;
            } else if (mode == FilterMode.EXPIRED) {
                if (!isExp) continue;
            } else if (mode == FilterMode.REDEEMED) {
                if (!isRed) continue;
            }
            filtered.add(code);
        }

        filtered.sort(this::compareCodes);

        displayedCodes.setValue(filtered);
    }

    private int compareCodes(PromoCode c1, PromoCode c2) {
        int p1 = getCodePriority(c1);
        int p2 = getCodePriority(c2);
        if (p1 != p2) {
            return Integer.compare(p1, p2);
        }

        if (c1.isStream() && c2.isStream()) {
            long rem1 = c1.getRemainingHours();
            long rem2 = c2.getRemainingHours();
            if (rem1 > 0 && rem2 > 0 && rem1 != rem2) {
                return Long.compare(rem1, rem2);
            }
        }

        if (c1.isExpired() && c2.isExpired()) {
            String e1 = c1.getExpiresAt() != null ? c1.getExpiresAt() : (c1.getCreatedAt() != null ? c1.getCreatedAt() : "");
            String e2 = c2.getExpiresAt() != null ? c2.getExpiresAt() : (c2.getCreatedAt() != null ? c2.getCreatedAt() : "");
            return e2.compareTo(e1);
        }

        if (c1.isNew() != c2.isNew()) {
            return c1.isNew() ? -1 : 1;
        }

        String d1 = c1.getCreatedAt() != null ? c1.getCreatedAt() : "";
        String d2 = c2.getCreatedAt() != null ? c2.getCreatedAt() : "";
        int dateCmp = d2.compareTo(d1);
        if (dateCmp != 0) {
            return dateCmp;
        }

        return c1.getCode().compareTo(c2.getCode());
    }

    private int getCodePriority(PromoCode code) {
        if (code.isExpired()) {
            return 4;
        }
        if (code.isRedeemed()) {
            return 3;
        }
        if (code.isStream() || "STREAM".equalsIgnoreCase(code.getStatus())) {
            return 0;
        }
        if (code.isPermanent()) {
            return 2;
        }
        return 1;
    }
}
