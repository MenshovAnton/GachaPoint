package ru.menshovanton.gachapoint.domain.models;

import androidx.room.Ignore;

public class Date {
    public int dayOfMonth;
    public int dayOfYear;
    public int dayOfWeek;
    public int year;
    public int status;
    public int subDaysRemaining;
    public int dailyStatus;
    public int month;

    public Date() {}

    @Ignore
    public Date(int dayOfMonth, int dayOfYear, int dayOfWeek, int status, int subDaysRemaining, int month, int year) {
        this(dayOfMonth, dayOfYear, dayOfWeek, status, subDaysRemaining, 0, month, year);
    }

    @Ignore
    public Date(int dayOfMonth, int dayOfYear, int dayOfWeek, int status, int subDaysRemaining, int dailyStatus, int month, int year) {
        this.dayOfMonth = dayOfMonth;
        this.dayOfYear = dayOfYear;
        this.dayOfWeek = dayOfWeek;
        this.status = status;
        this.subDaysRemaining = subDaysRemaining;
        this.dailyStatus = dailyStatus;
        this.month = month;
        this.year = year;
    }
}
