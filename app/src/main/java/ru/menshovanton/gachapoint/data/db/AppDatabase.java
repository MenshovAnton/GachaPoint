package ru.menshovanton.gachapoint.data.db;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.menshovanton.gachapoint.data.db.dao.CalendarDao;
import ru.menshovanton.gachapoint.data.db.dao.PullDao;
import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.data.db.entities.PullEntity;

@Database(
        entities = {CalendarEntity.class, PullEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "GachaPointDB.db";
    private static volatile AppDatabase INSTANCE;

    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public abstract CalendarDao calendarDao();
    public abstract PullDao pullDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static ExecutorService getExecutor() {
        return DB_EXECUTOR;
    }

    public static void postToMain(Runnable runnable) {
        MAIN_HANDLER.post(runnable);
    }

    public void checkpoint() {
        DB_EXECUTOR.execute(() -> runInTransaction(() -> {
            try (Cursor cursor = query("PRAGMA wal_checkpoint(FULL)", null)) {
                cursor.moveToFirst();
            }
        }));
    }
}