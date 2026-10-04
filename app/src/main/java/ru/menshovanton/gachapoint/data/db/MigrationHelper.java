package ru.menshovanton.gachapoint.data.db;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class MigrationHelper {
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_calendar_year_month ON calendar (year, month)"
            );
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_calendar_day_of_year ON calendar (day_of_year)"
            );
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_pull_game_banner ON pulls (game_type, banner_type)"
            );
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `subscriptions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`game_type` INTEGER NOT NULL, " +
                        "`count` INTEGER NOT NULL, " +
                        "`start_date` INTEGER NOT NULL, " +
                        "`end_date` INTEGER NOT NULL, " +
                        "`claimed` INTEGER NOT NULL DEFAULT 0, " +
                        "`missed` INTEGER NOT NULL DEFAULT 0, " +
                        "`wait` INTEGER NOT NULL DEFAULT 0)"
            );
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `idx_subscription_game_dates` ON `subscriptions` (`game_type`, `start_date`, `end_date`)"
            );
        }
    };
}
