package ru.menshovanton.gachapoint.helpers;

import static ru.menshovanton.gachapoint.activities.MainActivity.mainActivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import ru.menshovanton.gachapoint.activities.MainActivity;

public class DatabaseHelper extends SQLiteOpenHelper implements Serializable {
    public static final String DATABASE_NAME = "gachamanager.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "calendar";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_DAY_YEAR = "dayyear";
    public static final String COLUMN_DAY_WEEK = "dayweek";
    public static final String COLUMN_MONTH = "month";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_STATUSGENSHIN = "statusgenshin";
    public static final String COLUMN_DRGENSHIN = "drgenshin";
    public static final String COLUMN_STATUSHSR = "statushsr";
    public static final String COLUMN_DRHSR = "drhsr";
    public static final String COLUMN_STATUSZZZ = "statuszzz";
    public static final String COLUMN_DRZZZ = "drzzz";

    public int test = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
                COLUMN_STATUSGENSHIN + " INTEGER, " +
                COLUMN_DRGENSHIN + " INTEGER, " +
                COLUMN_STATUSHSR + " INTEGER, " +
                COLUMN_DRHSR + " INTEGER, " +
                COLUMN_STATUSZZZ + " INTEGER, " +
                COLUMN_DRZZZ + " INTEGER)";
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
                values.put(COLUMN_STATUSGENSHIN, status);
                values.put(COLUMN_DRGENSHIN, subDaysRemaining);
                break;
            case 1:
                values.put(COLUMN_STATUSHSR, status);
                values.put(COLUMN_DRHSR, subDaysRemaining);
                break;
            case 2:
                values.put(COLUMN_STATUSZZZ, status);
                values.put(COLUMN_DRZZZ, subDaysRemaining);
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

    private static String getDatabasePath() {
        return mainActivity.getDatabasePath(DATABASE_NAME).getPath();
    }

    public static void exportDataBase() throws IOException {
        String inFileName = getDatabasePath();
        String outFileName = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS)+"/GachaPoint/subs.db";

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

    public static void createExport(View view) {
        try {
            exportDataBase();
            Toast toast = Toast.makeText(mainActivity, "Данные успешно выгружены!",Toast.LENGTH_LONG);
            toast.show();
        } catch (IOException e) {
            Toast toast = Toast.makeText(mainActivity, "Ошибка экспорта! " + e.getLocalizedMessage(),Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
