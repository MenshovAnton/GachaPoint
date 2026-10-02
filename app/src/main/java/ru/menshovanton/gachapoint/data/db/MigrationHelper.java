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
}
