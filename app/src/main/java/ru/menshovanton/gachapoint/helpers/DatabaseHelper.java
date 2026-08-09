package ru.menshovanton.gachapoint.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.menshovanton.gachapoint.Date;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "GachaPointDB.db";
    private static final int DATABASE_VERSION = 1;

    public static final String CALENDAR_TABLE = "calendar";
    public static final String COLUMN_CALENDAR_ID = "id";
    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_DAY_YEAR = "day_of_year";
    public static final String COLUMN_DAY_WEEK = "day_of_week";
    public static final String COLUMN_MONTH = "month";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_STATUS_GENSHIN = "status_genshin";
    public static final String COLUMN_MOON_DAYS_REMAINING = "moon_days_remaining";
    public static final String COLUMN_STATUS_HSR = "status_hsr";
    public static final String COLUMN_EXPRESS_PASS_DAYS_REMAINING = "express_pass_days_remaining";
    public static final String COLUMN_STATUS_ZZZ = "status_zzz";
    public static final String COLUMN_INTERKNOT_DAYS_REMAINING = "interknot_days_remaining";

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCalendarTableQuery = "CREATE TABLE " + CALENDAR_TABLE + " (" +
                COLUMN_CALENDAR_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_DAY + " INTEGER, " +
                COLUMN_DAY_YEAR + " INTEGER, " +
                COLUMN_DAY_WEEK + " INTEGER, " +
                COLUMN_MONTH + " INTEGER, " +
                COLUMN_YEAR + " INTEGER, " +
                COLUMN_STATUS_GENSHIN + " INTEGER, " +
                COLUMN_MOON_DAYS_REMAINING + " INTEGER, " +
                COLUMN_STATUS_HSR + " INTEGER, " +
                COLUMN_EXPRESS_PASS_DAYS_REMAINING + " INTEGER, " +
                COLUMN_STATUS_ZZZ + " INTEGER, " +
                COLUMN_INTERKNOT_DAYS_REMAINING + " INTEGER)";
        db.execSQL(createCalendarTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + CALENDAR_TABLE);
        onCreate(db);
    }

    /**
     * Быстрое пакетное обновление/вставка через одну транзакцию
     */
    public void saveCalendarBatch(Date[] dateArray, int start, int count, int subType) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            String statusColumn = getStatusColumn(subType);
            String subDaysColumn = getSubDaysColumn(subType);

            int end = start + count;
            for (int i = start; i < end; i++) {
                Date date = dateArray[i];
                values.clear();
                values.put(COLUMN_CALENDAR_ID, i);
                values.put(COLUMN_DAY, date.dayOfMonth);
                values.put(COLUMN_DAY_YEAR, date.dayOfYear);
                values.put(COLUMN_DAY_WEEK, date.dayOfWeek);
                values.put(COLUMN_MONTH, date.month);
                values.put(COLUMN_YEAR, date.year);
                values.put(statusColumn, date.status);
                values.put(subDaysColumn, date.subDaysRemaining);

                db.insertWithOnConflict(CALENDAR_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getAllCalendarData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(CALENDAR_TABLE, null, null, null, null, null, COLUMN_CALENDAR_ID + " ASC");
    }

    public static String getStatusColumn(int subType) {
        switch (subType) {
            case 1: return COLUMN_STATUS_HSR;
            case 2: return COLUMN_STATUS_ZZZ;
            default: return COLUMN_STATUS_GENSHIN;
        }
    }

    public static String getSubDaysColumn(int subType) {
        switch (subType) {
            case 1: return COLUMN_EXPRESS_PASS_DAYS_REMAINING;
            case 2: return COLUMN_INTERKNOT_DAYS_REMAINING;
            default: return COLUMN_MOON_DAYS_REMAINING;
        }
    }
}