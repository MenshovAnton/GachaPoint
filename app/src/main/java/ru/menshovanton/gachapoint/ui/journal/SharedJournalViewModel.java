package ru.menshovanton.gachapoint.ui.journal;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import ru.menshovanton.gachapoint.domain.enums.GameType;

public class SharedJournalViewModel extends ViewModel {

    private final MutableLiveData<GameType> selectedGameType = new MutableLiveData<>(GameType.GENSHIN);

    public LiveData<GameType> getSelectedGameType() {
        return selectedGameType;
    }

    public void selectGameType(GameType gameType) {
        if (gameType != null && selectedGameType.getValue() != gameType) {
            selectedGameType.setValue(gameType);
        }
    }
}