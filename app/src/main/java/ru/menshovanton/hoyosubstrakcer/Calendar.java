package ru.menshovanton.hoyosubstrakcer;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class Calendar {
    public Date[] dateArray;
    public TextView[] dateViewArray;
    public ImageView[] dateBackArray;
    public Context context;
    public static MainActivity mainActivity;
    public DataManager dataManager;

    public static int calendarSize;

    Calendar(Context context, MainActivity mainActivity) {
        this.context = context;
        Calendar.mainActivity = mainActivity;

        calendarSize = mainActivity.getIntPreference(MainActivity.PREF_CALENDAR_SIZE);


        Date[] date = DataManager.readDB(context);
        if (date != null) {
            if (date[calendarSize - 1].year == HomeFragment.year) {
                calendarSize += 365;
                dateArray = new Date[calendarSize];
                System.arraycopy(date, 0, dateArray, 0, date.length);
                System.arraycopy(addYear(context, HomeFragment.year + 1), 0, dateArray, date.length, 365);
                DataManager.writeDB(context, dateArray, 0, dateArray.length);
                mainActivity.saveIntPreference(MainActivity.PREF_CALENDAR_SIZE, calendarSize);
            } else {
                dateArray = date;
            }
        } else {
            dateArray = initialization(context, HomeFragment.year);
        }

        dateViewArray = new TextView[calendarSize];
        for (int i = 0; i < dateViewArray.length; i++) {
            dateViewArray[i] = new TextView(context);
        }

        dateBackArray = new ImageView[calendarSize];
        for (int i = 0; i < dateBackArray.length; i++) {
            dateBackArray[i] = new ImageView(context);
        }
    }

    public void updateCalendar() {
        Date[] date = DataManager.readDB(context);
        if (date == null) {
            return;
        }
        dateArray = date;
    }

    public void drawCalendar() {
        int margin = 1200;
        int topMargin = 400;
        int j = 0;

        int daysOfYearForMonth = getDaysOfYearForMonth(HomeFragment.selectedMonth);

        for (int i = 0; i < calendarSize; i++) {
            if (dateArray[i].status == 0 && dateArray[i].subDaysRemaining > 0 && dateArray[i].dayOfYear < HomeFragment.toDayOfYear - 1) {
                HomeFragment.missesDays++;
            }

            if (dateArray[i].status == 1 && dateArray[i].subDaysRemaining > 0 && dateArray[i].dayOfYear < HomeFragment.toDayOfYear - 1) {
                HomeFragment.claimsDays++;
            }

            if (i >= daysOfYearForMonth && i < daysOfYearForMonth + getDaysOfMonth(HomeFragment.selectedMonth)) {
                if (j == 7) {
                    topMargin = topMargin + 150;
                    margin = 1200;
                    j = 0;
                }
                if (j <= 3) {
                    margin = margin - 300;
                    HomeFragment.createView(dateArray[i], dateViewArray[i], dateBackArray[i], 0, margin, topMargin);
                } else {
                    margin = margin + 300;
                    HomeFragment.createView(dateArray[i], dateViewArray[i], dateBackArray[i], margin, 0, topMargin);
                }
                j++;
            }
        }

        if (HomeFragment.missesDays > 0) {
            HomeFragment.missesDays++;
        }

        if (dateArray[HomeFragment.toDayOfYear - 1].status == 1) {
            HomeFragment.claimsDays++;
        }
    }

    public void removeCalendar(ConstraintLayout constraintLayout) {
        for (TextView textView : dateViewArray) {
            constraintLayout.removeView(textView);
        }
        for (ImageView imageView : dateBackArray) {
            constraintLayout.removeView(imageView);
        }
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
        if (HomeFragment.selectedYear > HomeFragment.year) {
            for (int i = 1; i <= HomeFragment.selectedYear - HomeFragment.year; i++) {
                num += 365;
            }
        }
        return num;
    }

    public static Date[] initialization(Context context, int srcYear) {
        MainActivity.showMessage(context, "Инициализация календаря. Это может занять некоторое время!");

        calendarSize = 730;
        mainActivity.saveIntPreference(MainActivity.PREF_CALENDAR_SIZE, calendarSize);

        Date[] array = new Date[calendarSize];
        int day = 0;
        int dayOfYear = 0;
        int month = 1;
        int year = srcYear;
        for (int i = 0; i < array.length; i++) {
            day++;
            dayOfYear++;
            if (dayOfYear > 365) {
                day = 0;
                dayOfYear = 0;
                month = 1;
                year++;
                i--;
            } else {
                if (day > getDaysOfMonth(month)) {
                    month++;
                    day = 0;
                    dayOfYear--;
                    i--;
                } else {
                    array[i] = new Date(i, day, dayOfYear, 0, 0, month, year);
                }
            }
        }
        DataManager.writeDB(context, array, 0, array.length);
        return array;
    }

    public static Date[] addYear(Context context, int year) {
        MainActivity.showMessage(context, "Обновление календаря. Это может занять некоторое время!");

        Date[] array = new Date[365];
        int day = 0;
        int month = 1;
        int id = calendarSize;
        for (int i = 0; i < array.length; i++) {
            day++;
            id++;
            if (day >= getDaysOfMonth(month)) {
                month++;
                day = 0;
                i--;
            } else {
                array[i] = new Date(id, day, i, 0, 0, month, year);
            }
        }
        return array;
    }
}
