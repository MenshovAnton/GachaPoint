package ru.menshovanton.gachapoint.data.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.menshovanton.gachapoint.data.db.dao.CalendarDao;
import ru.menshovanton.gachapoint.data.db.dao.GameDao;
import ru.menshovanton.gachapoint.data.db.dao.PullDao;
import ru.menshovanton.gachapoint.data.db.dao.SubscriptionDao;
import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.data.db.entities.GameEntity;
import ru.menshovanton.gachapoint.data.db.entities.PullEntity;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;

@Database(
        entities = {CalendarEntity.class, CalendarGameStatusEntity.class, PullEntity.class, SubscriptionEntity.class, GameEntity.class},
        version = 3
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "GachaPointDB.db";
    private static volatile AppDatabase INSTANCE;

    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public abstract CalendarDao calendarDao();
    public abstract PullDao pullDao();
    public abstract SubscriptionDao subscriptionDao();
    public abstract GameDao gameDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .addMigrations(MigrationHelper.MIGRATION_1_2, MigrationHelper.MIGRATION_2_3)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    syncGames(db);
                                }

                                @Override
                                public void onOpen(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    syncGames(db);
                                }

                                private void syncGames(androidx.sqlite.db.SupportSQLiteDatabase db) {
                                    for (GameType type : GameType.values()) {
                                        String sql = "INSERT INTO `games` (`id`, `code_name`, `display_name`) " +
                                                "VALUES (" + type.getCode() + ", '" + type.name() + "', '" + type.getName() + "') " +
                                                "ON CONFLICT(`id`) DO UPDATE SET `code_name` = excluded.`code_name`, `display_name` = excluded.`display_name`";
                                        db.execSQL(sql);
                                    }
                                }
                            })
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

    public static synchronized void destroyInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
        }
        INSTANCE = null;
    }
}