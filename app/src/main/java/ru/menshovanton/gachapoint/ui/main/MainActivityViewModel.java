package ru.menshovanton.gachapoint.ui.main;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;

import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class MainActivityViewModel extends AndroidViewModel {

    private final MutableLiveData<GameType> subType = new MutableLiveData<>(GameType.GENSHIN);

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

    private void ensureDatabaseInitialized() {
        Context context = getApplication().getApplicationContext();
        File dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);
        if (!dbFile.exists()) {
            AppDatabase.getExecutor().execute(() ->
                    AppDatabase.getInstance(context).getOpenHelper().getWritableDatabase());
        }
    }
}