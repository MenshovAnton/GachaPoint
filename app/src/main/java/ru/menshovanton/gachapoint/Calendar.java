package ru.menshovanton.gachapoint;

import android.content.Context;

import java.time.LocalDate;

import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.helpers.DateHelper;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;
import ru.menshovanton.gachapoint.models.Date;

public class Calendar {
    public Date[] datesArray;

    public Calendar calendar;

    public static int calendarSize;

    private final DateHelper dateHelper;

    public Calendar(Context context, MainActivity mainActivity) {

        PreferencesHelper preferencesHelper = new PreferencesHelper(context.getApplicationContext());
        dateHelper = new DateHelper(mainActivity);

        calendarSize = preferencesHelper.getIntPreference(PreferencesHelper.CALENDAR_SIZE);

        Date[] calendarArray = dateHelper.readDB(context);
        int year = LocalDate.now().getYear();
        if (calendarArray != null) {
            if (calendarArray[calendarSize - 1].year == year) {
                calendarSize += getCalendarSize(year);
                datesArray = new Date[calendarSize];
                System.arraycopy(calendarArray, 0, datesArray, 0, calendarArray.length);
                System.arraycopy(addYear(context, year + 1), 0, datesArray, calendarArray.length, 365);
                dateHelper.writeDB(context, datesArray, 0, datesArray.length);
                preferencesHelper.saveIntPreference(PreferencesHelper.CALENDAR_SIZE, calendarSize);
            } else {
                datesArray = calendarArray;
            }
        } else {
            datesArray = initialization(context, year);
        }

        calendar = this;
    }

    public Date[] initialization(Context context, int srcYear) {
        calendarSize = getCalendarSize(srcYear) + getCalendarSize(srcYear + 1);
        PreferencesHelper preferencesHelper = new PreferencesHelper(context.getApplicationContext());
        preferencesHelper.saveIntPreference(PreferencesHelper.CALENDAR_SIZE, calendarSize);

        Date[] array = new Date[calendarSize];
        LocalDate currentDate = LocalDate.of(srcYear, 1, 1);

        for (int i = 0; i < array.length; i++) {
            array[i] = new Date(
                    i,
                    currentDate.getDayOfMonth(),
                    currentDate.getDayOfYear(),
                    currentDate.getDayOfWeek().getValue(),
                    0,
                    0,
                    currentDate.getMonthValue(),
                    currentDate.getYear()
            );
            currentDate = currentDate.plusDays(1);
        }

        dateHelper.writeDB(context, array, 0, array.length);
        return array;
    }

    public Date[] addYear(Context context, int targetYear) {
        Date[] array = new Date[getCalendarSize(targetYear)];
        LocalDate currentDate = LocalDate.of(targetYear, 1, 1);

        for (int i = 0; i < array.length; i++) {
            array[i] = new Date(
                    calendarSize + i,
                    currentDate.getDayOfMonth(),
                    i + 1,
                    currentDate.getDayOfWeek().getValue(),
                    0,
                    0,
                    currentDate.getMonthValue(),
                    currentDate.getYear()
            );
            currentDate = currentDate.plusDays(1);
        }
        return array;
    }

    public int getSubDaysRemaining(int dayOfYear) {
        if (dayOfYear <= 0 || dayOfYear > datesArray.length) return 0;
        return this.datesArray[dayOfYear - 1].subDaysRemaining;
    }

    public int getCalendarSize(int year) {
        return LocalDate.of(year, 1, 1).isLeapYear() ? 366 : 365;
    }

    public int getStatus(int dayOfYear) {
        return this.datesArray[dayOfYear - 1].status;
    }
}