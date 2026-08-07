package ru.menshovanton.gachapoint;

import androidx.annotation.ColorRes;

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

        if ((dateObj.status == 0 || dateObj.status == 2)
                && dateObj.id < toDayOfYear
                && dateObj.subDaysRemaining != 0) {
            return MISSED;
        }

        if (dateObj.status == 1 || dateObj.status == 3) {
            return CHECKED;
        }

        if (dateObj.status == 0 && dateObj.subDaysRemaining > 0) {
            return CHECK;
        }

        return DEFAULT;
    }
}
