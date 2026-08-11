package ru.menshovanton.gachapoint.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ru.menshovanton.gachapoint.db.entities.CalendarEntity;

@Dao
public interface CalendarDao {

    @Query("SELECT * FROM calendar ORDER BY id ASC")
    List<CalendarEntity> getAllCalendarEntries();

    @Query("SELECT * FROM calendar WHERE id BETWEEN :startId AND :endId ORDER BY id ASC")
    List<CalendarEntity> getCalendarRange(int startId, int endId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateBatch(List<CalendarEntity> entities);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(CalendarEntity entity);
}