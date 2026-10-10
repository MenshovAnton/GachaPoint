package ru.menshovanton.gachapoint.data.db.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import ru.menshovanton.gachapoint.domain.enums.GameType;

@Entity(tableName = "games")
public class GameEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id;

    @NonNull
    @ColumnInfo(name = "code_name")
    public String codeName;

    @NonNull
    @ColumnInfo(name = "display_name")
    public String displayName;

    public GameEntity() {
        this.codeName = "";
        this.displayName = "";
    }

    @Ignore
    public GameEntity(int id, @NonNull String codeName, @NonNull String displayName) {
        this.id = id;
        this.codeName = codeName;
        this.displayName = displayName;
    }

    @Ignore
    public GameEntity(@NonNull GameType gameType) {
        this.id = gameType.getCode();
        this.codeName = gameType.name();
        this.displayName = gameType.getName();
    }
}
