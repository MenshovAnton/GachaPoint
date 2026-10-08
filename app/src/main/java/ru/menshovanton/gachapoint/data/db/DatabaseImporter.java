package ru.menshovanton.gachapoint.data.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.room.Room;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ru.menshovanton.gachapoint.R;

public class DatabaseImporter {

    public interface ImportCallback {
        void onSuccess();
        void onError(Exception e);
    }

    private static final String TEMP_DB_NAME = "temp_import.db";

    public static void importDatabase(Context context, Uri sourceUri, ImportCallback callback) {
        AppDatabase.getExecutor().execute(() -> {
            File dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            File tempFile = new File(parentDir, TEMP_DB_NAME);
            File tempWalFile = new File(parentDir, TEMP_DB_NAME + "-wal");
            File tempShmFile = new File(parentDir, TEMP_DB_NAME + "-shm");

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

                validateTempDatabase(context, tempFile);

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

                if (tempWalFile.exists()) tempWalFile.delete();
                if (tempShmFile.exists()) tempShmFile.delete();

                AppDatabase newDb = AppDatabase.getInstance(context);
                newDb.getOpenHelper().getWritableDatabase();

                AppDatabase.postToMain(callback::onSuccess);

            } catch (Exception e) {
                cleanupTempFiles(tempFile, tempWalFile, tempShmFile);
                AppDatabase.postToMain(() -> callback.onError(e));
            }
        });
    }

    private static void validateTempDatabase(Context context, File tempFile) throws Exception {
        if (!isValidSqliteHeader(tempFile)) {
            throw new Exception(context.getString(R.string.db_error_invalid_file));
        }

        try (SQLiteDatabase rawDb = SQLiteDatabase.openDatabase(
                tempFile.getAbsolutePath(),
                null,
                SQLiteDatabase.OPEN_READONLY
        )) {
            int version = rawDb.getVersion();
            if (version <= 0) {
                throw new Exception(context.getString(R.string.db_error_incorrect_db_version));
            }

            try (Cursor cursor = rawDb.rawQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('calendar', 'pulls', 'subscriptions')", null)) {
                if (!cursor.moveToFirst() || cursor.getInt(0) < 2) {
                    throw new Exception(context.getString(R.string.db_error_missing_tables));
                }
            }
        } catch (Exception e) {
            throw new Exception(context.getString(R.string.db_error_invalid_sqlite) + e.getMessage(), e);
        }

        AppDatabase tempDb = null;
        try {
            tempDb = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DatabaseImporter.TEMP_DB_NAME
                    )
                    .addMigrations(MigrationHelper.MIGRATION_1_2, MigrationHelper.MIGRATION_2_3)
                    .build();

            tempDb.getOpenHelper().getWritableDatabase();

        } catch (Throwable t) {
            throw new Exception(context.getString(R.string.db_error_incompatible_schema));
        } finally {
            if (tempDb != null && tempDb.isOpen()) {
                tempDb.close();
            }
        }
    }

    private static boolean isValidSqliteHeader(File file) {
        if (file == null || !file.exists() || file.length() < 16) {
            return false;
        }
        byte[] expectedHeader = "SQLite format 3\000".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] buffer = new byte[16];

        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(buffer);
            if (read != 16) return false;
            return java.util.Arrays.equals(expectedHeader, buffer);
        } catch (IOException e) {
            return false;
        }
    }

    private static void cleanupTempFiles(File... files) {
        for (File file : files) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }
}