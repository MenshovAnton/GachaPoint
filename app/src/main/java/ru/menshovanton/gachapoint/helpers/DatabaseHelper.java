package ru.menshovanton.gachapoint.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.models.Date;
import ru.menshovanton.gachapoint.models.Wish;

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

    public static final String WISHES_GENSHIN_TABLE = "wishes_genshin";
    public static final String WISHES_HSR_TABLE = "wishes_hsr";
    public static final String WISHES_ZZZ_TABLE = "wishes_zzz";
    public static final String COLUMN_WISHES_ID = "id";
    public static final String COLUMN_DATETIME = "date_time";
    public static final String COLUMN_DROP_RARE = "drop_rare";
    public static final String COLUMN_DROP_TYPE = "drop_type";


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

        String createWishesGenshinTableQuery = "CREATE TABLE " + WISHES_GENSHIN_TABLE + " (" +
                COLUMN_WISHES_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_DATETIME + " TEXT, " +
                COLUMN_DROP_RARE + " TEXT, " +
                COLUMN_DROP_TYPE + " TEXT) ";

        String createWishesHSRTableQuery = "CREATE TABLE " + WISHES_HSR_TABLE + " (" +
                COLUMN_WISHES_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_DATETIME + " TEXT, " +
                COLUMN_DROP_RARE + " TEXT, " +
                COLUMN_DROP_TYPE + " TEXT) ";

        String createWishesZZZTableQuery = "CREATE TABLE " + WISHES_ZZZ_TABLE + " (" +
                COLUMN_WISHES_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_DATETIME + " TEXT, " +
                COLUMN_DROP_RARE + " TEXT, " +
                COLUMN_DROP_TYPE + " TEXT) ";

        db.execSQL(createCalendarTableQuery);
        db.execSQL(createWishesGenshinTableQuery);
        db.execSQL(createWishesHSRTableQuery);
        db.execSQL(createWishesZZZTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + CALENDAR_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + WISHES_GENSHIN_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + WISHES_HSR_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + WISHES_ZZZ_TABLE);
        onCreate(db);
    }

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

    public List<Wish> getAllWishes(int subType) {
        String table = getCurrentTable(subType);

        List<Wish> wishesList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try (Cursor cursor = db.query(table, null, null, null, null, null, COLUMN_WISHES_ID + " DESC")) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_WISHES_ID);
                int dateTimeIdx = cursor.getColumnIndexOrThrow(COLUMN_DATETIME);
                int dropRareIdx = cursor.getColumnIndexOrThrow(COLUMN_DROP_RARE);
                int dropTypeIdx = cursor.getColumnIndexOrThrow(COLUMN_DROP_TYPE);

                do {
                    int id = cursor.getInt(idIdx);
                    String dateTime = cursor.getString(dateTimeIdx);
                    String dropRare = cursor.getString(dropRareIdx);
                    String dropType = cursor.getString(dropTypeIdx);

                    wishesList.add(new Wish(id, dropRare, dropType, dateTime));
                } while (cursor.moveToNext());
            }
        }
        return wishesList;
    }

    public void addWishes(String dateTime, String dropType, String dropRare, int count, int subType) {
        String table = getCurrentTable(subType);

        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "INSERT INTO " + table + " (" +
                COLUMN_DATETIME + ", " + COLUMN_DROP_RARE + ", " + COLUMN_DROP_TYPE + ") VALUES (?, ?, ?)";

        db.beginTransaction();
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindString(1, dateTime);
            statement.bindString(2, dropRare);
            statement.bindString(3, dropType);

            for (int i = 0; i < count; i++) {
                statement.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateWish(int id, String dateTime, String dropType, String dropRare, int subType) {
        String table = getCurrentTable(subType);
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_DATETIME, dateTime);
        values.put(COLUMN_DROP_TYPE, dropType);
        values.put(COLUMN_DROP_RARE, dropRare);

        String whereClause = COLUMN_WISHES_ID + " = ?";
        String[] whereArgs = new String[]{String.valueOf(id)};

        db.update(table, values, whereClause, whereArgs);
    }

    private String getCurrentTable(int subType) {
        switch (subType) {
            case 1: return WISHES_HSR_TABLE;
            case 2: return WISHES_ZZZ_TABLE;
            case 0: default: return WISHES_GENSHIN_TABLE;
        }
    }
}