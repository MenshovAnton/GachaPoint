package ru.menshovanton.gachapoint.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import ru.menshovanton.gachapoint.data.db.entities.PullEntity;

@Dao
public interface PullDao {

    @Query("SELECT * FROM pulls WHERE game_type = :gameType AND banner_type = :bannerType ORDER BY id DESC")
    List<PullEntity> getPulls(int gameType, String bannerType);

    @Insert
    void insert(PullEntity pull);

    @Insert
    void insertAll(List<PullEntity> pulls);

    @Update
    void update(PullEntity pull);

    @Query("UPDATE pulls SET date_time = :dateTime, drop_type = :dropType, drop_rare = :dropRare, " +
            "banner_type = :bannerType, is_reset_pity = :isResetPity WHERE id = :id")
    void updatePullFields(int id, String dateTime, String dropType, String dropRare, String bannerType, boolean isResetPity);

    @Transaction
    default void addPullsBatch(String dateTime, String dropType, String dropRare, int count,
                               int gameType, String bannerType, boolean isResetPity) {
        for (int i = 0; i < count; i++) {
            boolean currentReset = isResetPity && (i == count - 1);
            PullEntity entity = new PullEntity(gameType, dateTime, dropRare, dropType, bannerType, currentReset);
            insert(entity);
        }
    }
}