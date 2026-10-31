package ru.menshovanton.gachapoint.ui.fragment.help;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class HelpViewModel extends ViewModel {

    private final SingleLiveEvent<Void> navigateToMenuEvent = new SingleLiveEvent<>();

    public LiveData<Void> getNavigateToMenuEvent() {
        return navigateToMenuEvent;
    }

    public void onBackToMenuClicked() {
        navigateToMenuEvent.call();
    }
}
