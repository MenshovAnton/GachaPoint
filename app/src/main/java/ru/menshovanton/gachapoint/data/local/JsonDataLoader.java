package ru.menshovanton.gachapoint.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    private static final int TIMEOUT_MS = 10000;

    public interface OnDataUpdatedListener {
        void onDataUpdated(@NonNull String cacheFileName);
    }

    private static final java.util.List<OnDataUpdatedListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addDataUpdatedListener(@NonNull OnDataUpdatedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeDataUpdatedListener(@NonNull OnDataUpdatedListener listener) {
        listeners.remove(listener);
    }

    private static void notifyDataUpdated(@NonNull String cacheFileName) {
        for (OnDataUpdatedListener l : listeners) {
            try {
                l.onDataUpdated(cacheFileName);
            } catch (Exception e) {
                AppLogger.e("JsonDataLoader", "Error in OnDataUpdatedListener", e);
            }
        }
    }

    private JsonDataLoader() {}

    @NonNull
    public static String loadJsonData(
            @NonNull Context context,
            @NonNull String remoteUrl,
            @NonNull String cacheFileName,
            @NonNull String assetPath,
            boolean forceRemote
    ) {
        return loadJsonData(context, remoteUrl, null, cacheFileName, assetPath, forceRemote);
    }

    @NonNull
    public static String loadJsonData(
            @NonNull Context context,
            @NonNull String primaryRemoteUrl,
            @Nullable String fallbackRemoteUrl,
            @NonNull String cacheFileName,
            @NonNull String assetPath,
            boolean forceRemote
    ) {
        String cachedJson = readFromDiskCache(context, cacheFileName);

        if (!forceRemote && isValidJson(cachedJson, cacheFileName)) {
            AppDatabase.getExecutor().execute(() -> fetchRemoteWithFallback(context, primaryRemoteUrl, fallbackRemoteUrl, cacheFileName));
            return cachedJson;
        }

        String remoteJson = fetchRemoteWithFallback(context, primaryRemoteUrl, fallbackRemoteUrl, cacheFileName);
        if (isValidJson(remoteJson, cacheFileName)) {
            return remoteJson;
        }

        if (isValidJson(cachedJson, cacheFileName)) {
            return cachedJson;
        }

        String assetJson = readFromAssets(context, assetPath);
        if (isValidJson(assetJson, cacheFileName)) {
            writeToDiskCacheSafely(context, cacheFileName, assetJson);
            return assetJson;
        }

        return "{}";
    }

    public static boolean isValidJson(@Nullable String json, @NonNull String cacheFileName) {
        if (json == null) return false;
        String trimmed = json.trim();
        if (trimmed.isEmpty() || trimmed.equals("{}")) return false;

        try {
            JsonElement elem = JsonParser.parseString(trimmed);
            if (!elem.isJsonObject()) return false;
            JsonObject obj = elem.getAsJsonObject();

            if (cacheFileName.contains("promocode")) {
                return obj.has("codes") && obj.get("codes").isJsonArray();
            } else if (cacheFileName.contains("banner")) {
                return obj.has("games") && obj.get("games").isJsonArray() && obj.getAsJsonArray("games").size() > 0;
            } else if (cacheFileName.contains("catalog")) {
                return obj.has("games") && obj.get("games").isJsonArray() && obj.getAsJsonArray("games").size() > 0;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static String fetchRemoteWithEtag(
            @NonNull Context context,
            @NonNull String remoteUrl,
            @NonNull String cacheFileName
    ) {
        return fetchRemoteWithFallback(context, remoteUrl, null, cacheFileName);
    }

    @Nullable
    public static String fetchRemoteWithFallback(
            @NonNull Context context,
            @NonNull String primaryUrl,
            @Nullable String fallbackUrl,
            @NonNull String cacheFileName
    ) {
        // 1. Try primary URL (Edge CDN)
        String result = fetchSingleUrlWithEtag(context, primaryUrl, cacheFileName);
        if (isValidJson(result, cacheFileName)) {
            return result;
        }

        // 2. If primary failed and fallback is provided, try fallback URL (e.g. raw GitHub)
        if (fallbackUrl != null && !fallbackUrl.trim().isEmpty() && !fallbackUrl.equals(primaryUrl)) {
            AppLogger.d("JsonDataLoader", "Primary URL failed for " + cacheFileName + ", attempting fallback: " + fallbackUrl);
            String fallbackResult = fetchSingleUrlWithEtag(context, fallbackUrl, cacheFileName);
            if (isValidJson(fallbackResult, cacheFileName)) {
                return fallbackResult;
            }
        }

        return null;
    }

    @Nullable
    private static String fetchSingleUrlWithEtag(
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
            connection.setRequestProperty("User-Agent", "GachaPoint-Android");

            if (savedEtag != null && !savedEtag.isEmpty()) {
                connection.setRequestProperty("If-None-Match", savedEtag);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                AppLogger.d("JsonDataLoader", "HTTP 304 Not Modified for: " + cacheFileName);
                String cached = readFromDiskCache(context, cacheFileName);
                if (isValidJson(cached, cacheFileName)) {
                    return cached;
                }
                // If 304 returned but local cache is corrupt, invalidate ETag and retry
                prefs.edit().remove("etag_" + cacheFileName).apply();
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String contentType = connection.getContentType();
                if (contentType != null) {
                    String lower = contentType.toLowerCase(java.util.Locale.ROOT);
                    if (lower.contains("text/html")) {
                        AppLogger.w("JsonDataLoader", "Received HTML instead of JSON for " + cacheFileName + " from " + remoteUrl);
                        return null;
                    }
                }

                String newEtag = connection.getHeaderField("ETag");
                String content = readStream(connection.getInputStream());

                if (isValidJson(content, cacheFileName)) {
                    boolean saved = writeToDiskCacheSafely(context, cacheFileName, content);
                    if (saved) {
                        if (newEtag != null && !newEtag.isEmpty()) {
                            prefs.edit().putString("etag_" + cacheFileName, newEtag).apply();
                        }
                        AppLogger.d("JsonDataLoader", "Successfully downloaded and cached: " + cacheFileName);
                        notifyDataUpdated(cacheFileName);
                        return content;
                    }
                } else {
                    AppLogger.w("JsonDataLoader", "Remote response for " + cacheFileName + " failed JSON validation, keeping existing cache.");
                }
            } else {
                AppLogger.d("JsonDataLoader", "Non-OK HTTP code " + responseCode + " for " + cacheFileName + " from " + remoteUrl);
            }
        } catch (Exception e) {
            AppLogger.d("JsonDataLoader", "Network fetch failed for " + cacheFileName + " (" + remoteUrl + "): " + e.getMessage());
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
    public static String readFromDiskCache(@NonNull Context context, @NonNull String fileName) {
        File file = new File(context.getCacheDir(), fileName);
        if (!file.exists()) return null;

        try (FileInputStream fis = new FileInputStream(file)) {
            return readStream(fis);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean writeToDiskCacheSafely(@NonNull Context context, @NonNull String fileName, @NonNull String data) {
        if (!isValidJson(data, fileName)) {
            return false;
        }

        File cacheDir = context.getCacheDir();
        File targetFile = new File(cacheDir, fileName);
        File tmpFile = new File(cacheDir, fileName + ".tmp");

        try {
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                fos.write(data.getBytes(StandardCharsets.UTF_8));
                fos.flush();
                fos.getFD().sync();
            }

            // Verify temporary file is readable and non-corrupt before replacing target
            if (tmpFile.exists() && tmpFile.length() > 0) {
                if (targetFile.exists()) {
                    targetFile.delete();
                }
                return tmpFile.renameTo(targetFile);
            }
        } catch (Exception e) {
            AppLogger.e("JsonDataLoader", "Failed to safely write cache: " + fileName, e);
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
        return false;
    }

    public static void clearAllCache(@NonNull Context context) {
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(".json") || f.getName().endsWith(".tmp")) {
                            f.delete();
                        }
                    }
                }
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            AppLogger.i("JsonDataLoader", "All JSON cache files and ETags cleared.");
        } catch (Exception e) {
            AppLogger.e("JsonDataLoader", "Error clearing cache", e);
        }
    }

    public static long getJsonCacheSize(@NonNull Context context) {
        long total = 0;
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && (f.getName().endsWith(".json") || f.getName().endsWith(".tmp"))) {
                            total += f.length();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return total;
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
