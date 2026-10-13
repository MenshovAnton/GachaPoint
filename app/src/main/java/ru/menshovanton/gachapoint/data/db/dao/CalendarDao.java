package ru.menshovanton.gachapoint.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.domain.models.Date;

@Dao
public interface CalendarDao {

    @Query("SELECT COUNT(*) FROM calendar WHERE year = :year")
    int getYearEntriesCount(int year);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertCalendarDays(List<CalendarEntity> entities);

    // Note: Room directly maps SQL column aliases into ru.menshovanton.gachapoint.domain.models.Date
    // by matching field names (dayOfMonth, dayOfYear, dayOfWeek, year, month, status, subDaysRemaining, dailyStatus).
    @Query("SELECT c.day AS dayOfMonth, c.day_of_year AS dayOfYear, c.day_of_week AS dayOfWeek, " +
            "c.year AS year, c.month AS month, " +
            "COALESCE(s.status, 0) AS status, " +
            "COALESCE(s.sub_days_remaining, 0) AS subDaysRemaining, " +
            "COALESCE(s.daily_status, 0) AS dailyStatus " +
            "FROM calendar c " +
            "LEFT JOIN calendar_game_status s ON c.year = s.year AND c.day_of_year = s.day_of_year AND s.game_type = :gameType " +
            "WHERE c.year = :year AND c.month = :month " +
            "ORDER BY c.day_of_year ASC")
    List<Date> getCalendarForMonth(int year, int month, int gameType);

    @Query("SELECT c.day AS dayOfMonth, c.day_of_year AS dayOfYear, c.day_of_week AS dayOfWeek, " +
            "c.year AS year, c.month AS month, " +
            "COALESCE(s.status, 0) AS status, " +
            "COALESCE(s.sub_days_remaining, 0) AS subDaysRemaining, " +
            "COALESCE(s.daily_status, 0) AS dailyStatus " +
            "FROM calendar c " +
            "LEFT JOIN calendar_game_status s ON c.year = s.year AND c.day_of_year = s.day_of_year AND s.game_type = :gameType " +
            "WHERE c.year = :year AND c.day_of_year = :dayOfYear " +
            "LIMIT 1")
    Date getDay(int year, int dayOfYear, int gameType);

    @Query("SELECT c.day AS dayOfMonth, c.day_of_year AS dayOfYear, c.day_of_week AS dayOfWeek, " +
            "c.year AS year, c.month AS month, " +
            "COALESCE(s.status, 0) AS status, " +
            "COALESCE(s.sub_days_remaining, 0) AS subDaysRemaining, " +
            "COALESCE(s.daily_status, 0) AS dailyStatus " +
            "FROM calendar c " +
            "LEFT JOIN calendar_game_status s ON c.year = s.year AND c.day_of_year = s.day_of_year AND s.game_type = :gameType " +
            "WHERE c.year = :year AND c.day_of_year BETWEEN :startDay AND :endDay " +
            "ORDER BY c.day_of_year ASC")
    List<Date> getDaysRange(int year, int startDay, int endDay, int gameType);

    @Query("SELECT * FROM calendar_game_status WHERE year = :year AND day_of_year = :dayOfYear AND game_type = :gameType LIMIT 1")
    CalendarGameStatusEntity getGameStatus(int year, int dayOfYear, int gameType);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateGameStatus(CalendarGameStatusEntity statusEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateGameStatusBatch(List<CalendarGameStatusEntity> statusEntities);

    @Transaction
    default void updateSubscribeDaysTransaction(List<CalendarGameStatusEntity> statusEntities) {
        if (statusEntities != null && !statusEntities.isEmpty()) {
            insertOrUpdateGameStatusBatch(statusEntities);
        }
    }

    @Query("UPDATE calendar_game_status SET status = 0, sub_days_remaining = 0 " +
            "WHERE game_type = :gameType AND year = :year AND day_of_year BETWEEN :startDay AND :endDay")
    void resetStatusesInRangeSameYear(int gameType, int year, int startDay, int endDay);
}