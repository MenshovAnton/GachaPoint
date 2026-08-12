package ru.menshovanton.gachapoint.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ru.menshovanton.gachapoint.db.entities.CalendarEntity;

@Dao
public interface CalendarDao {

    @Query("SELECT * FROM calendar WHERE year = :year AND month = :month ORDER BY day_of_year ASC")
    List<CalendarEntity> getCalendarForMonth(int year, int month);

    @Query("SELECT * FROM calendar WHERE year = :year AND day_of_year = :dayOfYear LIMIT 1")
    CalendarEntity getDay(int year, int dayOfYear);

    @Query("SELECT * FROM calendar WHERE year = :year AND day_of_year BETWEEN :startDay AND :endDay ORDER BY day_of_year ASC")
    List<CalendarEntity> getDaysRange(int year, int startDay, int endDay);

    @Query("SELECT COUNT(*) FROM calendar WHERE year = :year")
    int getYearEntriesCount(int year);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateBatch(List<CalendarEntity> entities);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(CalendarEntity entity);
}