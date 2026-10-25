package ru.menshovanton.gachapoint.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ru.menshovanton.gachapoint.data.db.entities.PullEntity;

@Dao
public interface PullDao {

    @Query("SELECT * FROM pulls WHERE game_type = :gameType AND banner_type = :bannerType ORDER BY order_index DESC, id DESC")
    List<PullEntity> getPulls(int gameType, String bannerType);

    @Query("SELECT * FROM pulls WHERE game_type = :gameType ORDER BY order_index ASC, id ASC")
    List<PullEntity> getAllPullsForGame(int gameType);

    @Insert
    void insert(PullEntity pull);

    @Insert
    void insertAll(List<PullEntity> pulls);

    @Update
    void update(PullEntity pull);

    @Query("UPDATE pulls SET date_time = :dateTime, drop_type = :dropType, drop_rare = :dropRare, " +
            "banner_type = :bannerType, is_reset_pity = :isResetPity, pity_status = :pityStatus WHERE id = :id")
    void updatePullFields(int id, String dateTime, String dropType, String dropRare, String bannerType, boolean isResetPity, String pityStatus);

    @Query("UPDATE pulls SET order_index = :orderIndex WHERE id = :id")
    void updateOrderIndex(int id, long orderIndex);

    @Query("SELECT MAX(order_index) FROM pulls WHERE game_type = :gameType AND banner_type = :bannerType")
    Long getMaxOrderIndex(int gameType, String bannerType);

    @Query("DELETE FROM pulls WHERE id = :id")
    void deletePull(int id);

    @Query("DELETE FROM pulls WHERE id IN (:ids)")
    void deletePullsBatch(List<Integer> ids);
}