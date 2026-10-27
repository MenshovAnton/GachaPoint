package ru.menshovanton.gachapoint.data.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import ru.menshovanton.gachapoint.domain.enums.GameType;

@Entity(
        tableName = "savings",
        foreignKeys = @ForeignKey(
                entity = GameEntity.class,
                parentColumns = "id",
                childColumns = "game_type",
                onDelete = ForeignKey.RESTRICT
        )
)
public class SavingsEntity {

    @PrimaryKey
    @ColumnInfo(name = "game_type")
    public int gameType;

    @ColumnInfo(name = "target", defaultValue = "0")
    public int target;

    @ColumnInfo(name = "manual_progress", defaultValue = "0")
    public int manualProgress;

    @ColumnInfo(name = "subs_progress", defaultValue = "0")
    public int subsProgress;

    @ColumnInfo(name = "dailies_progress", defaultValue = "0")
    public int dailiesProgress;

    @ColumnInfo(name = "manual_gems", defaultValue = "0")
    public int manualGems;

    public SavingsEntity() {}

    @Ignore
    public SavingsEntity(int gameType, int target, int manualProgress, int subsProgress, int dailiesProgress) {
        this(gameType, target, manualProgress, subsProgress, dailiesProgress, 0);
    }

    @Ignore
    public SavingsEntity(int gameType, int target, int manualProgress, int subsProgress, int dailiesProgress, int manualGems) {
        this.gameType = gameType;
        this.target = target;
        this.manualProgress = manualProgress;
        this.subsProgress = subsProgress;
        this.dailiesProgress = dailiesProgress;
        this.manualGems = manualGems;
    }

    public GameType getGameTypeEnum() {
        return GameType.fromCode(gameType);
    }
}
