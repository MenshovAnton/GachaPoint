package ru.menshovanton.gachapoint.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.view.View;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;

public class DatabaseHelper extends SQLiteOpenHelper implements Serializable {
    MainActivity mainActivity;

    public static final String DATABASE_NAME = "GachaPointDB.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "calendar";

    public static final String COLUMN_ID = "id";
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

    public DatabaseHelper(Context context, MainActivity mainActivity) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mainActivity = mainActivity;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
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
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void updateValue(int id, int day, int dayOfYear, int dayOfWeek, int month, int year, int status, int subDaysRemaining) {
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

        id++;

        if (isRecordExists(id)) {
            db.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(id)});
        } else {
            db.insert(TABLE_NAME, null, values);
        }
    }

    public Cursor getValue() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, null,
                null, null, null, null, null);
    }

    public boolean isRecordExists(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT EXISTS(SELECT 1 FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ?)";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) == 1;
        }

        cursor.close();
        return exists;
    }

    public boolean isDatabaseEmpty() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT EXISTS(SELECT id FROM calendar WHERE day=\"1\") AS day, " +
                "EXISTS(SELECT id FROM calendar WHERE month=\"1\") AS month;";

        Cursor cursor = db.rawQuery(query, null);
        boolean isEmpty = true;

        if (cursor.moveToFirst()) {
            isEmpty = cursor.getInt(0) == 0;
        }

        cursor.close();
        //db.close();
        return isEmpty;
    }

    private String getDatabasePath() {
        return mainActivity.getDatabasePath(DATABASE_NAME).getPath();
    }

    public void exportDataBase() throws IOException {
        String inFileName = getDatabasePath();
        String outFileName = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS)+"/GachaPoint/GachaPointDB.db";

        File dbFile = new File(inFileName);
        File backupFile = new File(outFileName);

        if (!Objects.requireNonNull(backupFile.getParentFile()).exists()) {
            backupFile.getParentFile().mkdirs();
        }

        try (FileChannel inChannel = FileChannel.open(dbFile.toPath(), StandardOpenOption.READ);
             FileChannel outChannel = FileChannel.open(backupFile.toPath(), StandardOpenOption.WRITE,
                     StandardOpenOption.CREATE)) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
    }

    public void createExport(View view) {
        try {
            exportDataBase();
            MainActivity.showMessage(mainActivity.getApplicationContext(), mainActivity.getString(R.string.db_export_successful));
        } catch (IOException e) {
            MainActivity.showMessage(mainActivity.getApplicationContext(), mainActivity.getString(R.string.db_export_failed));
        }
    }
}
