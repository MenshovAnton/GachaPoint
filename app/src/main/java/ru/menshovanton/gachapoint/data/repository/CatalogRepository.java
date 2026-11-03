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
    private final Map<Integer, Map<String, CatalogItem>> lookupIndex = new HashMap<>();

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

    private static String normalizeName(@Nullable String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    public static String cleanKey(@Nullable String s) {
        if (s == null) return "";
        return s.trim()
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[^a-zA-Zа-яА-Я0-9]", "");
    }

    public synchronized boolean isStandardFiveStar(@Nullable GameType gameType, @Nullable String itemName) {
        if (gameType == null || itemName == null || itemName.trim().isEmpty()) {
            return false;
        }
        CatalogItem item = findItemByName(gameType, itemName);
        return item != null && item.isFiveStar() && item.isStandard();
    }

    @Nullable
    public synchronized CatalogItem findItemByName(@Nullable GameType gameType, @Nullable String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String queryNorm = normalizeName(name);
        String queryClean = cleanKey(name);

        if (gameType != null) {
            Map<String, CatalogItem> index = lookupIndex.get(gameType.getId());
            if (index != null) {
                CatalogItem direct = index.get(queryClean);
                if (direct != null) return direct;
            }

            List<CatalogItem> items = catalogCache.get(gameType.getId());
            if (items != null) {
                for (CatalogItem item : items) {
                    if (normalizeName(item.getName()).equals(queryNorm)
                            || normalizeName(item.getNameRu()).equals(queryNorm)
                            || normalizeName(item.getNameEn()).equals(queryNorm)) {
                        return item;
                    }
                    for (String alias : item.getAliases()) {
                        if (normalizeName(alias).equals(queryNorm)) {
                            return item;
                        }
                    }
                }
                // Partial containment fallback
                if (queryClean.length() >= 3) {
                    for (CatalogItem item : items) {
                        String itemClean = cleanKey(item.getNameRu());
                        String enClean = cleanKey(item.getNameEn());
                        if ((!itemClean.isEmpty() && (itemClean.contains(queryClean) || queryClean.contains(itemClean)))
                                || (!enClean.isEmpty() && (enClean.contains(queryClean) || queryClean.contains(enClean)))) {
                            return item;
                        }
                    }
                }
            }
        } else {
            for (Map<String, CatalogItem> index : lookupIndex.values()) {
                if (index != null) {
                    CatalogItem direct = index.get(queryClean);
                    if (direct != null) return direct;
                }
            }
            for (List<CatalogItem> items : catalogCache.values()) {
                if (items != null) {
                    for (CatalogItem item : items) {
                        if (normalizeName(item.getName()).equals(queryNorm)
                                || normalizeName(item.getNameRu()).equals(queryNorm)
                                || normalizeName(item.getNameEn()).equals(queryNorm)) {
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
                    int gameType = optInt(gObj, "game_type", 0);

                    List<CatalogItem> itemList = new ArrayList<>();
                    Map<String, CatalogItem> gameLookup = new HashMap<>();

                    if (gObj.has("items") && gObj.get("items").isJsonArray()) {
                        JsonArray items = gObj.getAsJsonArray("items");
                        for (JsonElement iElem : items) {
                            if (!iElem.isJsonObject()) continue;
                            JsonObject iObj = iElem.getAsJsonObject();

                            String nameRu = optString(iObj, "name_ru", "");
                            String nameEn = optString(iObj, "name_en", "");
                            if (nameRu.isEmpty() && nameEn.isEmpty()) continue;

                            String name = !nameRu.isEmpty() ? nameRu : nameEn;
                            String iconUrl = DataSyncConfig.resolveRemotePath(optString(iObj, "icon_path", null));
                            String rarity = optString(iObj, "rarity", "3★");
                            String type = optString(iObj, "type", "CHARACTER");
                            boolean isStandard = iObj.has("is_standard") && !iObj.get("is_standard").isJsonNull()
                                    && iObj.get("is_standard").getAsBoolean();

                            List<String> aliases = new ArrayList<>();
                            if (iObj.has("aliases") && iObj.get("aliases").isJsonArray()) {
                                JsonArray aliasArr = iObj.getAsJsonArray("aliases");
                                for (JsonElement aElem : aliasArr) {
                                    if (aElem.isJsonPrimitive()) {
                                        aliases.add(aElem.getAsString());
                                    }
                                }
                            }

                            CatalogItem catalogItem = new CatalogItem(name, nameRu, nameEn, aliases, rarity, type, isStandard, iconUrl);
                            itemList.add(catalogItem);

                            // Index for rapid O(1) resolution
                            putCleanKey(gameLookup, name, catalogItem);
                            putCleanKey(gameLookup, nameRu, catalogItem);
                            putCleanKey(gameLookup, nameEn, catalogItem);
                            for (String alias : aliases) {
                                putCleanKey(gameLookup, alias, catalogItem);
                            }
                        }
                    }
                    catalogCache.put(gameType, itemList);
                    lookupIndex.put(gameType, gameLookup);
                }
            }
        } catch (Exception e) {
            AppLogger.e("CatalogRepository", "Failed to parse catalog JSON", e);
        }
    }

    private static void putCleanKey(@NonNull Map<String, CatalogItem> map, @Nullable String key, @NonNull CatalogItem item) {
        String clean = cleanKey(key);
        if (!clean.isEmpty()) {
            map.put(clean, item);
        }
    }

    private static String optString(@NonNull JsonObject obj, @NonNull String key, @Nullable String defVal) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return defVal;
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return defVal;
        }
    }

    private static int optInt(@NonNull JsonObject obj, @NonNull String key, int defVal) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return defVal;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return defVal;
        }
    }
}
