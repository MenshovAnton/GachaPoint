package ru.menshovanton.gachapoint.data.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.menshovanton.gachapoint.data.db.dao.CalendarDao;
import ru.menshovanton.gachapoint.data.db.dao.GameDao;
import ru.menshovanton.gachapoint.data.db.dao.PullDao;
import ru.menshovanton.gachapoint.data.db.dao.SavingsDao;
import ru.menshovanton.gachapoint.data.db.dao.SubscriptionDao;
import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.data.db.entities.GameEntity;
import ru.menshovanton.gachapoint.data.db.entities.PullEntity;
import ru.menshovanton.gachapoint.data.db.entities.SavingsEntity;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;

@Database(
        entities = {CalendarEntity.class, CalendarGameStatusEntity.class, PullEntity.class, SubscriptionEntity.class, GameEntity.class, SavingsEntity.class},
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
    public abstract SavingsDao savingsDao();

    public static RoomDatabase.Builder<AppDatabase> createDatabaseBuilder(Context context, String databaseName) {
        return Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        databaseName
                )
                .addMigrations(MigrationHelper.MIGRATION_1_2, MigrationHelper.MIGRATION_2_3)
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        AppLogger.i("AppDatabase", "Room database created.");
                        MigrationHelper.syncGamesRaw(db);
                        MigrationHelper.syncSavingsRaw(context, db);
                        Preferences prefs = new Preferences(context);
                        prefs.saveBooleanPreference(Preferences.KEY_LEGACY_DATA_SYNCED, true);
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        AppLogger.i("AppDatabase", "Room database opened.");
                        ensureLegacyDataMigratedOnce(context, db);
                    }
                });
    }

    private static void ensureLegacyDataMigratedOnce(Context context, SupportSQLiteDatabase db) {
        Preferences prefs = new Preferences(context);
        boolean isMigrated = prefs.getBooleanPreference(Preferences.KEY_LEGACY_DATA_SYNCED, false);
        if (isMigrated) {
            return;
        }

        AppLogger.i("AppDatabase", "Performing legacy data migration / schema repair on app startup...");
        MigrationHelper.migrateLegacyCalendarAndSchemaIfNeeded(db);
        MigrationHelper.syncGamesRaw(db);
        MigrationHelper.syncSubscriptionsRaw(db);
        MigrationHelper.syncSavingsRaw(context, db);
        prefs.saveBooleanPreference(Preferences.KEY_LEGACY_DATA_SYNCED, true);
    }

    public static void syncGamesRaw(SupportSQLiteDatabase db) {
        MigrationHelper.syncGamesRaw(db);
    }

    public static void syncSubscriptionsRaw(SupportSQLiteDatabase db) {
        MigrationHelper.syncSubscriptionsRaw(db);
    }

    public static void syncSavingsRaw(Context context, SupportSQLiteDatabase db) {
        MigrationHelper.syncSavingsRaw(context, db);
    }

    public void syncGames() {
        List<GameEntity> entities = new ArrayList<>(GameType.values().length);
        for (GameType type : GameType.values()) {
            entities.add(new GameEntity(type));
        }
        gameDao().upsertAll(entities);
    }

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = createDatabaseBuilder(context, DATABASE_NAME).build();
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
        AppLogger.i("AppDatabase", "Database instance closed and reset.");
    }
}