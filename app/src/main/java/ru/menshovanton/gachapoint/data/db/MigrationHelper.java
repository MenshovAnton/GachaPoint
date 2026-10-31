package ru.menshovanton.gachapoint.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class MigrationHelper {

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            AppLogger.i("MigrationHelper", "Starting Room database migration from version 1 to 2...");
            try {
                migrateLegacyCalendarAndSchemaIfNeeded(database);
                AppLogger.i("MigrationHelper", "Room database migration 1 -> 2 completed successfully.");
            } catch (Throwable t) {
                AppLogger.e("MigrationHelper", "Room database migration 1 -> 2 FAILED!", t);
                throw t;
            }
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NotNull SupportSQLiteDatabase database) {
            AppLogger.i("MigrationHelper", "Starting Room database migration from version 2 to 3...");
            try {
                migrateLegacyCalendarAndSchemaIfNeeded(database);

                boolean hasOrderIndex = false;
                try (android.database.Cursor cursor = database.query(new SimpleSQLiteQuery("PRAGMA table_info(`pulls`)"))) {
                    int nameIndex = cursor.getColumnIndex("name");
                    while (cursor.moveToNext()) {
                        if ("order_index".equalsIgnoreCase(cursor.getString(nameIndex))) {
                            hasOrderIndex = true;
                            break;
                        }
                    }
                }
                if (!hasOrderIndex) {
                    database.execSQL("ALTER TABLE `pulls` ADD COLUMN `order_index` INTEGER NOT NULL DEFAULT 0");
                    database.execSQL("UPDATE `pulls` SET `order_index` = `id`");
                }
                database.execSQL("DROP INDEX IF EXISTS `idx_pull_order`");

                boolean hasManualGems = false;
                try (android.database.Cursor cursor = database.query(new SimpleSQLiteQuery("PRAGMA table_info(`savings`)"))) {
                    int nameIndex = cursor.getColumnIndex("name");
                    while (cursor.moveToNext()) {
                        if ("manual_gems".equalsIgnoreCase(cursor.getString(nameIndex))) {
                            hasManualGems = true;
                            break;
                        }
                    }
                }
                if (!hasManualGems) {
                    database.execSQL("ALTER TABLE `savings` ADD COLUMN `manual_gems` INTEGER NOT NULL DEFAULT 0");
                }

                AppLogger.i("MigrationHelper", "Room database migration 2 -> 3 completed successfully.");
            } catch (Throwable t) {
                AppLogger.e("MigrationHelper", "Room database migration 2 -> 3 FAILED!", t);
                throw t;
            }
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NotNull SupportSQLiteDatabase database) {
            AppLogger.i("MigrationHelper", "Starting Room database migration from version 3 to 4 (Honkai Impact 3rd support)...");
            try {
                database.execSQL("INSERT OR IGNORE INTO `games` (`id`, `code_name`, `display_name`) VALUES (3, 'HI3', 'HI3')");
                database.execSQL("INSERT OR IGNORE INTO `savings` (`game_type`, `target`, `manual_progress`, `subs_progress`, `dailies_progress`, `manual_gems`) " +
                        "VALUES (3, 0, 0, 0, 0, 0)");
                AppLogger.i("MigrationHelper", "Room database migration 3 -> 4 completed successfully.");
            } catch (Throwable t) {
                AppLogger.e("MigrationHelper", "Room database migration 3 -> 4 FAILED!", t);
                throw t;
            }
        }
    };

    public static void migrateLegacyCalendarAndSchemaIfNeeded(SupportSQLiteDatabase database) {
        boolean hasGameStatus = false;
        try (android.database.Cursor cur = database.query(new SimpleSQLiteQuery(
                "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='calendar_game_status'"))) {
            if (cur.moveToFirst() && cur.getInt(0) > 0) {
                hasGameStatus = true;
            }
        }

        if (hasGameStatus) {
            return;
        }

        boolean hasLegacyCalendarColumns = false;
        try (android.database.Cursor cur = database.query(new SimpleSQLiteQuery("PRAGMA table_info(`calendar`)"))) {
            int nameIdx = cur.getColumnIndex("name");
            while (cur.moveToNext()) {
                if ("status_genshin".equalsIgnoreCase(cur.getString(nameIdx))) {
                    hasLegacyCalendarColumns = true;
                    break;
                }
            }
        }

        if (!hasLegacyCalendarColumns) {
            return;
        }

        AppLogger.i("MigrationHelper", "Legacy calendar table detected. Migrating to normalized schema...");

        database.beginTransaction();
        try {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `games` (" +
                            "`id` INTEGER NOT NULL, " +
                            "`code_name` TEXT NOT NULL, " +
                            "`display_name` TEXT NOT NULL, " +
                            "PRIMARY KEY(`id`))"
            );

            database.execSQL("INSERT OR IGNORE INTO `games` (`id`, `code_name`, `display_name`) VALUES (0, 'GENSHIN', 'Genshin')");
            database.execSQL("INSERT OR IGNORE INTO `games` (`id`, `code_name`, `display_name`) VALUES (1, 'HSR', 'HSR')");
            database.execSQL("INSERT OR IGNORE INTO `games` (`id`, `code_name`, `display_name`) VALUES (2, 'ZZZ', 'ZZZ')");
            database.execSQL("INSERT OR IGNORE INTO `games` (`id`, `code_name`, `display_name`) VALUES (3, 'HI3', 'HI3')");

            database.execSQL(
                    "CREATE TEMP TABLE `calendar_temp_status` AS " +
                            "SELECT `year`, `day_of_year`, 0 AS `game_type`, `status_genshin` AS `status`, `moon_days_remaining` AS `sub_days_remaining` " +
                            "FROM `calendar` WHERE `status_genshin` != 0 OR `moon_days_remaining` != 0 " +
                            "UNION ALL " +
                            "SELECT `year`, `day_of_year`, 1 AS `game_type`, `status_hsr` AS `status`, `express_pass_days_remaining` AS `sub_days_remaining` " +
                            "FROM `calendar` WHERE `status_hsr` != 0 OR `express_pass_days_remaining` != 0 " +
                            "UNION ALL " +
                            "SELECT `year`, `day_of_year`, 2 AS `game_type`, `status_zzz` AS `status`, `interknot_days_remaining` AS `sub_days_remaining` " +
                            "FROM `calendar` WHERE `status_zzz` != 0 OR `interknot_days_remaining` != 0"
            );

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `calendar_new` (" +
                            "`day` INTEGER NOT NULL, " +
                            "`day_of_year` INTEGER NOT NULL, " +
                            "`day_of_week` INTEGER NOT NULL, " +
                            "`month` INTEGER NOT NULL, " +
                            "`year` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`year`, `day_of_year`))"
            );
            database.execSQL(
                    "INSERT INTO `calendar_new` (`day`, `day_of_year`, `day_of_week`, `month`, `year`) " +
                            "SELECT `day`, `day_of_year`, `day_of_week`, `month`, `year` FROM `calendar`"
            );
            database.execSQL("DROP TABLE `calendar`");
            database.execSQL("ALTER TABLE `calendar_new` RENAME TO `calendar`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_calendar_year_month` ON `calendar` (`year`, `month`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_calendar_day_of_year` ON `calendar` (`day_of_year`)");

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `calendar_game_status` (" +
                            "`year` INTEGER NOT NULL, " +
                            "`day_of_year` INTEGER NOT NULL, " +
                            "`game_type` INTEGER NOT NULL, " +
                            "`status` INTEGER NOT NULL DEFAULT 0, " +
                            "`sub_days_remaining` INTEGER NOT NULL DEFAULT 0, " +
                            "`daily_status` INTEGER NOT NULL DEFAULT 0, " +
                            "PRIMARY KEY(`year`, `day_of_year`, `game_type`), " +
                            "FOREIGN KEY(`year`, `day_of_year`) REFERENCES `calendar`(`year`, `day_of_year`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`game_type`) REFERENCES `games`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)"
            );
            database.execSQL(
                    "INSERT INTO `calendar_game_status` (`year`, `day_of_year`, `game_type`, `status`, `sub_days_remaining`, `daily_status`) " +
                            "SELECT `year`, `day_of_year`, `game_type`, `status`, `sub_days_remaining`, 0 FROM `calendar_temp_status`"
            );
            database.execSQL("DROP TABLE `calendar_temp_status`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_calendar_status_day` ON `calendar_game_status` (`year`, `day_of_year`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_calendar_status_game` ON `calendar_game_status` (`game_type`)");

            database.execSQL(
                    "UPDATE `pulls` SET `game_type` = 0 WHERE `game_type` NOT IN (SELECT `id` FROM `games`)"
            );
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pulls_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`game_type` INTEGER NOT NULL, " +
                            "`date_time` TEXT, " +
                            "`drop_rare` TEXT, " +
                            "`drop_type` TEXT, " +
                            "`banner_type` TEXT DEFAULT 'event', " +
                            "`is_reset_pity` INTEGER NOT NULL DEFAULT 0, " +
                            "`pity_status` TEXT DEFAULT 'NONE', " +
                            "`order_index` INTEGER NOT NULL DEFAULT 0, " +
                            "FOREIGN KEY(`game_type`) REFERENCES `games`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)"
            );
            database.execSQL(
                    "INSERT INTO `pulls_new` (`id`, `game_type`, `date_time`, `drop_rare`, `drop_type`, `banner_type`, `is_reset_pity`, `pity_status`, `order_index`) " +
                            "SELECT `id`, `game_type`, `date_time`, `drop_rare`, `drop_type`, " +
                            "COALESCE(`banner_type`, 'event'), `is_reset_pity`, 'NONE', `id` FROM `pulls`"
            );
            database.execSQL("DROP TABLE `pulls`");
            database.execSQL("ALTER TABLE `pulls_new` RENAME TO `pulls`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_pull_game_banner` ON `pulls` (`game_type`, `banner_type`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_pull_game_type` ON `pulls` (`game_type`)");

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `subscriptions` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`game_type` INTEGER NOT NULL, " +
                            "`count` INTEGER NOT NULL, " +
                            "`start_date` INTEGER NOT NULL, " +
                            "`end_date` INTEGER NOT NULL, " +
                            "`claimed` INTEGER NOT NULL DEFAULT 0, " +
                            "`missed` INTEGER NOT NULL DEFAULT 0, " +
                            "`wait` INTEGER NOT NULL DEFAULT 0, " +
                            "FOREIGN KEY(`game_type`) REFERENCES `games`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)"
            );
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_subscription_game_dates` ON `subscriptions` (`game_type`, `start_date`, `end_date`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_subscription_game_type` ON `subscriptions` (`game_type`)");

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `savings` (" +
                            "`game_type` INTEGER NOT NULL, " +
                            "`target` INTEGER NOT NULL DEFAULT 0, " +
                            "`manual_progress` INTEGER NOT NULL DEFAULT 0, " +
                            "`subs_progress` INTEGER NOT NULL DEFAULT 0, " +
                            "`dailies_progress` INTEGER NOT NULL DEFAULT 0, " +
                            "`manual_gems` INTEGER NOT NULL DEFAULT 0, " +
                            "PRIMARY KEY(`game_type`), " +
                            "FOREIGN KEY(`game_type`) REFERENCES `games`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)"
            );

            database.execSQL("INSERT OR IGNORE INTO `savings` (`game_type`, `target`, `manual_progress`, `subs_progress`, `dailies_progress`, `manual_gems`) " +
                    "VALUES (0, 0, 0, 0, 0, 0), (1, 0, 0, 0, 0, 0), (2, 0, 0, 0, 0, 0), (3, 0, 0, 0, 0, 0)");

            syncSubscriptionsRaw(database);
            database.setTransactionSuccessful();
            AppLogger.i("MigrationHelper", "Legacy calendar migration completed successfully.");
        } finally {
            database.endTransaction();
        }
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
            LocalDate today = LocalDate.now();
            long todayEpoch = today.toEpochDay();

            List<Integer> corruptSubIds = new ArrayList<>();
            try (android.database.Cursor cur = db.query(new SimpleSQLiteQuery(
                    "SELECT `id`, `game_type`, `count`, `start_date`, `end_date` FROM `subscriptions` WHERE `count` > 1"))) {
                while (cur.moveToNext()) {
                    int subId = cur.getInt(0);
                    int gType = cur.getInt(1);
                    long sEpoch = cur.getLong(3);
                    long eEpoch = cur.getLong(4);

                    try (android.database.Cursor statusCur = db.query(new SimpleSQLiteQuery(
                            "SELECT `year`, `day_of_year`, `sub_days_remaining` " +
                                    "FROM `calendar_game_status` " +
                                    "WHERE `game_type` = ? AND `sub_days_remaining` > 0",
                            new Object[]{gType}))) {
                        while (statusCur.moveToNext()) {
                            int y = statusCur.getInt(0);
                            int doy = statusCur.getInt(1);
                            int rem = statusCur.getInt(2);
                            LocalDate d = LocalDate.ofYearDay(y, doy);
                            long dEpoch = d.toEpochDay();
                            if (dEpoch >= sEpoch && dEpoch <= eEpoch) {
                                LocalDate expectedEnd = d.plusDays(rem - 1);
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
                try (android.database.Cursor cur = db.query(new SimpleSQLiteQuery(
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
                    final LocalDate date;
                    final long epoch;

                    DayRecord(int y, int doy, int st, int rem) {
                        this.year = y;
                        this.dayOfYear = doy;
                        this.status = st;
                        this.subDays = rem;
                        this.date = LocalDate.ofYearDay(y, doy);
                        this.epoch = this.date.toEpochDay();
                    }
                }

                List<DayRecord> uncovered = new ArrayList<>();
                try (android.database.Cursor cur = db.query(new SimpleSQLiteQuery(
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
                    LocalDate maxEndInCluster = currentCluster.get(0).date.plusDays(Math.max(1, currentCluster.get(0).subDays) - 1);
                    for (DayRecord r : currentCluster) {
                        if (r.subDays > 0) {
                            LocalDate endR = r.date.plusDays(r.subDays - 1);
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
                    LocalDate minDate = cluster.get(0).date;
                    LocalDate maxDate = cluster.get(cluster.size() - 1).date;

                    LocalDate candidateEnd = null;
                    for (DayRecord r : cluster) {
                        if (r.subDays > 0) {
                            LocalDate endFromRec = r.date.plusDays(r.subDays - 1);
                            if (candidateEnd == null || endFromRec.isAfter(candidateEnd)) {
                                candidateEnd = endFromRec;
                            }
                        }
                    }

                    LocalDate startDate;
                    LocalDate endDate;
                    int count;

                    if (candidateEnd != null) {
                        endDate = candidateEnd;
                        long span = ChronoUnit.DAYS.between(minDate, endDate) + 1;
                        count = Math.max(1, (int) Math.ceil((double) span / subDuration));
                        startDate = endDate.minusDays((long) count * subDuration - 1);
                        if (startDate.isAfter(minDate)) {
                            startDate = minDate;
                            long adjSpan = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                            count = Math.max(1, (int) Math.ceil((double) adjSpan / subDuration));
                            endDate = startDate.plusDays((long) count * subDuration - 1);
                        }
                    } else {
                        long span = ChronoUnit.DAYS.between(minDate, maxDate) + 1;
                        count = Math.max(1, (int) Math.ceil((double) span / subDuration));
                        startDate = minDate;
                        endDate = startDate.plusDays((long) count * subDuration - 1);
                    }

                    long startEpoch = startDate.toEpochDay();
                    long endEpoch = endDate.toEpochDay();
                    for (long[] r : existingRanges) {
                        if (endEpoch >= r[0] && startEpoch <= r[1]) {
                            if (startEpoch < r[0]) {
                                endDate = LocalDate.ofEpochDay(r[0] - 1);
                                endEpoch = endDate.toEpochDay();
                            } else {
                                startDate = LocalDate.ofEpochDay(r[1] + 1);
                                startEpoch = startDate.toEpochDay();
                            }
                        }
                    }

                    if (startEpoch > endEpoch) {
                        continue;
                    }

                    int claims = 0;
                    int misses = 0;
                    try (android.database.Cursor datesCursor = db.query(new SimpleSQLiteQuery(
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
                            LocalDate d = LocalDate.ofYearDay(yr, doy);
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

                    LocalDate cur = startDate;
                    while (!cur.isAfter(endDate)) {
                        int rem = (int) (endEpoch - cur.toEpochDay() + 1);
                        int updated;
                        try (SupportSQLiteStatement updateStmt = db.compileStatement(
                                "UPDATE `calendar_game_status` SET `sub_days_remaining` = ? " +
                                        "WHERE `year` = ? AND `day_of_year` = ? AND `game_type` = ?")) {
                            updateStmt.bindLong(1, rem);
                            updateStmt.bindLong(2, cur.getYear());
                            updateStmt.bindLong(3, cur.getDayOfYear());
                            updateStmt.bindLong(4, gameCode);
                            updated = updateStmt.executeUpdateDelete();
                        }
                        if (updated == 0) {
                            try (SupportSQLiteStatement insertCalStmt = db.compileStatement(
                                    "INSERT OR IGNORE INTO `calendar` (`day`, `day_of_year`, `day_of_week`, `month`, `year`) " +
                                            "VALUES (?, ?, ?, ?, ?)")) {
                                insertCalStmt.bindLong(1, cur.getDayOfMonth());
                                insertCalStmt.bindLong(2, cur.getDayOfYear());
                                insertCalStmt.bindLong(3, cur.getDayOfWeek().getValue());
                                insertCalStmt.bindLong(4, cur.getMonthValue());
                                insertCalStmt.bindLong(5, cur.getYear());
                                insertCalStmt.executeInsert();
                            }
                            try (SupportSQLiteStatement insertStmt = db.compileStatement(
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
            AppLogger.e("MigrationHelper", "Error syncing subscriptions", e);
        } finally {
            db.endTransaction();
        }
    }

    public static void syncSavingsRaw(Context context, SupportSQLiteDatabase db) {
        db.beginTransaction();
        try {
            boolean hasManualGems = false;
            try (android.database.Cursor cursor = db.query(new SimpleSQLiteQuery("PRAGMA table_info(`savings`)"))) {
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
                try (android.database.Cursor cursor = db.query(new SimpleSQLiteQuery(
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
            AppLogger.e("MigrationHelper", "Error syncing savings", e);
        } finally {
            db.endTransaction();
        }
    }
}
