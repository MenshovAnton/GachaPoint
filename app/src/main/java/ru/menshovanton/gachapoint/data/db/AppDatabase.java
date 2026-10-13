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
import ru.menshovanton.gachapoint.data.db.dao.SubscriptionDao;
import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.data.db.entities.GameEntity;
import ru.menshovanton.gachapoint.data.db.entities.PullEntity;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;

@Database(
        entities = {CalendarEntity.class, CalendarGameStatusEntity.class, PullEntity.class, SubscriptionEntity.class, GameEntity.class},
        version = 2
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

    public static RoomDatabase.Builder<AppDatabase> createDatabaseBuilder(Context context, String databaseName) {
        return Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        databaseName
                )
                .addMigrations(MigrationHelper.MIGRATION_1_2)
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        syncGamesRaw(db);
                        syncSubscriptionsRaw(db);
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        syncGamesRaw(db);
                        syncSubscriptionsRaw(db);
                    }
                });
    }

    public static void syncGamesRaw(SupportSQLiteDatabase db) {
        db.beginTransaction();
        try {
            String insertSql = "INSERT OR IGNORE INTO `games` (`id`, `code_name`, `display_name`) VALUES (?, ?, ?)";
            String updateSql = "UPDATE `games` SET `code_name` = ?, `display_name` = ? WHERE `id` = ?";
            for (GameType type : GameType.values()) {
                db.execSQL(insertSql, new Object[]{type.getCode(), type.name(), type.getName()});
                db.execSQL(updateSql, new Object[]{type.name(), type.getName(), type.getCode()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static void syncSubscriptionsRaw(SupportSQLiteDatabase db) {
        db.beginTransaction();
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            long todayEpoch = today.toEpochDay();

            for (GameType gameType : GameType.values()) {
                int gameCode = gameType.getCode();
                int activeSubsCount = 0;
                try (android.database.Cursor subCursor = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                        "SELECT COUNT(*) FROM `subscriptions` WHERE `game_type` = ? AND `end_date` >= ?",
                        new Object[]{gameCode, todayEpoch}))) {
                    if (subCursor.moveToFirst()) {
                        activeSubsCount = subCursor.getInt(0);
                    }
                }

                if (activeSubsCount > 0) {
                    continue;
                }

                int lastYear = 0;
                int lastDoy = 0;
                int lastRem = 0;

                try (android.database.Cursor todayCursor = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                        "SELECT `year`, `day_of_year`, `sub_days_remaining` " +
                                "FROM `calendar_game_status` " +
                                "WHERE `game_type` = ? AND `year` = ? AND `day_of_year` = ? AND `sub_days_remaining` > 0",
                        new Object[]{gameCode, today.getYear(), today.getDayOfYear()}))) {
                    if (todayCursor.moveToFirst()) {
                        lastYear = todayCursor.getInt(0);
                        lastDoy = todayCursor.getInt(1);
                        lastRem = todayCursor.getInt(2);
                    }
                }

                if (lastRem <= 0) {
                    try (android.database.Cursor latestCursor = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                            "SELECT `year`, `day_of_year`, `sub_days_remaining` " +
                                    "FROM `calendar_game_status` " +
                                    "WHERE `game_type` = ? AND `sub_days_remaining` > 0 " +
                                    "ORDER BY `year` DESC, `day_of_year` DESC LIMIT 1",
                            new Object[]{gameCode}))) {
                        if (latestCursor.moveToFirst()) {
                            lastYear = latestCursor.getInt(0);
                            lastDoy = latestCursor.getInt(1);
                            lastRem = latestCursor.getInt(2);
                        }
                    }
                }

                if (lastRem <= 0 || lastYear <= 0 || lastDoy <= 0) {
                    continue;
                }

                int maxRem = lastRem;
                try (android.database.Cursor maxCursor = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                        "SELECT MAX(`sub_days_remaining`) FROM `calendar_game_status` WHERE `game_type` = ?",
                        new Object[]{gameCode}))) {
                    if (maxCursor.moveToFirst() && !maxCursor.isNull(0)) {
                        maxRem = Math.max(maxRem, maxCursor.getInt(0));
                    }
                }

                int count = Math.max(1, Math.min(6, (int) Math.ceil(maxRem / 30.0)));
                int totalDays = count * 30;

                java.time.LocalDate latestDate = java.time.LocalDate.ofYearDay(lastYear, lastDoy);
                java.time.LocalDate endDate = latestDate.plusDays(lastRem - 1);
                java.time.LocalDate startDate;

                if (endDate.isBefore(today)) {
                    endDate = today.plusDays(lastRem - 1);
                }
                startDate = endDate.minusDays(totalDays - 1);

                long startEpoch = startDate.toEpochDay();
                long endEpoch = endDate.toEpochDay();

                int claims = 0;
                int misses = 0;

                try (android.database.Cursor datesCursor = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                        "SELECT c.`year`, c.`day_of_year`, COALESCE(s.`status`, 0) AS `status` " +
                                "FROM `calendar` c " +
                                "LEFT JOIN `calendar_game_status` s " +
                                "ON c.`year` = s.`year` AND c.`day_of_year` = s.`day_of_year` AND s.`game_type` = ? " +
                                "WHERE (c.`year` > ? OR (c.`year` = ? AND c.`day_of_year` >= ?)) " +
                                "AND (c.`year` < ? OR (c.`year` = ? AND c.`day_of_year` <= ?))",
                        new Object[]{
                                gameCode,
                                startDate.getYear(), startDate.getYear(), startDate.getDayOfYear(),
                                today.getYear(), today.getYear(), today.getDayOfYear()
                        }))) {
                    while (datesCursor.moveToNext()) {
                        int yr = datesCursor.getInt(0);
                        int doy = datesCursor.getInt(1);
                        int status = datesCursor.getInt(2);
                        boolean isToday = (yr == today.getYear() && doy == today.getDayOfYear());

                        if (status == ru.menshovanton.gachapoint.domain.enums.DayState.STATUS_RECEIVED
                                || status == ru.menshovanton.gachapoint.domain.enums.DayState.STATUS_OLD_RECEIVED) {
                            claims++;
                        } else if (!isToday && status != 0) {
                            misses++;
                        }
                    }
                }

                int primogemsPerDay = gameType.getCurrencyPerDay();
                int claimed = claims * primogemsPerDay;
                int missed = misses * primogemsPerDay;
                int totalPrimogems = count * 30 * primogemsPerDay;
                int wait = Math.max(0, totalPrimogems - claimed - missed);

                db.execSQL("DELETE FROM `subscriptions` WHERE `game_type` = ?", new Object[]{gameCode});

                String insertSubSql = "INSERT INTO `subscriptions` " +
                        "(`game_type`, `count`, `start_date`, `end_date`, `claimed`, `missed`, `wait`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
                db.execSQL(insertSubSql, new Object[]{gameCode, count, startEpoch, endEpoch, claimed, missed, wait});
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
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
    }
}