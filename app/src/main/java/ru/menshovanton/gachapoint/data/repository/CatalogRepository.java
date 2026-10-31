package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.DataSyncConfig;
import ru.menshovanton.gachapoint.data.local.JsonDataLoader;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.CatalogItem;

public class CatalogRepository {

    private static volatile CatalogRepository INSTANCE;

    private final Context context;
    private final Gson gson;
    private final Map<Integer, List<CatalogItem>> catalogCache = new HashMap<>();

    private CatalogRepository(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadCatalog(false);
    }

    public static CatalogRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CatalogRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CatalogRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public synchronized void reload(boolean forceRemote) {
        loadCatalog(forceRemote);
    }

    @NonNull
    public synchronized List<CatalogItem> getItemsForGame(@Nullable GameType gameType) {
        if (gameType == null) return Collections.emptyList();
        List<CatalogItem> items = catalogCache.get(gameType.getId());
        return items != null ? new ArrayList<>(items) : Collections.emptyList();
    }

    public synchronized boolean isStandardFiveStar(@Nullable GameType gameType, @Nullable String itemName) {
        if (gameType == null || itemName == null || itemName.trim().isEmpty()) {
            return false;
        }
        String query = itemName.trim().toLowerCase(Locale.ROOT);
        List<CatalogItem> items = catalogCache.get(gameType.getId());
        if (items != null) {
            for (CatalogItem item : items) {
                if (item.getName().toLowerCase(Locale.ROOT).equals(query)
                        || item.getNameRu().toLowerCase(Locale.ROOT).equals(query)
                        || item.getNameEn().toLowerCase(Locale.ROOT).equals(query)) {
                    return item.isFiveStar() && item.isStandard();
                }
            }
        }
        return false;
    }

    @Nullable
    public synchronized CatalogItem findItemByName(@Nullable GameType gameType, @Nullable String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String query = name.trim().toLowerCase(Locale.ROOT);
        if (gameType != null) {
            List<CatalogItem> items = catalogCache.get(gameType.getId());
            if (items != null) {
                for (CatalogItem item : items) {
                    if (item.getName().toLowerCase(Locale.ROOT).equals(query)
                            || item.getNameRu().toLowerCase(Locale.ROOT).equals(query)
                            || item.getNameEn().toLowerCase(Locale.ROOT).equals(query)) {
                        return item;
                    }
                }
            }
        } else {
            for (List<CatalogItem> items : catalogCache.values()) {
                if (items != null) {
                    for (CatalogItem item : items) {
                        if (item.getName().toLowerCase(Locale.ROOT).equals(query)
                                || item.getNameRu().toLowerCase(Locale.ROOT).equals(query)
                                || item.getNameEn().toLowerCase(Locale.ROOT).equals(query)) {
                            return item;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void loadCatalog(boolean forceRemote) {
        String json = JsonDataLoader.loadJsonData(
                context,
                DataSyncConfig.CATALOG_REMOTE_URL,
                DataSyncConfig.CACHE_CATALOG,
                DataSyncConfig.ASSET_CATALOG,
                forceRemote
        );

        if (json.trim().isEmpty()) return;

        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root != null && root.has("games")) {
                JsonArray games = root.getAsJsonArray("games");
                for (JsonElement gElem : games) {
                    if (!gElem.isJsonObject()) continue;
                    JsonObject gObj = gElem.getAsJsonObject();
                    int gameType = gObj.has("game_type") ? gObj.get("game_type").getAsInt() : 0;

                    List<CatalogItem> itemList = new ArrayList<>();
                    if (gObj.has("items")) {
                        JsonArray items = gObj.getAsJsonArray("items");
                        for (JsonElement iElem : items) {
                            if (!iElem.isJsonObject()) continue;
                            JsonObject iObj = iElem.getAsJsonObject();

                            String name = iObj.has("name") ? iObj.get("name").getAsString() : "";
                            String nameRu = iObj.has("name_ru") ? iObj.get("name_ru").getAsString() : name;
                            String nameEn = iObj.has("name_en") ? iObj.get("name_en").getAsString() : name;
                            String iconUrl = iObj.has("icon_url") ? iObj.get("icon_url").getAsString() : null;
                            String rarity = iObj.has("rarity") ? iObj.get("rarity").getAsString() : "3★";
                            String type = iObj.has("type") ? iObj.get("type").getAsString() : "CHARACTER";
                            boolean isStandard = iObj.has("is_standard") && iObj.get("is_standard").getAsBoolean();

                            itemList.add(new CatalogItem(name, nameRu, nameEn, rarity, type, isStandard, iconUrl));
                        }
                    }
                    catalogCache.put(gameType, itemList);
                }
            }
        } catch (Exception e) {
            AppLogger.e("CatalogRepository", "Failed to parse catalog JSON", e);
        }
    }
}
