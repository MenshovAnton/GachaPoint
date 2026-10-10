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
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Date;

@Dao
public interface CalendarDao {

    @Query("SELECT COUNT(*) FROM calendar WHERE year = :year")
    int getYearEntriesCount(int year);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertCalendarDays(List<CalendarEntity> entities);

    @Query("SELECT c.day AS dayOfMonth, c.day_of_year AS dayOfYear, c.day_of_week AS dayOfWeek, " +
            "c.year AS year, c.month AS month, " +
            "COALESCE(s.status, 0) AS status, " +
            "COALESCE(s.sub_days_remaining, 0) AS subDaysRemaining " +
            "FROM calendar c " +
            "LEFT JOIN calendar_game_status s ON c.year = s.year AND c.day_of_year = s.day_of_year AND s.game_type = :gameType " +
            "WHERE c.year = :year AND c.month = :month " +
            "ORDER BY c.day_of_year ASC")
    List<Date> getCalendarForMonth(int year, int month, int gameType);

    @Query("SELECT c.day AS dayOfMonth, c.day_of_year AS dayOfYear, c.day_of_week AS dayOfWeek, " +
            "c.year AS year, c.month AS month, " +
            "COALESCE(s.status, 0) AS status, " +
            "COALESCE(s.sub_days_remaining, 0) AS subDaysRemaining " +
            "FROM calendar c " +
            "LEFT JOIN calendar_game_status s ON c.year = s.year AND c.day_of_year = s.day_of_year AND s.game_type = :gameType " +
            "WHERE c.year = :year AND c.day_of_year = :dayOfYear " +
            "LIMIT 1")
    Date getDay(int year, int dayOfYear, int gameType);

    @Query("SELECT c.day AS dayOfMonth, c.day_of_year AS dayOfYear, c.day_of_week AS dayOfWeek, " +
            "c.year AS year, c.month AS month, " +
            "COALESCE(s.status, 0) AS status, " +
            "COALESCE(s.sub_days_remaining, 0) AS subDaysRemaining " +
            "FROM calendar c " +
            "LEFT JOIN calendar_game_status s ON c.year = s.year AND c.day_of_year = s.day_of_year AND s.game_type = :gameType " +
            "WHERE c.year = :year AND c.day_of_year BETWEEN :startDay AND :endDay " +
            "ORDER BY c.day_of_year ASC")
    List<Date> getDaysRange(int year, int startDay, int endDay, int gameType);

    @Query("SELECT c.day AS dayOfMonth, c.day_of_year AS dayOfYear, c.day_of_week AS dayOfWeek, " +
            "c.year AS year, c.month AS month, " +
            "COALESCE(s.status, 0) AS status, " +
            "COALESCE(s.sub_days_remaining, 0) AS subDaysRemaining " +
            "FROM calendar c " +
            "LEFT JOIN calendar_game_status s ON c.year = s.year AND c.day_of_year = s.day_of_year AND s.game_type = :gameType " +
            "WHERE c.year = :year AND c.day_of_year BETWEEN :from AND :to " +
            "ORDER BY c.day_of_year ASC")
    List<Date> getDaysRangeInclusive(int year, int from, int to, int gameType);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateGameStatus(CalendarGameStatusEntity statusEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateGameStatusBatch(List<CalendarGameStatusEntity> statusEntities);

    @Transaction
    default void updateSubscribeDaysTransaction(int startYear, int startDayOfYear, GameType gameType, int todayStatus, int totalDays) {
        if (gameType == null) return;
        int gameTypeCode = gameType.getCode();
        int span = (totalDays > 0) ? totalDays : 180;

        LocalDate startDate = LocalDate.ofYearDay(startYear, startDayOfYear);
        LocalDate endDate = startDate.plusDays(span - 1);

        List<Date> all = new ArrayList<>(span);
        if (startDate.getYear() == endDate.getYear()) {
            all.addAll(getDaysRangeInclusive(startDate.getYear(),
                    startDate.getDayOfYear(), endDate.getDayOfYear(), gameTypeCode));
        } else {
            all.addAll(getDaysRangeInclusive(startDate.getYear(),
                    startDate.getDayOfYear(), startDate.lengthOfYear(), gameTypeCode));
            all.addAll(getDaysRangeInclusive(endDate.getYear(),
                    1, endDate.getDayOfYear(), gameTypeCode));
        }

        List<CalendarGameStatusEntity> batchList = new ArrayList<>(all.size());
        int i = 0;
        for (Date date : all) {
            int targetSubDays = Math.max(0, totalDays - i);
            int statusToSet;
            if (totalDays == 0) {
                statusToSet = 0;
            } else {
                statusToSet = (i == 0)
                        ? todayStatus
                        : (targetSubDays > 0 ? date.status : 0);
            }
            batchList.add(new CalendarGameStatusEntity(date.year, date.dayOfYear, gameTypeCode, statusToSet, targetSubDays));
            i++;
        }

        if (!batchList.isEmpty()) {
            insertOrUpdateGameStatusBatch(batchList);
        }
    }
}