package ru.menshovanton.gachapoint.ui.main;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class MainActivityViewModel extends AndroidViewModel {

    private final MutableLiveData<GameType> subType;
    private final MutableLiveData<List<GameType>> enabledGamesLiveData;
    private final MutableLiveData<Boolean> isDbReadyLiveData = new MutableLiveData<>(false);

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        Preferences preferences = new Preferences(application);
        GameType defaultGame = preferences.getDefaultGame();
        subType = new MutableLiveData<>(defaultGame);
        enabledGamesLiveData = new MutableLiveData<>(preferences.getEnabledGames());
        ensureDatabaseInitialized();
    }

    public LiveData<GameType> getSubTypeLiveData() {
        return subType;
    }

    public GameType getSubType() {
        return subType.getValue() != null ? subType.getValue() : GameType.GENSHIN;
    }

    public void setSubType(GameType type) {
        if (type != null) {
            subType.setValue(type);
        }
    }

    public void setSubType(int code) {
        setSubType(GameType.fromCode(code));
    }

    public LiveData<List<GameType>> getEnabledGamesLiveData() {
        return enabledGamesLiveData;
    }

    public List<GameType> getEnabledGames() {
        List<GameType> val = enabledGamesLiveData.getValue();
        if (val == null || val.isEmpty()) {
            Preferences preferences = new Preferences(getApplication());
            return preferences.getEnabledGames();
        }
        return val;
    }

    public void refreshEnabledGames() {
        Preferences preferences = new Preferences(getApplication());
        List<GameType> enabled = preferences.getEnabledGames();
        enabledGamesLiveData.setValue(enabled);

        GameType current = subType.getValue();
        if (current == null || !enabled.contains(current)) {
            setSubType(preferences.getDefaultGame());
        }
    }

    public LiveData<Boolean> isDbReady() {
        return isDbReadyLiveData;
    }

    private void ensureDatabaseInitialized() {
        Context context = getApplication().getApplicationContext();
        AppDatabase.getExecutor().execute(() -> {
            try {
                AppDatabase.getInstance(context).getOpenHelper().getWritableDatabase();
            } catch (Throwable t) {
                AppLogger.e("MainActivityViewModel", "Error initializing database on startup", t);
            } finally {
                AppDatabase.postToMain(() -> isDbReadyLiveData.setValue(true));
            }
        });
    }
}
