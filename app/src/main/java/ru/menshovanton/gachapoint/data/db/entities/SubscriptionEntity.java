package ru.menshovanton.gachapoint.data.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import ru.menshovanton.gachapoint.domain.enums.GameType;

@Entity(
        tableName = "subscriptions",
        foreignKeys = @ForeignKey(
                entity = GameEntity.class,
                parentColumns = "id",
                childColumns = "game_type",
                onDelete = ForeignKey.RESTRICT
        ),
        indices = {
                @Index(value = {"game_type", "start_date", "end_date"}, name = "idx_subscription_game_dates"),
                @Index(value = "game_type", name = "idx_subscription_game_type")
        }
)
public class SubscriptionEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "game_type")
    public int gameType;

    @ColumnInfo(name = "count")
    public int count;

    @ColumnInfo(name = "start_date")
    public long startDate;

    @ColumnInfo(name = "end_date")
    public long endDate;

    @ColumnInfo(name = "claimed", defaultValue = "0")
    public int claimed;

    @ColumnInfo(name = "missed", defaultValue = "0")
    public int missed;

    @ColumnInfo(name = "wait", defaultValue = "0")
    public int wait;

    public SubscriptionEntity() {}

    @Ignore
    public SubscriptionEntity(int gameType, int count, long startDate, long endDate, int claimed, int missed, int wait) {
        this.gameType = gameType;
        this.count = count;
        this.startDate = startDate;
        this.endDate = endDate;
        this.claimed = claimed;
        this.missed = missed;
        this.wait = wait;
    }

    public GameType getGameTypeEnum() {
        return GameType.fromCode(gameType);
    }

    public void setGameTypeEnum(GameType gameType) {
        this.gameType = gameType != null ? gameType.getCode() : 0;
    }
}
