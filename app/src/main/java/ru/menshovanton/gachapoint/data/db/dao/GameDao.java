package ru.menshovanton.gachapoint.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ru.menshovanton.gachapoint.data.db.entities.GameEntity;

@Dao
public interface GameDao {

    @Query("SELECT * FROM games ORDER BY id ASC")
    List<GameEntity> getAllGames();

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    GameEntity getGameById(int id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<GameEntity> games);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(GameEntity game);
}
