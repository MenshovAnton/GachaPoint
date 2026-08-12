package ru.menshovanton.gachapoint.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import ru.menshovanton.gachapoint.enums.GameType;
import ru.menshovanton.gachapoint.models.Date;

@Entity(tableName = "calendar")
public class CalendarEntity {

    @PrimaryKey
    public int id;

    public int day;

    @ColumnInfo(name = "day_of_year")
    public int dayOfYear;

    @ColumnInfo(name = "day_of_week")
    public int dayOfWeek;

    public int month;
    public int year;

    @ColumnInfo(name = "status_genshin")
    public int statusGenshin;

    @ColumnInfo(name = "moon_days_remaining")
    public int moonDaysRemaining;

    @ColumnInfo(name = "status_hsr")
    public int statusHsr;

    @ColumnInfo(name = "express_pass_days_remaining")
    public int expressPassDaysRemaining;

    @ColumnInfo(name = "status_zzz")
    public int statusZzz;

    @ColumnInfo(name = "interknot_days_remaining")
    public int interknotDaysRemaining;

    public CalendarEntity() {}

    public Date toDateModel(GameType gameType) {
        int status;
        int subDays;

        switch (gameType) {
            case HSR:
                status = this.statusHsr;
                subDays = this.expressPassDaysRemaining;
                break;
            case ZZZ:
                status = this.statusZzz;
                subDays = this.interknotDaysRemaining;
                break;
            case GENSHIN:
            default:
                status = this.statusGenshin;
                subDays = this.moonDaysRemaining;
                break;
        }

        return new Date(id, day, dayOfYear, dayOfWeek, status, subDays, month, year);
    }

    public void updateForGame(GameType gameType, int status, int subDaysRemaining) {
        switch (gameType) {
            case HSR:
                this.statusHsr = status;
                this.expressPassDaysRemaining = subDaysRemaining;
                break;
            case ZZZ:
                this.statusZzz = status;
                this.interknotDaysRemaining = subDaysRemaining;
                break;
            case GENSHIN:
            default:
                this.statusGenshin = status;
                this.moonDaysRemaining = subDaysRemaining;
                break;
        }
    }
}