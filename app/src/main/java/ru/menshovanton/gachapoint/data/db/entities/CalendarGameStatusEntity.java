package ru.menshovanton.gachapoint.data.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

@Entity(
        tableName = "calendar_game_status",
        primaryKeys = {"year", "day_of_year", "game_type"},
        foreignKeys = {
                @ForeignKey(
                        entity = CalendarEntity.class,
                        parentColumns = {"year", "day_of_year"},
                        childColumns = {"year", "day_of_year"},
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = GameEntity.class,
                        parentColumns = "id",
                        childColumns = "game_type",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"year", "day_of_year"}, name = "idx_calendar_status_day"),
                @Index(value = "game_type", name = "idx_calendar_status_game")
        }
)
public class CalendarGameStatusEntity {

    public int year;

    @ColumnInfo(name = "day_of_year")
    public int dayOfYear;

    @ColumnInfo(name = "game_type")
    public int gameType;

    @ColumnInfo(name = "status", defaultValue = "0")
    public int status;

    @ColumnInfo(name = "sub_days_remaining", defaultValue = "0")
    public int subDaysRemaining;

    public CalendarGameStatusEntity() {}

    @Ignore
    public CalendarGameStatusEntity(int year, int dayOfYear, int gameType, int status, int subDaysRemaining) {
        this.year = year;
        this.dayOfYear = dayOfYear;
        this.gameType = gameType;
        this.status = status;
        this.subDaysRemaining = subDaysRemaining;
    }
}
