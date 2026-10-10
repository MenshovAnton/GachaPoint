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

    public static final int STATUS_MISSED = 0;
    public static final int STATUS_RECEIVED = 1;
    public static final int STATUS_OLD_MISSED = 2;
    public static final int STATUS_OLD_RECEIVED = 3;

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

        if (dateObj.status == STATUS_RECEIVED || dateObj.status == STATUS_OLD_RECEIVED) {
            return CHECKED;
        }

        int todayYear = LocalDate.now().getYear();

        boolean isPastDay = dateObj.year < todayYear
                || (dateObj.year == todayYear && dateObj.dayOfYear < toDayOfYear);

        if (isPastDay && (dateObj.status == STATUS_MISSED || dateObj.status == STATUS_OLD_MISSED) && dateObj.subDaysRemaining > 0) {
            return MISSED;
        }

        if (dateObj.status == STATUS_MISSED && dateObj.subDaysRemaining > 0) {
            return CHECK;
        }

        return DEFAULT;
    }
}