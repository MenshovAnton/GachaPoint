package ru.menshovanton.gachapoint;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.YearMonth;

import ru.menshovanton.gachapoint.helpers.DateHelper;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;

public class Calendar {
    public Date[] datesArray;
    public TextView[] datesCellsLabelsArray;
    public ImageView[] datesCellsBackgroundArray;

    public Context context;
    public Calendar calendar;
    private static PreferencesHelper preferencesHelper;

    public static int calendarSize;

    public static int year = LocalDate.now().getYear();

    public Calendar(Context context) {
        this.context = context;

        preferencesHelper = new PreferencesHelper(context.getApplicationContext());

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

        datesCellsLabelsArray = new TextView[calendarSize];
        for (int i = 0; i < datesCellsLabelsArray.length; i++) {
            datesCellsLabelsArray[i] = new TextView(context);
        }

        datesCellsBackgroundArray = new ImageView[calendarSize];
        for (int i = 0; i < datesCellsBackgroundArray.length; i++) {
            ImageView imageView = new ImageView(context);
            imageView.setId(View.generateViewId());
            datesCellsBackgroundArray[i] = imageView;
        }

        calendar = this;
    }

    public Date[] initialization(Context context, int srcYear) {
        calendarSize = getCalendarSize(srcYear) + getCalendarSize(srcYear + 1);
        preferencesHelper.saveIntPreference(PreferencesHelper.CALENDAR_SIZE, calendarSize);

        Date[] array = new Date[calendarSize];
        int day = 0;
        int dayOfYear = 0;
        int dayOfWeek = LocalDate.of(srcYear, 1, 1).getDayOfWeek().getValue() - 1;
        int month = 1;
        int year = srcYear;

        for (int i = 0; i < array.length; i++) {
            day++;
            dayOfWeek++;
            dayOfYear++;
            if (dayOfWeek > 7) {
                dayOfWeek = 1;
            }
            if (dayOfYear > 365) {
                day = 0;
                dayOfYear = 0;
                month = 1;
                year++;
                dayOfWeek = LocalDate.of(year, 1, 1).getDayOfWeek().getValue() - 1;
                i--;
            } else {
                if (day > YearMonth.of(year, month).lengthOfMonth()) {
                    month++;
                    day = 0;
                    dayOfYear--;
                    dayOfWeek = LocalDate.of(year, month, 1).getDayOfWeek().getValue() - 1;
                    i--;
                } else {
                    array[i] = new Date(i, day, dayOfYear, dayOfWeek, 0, 0, month, year);
                }
            }
        }
        DateHelper.writeDB(context, array, 0, array.length);
        return array;
    }

    @NonNull
    public Date[] addYear(Context context, int year) {
        Date[] array = new Date[getCalendarSize(year)];
        int day = 0;
        int dayOfWeek = LocalDate.of(year, 1, 1).getDayOfWeek().getValue() - 1;
        int month = 1;
        int id = calendarSize;
        for (int i = 0; i < array.length; i++) {
            day++;
            dayOfWeek++;
            id++;
            if (dayOfWeek > 7) {
                dayOfWeek = 1;
            }
            if (day >= YearMonth.of(year, month).lengthOfMonth()) {
                month++;
                day = 0;
                i--;
                dayOfWeek = LocalDate.of(year, month, 1).getDayOfWeek().getValue() - 1;
            } else {
                array[i] = new Date(id, day, i, dayOfWeek, 0, 0, month, year);
            }
        }
        return array;
    }

    public int getSubDaysRemaining(int dayOfYear) {
        return this.datesArray[dayOfYear - 1].subDaysRemaining;
    }

    public int getStatus(int dayOfYear) {
        return this.datesArray[dayOfYear - 1].status;
    }

    public int getCalendarSize(int year) {
        if (LocalDate.of(year, 1, 1).isLeapYear()) {
            return 366;
        } else {
            return 365;
        }
    }
}