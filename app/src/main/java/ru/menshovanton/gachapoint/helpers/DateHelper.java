package ru.menshovanton.gachapoint.helpers;

import android.content.Context;

import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.models.Date;

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

    // TODO: Возвращает всю БД в list, а БД то размером не маленьким
    public Date[] readDB(Context context) {
        return dbHelper.getAllCalendarData(mainActivity.getSubType());
    }
}