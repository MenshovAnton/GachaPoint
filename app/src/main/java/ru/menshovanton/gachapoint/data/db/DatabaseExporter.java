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

import ru.menshovanton.gachapoint.data.local.AppLogger;

public final class DatabaseExporter {

    private DatabaseExporter() {}

    public static boolean export(Context context, Uri targetUri) {
        AppLogger.i("DatabaseExporter", "Starting database export to: " + targetUri);
        AppDatabase room = AppDatabase.getInstance(context);

        try {
            checkpoint(room);
        } catch (Exception e) {
            AppLogger.e("DatabaseExporter", "Error during WAL checkpoint before export", e);
            return false;
        }

        File dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);
        if (!dbFile.exists()) {
            AppLogger.w("DatabaseExporter", "Database file does not exist: " + dbFile.getAbsolutePath());
            return false;
        }

        try (InputStream in  = new FileInputStream(dbFile);
             OutputStream out = context.getContentResolver().openOutputStream(targetUri)) {

            if (out == null) {
                AppLogger.e("DatabaseExporter", "Target OutputStream is null for URI: " + targetUri);
                return false;
            }

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
            AppLogger.i("DatabaseExporter", "Database exported successfully (" + dbFile.length() + " bytes).");
            return true;

        } catch (IOException e) {
            AppLogger.e("DatabaseExporter", "IOException during database export", e);
            return false;
        }
    }

    private static void checkpoint(RoomDatabase room) {
        SupportSQLiteDatabase db = room.getOpenHelper().getWritableDatabase();

        try (android.database.Cursor cursor = db.query(new androidx.sqlite.db.SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)"))) {
            cursor.moveToFirst();
        } catch (Exception e) {
            AppLogger.e("DatabaseExporter", "WAL checkpoint failed", e);
        }
    }
}
