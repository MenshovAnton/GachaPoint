package ru.menshovanton.gachapoint;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import ru.menshovanton.gachapoint.fragments.TrackerFragment;
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
                calendarSize += 365;
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

    public enum Months {
        Default,
        January,
        February,
        March,
        April,
        May,
        June,
        July,
        August,
        September,
        October,
        November,
        December
    }

    public static int getDaysOfMonth(int month) {
        if (month == Months.January.ordinal() ||
                month == Months.March.ordinal() ||
                month == Months.May.ordinal() ||
                month == Months.July.ordinal() ||
                month == Months.August.ordinal() ||
                month == Months.October.ordinal() ||
                month == Months.December.ordinal()) {
            return 31;
        } else if (month == Months.February.ordinal()) {
            return 28;
        } else {
            return 30;
        }
    }

    public static int getDaysOfYearForMonth(int month) {
        int num = 0;
        for (int i = 1; i < month; i++) {
            num += getDaysOfMonth(i);
        }
        if (TrackerFragment.selectedYear > year) {
            for (int i = 1; i <= TrackerFragment.selectedYear - year; i++) {
                num += 365;
            }
        }
        return num;
    }

    public static Date[] initialization(Context context, int srcYear) {
        //MainActivity.showMessage(context, "Инициализация календаря. Это может занять некоторое время!");

        calendarSize = 730;
        preferencesHelper.saveIntPreference(PreferencesHelper.CALENDAR_SIZE, calendarSize);

        Date[] array = new Date[calendarSize];
        int day = 0;
        int dayOfYear = 0;
        int dayOfWeek = getDayOfWeek(srcYear, 1);
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
                dayOfWeek = getDayOfWeek(year, 1);
                i--;
            } else {
                if (day > getDaysOfMonth(month)) {
                    month++;
                    day = 0;
                    dayOfYear--;
                    dayOfWeek = getDayOfWeek(year, month);
                    i--;
                } else {
                    array[i] = new Date(i, day, dayOfYear, dayOfWeek, 0, 0, month, year);
                }
            }
        }
        DateHelper.writeDB(context, array, 0, array.length);
        return array;
    }

    private static int getDayOfWeek(int year, int month) {
        LocalDate date = LocalDate.of(year, month, 1);
        Locale langRu = new Locale("ru");

        DayOfWeek num = date.getDayOfWeek();
        String str = num.getDisplayName(TextStyle.FULL, langRu);

        return num.getValue() - 1;
    }

    @NonNull
    public static Date[] addYear(Context context, int year) {
        //MainActivity.showMessage(context, "Обновление календаря. Это может занять некоторое время!");

        Date[] array = new Date[365];
        int day = 0;
        int dayOfWeek = getDayOfWeek(year, 1);
        int month = 1;
        int id = calendarSize;
        for (int i = 0; i < array.length; i++) {
            day++;
            dayOfWeek++;
            id++;
            if (dayOfWeek > 7) {
                dayOfWeek = 1;
            }
            if (day >= getDaysOfMonth(month)) {
                month++;
                day = 0;
                i--;
                dayOfWeek = getDayOfWeek(year, month);
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
}
