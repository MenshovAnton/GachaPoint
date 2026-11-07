package ru.menshovanton.gachapoint.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class JsonDataLoader {

    private static final String PREFS_NAME = "json_data_cache_prefs";
    private static final int TIMEOUT_MS = 8000;
    private static final long BACKGROUND_CHECK_COOLDOWN_MS = 15 * 60 * 1000L;

    public enum SyncStatus {
        SUCCESS,
        NOT_MODIFIED,
        OFFLINE,
        ERROR
    }

    public interface OnDataUpdatedListener {
        void onDataUpdated(@NonNull String cacheFileName);
    }

    public interface OnDataSyncResultListener {
        void onSyncResult(@NonNull String cacheFileName, @NonNull SyncStatus status);
    }

    private static final List<OnDataUpdatedListener> listeners = new CopyOnWriteArrayList<>();
    private static final List<OnDataSyncResultListener> syncResultListeners = new CopyOnWriteArrayList<>();
    private static final Set<String> IN_FLIGHT = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void addDataUpdatedListener(@NonNull OnDataUpdatedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeDataUpdatedListener(@NonNull OnDataUpdatedListener listener) {
        listeners.remove(listener);
    }

    public static void addSyncResultListener(@NonNull OnDataSyncResultListener listener) {
        if (!syncResultListeners.contains(listener)) {
            syncResultListeners.add(listener);
        }
    }

    public static void removeSyncResultListener(@NonNull OnDataSyncResultListener listener) {
        syncResultListeners.remove(listener);
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

    private static void notifySyncResult(@NonNull String cacheFileName, @NonNull SyncStatus status) {
        for (OnDataSyncResultListener l : syncResultListeners) {
            try {
                l.onSyncResult(cacheFileName, status);
            } catch (Exception e) {
                AppLogger.e("JsonDataLoader", "Error in OnDataSyncResultListener", e);
            }
        }
    }

    private JsonDataLoader() {}

    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network activeNet = cm.getActiveNetwork();
        if (activeNet == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNet);
        return caps != null && (
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
    }

    public static long getLastSuccessfulUpdateTime(@NonNull Context context, @NonNull String cacheFileName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong("last_success_" + cacheFileName, 0L);
    }

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
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (!forceRemote && isValidJson(cachedJson, cacheFileName)) {
            long lastCheck = prefs.getLong("last_check_" + cacheFileName, 0L);
            long now = System.currentTimeMillis();
            if (now - lastCheck > BACKGROUND_CHECK_COOLDOWN_MS) {
                triggerBackgroundFetch(context, primaryRemoteUrl, fallbackRemoteUrl, cacheFileName);
            }
            return cachedJson;
        }

        if (forceRemote) {
            String remoteJson = fetchRemoteWithFallback(context, primaryRemoteUrl, fallbackRemoteUrl, cacheFileName);
            if (isValidJson(remoteJson, cacheFileName)) {
                return remoteJson;
            }
        }

        if (isValidJson(cachedJson, cacheFileName)) {
            if (forceRemote) {
                notifySyncResult(cacheFileName, isNetworkAvailable(context) ? SyncStatus.ERROR : SyncStatus.OFFLINE);
            }
            return cachedJson;
        }

        String assetJson = readFromAssets(context, assetPath);
        if (isValidJson(assetJson, cacheFileName)) {
            writeToDiskCacheSafely(context, cacheFileName, assetJson);
            return assetJson;
        }

        return "{}";
    }

    private static void triggerBackgroundFetch(
            @NonNull Context context,
            @NonNull String primaryRemoteUrl,
            @Nullable String fallbackRemoteUrl,
            @NonNull String cacheFileName
    ) {
        if (!IN_FLIGHT.add(cacheFileName)) {
            return;
        }

        NetworkExecutor.execute(() -> {
            try {
                fetchRemoteWithFallback(context, primaryRemoteUrl, fallbackRemoteUrl, cacheFileName);
            } finally {
                IN_FLIGHT.remove(cacheFileName);
            }
        });
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
                return obj.has("games") && obj.get("games").isJsonArray() && !obj.getAsJsonArray("games").isEmpty();
            } else if (cacheFileName.contains("catalog")) {
                return obj.has("games") && obj.get("games").isJsonArray() && !obj.getAsJsonArray("games").isEmpty();
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
        if (!isNetworkAvailable(context)) {
            notifySyncResult(cacheFileName, SyncStatus.OFFLINE);
            return null;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong("last_check_" + cacheFileName, System.currentTimeMillis()).apply();

        String result = fetchSingleUrlWithEtag(context, primaryUrl, cacheFileName);
        if (isValidJson(result, cacheFileName)) {
            return result;
        }

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
                    prefs.edit().putLong("last_success_" + cacheFileName, System.currentTimeMillis()).apply();
                    notifySyncResult(cacheFileName, SyncStatus.NOT_MODIFIED);
                    return cached;
                }
                prefs.edit().remove("etag_" + cacheFileName).apply();
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String contentType = connection.getContentType();
                if (contentType != null) {
                    String lower = contentType.toLowerCase(java.util.Locale.ROOT);
                    if (lower.contains("text/html")) {
                        AppLogger.w("JsonDataLoader", "Received HTML instead of JSON for " + cacheFileName + " from " + remoteUrl);
                        notifySyncResult(cacheFileName, SyncStatus.ERROR);
                        return null;
                    }
                }

                String newEtag = connection.getHeaderField("ETag");
                String content = readStream(connection.getInputStream());

                if (isValidJson(content, cacheFileName)) {
                    boolean saved = writeToDiskCacheSafely(context, cacheFileName, content);
                    if (saved) {
                        SharedPreferences.Editor editor = prefs.edit();
                        if (newEtag != null && !newEtag.isEmpty()) {
                            editor.putString("etag_" + cacheFileName, newEtag);
                        }
                        editor.putLong("last_success_" + cacheFileName, System.currentTimeMillis());
                        editor.apply();

                        AppLogger.d("JsonDataLoader", "Successfully downloaded and cached: " + cacheFileName);
                        notifyDataUpdated(cacheFileName);
                        notifySyncResult(cacheFileName, SyncStatus.SUCCESS);
                        return content;
                    }
                } else {
                    AppLogger.w("JsonDataLoader", "Remote response for " + cacheFileName + " failed validation.");
                    notifySyncResult(cacheFileName, SyncStatus.ERROR);
                }
            } else {
                AppLogger.d("JsonDataLoader", "Non-OK HTTP code " + responseCode + " for " + cacheFileName);
                notifySyncResult(cacheFileName, SyncStatus.ERROR);
            }
        } catch (Exception e) {
            AppLogger.d("JsonDataLoader", "Network fetch failed for " + cacheFileName + ": " + e.getMessage());
            notifySyncResult(cacheFileName, isNetworkAvailable(context) ? SyncStatus.ERROR : SyncStatus.OFFLINE);
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
