package ru.menshovanton.gachapoint.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ru.menshovanton.gachapoint.data.db.entities.SavingsEntity;

@Dao
public interface SavingsDao {

    @Query("SELECT * FROM savings WHERE game_type = :gameType LIMIT 1")
    SavingsEntity getSavings(int gameType);

    @Query("SELECT * FROM savings")
    List<SavingsEntity> getAllSavings();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SavingsEntity savings);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertOrIgnoreAll(List<SavingsEntity> list);

    @Query("UPDATE savings SET target = :target WHERE game_type = :gameType")
    void updateTarget(int gameType, int target);

    @Query("UPDATE savings SET manual_progress = :manualProgress WHERE game_type = :gameType")
    void updateManualProgress(int gameType, int manualProgress);

    @Query("UPDATE savings SET manual_gems = :manualGems WHERE game_type = :gameType")
    void updateManualGems(int gameType, int manualGems);

    @Query("UPDATE savings SET manual_progress = :manualProgress, manual_gems = :manualGems WHERE game_type = :gameType")
    void updateManualProgressAndGems(int gameType, int manualProgress, int manualGems);

    @Query("UPDATE savings SET subs_progress = :subsProgress WHERE game_type = :gameType")
    void updateSubsProgress(int gameType, int subsProgress);

    @Query("UPDATE savings SET dailies_progress = :dailiesProgress WHERE game_type = :gameType")
    void updateDailiesProgress(int gameType, int dailiesProgress);

    @Query("UPDATE savings SET target = 0, manual_progress = 0, manual_gems = 0 WHERE game_type = :gameType")
    void reset(int gameType);
}
