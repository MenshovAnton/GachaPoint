package ru.menshovanton.gachapoint.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import ru.menshovanton.gachapoint.db.entities.WishEntity;

@Dao
public interface WishDao {

    @Query("SELECT * FROM wishes WHERE game_type = :gameType AND banner_type = :bannerType ORDER BY id DESC")
    List<WishEntity> getWishes(int gameType, String bannerType);

    @Insert
    void insert(WishEntity wish);

    @Insert
    void insertAll(List<WishEntity> wishes);

    @Update
    void update(WishEntity wish);

    @Query("UPDATE wishes SET date_time = :dateTime, drop_type = :dropType, drop_rare = :dropRare, " +
            "banner_type = :bannerType, is_reset_pity = :isResetPity WHERE id = :id")
    void updateWishFields(int id, String dateTime, String dropType, String dropRare, String bannerType, boolean isResetPity);

    @Transaction
    default void addWishesBatch(String dateTime, String dropType, String dropRare, int count,
                                int gameType, String bannerType, boolean isResetPity) {
        for (int i = 0; i < count; i++) {
            boolean currentReset = isResetPity && (i == count - 1);
            WishEntity entity = new WishEntity(gameType, dateTime, dropRare, dropType, bannerType, currentReset);
            insert(entity);
        }
    }
}