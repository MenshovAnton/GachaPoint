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
                    "CREATE TABLE IF NOT EXISTS `games` (" +
                            "`id` INTEGER NOT NULL, " +
                            "`code_name` TEXT NOT NULL, " +
                            "`display_name` TEXT NOT NULL, " +
                            "PRIMARY KEY(`id`))"
            );

            database.execSQL("INSERT OR IGNORE INTO `games` (`id`, `code_name`, `display_name`) VALUES (0, 'GENSHIN', 'Genshin')");
            database.execSQL("INSERT OR IGNORE INTO `games` (`id`, `code_name`, `display_name`) VALUES (1, 'HSR', 'HSR')");
            database.execSQL("INSERT OR IGNORE INTO `games` (`id`, `code_name`, `display_name`) VALUES (2, 'ZZZ', 'ZZZ')");

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
                            "PRIMARY KEY(`year`, `day_of_year`, `game_type`), " +
                            "FOREIGN KEY(`year`, `day_of_year`) REFERENCES `calendar`(`year`, `day_of_year`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`game_type`) REFERENCES `games`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)"
            );
            database.execSQL(
                    "INSERT INTO `calendar_game_status` (`year`, `day_of_year`, `game_type`, `status`, `sub_days_remaining`) " +
                            "SELECT `year`, `day_of_year`, `game_type`, `status`, `sub_days_remaining` FROM `calendar_temp_status`"
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
                            "FOREIGN KEY(`game_type`) REFERENCES `games`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)"
            );
            database.execSQL(
                    "INSERT INTO `pulls_new` (`id`, `game_type`, `date_time`, `drop_rare`, `drop_type`, `banner_type`, `is_reset_pity`) " +
                            "SELECT `id`, `game_type`, `date_time`, `drop_rare`, `drop_type`, `banner_type`, `is_reset_pity` FROM `pulls`"
            );
            database.execSQL("DROP TABLE `pulls`");
            database.execSQL("ALTER TABLE `pulls_new` RENAME TO `pulls`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_pull_game_banner` ON `pulls` (`game_type`, `banner_type`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_pull_game_type` ON `pulls` (`game_type`)");

            boolean hasSubscriptionsTable = false;
            try (android.database.Cursor cursor = database.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name='subscriptions'")) {
                hasSubscriptionsTable = cursor.moveToFirst();
            }

            if (hasSubscriptionsTable) {
                database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `subscriptions_new` (" +
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
                database.execSQL(
                        "INSERT OR IGNORE INTO `subscriptions_new` (`id`, `game_type`, `count`, `start_date`, `end_date`, `claimed`, `missed`, `wait`) " +
                                "SELECT `id`, CASE WHEN `game_type` IN (SELECT `id` FROM `games`) THEN `game_type` ELSE 0 END, " +
                                "`count`, `start_date`, `end_date`, `claimed`, `missed`, `wait` FROM `subscriptions`"
                );
                database.execSQL("DROP TABLE `subscriptions`");
                database.execSQL("ALTER TABLE `subscriptions_new` RENAME TO `subscriptions`");
            } else {
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
            }
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_subscription_game_dates` ON `subscriptions` (`game_type`, `start_date`, `end_date`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_subscription_game_type` ON `subscriptions` (`game_type`)");
        }
    };
}
