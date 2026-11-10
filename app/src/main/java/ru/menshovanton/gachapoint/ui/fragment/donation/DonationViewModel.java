package ru.menshovanton.gachapoint.ui.fragment.donation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ru.menshovanton.gachapoint.data.local.DataSyncConfig;
import ru.menshovanton.gachapoint.data.local.JsonDataLoader;
import ru.menshovanton.gachapoint.data.repository.SupportersRepository;
import ru.menshovanton.gachapoint.domain.models.SupportersData;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class DonationViewModel extends AndroidViewModel {

    private final SingleLiveEvent<Void> navigateBackEvent = new SingleLiveEvent<>();
    private final MutableLiveData<SupportersData> supportersData = new MutableLiveData<>();

    private final JsonDataLoader.OnDataUpdatedListener dataUpdatedListener = cacheFileName -> {
        if (DataSyncConfig.CACHE_SUPPORTERS.equals(cacheFileName)) {
            SupportersRepository.getInstance(getApplication()).reload(false);
            loadSupporters();
        }
    };

    public DonationViewModel(@NonNull Application application) {
        super(application);
        JsonDataLoader.addDataUpdatedListener(dataUpdatedListener);
        supportersData.setValue(SupportersRepository.getInstance(application).getSupportersData());
    }

    public LiveData<Void> getNavigateBackEvent() {
        return navigateBackEvent;
    }

    public LiveData<SupportersData> getSupportersData() {
        return supportersData;
    }

    public void onBackClicked() {
        navigateBackEvent.call();
    }

    public void loadSupporters() {
        supportersData.postValue(SupportersRepository.getInstance(getApplication()).getSupportersData());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        JsonDataLoader.removeDataUpdatedListener(dataUpdatedListener);
    }
}
