package ru.menshovanton.hoyosubstrakcer;

public class Date {
    public int id;
    public int dayOfMonth;
    public int dayOfYear;
    public int year;
    public int status;
    public int subDaysRemaining;
    public int month;

    Date(int id, int dayOfMonth, int dayOfYear, int status, int subDaysRemaining, int month, int year) {
        this.id = id;
        this.dayOfMonth = dayOfMonth;
        this.dayOfYear = dayOfYear;
        this.status = status;
        this.subDaysRemaining = subDaysRemaining;
        this.month = month;
        this.year = year;
    }
}
