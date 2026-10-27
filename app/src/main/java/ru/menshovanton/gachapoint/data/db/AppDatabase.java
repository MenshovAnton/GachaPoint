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
                        syncGamesRaw(db);
                        syncSubscriptionsRaw(db);
                        syncSavingsRaw(context, db);
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        AppLogger.i("AppDatabase", "Room database opened.");
                        syncGamesRaw(db);
                        syncSubscriptionsRaw(db);
                        syncSavingsRaw(context, db);
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

            List<Integer> corruptSubIds = new ArrayList<>();
            try (android.database.Cursor cur = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                    "SELECT `id`, `game_type`, `count`, `start_date`, `end_date` FROM `subscriptions` WHERE `count` > 1"))) {
                while (cur.moveToNext()) {
                    int subId = cur.getInt(0);
                    int gType = cur.getInt(1);
                    long sEpoch = cur.getLong(3);
                    long eEpoch = cur.getLong(4);

                    try (android.database.Cursor statusCur = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                            "SELECT `year`, `day_of_year`, `sub_days_remaining` " +
                                    "FROM `calendar_game_status` " +
                                    "WHERE `game_type` = ? AND `sub_days_remaining` > 0",
                            new Object[]{gType}))) {
                        while (statusCur.moveToNext()) {
                            int y = statusCur.getInt(0);
                            int doy = statusCur.getInt(1);
                            int rem = statusCur.getInt(2);
                            java.time.LocalDate d = java.time.LocalDate.ofYearDay(y, doy);
                            long dEpoch = d.toEpochDay();
                            if (dEpoch >= sEpoch && dEpoch <= eEpoch) {
                                java.time.LocalDate expectedEnd = d.plusDays(rem - 1);
                                if (expectedEnd.toEpochDay() < eEpoch - 1) {
                                    corruptSubIds.add(subId);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            for (int badId : corruptSubIds) {
                db.execSQL("DELETE FROM `subscriptions` WHERE `id` = ?", new Object[]{badId});
            }

            for (GameType gameType : GameType.values()) {
                int gameCode = gameType.getCode();
                int currencyPerDay = gameType.getCurrencyPerDay();
                int subDuration = gameType.getSubscriptionDurationDays();

                List<long[]> existingRanges = new ArrayList<>();
                try (android.database.Cursor cur = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                        "SELECT `start_date`, `end_date` FROM `subscriptions` WHERE `game_type` = ? ORDER BY `start_date` ASC",
                        new Object[]{gameCode}))) {
                    while (cur.moveToNext()) {
                        existingRanges.add(new long[]{cur.getLong(0), cur.getLong(1)});
                    }
                }

                if (!existingRanges.isEmpty()) {
                    continue;
                }

                class DayRecord {
                    final int year;
                    final int dayOfYear;
                    final int status;
                    final int subDays;
                    final java.time.LocalDate date;
                    final long epoch;

                    DayRecord(int y, int doy, int st, int rem) {
                        this.year = y;
                        this.dayOfYear = doy;
                        this.status = st;
                        this.subDays = rem;
                        this.date = java.time.LocalDate.ofYearDay(y, doy);
                        this.epoch = this.date.toEpochDay();
                    }
                }

                List<DayRecord> uncovered = new ArrayList<>();
                try (android.database.Cursor cur = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                        "SELECT `year`, `day_of_year`, `status`, `sub_days_remaining` " +
                                "FROM `calendar_game_status` " +
                                "WHERE `game_type` = ? AND (`status` = 1 OR `status` = 3 OR `sub_days_remaining` > 0) " +
                                "ORDER BY `year` ASC, `day_of_year` ASC",
                        new Object[]{gameCode}))) {
                    while (cur.moveToNext()) {
                        DayRecord rec = new DayRecord(cur.getInt(0), cur.getInt(1), cur.getInt(2), cur.getInt(3));
                        boolean covered = false;
                        for (long[] r : existingRanges) {
                            if (rec.epoch >= r[0] && rec.epoch <= r[1]) {
                                covered = true;
                                break;
                            }
                        }
                        if (!covered) {
                            uncovered.add(rec);
                        }
                    }
                }

                if (uncovered.isEmpty()) {
                    continue;
                }

                List<List<DayRecord>> clusters = new ArrayList<>();
                List<DayRecord> currentCluster = new ArrayList<>();
                currentCluster.add(uncovered.get(0));

                for (int i = 1; i < uncovered.size(); i++) {
                    DayRecord curr = uncovered.get(i);
                    java.time.LocalDate maxEndInCluster = currentCluster.get(0).date.plusDays(Math.max(1, currentCluster.get(0).subDays) - 1);
                    for (DayRecord r : currentCluster) {
                        if (r.subDays > 0) {
                            java.time.LocalDate endR = r.date.plusDays(r.subDays - 1);
                            if (endR.isAfter(maxEndInCluster)) {
                                maxEndInCluster = endR;
                            }
                        }
                    }

                    if (curr.date.isAfter(maxEndInCluster)) {
                        clusters.add(currentCluster);
                        currentCluster = new ArrayList<>();
                    }
                    currentCluster.add(curr);
                }
                clusters.add(currentCluster);

                for (List<DayRecord> cluster : clusters) {
                    java.time.LocalDate minDate = cluster.get(0).date;
                    java.time.LocalDate maxDate = cluster.get(cluster.size() - 1).date;

                    java.time.LocalDate candidateEnd = null;
                    for (DayRecord r : cluster) {
                        if (r.subDays > 0) {
                            java.time.LocalDate endFromRec = r.date.plusDays(r.subDays - 1);
                            if (candidateEnd == null || endFromRec.isAfter(candidateEnd)) {
                                candidateEnd = endFromRec;
                            }
                        }
                    }

                    java.time.LocalDate startDate;
                    java.time.LocalDate endDate;
                    int count;

                    if (candidateEnd != null) {
                        endDate = candidateEnd;
                        long span = java.time.temporal.ChronoUnit.DAYS.between(minDate, endDate) + 1;
                        count = Math.max(1, (int) Math.ceil((double) span / subDuration));
                        startDate = endDate.minusDays((long) count * subDuration - 1);
                        if (startDate.isAfter(minDate)) {
                            startDate = minDate;
                            long adjSpan = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
                            count = Math.max(1, (int) Math.ceil((double) adjSpan / subDuration));
                            endDate = startDate.plusDays((long) count * subDuration - 1);
                        }
                    } else {
                        long span = java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate) + 1;
                        count = Math.max(1, (int) Math.ceil((double) span / subDuration));
                        startDate = minDate;
                        endDate = startDate.plusDays((long) count * subDuration - 1);
                    }

                    long startEpoch = startDate.toEpochDay();
                    long endEpoch = endDate.toEpochDay();
                    for (long[] r : existingRanges) {
                        if (endEpoch >= r[0] && startEpoch <= r[1]) {
                            if (startEpoch < r[0]) {
                                endDate = java.time.LocalDate.ofEpochDay(r[0] - 1);
                                endEpoch = endDate.toEpochDay();
                            } else {
                                startDate = java.time.LocalDate.ofEpochDay(r[1] + 1);
                                startEpoch = startDate.toEpochDay();
                            }
                        }
                    }

                    if (startEpoch > endEpoch) {
                        continue;
                    }

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
                                    endDate.getYear(), endDate.getYear(), endDate.getDayOfYear()
                            }))) {
                        while (datesCursor.moveToNext()) {
                            int yr = datesCursor.getInt(0);
                            int doy = datesCursor.getInt(1);
                            int st = datesCursor.getInt(2);
                            java.time.LocalDate d = java.time.LocalDate.ofYearDay(yr, doy);
                            if (st == 1 || st == 3) {
                                claims++;
                            } else if (d.isBefore(today)) {
                                misses++;
                            }
                        }
                    }

                    int claimed = claims * currencyPerDay;
                    int missed = misses * currencyPerDay;
                    int totalPrimogems = count * subDuration * currencyPerDay;
                    int wait = Math.max(0, totalPrimogems - claimed - missed);

                    String insertSubSql = "INSERT INTO `subscriptions` " +
                            "(`game_type`, `count`, `start_date`, `end_date`, `claimed`, `missed`, `wait`) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";
                    db.execSQL(insertSubSql, new Object[]{gameCode, count, startEpoch, endEpoch, claimed, missed, wait});

                    existingRanges.add(new long[]{startEpoch, endEpoch});

                    java.time.LocalDate cur = startDate;
                    while (!cur.isAfter(endDate)) {
                        int rem = (int) (endEpoch - cur.toEpochDay() + 1);
                        int updated;
                        try (androidx.sqlite.db.SupportSQLiteStatement updateStmt = db.compileStatement(
                                "UPDATE `calendar_game_status` SET `sub_days_remaining` = ? " +
                                        "WHERE `year` = ? AND `day_of_year` = ? AND `game_type` = ?")) {
                            updateStmt.bindLong(1, rem);
                            updateStmt.bindLong(2, cur.getYear());
                            updateStmt.bindLong(3, cur.getDayOfYear());
                            updateStmt.bindLong(4, gameCode);
                            updated = updateStmt.executeUpdateDelete();
                        }
                        if (updated == 0) {
                            try (androidx.sqlite.db.SupportSQLiteStatement insertCalStmt = db.compileStatement(
                                    "INSERT OR IGNORE INTO `calendar` (`day`, `day_of_year`, `day_of_week`, `month`, `year`) " +
                                            "VALUES (?, ?, ?, ?, ?)")) {
                                insertCalStmt.bindLong(1, cur.getDayOfMonth());
                                insertCalStmt.bindLong(2, cur.getDayOfYear());
                                insertCalStmt.bindLong(3, cur.getDayOfWeek().getValue());
                                insertCalStmt.bindLong(4, cur.getMonthValue());
                                insertCalStmt.bindLong(5, cur.getYear());
                                insertCalStmt.executeInsert();
                            }
                            try (androidx.sqlite.db.SupportSQLiteStatement insertStmt = db.compileStatement(
                                    "INSERT OR IGNORE INTO `calendar_game_status` (`year`, `day_of_year`, `game_type`, `status`, `sub_days_remaining`, `daily_status`) " +
                                            "VALUES (?, ?, ?, 0, ?, 0)")) {
                                insertStmt.bindLong(1, cur.getYear());
                                insertStmt.bindLong(2, cur.getDayOfYear());
                                insertStmt.bindLong(3, gameCode);
                                insertStmt.bindLong(4, rem);
                                insertStmt.executeInsert();
                            }
                        }
                        cur = cur.plusDays(1);
                    }
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            AppLogger.e("AppDatabase", "Error syncing subscriptions", e);
        } finally {
            db.endTransaction();
        }
    }

    public static void syncSavingsRaw(Context context, SupportSQLiteDatabase db) {
        db.beginTransaction();
        try {
            boolean hasManualGems = false;
            try (android.database.Cursor cursor = db.query(new androidx.sqlite.db.SimpleSQLiteQuery("PRAGMA table_info(`savings`)"))) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if ("manual_gems".equalsIgnoreCase(cursor.getString(nameIndex))) {
                        hasManualGems = true;
                        break;
                    }
                }
            }
            if (!hasManualGems) {
                db.execSQL("ALTER TABLE `savings` ADD COLUMN `manual_gems` INTEGER NOT NULL DEFAULT 0");
            }

            Preferences prefs = new Preferences(context);
            boolean alreadyMigrated = prefs.getBooleanPreference("savings_migrated_to_db", false);
            for (GameType game : GameType.values()) {
                int gameCode = game.getCode();
                boolean hasRow = false;
                try (android.database.Cursor cursor = db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                        "SELECT COUNT(*) FROM `savings` WHERE `game_type` = ?",
                        new Object[]{gameCode}))) {
                    if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                        hasRow = true;
                    }
                }

                if (!hasRow) {
                    int prefTarget = prefs.getPiggyBankTarget(game);
                    int prefManual = prefs.getPiggyBankManualProgress(game);
                    int prefSubs = prefs.getPiggyBankSubsProgress(game);
                    int prefDailies = prefs.getPiggyBankDailiesProgress(game);
                    db.execSQL("INSERT INTO `savings` (`game_type`, `target`, `manual_progress`, `subs_progress`, `dailies_progress`, `manual_gems`) VALUES (?, ?, ?, ?, ?, 0)",
                            new Object[]{gameCode, prefTarget, prefManual, prefSubs, prefDailies});
                } else if (!alreadyMigrated) {
                    int prefTarget = prefs.getPiggyBankTarget(game);
                    int prefManual = prefs.getPiggyBankManualProgress(game);
                    int prefSubs = prefs.getPiggyBankSubsProgress(game);
                    int prefDailies = prefs.getPiggyBankDailiesProgress(game);
                    if (prefTarget > 0 || prefManual != 0 || prefSubs > 0 || prefDailies > 0) {
                        db.execSQL("UPDATE `savings` SET `target` = ?, `manual_progress` = ?, `subs_progress` = ?, `dailies_progress` = ? WHERE `game_type` = ?",
                                new Object[]{prefTarget, prefManual, prefSubs, prefDailies, gameCode});
                    }
                }
            }
            prefs.saveBooleanPreference("savings_migrated_to_db", true);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            AppLogger.e("AppDatabase", "Error syncing savings", e);
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
        AppLogger.i("AppDatabase", "Database instance closed and reset.");
    }
}