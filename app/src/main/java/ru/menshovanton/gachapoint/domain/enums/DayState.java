package ru.menshovanton.gachapoint.domain.enums;

import androidx.annotation.ColorRes;

import java.time.LocalDate;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.models.Date;

public enum DayState {
    CHECKED(R.color.checked),
    MISSED(R.color.missed),
    CHECK(R.color.check),
    DEFAULT(R.color.white);

    @ColorRes
    private final int colorResId;

    DayState(@ColorRes int colorResId) {
        this.colorResId = colorResId;
    }

    public int getColorResId() {
        return colorResId;
    }

    public static DayState from(Date dateObj, int selectedMonth, int toDayOfYear) {
        if (dateObj == null || dateObj.month != selectedMonth) {
            return DEFAULT;
        }

        if (dateObj.status == 1 || dateObj.status == 3) {
            return CHECKED;
        }

        int todayYear = LocalDate.now().getYear();

        boolean isPastDay = dateObj.year < todayYear
                || (dateObj.year == todayYear && dateObj.dayOfYear < toDayOfYear);

        if (isPastDay && (dateObj.status == 0 || dateObj.status == 2) && dateObj.subDaysRemaining > 0) {
            return MISSED;
        }

        if (dateObj.status == 0 && dateObj.subDaysRemaining > 0) {
            return CHECK;
        }

        return DEFAULT;
    }
}