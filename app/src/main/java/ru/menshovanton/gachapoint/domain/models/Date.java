package ru.menshovanton.gachapoint.domain.models;

public class Date {
    public int id;
    public int dayOfMonth;
    public int dayOfYear;
    public int dayOfWeek;
    public int year;
    public int status;
    public int subDaysRemaining;
    public int month;

    public Date(int id, int dayOfMonth, int dayOfYear, int dayOfWeek, int status, int subDaysRemaining, int month, int year) {
        this.id = id;
        this.dayOfMonth = dayOfMonth;
        this.dayOfYear = dayOfYear;
        this.dayOfWeek = dayOfWeek;
        this.status = status;
        this.subDaysRemaining = subDaysRemaining;
        this.month = month;
        this.year = year;
    }
}
