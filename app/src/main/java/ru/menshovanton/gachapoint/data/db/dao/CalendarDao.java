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

    @Query("SELECT * FROM calendar WHERE year = :year AND day_of_year BETWEEN :from AND :to ORDER BY day_of_year ASC")
    List<CalendarEntity> getDaysRangeInclusive(int year, int from, int to);

    @Transaction
    default void updateSubscribeDaysTransaction(int startYear, int startDayOfYear, GameType gameType, int todayStatus, int totalDays) {

        int span = (totalDays > 0) ? totalDays : 180;

        LocalDate startDate = LocalDate.ofYearDay(startYear, startDayOfYear);
        LocalDate endDate = startDate.plusDays(span - 1);

        List<CalendarEntity> all = new ArrayList<>(span);
        if (startDate.getYear() == endDate.getYear()) {
            all.addAll(getDaysRangeInclusive(startDate.getYear(),
                    startDate.getDayOfYear(), endDate.getDayOfYear()));
        } else {
            all.addAll(getDaysRangeInclusive(startDate.getYear(),
                    startDate.getDayOfYear(), startDate.lengthOfYear()));
            all.addAll(getDaysRangeInclusive(endDate.getYear(),
                    1, endDate.getDayOfYear()));
        }

        List<CalendarEntity> batchList = new ArrayList<>(all.size());
        int i = 0;
        for (CalendarEntity entity : all) {
            int targetSubDays = Math.max(0, totalDays - i);
            int statusToSet;
            if (totalDays == 0) {
                statusToSet = 0;
            } else {
                statusToSet = (i == 0)
                        ? todayStatus
                        : (targetSubDays > 0 ? entity.getStatusForGame(gameType) : 0);
            }
            entity.updateForGame(gameType, statusToSet, targetSubDays);
            batchList.add(entity);
            i++;
        }

        if (!batchList.isEmpty()) {
            insertOrUpdateBatch(batchList);
        }
    }
}