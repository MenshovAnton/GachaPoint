package ru.menshovanton.gachapoint.data.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import ru.menshovanton.gachapoint.domain.models.Pull;

@Entity(tableName = "pulls")
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

    public PullEntity() {}

    public PullEntity(int gameType, String dateTime, String dropRare, String dropType, String bannerType, boolean isResetPity) {
        this.gameType = gameType;
        this.dateTime = dateTime;
        this.dropRare = dropRare;
        this.dropType = dropType;
        this.bannerType = bannerType;
        this.isResetPity = isResetPity;
    }

    public Pull toWishModel() {
        return new Pull(id, dropRare, dropType, dateTime, bannerType, isResetPity);
    }
}