package ru.menshovanton.gachapoint.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ru.menshovanton.gachapoint.data.db.AppDatabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class JsonDataLoader {

    private static final String PREFS_NAME = "json_data_cache_prefs";
    private static final int TIMEOUT_MS = 5000;

    private JsonDataLoader() {}

    @NonNull
    public static String loadJsonData(
            @NonNull Context context,
            @NonNull String remoteUrl,
            @NonNull String cacheFileName,
            @NonNull String assetPath,
            boolean forceRemote
    ) {
        String cachedJson = readFromDiskCache(context, cacheFileName);

        if (!forceRemote && cachedJson != null && !cachedJson.trim().isEmpty() && !isJsonEmpty(cachedJson)) {
            AppDatabase.getExecutor().execute(() -> fetchRemoteWithEtag(context, remoteUrl, cacheFileName));
            return cachedJson;
        }

        String remoteJson = fetchRemoteWithEtag(context, remoteUrl, cacheFileName);
        if (remoteJson != null && !remoteJson.trim().isEmpty() && !isJsonEmpty(remoteJson)) {
            return remoteJson;
        }

        if (cachedJson != null && !cachedJson.trim().isEmpty()) {
            return cachedJson;
        }

        String assetJson = readFromAssets(context, assetPath);
        if (assetJson != null && !assetJson.trim().isEmpty()) {
            writeToDiskCache(context, cacheFileName, assetJson);
            return assetJson;
        }

        return "{}";
    }

    private static boolean isJsonEmpty(String json) {
        if (json == null) return true;
        String trimmed = json.trim();
        return trimmed.isEmpty()
                || trimmed.equals("{}")
                || trimmed.equals("{\"codes\":[]}")
                || trimmed.contains("\"codes\": []")
                || trimmed.contains("\"codes\":[]");
    }

    @Nullable
    public static String fetchRemoteWithEtag(
            @NonNull Context context,
            @NonNull String remoteUrl,
            @NonNull String cacheFileName
    ) {
        HttpURLConnection connection = null;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String savedEtag = prefs.getString("etag_" + cacheFileName, null);

            URL url = new URL(remoteUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");

            if (savedEtag != null && !savedEtag.isEmpty()) {
                connection.setRequestProperty("If-None-Match", savedEtag);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                AppLogger.d("JsonDataLoader", "HTTP 304 Not Modified for: " + cacheFileName);
                return readFromDiskCache(context, cacheFileName);
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String newEtag = connection.getHeaderField("ETag");
                String content = readStream(connection.getInputStream());

                if (!content.isEmpty()) {
                    writeToDiskCache(context, cacheFileName, content);
                    if (newEtag != null) {
                        prefs.edit().putString("etag_" + cacheFileName, newEtag).apply();
                    }
                    AppLogger.d("JsonDataLoader", "Successfully downloaded and cached: " + cacheFileName);
                    return content;
                }
            }
        } catch (Exception e) {
            AppLogger.d("JsonDataLoader", "Network fetch failed for " + cacheFileName + ": " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    @Nullable
    public static String readFromAssets(@NonNull Context context, @NonNull String assetPath) {
        try (InputStream is = context.getAssets().open(assetPath)) {
            return readStream(is);
        } catch (Exception e) {
            AppLogger.e("JsonDataLoader", "Failed to read asset: " + assetPath, e);
            return null;
        }
    }

    @Nullable
    private static String readFromDiskCache(@NonNull Context context, @NonNull String fileName) {
        File file = new File(context.getCacheDir(), fileName);
        if (!file.exists()) return null;

        try (FileInputStream fis = new FileInputStream(file)) {
            return readStream(fis);
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeToDiskCache(@NonNull Context context, @NonNull String fileName, @NonNull String data) {
        try {
            File file = new File(context.getCacheDir(), fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            AppLogger.e("JsonDataLoader", "Failed to write cache: " + fileName, e);
        }
    }

    @NonNull
    private static String readStream(@NonNull InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
