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
    public MainActivity mainActivity;
    public DataManager dataManager;

    public static int init_days = 730; // 2 years

    Calendar(Context context, MainActivity mainActivity) {
        this.context = context;
        this.mainActivity = mainActivity;

        Date[] date = DataManager.readDB(context);
        if (date != null) {
            if (date[init_days - 1].year == HomeFragment.year) {
                init_days += 365;
                dateArray = new Date[init_days];
                System.arraycopy(date, 0, dateArray, 0, date.length);
                System.arraycopy(addYear(context, HomeFragment.year + 1), 0, dateArray, date.length, 365);
                DataManager.writeDB(context, dateArray, 0, dateArray.length);
            } else {
                dateArray = date;
            }
        } else {
            dateArray = initialization(context, HomeFragment.year);
        }

        dateViewArray = new TextView[init_days];
        for (int i = 0; i < dateViewArray.length; i++) {
            dateViewArray[i] = new TextView(context);
        }

        dateBackArray = new ImageView[init_days];
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

        for (int i = 0; i < init_days; i++) {
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
        Date[] array = new Date[init_days];
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
        Date[] array = new Date[365];
        int day = 0;
        int month = 1;
        int id = init_days;
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
