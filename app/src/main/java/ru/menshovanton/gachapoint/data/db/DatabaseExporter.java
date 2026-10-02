package ru.menshovanton.gachapoint.data.db;

import android.content.Context;
import android.net.Uri;

import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class DatabaseExporter {

    private DatabaseExporter() {}

    public static boolean export(Context context, Uri targetUri) {
        AppDatabase room = AppDatabase.getInstance(context);

        try {
            checkpoint(room);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        File dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);
        if (!dbFile.exists()) return false;

        File walFile = new File(dbFile.getPath() + "-wal");
        if (walFile.exists() && walFile.length() > 0) return false;

        try (InputStream in  = new FileInputStream(dbFile);
             OutputStream out = context.getContentResolver().openOutputStream(targetUri)) {

            if (out == null) return false;

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void checkpoint(RoomDatabase room) {
        SupportSQLiteDatabase db = room.getOpenHelper().getWritableDatabase();

        String sql = "PRAGMA wal_checkpoint(" + "TRUNCATE" + ")";

        try {
            db.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}