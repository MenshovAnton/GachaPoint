package ru.menshovanton.gachapoint.data.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.AppLogger;

public class DatabaseImporter {

    public interface ImportCallback {
        void onSuccess();
        void onError(Exception e);
    }

    private static final String TEMP_DB_NAME = "temp_import.db";

    public static void importDatabase(Context context, Uri sourceUri, ImportCallback callback) {
        AppLogger.i("DatabaseImporter", "Starting database import from: " + sourceUri);
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
                        throw new Exception(context.getString(R.string.db_error_cannot_open_file));
                    }

                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    outputStream.flush();
                }

                AppLogger.i("DatabaseImporter", "Temp file written (" + tempFile.length() + " bytes). Validating schema...");
                validateTempDatabase(context, tempFile);
                AppLogger.i("DatabaseImporter", "Temp database validated successfully.");

                AppDatabase.destroyInstance();

                File bakDbFile = new File(dbFile.getPath() + ".bak");
                File bakWalFile = new File(walFile.getPath() + ".bak");
                File bakShmFile = new File(shmFile.getPath() + ".bak");

                cleanupTempFiles(bakDbFile, bakWalFile, bakShmFile);

                if (dbFile.exists()) dbFile.renameTo(bakDbFile);
                if (walFile.exists()) walFile.renameTo(bakWalFile);
                if (shmFile.exists()) shmFile.renameTo(bakShmFile);

                try {
                    if (!tempFile.renameTo(dbFile)) {
                        try (InputStream in = new FileInputStream(tempFile);
                             OutputStream out = new FileOutputStream(dbFile)) {
                            byte[] buffer = new byte[8192];
                            int length;
                            while ((length = in.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                            out.flush();
                        } finally {
                            tempFile.delete();
                        }
                    }

                    if (tempWalFile.exists()) tempWalFile.delete();
                    if (tempShmFile.exists()) tempShmFile.delete();

                    AppDatabase newDb = AppDatabase.getInstance(context);
                    androidx.sqlite.db.SupportSQLiteDatabase writableDb = newDb.getOpenHelper().getWritableDatabase();
                    MigrationHelper.syncGamesRaw(writableDb);
                    MigrationHelper.syncSubscriptionsRaw(writableDb);
                    MigrationHelper.syncSavingsRaw(context, writableDb);

                    cleanupTempFiles(bakDbFile, bakWalFile, bakShmFile);

                    if (context.getApplicationContext() instanceof ru.menshovanton.gachapoint.App) {
                        ((ru.menshovanton.gachapoint.App) context.getApplicationContext()).getAppContainer().reload(null);
                    }

                    AppLogger.i("DatabaseImporter", "Database imported and synced successfully.");
                    AppDatabase.postToMain(callback::onSuccess);
                } catch (Exception innerEx) {
                    AppLogger.e("DatabaseImporter", "Import step failed, rolling back to original database...", innerEx);
                    AppDatabase.destroyInstance();
                    cleanupTempFiles(dbFile, walFile, shmFile);
                    if (bakDbFile.exists()) bakDbFile.renameTo(dbFile);
                    if (bakWalFile.exists()) bakWalFile.renameTo(walFile);
                    if (bakShmFile.exists()) bakShmFile.renameTo(shmFile);
                    throw innerEx;
                }

            } catch (Exception e) {
                AppLogger.e("DatabaseImporter", "Failed to import database from: " + sourceUri, e);
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
            tempDb = AppDatabase.createDatabaseBuilder(context, DatabaseImporter.TEMP_DB_NAME).build();
            androidx.sqlite.db.SupportSQLiteDatabase db = tempDb.getOpenHelper().getWritableDatabase();
            try (Cursor cursor = db.query(new androidx.sqlite.db.SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)"))) {
                cursor.moveToFirst();
            }
        } catch (Throwable t) {
            throw new Exception(context.getString(R.string.db_error_incompatible_schema) + " (" + t.getMessage() + ")", t);
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
