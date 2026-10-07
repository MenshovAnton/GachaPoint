package ru.menshovanton.gachapoint.ui.fragment.more;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class MoreViewModel extends AndroidViewModel {

    private final SingleLiveEvent<Void> navigateToInfoEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> navigateToSettingsEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> navigateToFeedbackEvent = new SingleLiveEvent<>();

    public MoreViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Void> getNavigateToInfoEvent() {
        return navigateToInfoEvent;
    }

    public LiveData<Void> getNavigateToSettingsEvent() {
        return navigateToSettingsEvent;
    }

    public LiveData<Void> getNavigateToFeedbackEvent() {
        return navigateToFeedbackEvent;
    }

    public void onInfoButtonClicked() {
        navigateToInfoEvent.call();
    }
    public void onSettingsButtonClicked() {
        navigateToSettingsEvent.call();
    }

    public void onFeedbackButtonClicked() {
        navigateToFeedbackEvent.call();
    }
}
