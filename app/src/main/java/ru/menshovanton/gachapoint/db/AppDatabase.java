package ru.menshovanton.gachapoint.db;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import ru.menshovanton.gachapoint.db.dao.CalendarDao;
import ru.menshovanton.gachapoint.db.dao.WishDao;
import ru.menshovanton.gachapoint.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.db.entities.WishEntity;

@Database(
        entities = {CalendarEntity.class, WishEntity.class},
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "GachaPointDB.db";
    private static volatile AppDatabase INSTANCE;

    public abstract CalendarDao calendarDao();
    public abstract WishDao wishDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public void checkpoint() {
        runInTransaction(() -> {
            try (Cursor cursor = query("PRAGMA wal_checkpoint(FULL)", null)) {
                cursor.moveToFirst();
            }
        });
    }
}