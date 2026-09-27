package ru.menshovanton.gachapoint.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseImporter {

    public interface ImportCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public static void importDatabase(Context context, Uri sourceUri, ImportCallback callback) {
        AppDatabase.getExecutor().execute(() -> {
            File dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            File tempFile = new File(parentDir, "temp_import.db");
            File walFile = new File(dbFile.getPath() + "-wal");
            File shmFile = new File(dbFile.getPath() + "-shm");

            try {
                try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                     OutputStream outputStream = new FileOutputStream(tempFile)) {

                    if (inputStream == null) {
                        throw new Exception(context.getString(ru.menshovanton.gachapoint.R.string.db_error_cannot_open_file));
                    }

                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    outputStream.flush();
                }

                try (SQLiteDatabase testDb = SQLiteDatabase.openDatabase(
                        tempFile.getAbsolutePath(),
                        null,
                        SQLiteDatabase.OPEN_READONLY
                )) {
                    if (!testDb.isOpen()) {
                        throw new Exception(context.getString(ru.menshovanton.gachapoint.R.string.db_error_invalid_sqlite));
                    }

                    try (android.database.Cursor cursor = testDb.rawQuery(
                            "SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('calendar', 'pulls')", null)) {
                        if (!cursor.moveToFirst() || cursor.getInt(0) == 0) {
                            throw new Exception(context.getString(ru.menshovanton.gachapoint.R.string.db_error_incompatible_schema));
                        }
                    }
                } catch (android.database.sqlite.SQLiteException e) {
                    throw new Exception(context.getString(ru.menshovanton.gachapoint.R.string.db_error_invalid_sqlite));
                }

                AppDatabase.destroyInstance();

                if (walFile.exists()) {
                    walFile.delete();
                }
                if (shmFile.exists()) {
                    shmFile.delete();
                }

                if (dbFile.exists()) {
                    dbFile.delete();
                }
                if (!tempFile.renameTo(dbFile)) {
                    try (InputStream in = new FileInputStream(tempFile);
                         OutputStream out = new FileOutputStream(dbFile)) {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                        out.flush();
                    }
                    tempFile.delete();
                }

                AppDatabase.postToMain(callback::onSuccess);

            } catch (Exception e) {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                AppDatabase.postToMain(() -> callback.onError(e));
            }
        });
    }
}
