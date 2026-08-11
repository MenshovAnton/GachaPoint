package ru.menshovanton.gachapoint.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    public CalendarEntity(int id, int day, int dayOfYear, int dayOfWeek, int month, int year,
                          int statusGenshin, int moonDaysRemaining,
                          int statusHsr, int expressPassDaysRemaining,
                          int statusZzz, int interknotDaysRemaining) {
        this.id = id;
        this.day = day;
        this.dayOfYear = dayOfYear;
        this.dayOfWeek = dayOfWeek;
        this.month = month;
        this.year = year;
        this.statusGenshin = statusGenshin;
        this.moonDaysRemaining = moonDaysRemaining;
        this.statusHsr = statusHsr;
        this.expressPassDaysRemaining = expressPassDaysRemaining;
        this.statusZzz = statusZzz;
        this.interknotDaysRemaining = interknotDaysRemaining;
    }

    public Date toDateModel(int subType) {
        int status;
        int subDays;

        switch (subType) {
            case 1:
                status = this.statusHsr;
                subDays = this.expressPassDaysRemaining;
                break;
            case 2:
                status = this.statusZzz;
                subDays = this.interknotDaysRemaining;
                break;
            case 0:
            default:
                status = this.statusGenshin;
                subDays = this.moonDaysRemaining;
                break;
        }

        return new Date(id, day, dayOfYear, dayOfWeek, status, subDays, month, year);
    }

    public void updateForGame(int subType, Date date) {
        this.day = date.dayOfMonth;
        this.dayOfYear = date.dayOfYear;
        this.dayOfWeek = date.dayOfWeek;
        this.month = date.month;
        this.year = date.year;

        switch (subType) {
            case 1:
                this.statusHsr = date.status;
                this.expressPassDaysRemaining = date.subDaysRemaining;
                break;
            case 2:
                this.statusZzz = date.status;
                this.interknotDaysRemaining = date.subDaysRemaining;
                break;
            case 0:
            default:
                this.statusGenshin = date.status;
                this.moonDaysRemaining = date.subDaysRemaining;
                break;
        }
    }
}
