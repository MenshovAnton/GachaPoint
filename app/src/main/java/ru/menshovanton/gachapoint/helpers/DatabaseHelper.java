package ru.menshovanton.gachapoint.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.menshovanton.gachapoint.activities.MainActivity;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper instance;

    public static final String DATABASE_NAME = "GachaPointDB.db";
    private static final int DATABASE_VERSION = 1;

    private static final String CALENDAR_TABLE = "calendar";
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


    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCalendarTableQuery = "CREATE TABLE " + CALENDAR_TABLE + " (" +
                COLUMN_CALENDAR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
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

    public void updateCalendarValue(int id, int day, int dayOfYear, int dayOfWeek, int month, int year,
                                    int status, int subDaysRemaining) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DAY, day);
        values.put(COLUMN_DAY_YEAR, dayOfYear);
        values.put(COLUMN_DAY_WEEK, dayOfWeek);
        values.put(COLUMN_MONTH, month);
        values.put(COLUMN_YEAR, year);

        switch (MainActivity.subType) {
            case 0:
                values.put(COLUMN_STATUS_GENSHIN, status);
                values.put(COLUMN_MOON_DAYS_REMAINING, subDaysRemaining);
                break;
            case 1:
                values.put(COLUMN_STATUS_HSR, status);
                values.put(COLUMN_EXPRESS_PASS_DAYS_REMAINING, subDaysRemaining);
                break;
            case 2:
                values.put(COLUMN_STATUS_ZZZ, status);
                values.put(COLUMN_INTERKNOT_DAYS_REMAINING, subDaysRemaining);
                break;
        }

        if (isCalendarRecordExists(id)) {
            db.update(CALENDAR_TABLE, values, COLUMN_CALENDAR_ID + " = ?", new String[]{String.valueOf(id)});
        } else {
            values.put(COLUMN_CALENDAR_ID, id);
            db.insert(CALENDAR_TABLE, null, values);
        }
    }

    public Cursor getAllCalendarData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(CALENDAR_TABLE, null, null, null, null, null, null);
    }

    public boolean isCalendarRecordExists(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT EXISTS(SELECT 1 FROM " + CALENDAR_TABLE + " WHERE " + COLUMN_CALENDAR_ID + " = ?)";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) == 1;
            }
        }
        return false;
    }

    public boolean isCalendarEmpty() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT EXISTS(SELECT 1 FROM " + CALENDAR_TABLE + ")";
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) == 0;
            }
        }
        return true;
    }
}