package ru.menshovanton.gachapoint.ui.tracker.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

public class CalendarCellUiModel {
    public final int dayOfMonth;
    public final boolean isVisible;
    @DrawableRes public final int backgroundRes;
    @ColorRes public final int textColorRes;

    public CalendarCellUiModel(int dayOfMonth, boolean isVisible, int backgroundRes, int textColorRes) {
        this.dayOfMonth = dayOfMonth;
        this.isVisible = isVisible;
        this.backgroundRes = backgroundRes;
        this.textColorRes = textColorRes;
    }

    public static CalendarCellUiModel empty() {
        return new CalendarCellUiModel(0, false, 0, 0);
    }
}