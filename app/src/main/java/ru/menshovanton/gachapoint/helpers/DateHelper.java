package ru.menshovanton.gachapoint.helpers;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.Date;
import ru.menshovanton.gachapoint.activities.MainActivity;

public class DateHelper {
    private final MainActivity mainActivity;
    private final DatabaseHelper dbHelper;

    public DateHelper(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.dbHelper = new DatabaseHelper(mainActivity);
    }

    public void writeDB(Context context, Date[] dateArray, int start, int count) {
        if (dateArray == null || dateArray.length == 0) return;
        dbHelper.saveCalendarBatch(dateArray, start, count, mainActivity.getSubType());
    }

    public Date[] readDB(Context context) {
        try (Cursor cursor = dbHelper.getAllCalendarData()) {
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            // Кэшируем индексы колонок один раз перед циклом O(1)
            int idIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CALENDAR_ID);
            int dayIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DAY);
            int dayOfYearIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DAY_YEAR);
            int dayOfWeekIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DAY_WEEK);
            int monthIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MONTH);
            int yearIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_YEAR);

            int subType = mainActivity.getSubType();
            int statusIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.getStatusColumn(subType));
            int subDaysIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.getSubDaysColumn(subType));

            List<Date> list = new ArrayList<>();

            // Одиночный проход по курсору O(N)
            do {
                int id = cursor.getInt(idIdx);
                int day = cursor.getInt(dayIdx);
                int dayOfYear = cursor.getInt(dayOfYearIdx);
                int dayOfWeek = cursor.getInt(dayOfWeekIdx);
                int status = cursor.getInt(statusIdx);
                int subDays = cursor.getInt(subDaysIdx);
                int month = cursor.getInt(monthIdx);
                int year = cursor.getInt(yearIdx);

                list.add(new Date(id, day, dayOfYear, dayOfWeek, status, subDays, month, year));
            } while (cursor.moveToNext());

            return list.toArray(new Date[0]);
        }
    }

    public enum DataTypes {
        dayOfMonth,
        dayOfYear,
        month,
        year,
        status,
        sdr
    }
}