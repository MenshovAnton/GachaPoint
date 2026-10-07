package ru.menshovanton.gachapoint.ui.fragment.feedback;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class FeedbackViewModel extends ViewModel {

    private final SingleLiveEvent<Void> errorMessageEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> popBackStackEvent = new SingleLiveEvent<>();

    public FeedbackViewModel() {}

    public LiveData<Void> getErrorMessage() {
        return errorMessageEvent;
    }

    public LiveData<Void> getPopBackStack() {
        return popBackStackEvent;
    }

    public void onBackTriggered() {
        popBackStackEvent.call();
    }

    public void sendErrorMessage() {
        errorMessageEvent.call();
    }
}
