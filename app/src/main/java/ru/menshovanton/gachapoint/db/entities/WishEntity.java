package ru.menshovanton.gachapoint.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import ru.menshovanton.gachapoint.models.Wish;

@Entity(tableName = "wishes")
public class WishEntity {

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

    public WishEntity() {}

    public WishEntity(int gameType, String dateTime, String dropRare, String dropType, String bannerType, boolean isResetPity) {
        this.gameType = gameType;
        this.dateTime = dateTime;
        this.dropRare = dropRare;
        this.dropType = dropType;
        this.bannerType = bannerType;
        this.isResetPity = isResetPity;
    }

    public Wish toWishModel() {
        return new Wish(id, dropRare, dropType, dateTime, bannerType, isResetPity);
    }
}