package ru.menshovanton.gachapoint.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;

import ru.menshovanton.gachapoint.Calendar;
import ru.menshovanton.gachapoint.Date;
import ru.menshovanton.gachapoint.activities.MainActivity;

public class DateHelper {

    public static void writeDB(Context context, Date[] dateArray, int start, int stop) {
        for (int i = start; i < stop + start; i++) {
            MainActivity.dbHelper.updateValue(i, dateArray[i].dayOfMonth, dateArray[i].dayOfYear, dateArray[i].dayOfWeek, dateArray[i].month,
                    dateArray[i].year, dateArray[i].status, dateArray[i].subDaysRemaining);
        }
    }

    public static Date[] readDB(Context context) {
        Cursor cursor = MainActivity.dbHelper.getValue();

        if (MainActivity.dbHelper.isDatabaseEmpty()) {
            return null;
        }

        Date[] array = new Date[Calendar.calendarSize];

        for (int i = 0; i < Calendar.calendarSize; i++) {
            array[i] = new Date(
                    i,
                    getDayById(i, cursor),
                    getDayOfYearById(i, cursor),
                    getDayOfWeekById(i, cursor),
                    getStatusById(i, cursor),
                    getSubDaysRemainingById(i, cursor),
                    getMonthById(i, cursor),
                    getYearById(i, cursor));
        }

        cursor.close();
        return array;
    }

    @SuppressLint("Range")
    public static int getDayById(int id, Cursor cursor) {
        int day = 1;

        if (cursor.moveToFirst()) {
            cursor.moveToPosition(id);
            day = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_DAY));
        }

        return day;
    }

    @SuppressLint("Range")
    public static int getDayOfYearById(int id, Cursor cursor) {
        int day = 1;

        if (cursor.moveToFirst()) {
            cursor.moveToPosition(id);
            day = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_DAY_YEAR));
        }

        return day;
    }

    @SuppressLint("Range")
    public static int getDayOfWeekById(int id, Cursor cursor) {
        int day = 1;

        if (cursor.moveToFirst()) {
            cursor.moveToPosition(id);
            day = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_DAY_WEEK));
        }

        return day;
    }

    @SuppressLint("Range")
    public static int getMonthById(int id, Cursor cursor) {
        int month = 1;

        if (cursor.moveToFirst()) {
            cursor.moveToPosition(id);
            month = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_MONTH));
        }

        return month;
    }

    @SuppressLint("Range")
    public static int getYearById(int id, Cursor cursor) {
        int year = 1;

        if (cursor.moveToFirst()) {
            cursor.moveToPosition(id);
            year = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_YEAR));
        }

        return year;
    }

    @SuppressLint("Range")
    public static int getStatusById(int id, Cursor cursor) {
        int status = 0;

        if (cursor.moveToFirst()) {
            cursor.moveToPosition(id);

            switch (MainActivity.subType) {
                case 0:
                    status = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS_GENSHIN));
                    break;
                case 1:
                    status = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS_HSR));
                    break;
                case 2:
                    status = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS_ZZZ));
                    break;
            }
        }

        return status;
    }

    @SuppressLint("Range")
    public static int getSubDaysRemainingById(int id, Cursor cursor) {
        int days = 0;

        if (cursor.moveToFirst()) {
            cursor.moveToPosition(id);

            switch (MainActivity.subType) {
                case 0:
                    days = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_MOON_DAYS_REMAINING));
                    break;
                case 1:
                    days = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_EXPRESS_PASS_DAYS_REMAINING));
                    break;
                case 2:
                    days = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_INTERKNOT_DAYS_REMAINING));
                    break;
            }
        }

        return days;
    }


    public enum DataTypes {
        dayOfMonth,
        dayOfYear,
        month,
        year,
        status,
        sdr
    }

    private static class DataItems {
        private Date[] dates;

        Date[] getDates() {
            return dates;
        }
        void setDates(Date[] dates) {
            this.dates = dates;
        }
    }
}
