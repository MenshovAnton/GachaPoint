package ru.menshovanton.gachapoint.data.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

import ru.menshovanton.gachapoint.domain.models.Date;

@Entity(
        tableName = "calendar",
        primaryKeys = {"year", "day_of_year"},
        indices = {
                @Index(value = {"year", "month"}, name = "idx_calendar_year_month"),
                @Index(value = {"day_of_year"}, name = "idx_calendar_day_of_year")
        }
)
public class CalendarEntity {

    public int day;

    @ColumnInfo(name = "day_of_year")
    public int dayOfYear;

    @ColumnInfo(name = "day_of_week")
    public int dayOfWeek;

    public int month;
    public int year;

    public CalendarEntity() {}

    @Ignore
    public CalendarEntity(int day, int dayOfYear, int dayOfWeek, int month, int year) {
        this.day = day;
        this.dayOfYear = dayOfYear;
        this.dayOfWeek = dayOfWeek;
        this.month = month;
        this.year = year;
    }

    public Date toDateModel(int status, int subDaysRemaining) {
        return new Date(day, dayOfYear, dayOfWeek, status, subDaysRemaining, month, year);
    }
}