package ru.menshovanton.gachapoint;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.LocalDate;

import ru.menshovanton.gachapoint.helpers.DateHelper;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;

public class Calendar {
    public Date[] datesArray;
    public TextView[] datesCellsLabelsArray;
    public ImageView[] datesCellsBackgroundArray;

    public Context context;
    public Calendar calendar;

    public static int calendarSize;

    public static int year = LocalDate.now().getYear();

    public Calendar(Context context) {
        this.context = context;

        PreferencesHelper preferencesHelper = new PreferencesHelper(context.getApplicationContext());

        calendarSize = preferencesHelper.getIntPreference(PreferencesHelper.CALENDAR_SIZE);

        Date[] calendarArray = DateHelper.readDB(context);
        if (calendarArray != null) {
            if (calendarArray[calendarSize - 1].year == year) {
                calendarSize += getCalendarSize(year);
                datesArray = new Date[calendarSize];
                System.arraycopy(calendarArray, 0, datesArray, 0, calendarArray.length);
                System.arraycopy(addYear(context, year + 1), 0, datesArray, calendarArray.length, 365);
                DateHelper.writeDB(context, datesArray, 0, datesArray.length);
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

        DateHelper.writeDB(context, array, 0, array.length);
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