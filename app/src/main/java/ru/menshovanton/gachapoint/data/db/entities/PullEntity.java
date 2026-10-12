package ru.menshovanton.gachapoint.data.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import ru.menshovanton.gachapoint.domain.enums.PityStatus;
import ru.menshovanton.gachapoint.domain.models.Pull;

@Entity(
        tableName = "pulls",
        foreignKeys = @ForeignKey(
                entity = GameEntity.class,
                parentColumns = "id",
                childColumns = "game_type",
                onDelete = ForeignKey.RESTRICT
        ),
        indices = {
                @Index(value = {"game_type", "banner_type"}, name = "idx_pull_game_banner"),
                @Index(value = "game_type", name = "idx_pull_game_type")
        }
)
public class PullEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "game_type")
    public int gameType;

    @ColumnInfo(name = "date_time")
    public String dateTime;

    @ColumnInfo(name = "drop_rare")
    public String dropRare;

    @ColumnInfo(name = "drop_type")
    public String dropType;

    @ColumnInfo(name = "banner_type", defaultValue = "event")
    public String bannerType;

    @ColumnInfo(name = "is_reset_pity", defaultValue = "0")
    public boolean isResetPity;

    @ColumnInfo(name = "pity_status", defaultValue = "NONE")
    public String pityStatus = "NONE";

    public PullEntity() {}

    @Ignore
    public PullEntity(int gameType, String dateTime, String dropRare, String dropType, String bannerType, boolean isResetPity, String pityStatus) {
        this.gameType = gameType;
        this.dateTime = dateTime;
        this.dropRare = dropRare;
        this.dropType = dropType;
        this.bannerType = bannerType;
        this.isResetPity = isResetPity;
        this.pityStatus = pityStatus != null ? pityStatus : "NONE";
    }

    @Ignore
    public PullEntity(int gameType, String dateTime, String dropRare, String dropType, String bannerType, boolean isResetPity) {
        this(gameType, dateTime, dropRare, dropType, bannerType, isResetPity, "NONE");
    }

    public Pull toWishModel() {
        return new Pull(id, dropRare, dropType, dateTime, bannerType, isResetPity, PityStatus.fromName(pityStatus));
    }
}