package ru.menshovanton.gachapoint.data.db.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

import ru.menshovanton.gachapoint.data.db.entities.GameEntity;

@Dao
public interface GameDao {

    @Query("SELECT * FROM games ORDER BY id ASC")
    List<GameEntity> getAllGames();

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    GameEntity getGameById(int id);

    @Upsert
    void upsertAll(List<GameEntity> games);
}
