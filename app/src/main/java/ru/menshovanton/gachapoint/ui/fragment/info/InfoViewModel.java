package ru.menshovanton.gachapoint.ui.fragment.info;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class InfoViewModel extends ViewModel {

    private final SingleLiveEvent<Void> navigateToSettingsEvent = new SingleLiveEvent<>();

    public LiveData<Void> getNavigateToSettingsEvent() {
        return navigateToSettingsEvent;
    }

    public void onBackToSettingsClicked() {
        navigateToSettingsEvent.call();
    }
}