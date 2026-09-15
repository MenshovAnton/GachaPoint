package ru.menshovanton.gachapoint.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;

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

    @Transaction
    default void updateSubscribeDaysTransaction(int startYear, int startDayOfYear, GameType gameType, int todayStatus, int totalDays) {
        List<CalendarEntity> batchList = new ArrayList<>(181);
        LocalDate startDate = LocalDate.ofYearDay(startYear, startDayOfYear);

        for (int i = 0; i <= 180; i++) {
            LocalDate targetDate = startDate.plusDays(i);
            int targetYear = targetDate.getYear();
            int targetDayOfYear = targetDate.getDayOfYear();

            CalendarEntity entity = getDay(targetYear, targetDayOfYear);
            if (entity != null) {
                int targetSubDays = Math.max(0, totalDays - i);
                int statusToSet = (i == 0) ? todayStatus : 0;

                entity.updateForGame(gameType, statusToSet, targetSubDays);
                batchList.add(entity);
            }
        }

        if (!batchList.isEmpty()) {
            insertOrUpdateBatch(batchList);
        }
    }
}