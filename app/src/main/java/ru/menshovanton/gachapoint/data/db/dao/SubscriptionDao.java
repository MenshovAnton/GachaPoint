package ru.menshovanton.gachapoint.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;

@Dao
public interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions WHERE game_type = :gameType AND :epochDay BETWEEN start_date AND end_date LIMIT 1")
    SubscriptionEntity getActiveSubscription(int gameType, long epochDay);

    @Query("SELECT COUNT(*) FROM subscriptions WHERE game_type = :gameType")
    int getSubscriptionsCountForGame(int gameType);

    @Query("SELECT * FROM subscriptions WHERE game_type = :gameType AND NOT (end_date < :startEpoch OR start_date > :endEpoch) ORDER BY start_date ASC")
    List<SubscriptionEntity> getSubscriptionsInRange(int gameType, long startEpoch, long endEpoch);

    @Query("SELECT * FROM subscriptions WHERE game_type = :gameType ORDER BY start_date DESC")
    List<SubscriptionEntity> getAllSubscriptionsForGame(int gameType);

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    SubscriptionEntity getById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SubscriptionEntity entity);

    @Update
    void update(SubscriptionEntity entity);

    @Delete
    void delete(SubscriptionEntity entity);

    @Query("DELETE FROM subscriptions WHERE id = :id")
    void deleteById(int id);
}
