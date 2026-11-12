package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.DataSyncConfig;
import ru.menshovanton.gachapoint.data.local.JsonDataLoader;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;
import ru.menshovanton.gachapoint.domain.models.PatchInfo;

public class BannerScheduleRepository {

    private static volatile BannerScheduleRepository INSTANCE;

    private final Context context;
    private final Map<Integer, List<PatchInfo>> patchCache = new HashMap<>();
    private final Map<Integer, List<BannerScheduleItem>> bannerCache = new HashMap<>();

    private BannerScheduleRepository(Context context) {
        this.context = context.getApplicationContext();
        loadSchedule(false);
    }

    public static BannerScheduleRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BannerScheduleRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BannerScheduleRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public synchronized void reload(boolean forceRemote) {
        loadSchedule(forceRemote);
    }

    @Nullable
    public synchronized BannerScheduleItem getCurrentBanner(@Nullable GameType gameType) {
        if (gameType == null) return null;
        List<BannerScheduleItem> banners = bannerCache.get(gameType.getId());
        if (banners == null || banners.isEmpty()) return null;

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (BannerScheduleItem banner : banners) {
            if ("CURRENT".equalsIgnoreCase(banner.getStatus())) {
                return banner;
            }
        }

        for (BannerScheduleItem banner : banners) {
            try {
                LocalDate start = LocalDate.parse(banner.getStartDate(), fmt);
                LocalDate end = LocalDate.parse(banner.getEndDate(), fmt);
                if (!today.isBefore(start) && !today.isAfter(end)) {
                    return banner;
                }
            } catch (Exception ignored) {}
        }

        return banners.get(banners.size() - 1);
    }

    @Nullable
    public synchronized BannerScheduleItem getBannerForDate(@Nullable GameType gameType, @Nullable LocalDate date) {
        if (gameType == null || date == null) return null;
        List<BannerScheduleItem> banners = bannerCache.get(gameType.getId());
        if (banners == null || banners.isEmpty()) return null;

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (BannerScheduleItem banner : banners) {
            try {
                LocalDate start = LocalDate.parse(banner.getStartDate(), fmt);
                LocalDate end = LocalDate.parse(banner.getEndDate(), fmt);
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    return banner;
                }
            } catch (Exception ignored) {}
        }

        if (date.equals(LocalDate.now())) {
            return getCurrentBanner(gameType);
        }

        BannerScheduleItem closest = null;
        long minDiff = Long.MAX_VALUE;
        for (BannerScheduleItem banner : banners) {
            try {
                LocalDate start = LocalDate.parse(banner.getStartDate(), fmt);
                long diff = Math.abs(ChronoUnit.DAYS.between(start, date));
                if (diff < minDiff) {
                    minDiff = diff;
                    closest = banner;
                }
            } catch (Exception ignored) {}
        }
        return closest != null ? closest : getCurrentBanner(gameType);
    }

    @NonNull
    public synchronized List<BannerScheduleItem> getBanners(@Nullable GameType gameType) {
        if (gameType == null) return Collections.emptyList();
        List<BannerScheduleItem> list = bannerCache.get(gameType.getId());
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<BannerScheduleItem> result = new ArrayList<>(list);
        Collections.reverse(result);
        return result;
    }

    public synchronized int getUnviewedBannersCount() {
        return getUnviewedBannersCount(null);
    }

    public synchronized int getUnviewedBannersCount(@Nullable GameType gameType) {
        ru.menshovanton.gachapoint.data.local.Preferences prefs =
                new ru.menshovanton.gachapoint.data.local.Preferences(context);
        java.util.Set<String> viewed = prefs.getViewedBanners();
        int count = 0;
        if (gameType != null) {
            List<BannerScheduleItem> list = bannerCache.get(gameType.getId());
            if (list != null) {
                for (BannerScheduleItem banner : list) {
                    String st = banner.getStatus();
                    if ("CURRENT".equalsIgnoreCase(st) || "ANNOUNCED".equalsIgnoreCase(st)) {
                        if (!viewed.contains(banner.getId())) {
                            count++;
                        }
                    }
                }
            }
        } else {
            for (GameType g : prefs.getEnabledGames()) {
                List<BannerScheduleItem> list = bannerCache.get(g.getId());
                if (list == null) continue;
                for (BannerScheduleItem banner : list) {
                    String st = banner.getStatus();
                    if ("CURRENT".equalsIgnoreCase(st) || "ANNOUNCED".equalsIgnoreCase(st)) {
                        if (!viewed.contains(banner.getId())) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    public synchronized void markBannersAsViewed(@NonNull GameType gameType) {
        ru.menshovanton.gachapoint.data.local.Preferences prefs =
                new ru.menshovanton.gachapoint.data.local.Preferences(context);
        List<String> ids = new ArrayList<>();
        List<BannerScheduleItem> list = bannerCache.get(gameType.getId());
        if (list != null) {
            for (BannerScheduleItem banner : list) {
                String st = banner.getStatus();
                if ("CURRENT".equalsIgnoreCase(st) || "ANNOUNCED".equalsIgnoreCase(st)) {
                    ids.add(banner.getId());
                }
            }
        }
        prefs.markBannersAsViewed(ids);
    }

    public synchronized void markAllBannersAsViewed() {
        ru.menshovanton.gachapoint.data.local.Preferences prefs =
                new ru.menshovanton.gachapoint.data.local.Preferences(context);
        List<String> ids = new ArrayList<>();
        for (List<BannerScheduleItem> list : bannerCache.values()) {
            if (list == null) continue;
            for (BannerScheduleItem banner : list) {
                String st = banner.getStatus();
                if ("CURRENT".equalsIgnoreCase(st) || "ANNOUNCED".equalsIgnoreCase(st)) {
                    ids.add(banner.getId());
                }
            }
        }
        prefs.markBannersAsViewed(ids);
    }

    @NonNull
    public synchronized List<PatchInfo> getPatches(@Nullable GameType gameType) {
        if (gameType == null) return Collections.emptyList();
        List<PatchInfo> list = patchCache.get(gameType.getId());
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    @Nullable
    public synchronized String getPatchForDate(@Nullable GameType gameType, @Nullable String dateIso) {
        if (gameType == null || dateIso == null) return null;
        List<PatchInfo> patches = patchCache.get(gameType.getId());
        if (patches == null || patches.isEmpty()) return null;

        try {
            LocalDate date = LocalDate.parse(dateIso, DateTimeFormatter.ISO_LOCAL_DATE);
            for (PatchInfo patch : patches) {
                LocalDate start = LocalDate.parse(patch.getStartDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDate end = LocalDate.parse(patch.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    return context.getString(R.string.patch_format, patch.getVersion());
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static boolean listContainsName(List<String> list, String query) {
        if (list == null || query == null || query.trim().isEmpty()) return false;
        String cleanQuery = query.trim().toLowerCase(java.util.Locale.ROOT).replace('ё', 'е');
        for (String item : list) {
            if (item != null) {
                String cleanItem = item.trim().toLowerCase(java.util.Locale.ROOT).replace('ё', 'е');
                if (cleanItem.equals(cleanQuery) || cleanItem.contains(cleanQuery) || cleanQuery.contains(cleanItem)) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized int getDaysSinceLastRerun(@Nullable GameType gameType, @NonNull String characterName) {
        if (gameType == null) return -1;
        List<BannerScheduleItem> banners = bannerCache.get(gameType.getId());
        if (banners == null) return -1;

        LocalDate today = LocalDate.now();
        LocalDate latestEnd = null;

        for (BannerScheduleItem banner : banners) {
            if (listContainsName(banner.getFeaturedFiveStars(), characterName)
                    || listContainsName(banner.getFeaturedFiveStarsRu(), characterName)
                    || listContainsName(banner.getFeaturedFiveStarsEn(), characterName)) {
                try {
                    LocalDate end = LocalDate.parse(banner.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                    if (latestEnd == null || end.isAfter(latestEnd)) {
                        latestEnd = end;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (latestEnd != null) {
            return (int) ChronoUnit.DAYS.between(latestEnd, today);
        }
        return -1;
    }

    @NonNull
    public synchronized List<ru.menshovanton.gachapoint.domain.models.CharacterRerunInfo> getCharacterRerunHistory(
            @Nullable GameType gameType,
            @NonNull List<ru.menshovanton.gachapoint.domain.models.CatalogItem> catalogItems
    ) {
        if (gameType == null) return Collections.emptyList();
        List<BannerScheduleItem> banners = bannerCache.get(gameType.getId());
        if (banners == null) return Collections.emptyList();

        LocalDate today = LocalDate.now();
        List<ru.menshovanton.gachapoint.domain.models.CharacterRerunInfo> result = new ArrayList<>();

        for (ru.menshovanton.gachapoint.domain.models.CatalogItem item : catalogItems) {
            if (!item.isFiveStar() || item.isStandard() || !"CHARACTER".equalsIgnoreCase(item.getType())) {
                continue;
            }

            String charName = item.getName();
            LocalDate latestEnd = null;
            String latestVersion = null;

            for (BannerScheduleItem banner : banners) {
                boolean matches = listContainsName(banner.getFeaturedFiveStars(), charName)
                        || listContainsName(banner.getFeaturedFiveStars(), item.getNameRu())
                        || listContainsName(banner.getFeaturedFiveStars(), item.getNameEn())
                        || listContainsName(banner.getFeaturedFiveStarsRu(), charName)
                        || listContainsName(banner.getFeaturedFiveStarsRu(), item.getNameRu())
                        || listContainsName(banner.getFeaturedFiveStarsEn(), item.getNameEn());

                if (matches) {
                    try {
                        LocalDate end = LocalDate.parse(banner.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                        if (latestEnd == null || end.isAfter(latestEnd)) {
                            latestEnd = end;
                            latestVersion = banner.getVersion();
                        }
                    } catch (Exception ignored) {}
                }
            }

            int days = (latestEnd != null) ? (int) ChronoUnit.DAYS.between(latestEnd, today) : 999;
            String endDateStr = (latestEnd != null) ? latestEnd.toString() : null;
            String nameRu = !item.getNameRu().isEmpty() ? item.getNameRu() : charName;
            String nameEn = !item.getNameEn().isEmpty() ? item.getNameEn() : charName;
            result.add(new ru.menshovanton.gachapoint.domain.models.CharacterRerunInfo(
                    nameRu,
                    nameEn,
                    gameType.getId(),
                    days,
                    latestVersion,
                    endDateStr
            ));
        }

        if (result.isEmpty() && !banners.isEmpty()) {
            Map<String, LocalDate> charLastDates = new HashMap<>();
            Map<String, String> charLastVersions = new HashMap<>();
            Map<String, String> charNamesRu = new HashMap<>();
            Map<String, String> charNamesEn = new HashMap<>();
            for (BannerScheduleItem b : banners) {
                LocalDate bEnd = null;
                try {
                    bEnd = LocalDate.parse(b.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception ignored) {}
                List<String> ruList = b.getFeaturedFiveStarsRu();
                List<String> enList = b.getFeaturedFiveStarsEn();
                int count = Math.max(ruList.size(), enList.size());
                for (int i = 0; i < count; i++) {
                    String nameRu = i < ruList.size() ? ruList.get(i) : "";
                    String nameEn = i < enList.size() ? enList.get(i) : "";
                    String key = !nameEn.isEmpty() ? nameEn : nameRu;
                    if (key.isEmpty()) continue;
                    charNamesRu.put(key, !nameRu.isEmpty() ? nameRu : key);
                    charNamesEn.put(key, !nameEn.isEmpty() ? nameEn : key);
                    if (bEnd != null) {
                        LocalDate prev = charLastDates.get(key);
                        if (prev == null || bEnd.isAfter(prev)) {
                            charLastDates.put(key, bEnd);
                            charLastVersions.put(key, b.getVersion());
                        }
                    }
                }
            }
            for (Map.Entry<String, LocalDate> entry : charLastDates.entrySet()) {
                int days = (int) ChronoUnit.DAYS.between(entry.getValue(), today);
                String key = entry.getKey();
                result.add(new ru.menshovanton.gachapoint.domain.models.CharacterRerunInfo(
                        Objects.requireNonNull(charNamesRu.getOrDefault(key, key)),
                        Objects.requireNonNull(charNamesEn.getOrDefault(key, key)),
                        gameType.getId(),
                        days,
                        charLastVersions.get(key),
                        entry.getValue().toString()
                ));
            }
        }

        result.sort((a, b) -> Integer.compare(b.getDaysSinceLastRerun(), a.getDaysSinceLastRerun()));
        return result;
    }

    private void loadSchedule(boolean forceRemote) {
        String json = JsonDataLoader.loadJsonData(
                context,
                DataSyncConfig.BANNERS_REMOTE_URL,
                DataSyncConfig.CACHE_BANNERS,
                DataSyncConfig.ASSET_BANNERS,
                forceRemote
        );

        if (json.trim().isEmpty()) return;

        try {
            parseScheduleJson(json, patchCache, bannerCache);
        } catch (Exception e) {
            AppLogger.e("BannerScheduleRepository", "Failed to parse banners schedule JSON", e);
        }
    }

    static void parseScheduleJson(
            @NonNull String json,
            @NonNull Map<Integer, List<PatchInfo>> patchTarget,
            @NonNull Map<Integer, List<BannerScheduleItem>> bannerTarget
    ) {
        Gson parser = new Gson();
        JsonObject root = parser.fromJson(json, JsonObject.class);
        if (root == null || !root.has("games")) return;

        JsonArray games = root.getAsJsonArray("games");
        for (JsonElement gElem : games) {
            if (!gElem.isJsonObject()) continue;
            JsonObject gObj = gElem.getAsJsonObject();
            int gameType = optInt(gObj, "game_type", 0);

            List<PatchInfo> patchList = new ArrayList<>();
            if (gObj.has("patches") && gObj.get("patches").isJsonArray()) {
                for (JsonElement pElem : gObj.getAsJsonArray("patches")) {
                    if (!pElem.isJsonObject()) continue;
                    JsonObject pObj = pElem.getAsJsonObject();
                    patchList.add(new PatchInfo(
                            optString(pObj, "version", ""),
                            optString(pObj, "title", ""),
                            optString(pObj, "start_date", ""),
                            optString(pObj, "end_date", "")
                    ));
                }
            }
            patchTarget.put(gameType, patchList);

            List<BannerScheduleItem> bannerList = new ArrayList<>();
            if (gObj.has("banners") && gObj.get("banners").isJsonArray()) {
                for (JsonElement bElem : gObj.getAsJsonArray("banners")) {
                    if (!bElem.isJsonObject()) continue;
                    JsonObject bObj = bElem.getAsJsonObject();

                    List<String> fiveStarsRu = safeStringList(bObj, "featured_five_stars_ru");
                    List<String> fiveStarsEn = safeStringList(bObj, "featured_five_stars_en");
                    List<String> fiveStars = !fiveStarsRu.isEmpty() ? fiveStarsRu : fiveStarsEn;

                    List<String> fourStarsRu = safeStringList(bObj, "featured_four_stars_ru");
                    List<String> fourStarsEn = safeStringList(bObj, "featured_four_stars_en");
                    List<String> fourStars = !fourStarsRu.isEmpty() ? fourStarsRu : fourStarsEn;

                    String bannerImageUrl = bObj.has("banner_image_url") && !bObj.get("banner_image_url").isJsonNull()
                            ? optString(bObj, "banner_image_url", null) : null;

                    bannerList.add(new BannerScheduleItem(
                            optString(bObj, "id", ""),
                            gameType,
                            optString(bObj, "version", ""),
                            optInt(bObj, "phase", 1),
                            optString(bObj, "start_date", ""),
                            optString(bObj, "end_date", ""),
                            optString(bObj, "status", "PAST"),
                            fiveStars,
                            fourStars,
                            fiveStarsRu,
                            fiveStarsEn,
                            fourStarsRu,
                            fourStarsEn,
                            bannerImageUrl
                    ));
                }
            }
            bannerTarget.put(gameType, bannerList);
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

    @NonNull
    private static List<String> safeStringList(@NonNull JsonObject obj, @NonNull String key) {
        List<String> list = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement elem : obj.getAsJsonArray(key)) {
                if (elem != null && !elem.isJsonNull()) {
                    try {
                        String s = elem.getAsString().trim();
                        if (!s.isEmpty()) {
                            list.add(s);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return list;
    }
}
