package ru.menshovanton.gachapoint.ui.main;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.repository.DatabaseRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;

public class MainActivityViewModel extends AndroidViewModel {

    private final MutableLiveData<GameType> subType = new MutableLiveData<>(GameType.GENSHIN);
    private final MutableLiveData<Integer> selectedNavId = new MutableLiveData<>(R.id.nav_home);
    private final SingleLiveEvent<String> navigateToTagEvent = new SingleLiveEvent<>();

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        ensureDatabaseInitialized();
    }

    public LiveData<GameType> getSubTypeLiveData() {
        return subType;
    }

    public GameType getSubType() {
        return subType.getValue() != null ? subType.getValue() : GameType.GENSHIN;
    }

    public void setSubType(GameType type) {
        subType.setValue(type);
    }

    public void setSubType(int code) {
        subType.setValue(GameType.fromCode(code));
    }

    public LiveData<Integer> getSelectedNavId() {
        return selectedNavId;
    }

    public LiveData<String> getNavigateToTagEvent() {
        return navigateToTagEvent;
    }

    public boolean onNavigationItemSelected(int itemId) {
        selectedNavId.setValue(itemId);

        if (itemId == R.id.nav_home) {
            navigateToTagEvent.setValue(MainActivityView.HOME_TAG);
            return true;
        } else if (itemId == R.id.nav_tracker) {
            navigateToTagEvent.setValue(MainActivityView.TRACKER_TAG);
            return true;
        } else if (itemId == R.id.nav_journal) {
            navigateToTagEvent.setValue(MainActivityView.JOURNAL_TAG);
            return true;
        } else if (itemId == R.id.nav_settings) {
            navigateToTagEvent.setValue(MainActivityView.SETTINGS_TAG);
            return true;
        }
        return false;
    }

    private void ensureDatabaseInitialized() {
        Context context = getApplication().getApplicationContext();
        File dbFile = context.getDatabasePath(DatabaseRepository.DATABASE_NAME);
        if (!dbFile.exists()) {
            AppDatabase.getExecutor().execute(() ->
                    AppDatabase.getInstance(context).getOpenHelper().getWritableDatabase());
        }
    }
}