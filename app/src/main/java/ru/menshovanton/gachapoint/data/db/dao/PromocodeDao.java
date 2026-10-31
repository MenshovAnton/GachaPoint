package ru.menshovanton.gachapoint.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ru.menshovanton.gachapoint.data.db.entities.PromocodeStatusEntity;

@Dao
public interface PromocodeDao {

    @Query("SELECT * FROM promocode_statuses")
    List<PromocodeStatusEntity> getAllStatusesSync();

    @Query("SELECT * FROM promocode_statuses")
    LiveData<List<PromocodeStatusEntity>> getAllStatusesLiveData();

    @Query("SELECT * FROM promocode_statuses WHERE code_id = :codeId LIMIT 1")
    PromocodeStatusEntity getStatusSync(String codeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(PromocodeStatusEntity status);

    @Query("UPDATE promocode_statuses SET is_redeemed = :isRedeemed, redeemed_at = :redeemedAt WHERE code_id = :codeId")
    void updateRedeemed(String codeId, boolean isRedeemed, long redeemedAt);

    @Query("UPDATE promocode_statuses SET is_notified = 1 WHERE code_id = :codeId")
    void markAsNotified(String codeId);

    @Query("UPDATE promocode_statuses SET is_expired_by_user = :isExpired WHERE code_id = :codeId")
    void updateExpiredByUser(String codeId, boolean isExpired);
}
