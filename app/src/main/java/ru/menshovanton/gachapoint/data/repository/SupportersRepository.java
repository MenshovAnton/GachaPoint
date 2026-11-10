package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.DataSyncConfig;
import ru.menshovanton.gachapoint.data.local.JsonDataLoader;
import ru.menshovanton.gachapoint.domain.models.SupportersData;

public class SupportersRepository {

    private static volatile SupportersRepository INSTANCE;

    private final Context context;
    private SupportersData cachedData;

    private SupportersRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        loadSupporters(false);
    }

    public static SupportersRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (SupportersRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SupportersRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public synchronized void reload(boolean forceRemote) {
        loadSupporters(forceRemote);
    }

    @NonNull
    public synchronized SupportersData getSupportersData() {
        if (cachedData == null) {
            cachedData = new SupportersData();
        }
        return cachedData;
    }

    private void loadSupporters(boolean forceRemote) {
        String json = JsonDataLoader.loadJsonData(
                context,
                DataSyncConfig.SUPPORTERS_REMOTE_URL,
                DataSyncConfig.CACHE_SUPPORTERS,
                DataSyncConfig.ASSET_SUPPORTERS,
                forceRemote
        );

        if (json.trim().isEmpty()) return;

        try {
            SupportersData parsed = parseJson(json);
            if (parsed != null) {
                cachedData = parsed;
            }
        } catch (Exception e) {
            AppLogger.e("SupportersRepository", "Failed to parse supporters JSON", e);
        }
    }

    @Nullable
    static SupportersData parseJson(@NonNull String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root == null || !root.has("supporters")) return null;
        Gson gson = new Gson();
        return gson.fromJson(root.get("supporters"), SupportersData.class);
    }
}
