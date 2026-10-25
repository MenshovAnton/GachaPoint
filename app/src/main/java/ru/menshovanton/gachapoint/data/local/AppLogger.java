package ru.menshovanton.gachapoint.data.local;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppLogger {

    private static final String TAG = "AppLogger";
    public static final String LOG_FILE_NAME = "gachapoint_log.txt";
    private static final String LOG_DIR_NAME = "logs";
    private static final long MAX_FILE_SIZE_BYTES = 512 * 1024; // 512 KB
    private static final long TRIM_TARGET_BYTES = 256 * 1024;   // 256 KB

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static final ExecutorService LOG_EXECUTOR = Executors.newSingleThreadExecutor();

    private static volatile Context appContext;
    private static volatile File logFile;

    private AppLogger() {}

    public static synchronized void init(Context context) {
        if (context == null) return;
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
        if (logFile == null) {
            File logsDir = new File(appContext.getCacheDir(), LOG_DIR_NAME);
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            logFile = new File(logsDir, LOG_FILE_NAME);
            if (!logFile.exists() || logFile.length() == 0) {
                writeSessionHeader();
            }
        }
    }

    public static void i(String tag, String message) {
        log("INFO", tag, message, null);
    }

    public static void w(String tag, String message) {
        log("WARN", tag, message, null);
    }

    public static void e(String tag, String message) {
        log("ERROR", tag, message, null);
    }

    public static void e(String tag, String message, @Nullable Throwable throwable) {
        log("ERROR", tag, message, throwable);
    }

    private static void log(String level, String tag, String message, @Nullable Throwable throwable) {
        int priority;
        switch (level) {
            case "WARN":
                priority = Log.WARN;
                break;
            case "ERROR":
                priority = Log.ERROR;
                break;
            default:
                priority = Log.INFO;
                break;
        }

        String logcatMsg = message;
        if (throwable != null) {
            logcatMsg += "\n" + Log.getStackTraceString(throwable);
        }
        Log.println(priority, tag != null ? tag : TAG, logcatMsg);

        LOG_EXECUTOR.execute(() -> {
            try {
                if (logFile == null && appContext != null) {
                    init(appContext);
                }
                if (logFile == null) return;

                checkAndRotateLogFile();

                String time = DATE_FORMAT.format(new Date());
                StringBuilder sb = new StringBuilder();
                sb.append("[").append(time).append("] [").append(level).append("] [")
                        .append(tag != null ? tag : "App").append("] ")
                        .append(message).append("\n");

                if (throwable != null) {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    sb.append(sw).append("\n");
                }

                try (FileOutputStream fos = new FileOutputStream(logFile, true);
                     OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                     BufferedWriter writer = new BufferedWriter(osw)) {
                    writer.write(sb.toString());
                    writer.flush();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to write diagnostic log entry", e);
            }
        });
    }

    private static void checkAndRotateLogFile() {
        if (logFile == null || !logFile.exists()) return;
        if (logFile.length() > MAX_FILE_SIZE_BYTES) {
            try {
                long fileLength = logFile.length();
                byte[] bytes = new byte[(int) fileLength];
                int read;
                try (FileInputStream fis = new FileInputStream(logFile)) {
                    read = fis.read(bytes);
                }

                if (read <= TRIM_TARGET_BYTES) return;

                int offset = (int) (read - TRIM_TARGET_BYTES);
                while (offset < read && bytes[offset] != '\n') {
                    offset++;
                }
                if (offset < read) {
                    offset++;
                }

                byte[] trimmed = new byte[read - offset];
                System.arraycopy(bytes, offset, trimmed, 0, trimmed.length);

                try (FileOutputStream fos = new FileOutputStream(logFile, false)) {
                    String marker = "--- [Log rotated: older entries truncated] ---\n";
                    fos.write(marker.getBytes(StandardCharsets.UTF_8));
                    fos.write(trimmed);
                    fos.flush();
                }
            } catch (Exception e) {
                Log.e(TAG, "Log rotation failed", e);
            }
        }
    }

    private static void writeSessionHeader() {
        if (logFile == null || appContext == null) return;
        String version = "Unknown";
        try {
            version = appContext.getPackageManager()
                    .getPackageInfo(appContext.getPackageName(), 0).versionName;
        } catch (Exception ignored) {}

        String header = String.format(Locale.US,
                "=== GachaPoint Diagnostic Log ===\n" +
                "Device: %s %s (Android %s, API %d)\n" +
                "App Version: %s\n" +
                "Session Started: %s\n" +
                "=================================\n\n",
                Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT,
                version, DATE_FORMAT.format(new Date()));

        try (FileOutputStream fos = new FileOutputStream(logFile, false)) {
            fos.write(header.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write session header", e);
        }
    }

    public static File getLogFile(Context context) {
        init(context);
        if (logFile != null && (!logFile.exists() || logFile.length() == 0)) {
            writeSessionHeader();
        }
        return logFile;
    }
}
