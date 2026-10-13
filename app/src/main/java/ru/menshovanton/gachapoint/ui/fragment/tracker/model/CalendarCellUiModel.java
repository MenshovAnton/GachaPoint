package ru.menshovanton.gachapoint.ui.fragment.tracker.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

public class CalendarCellUiModel {
    public final int dayOfMonth;
    public final int dayOfYear;
    public final int month;
    public final int year;
    public final boolean isVisible;
    public final boolean isToday;
    public final int status;
    public final int subDaysRemaining;
    public final int dailyStatus;
    @DrawableRes public final int backgroundRes;
    @ColorRes public final int textColorRes;

    public CalendarCellUiModel(
            int dayOfMonth,
            int dayOfYear,
            int month,
            int year,
            boolean isVisible,
            boolean isToday,
            int status,
            int subDaysRemaining,
            int dailyStatus,
            int backgroundRes,
            int textColorRes
    ) {
        this.dayOfMonth = dayOfMonth;
        this.dayOfYear = dayOfYear;
        this.month = month;
        this.year = year;
        this.isVisible = isVisible;
        this.isToday = isToday;
        this.status = status;
        this.subDaysRemaining = subDaysRemaining;
        this.dailyStatus = dailyStatus;
        this.backgroundRes = backgroundRes;
        this.textColorRes = textColorRes;
    }

    public static CalendarCellUiModel empty() {
        return new CalendarCellUiModel(0, 0, 0, 0, false, false, 0, 0, 0, 0, 0);
    }
}