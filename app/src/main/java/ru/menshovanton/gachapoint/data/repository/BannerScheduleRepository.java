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

import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.DataSyncConfig;
import ru.menshovanton.gachapoint.data.local.JsonDataLoader;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;
import ru.menshovanton.gachapoint.domain.models.PatchInfo;

public class BannerScheduleRepository {

    private static volatile BannerScheduleRepository INSTANCE;

    private final Context context;
    private final Gson gson;
    private final Map<Integer, List<PatchInfo>> patchCache = new HashMap<>();
    private final Map<Integer, List<BannerScheduleItem>> bannerCache = new HashMap<>();

    private BannerScheduleRepository(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
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

    @NonNull
    public synchronized List<BannerScheduleItem> getBanners(@Nullable GameType gameType) {
        if (gameType == null) return Collections.emptyList();
        List<BannerScheduleItem> list = bannerCache.get(gameType.getId());
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
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
                    return "v" + patch.getVersion();
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    public synchronized int getDaysSinceLastRerun(@Nullable GameType gameType, @NonNull String characterName) {
        if (gameType == null) return -1;
        List<BannerScheduleItem> banners = bannerCache.get(gameType.getId());
        if (banners == null) return -1;

        LocalDate today = LocalDate.now();
        LocalDate latestEnd = null;

        for (BannerScheduleItem banner : banners) {
            if (banner.getFeaturedFiveStars().contains(characterName)) {
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
                boolean matches = banner.getFeaturedFiveStars().contains(charName)
                        || banner.getFeaturedFiveStars().contains(item.getNameRu())
                        || banner.getFeaturedFiveStars().contains(item.getNameEn())
                        || banner.getFeaturedFiveStarsRu().contains(charName)
                        || banner.getFeaturedFiveStarsRu().contains(item.getNameRu())
                        || banner.getFeaturedFiveStarsEn().contains(item.getNameEn());

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
            result.add(new ru.menshovanton.gachapoint.domain.models.CharacterRerunInfo(
                    charName,
                    gameType.getId(),
                    days,
                    latestVersion,
                    endDateStr
            ));
        }

        if (result.isEmpty() && !banners.isEmpty()) {
            Map<String, LocalDate> charLastDates = new HashMap<>();
            Map<String, String> charLastVersions = new HashMap<>();
            for (BannerScheduleItem b : banners) {
                LocalDate bEnd = null;
                try {
                    bEnd = LocalDate.parse(b.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception ignored) {}
                for (String c : b.getFeaturedFiveStars()) {
                    if (bEnd != null) {
                        LocalDate prev = charLastDates.get(c);
                        if (prev == null || bEnd.isAfter(prev)) {
                            charLastDates.put(c, bEnd);
                            charLastVersions.put(c, b.getVersion());
                        }
                    }
                }
            }
            for (Map.Entry<String, LocalDate> entry : charLastDates.entrySet()) {
                int days = (int) ChronoUnit.DAYS.between(entry.getValue(), today);
                result.add(new ru.menshovanton.gachapoint.domain.models.CharacterRerunInfo(
                        entry.getKey(),
                        gameType.getId(),
                        days,
                        charLastVersions.get(entry.getKey()),
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
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root != null && root.has("games")) {
                JsonArray games = root.getAsJsonArray("games");
                for (JsonElement gElem : games) {
                    if (!gElem.isJsonObject()) continue;
                    JsonObject gObj = gElem.getAsJsonObject();
                    int gameType = gObj.has("game_type") ? gObj.get("game_type").getAsInt() : 0;

                    List<PatchInfo> patchList = new ArrayList<>();
                    if (gObj.has("patches")) {
                        for (JsonElement pElem : gObj.getAsJsonArray("patches")) {
                            if (!pElem.isJsonObject()) continue;
                            JsonObject pObj = pElem.getAsJsonObject();
                            patchList.add(new PatchInfo(
                                    pObj.has("version") ? pObj.get("version").getAsString() : "",
                                    pObj.has("title") ? pObj.get("title").getAsString() : "",
                                    pObj.has("start_date") ? pObj.get("start_date").getAsString() : "",
                                    pObj.has("end_date") ? pObj.get("end_date").getAsString() : ""
                            ));
                        }
                    }
                    patchCache.put(gameType, patchList);

                    List<BannerScheduleItem> bannerList = new ArrayList<>();
                    if (gObj.has("banners")) {
                        for (JsonElement bElem : gObj.getAsJsonArray("banners")) {
                            if (!bElem.isJsonObject()) continue;
                            JsonObject bObj = bElem.getAsJsonObject();

                            List<String> fiveStars = new ArrayList<>();
                            if (bObj.has("featured_five_stars") && bObj.get("featured_five_stars").isJsonArray()) {
                                for (JsonElement fe : bObj.getAsJsonArray("featured_five_stars")) {
                                    fiveStars.add(fe.getAsString());
                                }
                            }

                            List<String> fiveStarsRu = new ArrayList<>();
                            if (bObj.has("featured_five_stars_ru") && bObj.get("featured_five_stars_ru").isJsonArray()) {
                                for (JsonElement fe : bObj.getAsJsonArray("featured_five_stars_ru")) {
                                    fiveStarsRu.add(fe.getAsString());
                                }
                            }

                            List<String> fiveStarsEn = new ArrayList<>();
                            if (bObj.has("featured_five_stars_en") && bObj.get("featured_five_stars_en").isJsonArray()) {
                                for (JsonElement fe : bObj.getAsJsonArray("featured_five_stars_en")) {
                                    fiveStarsEn.add(fe.getAsString());
                                }
                            }

                            List<String> fourStars = new ArrayList<>();
                            if (bObj.has("featured_four_stars") && bObj.get("featured_four_stars").isJsonArray()) {
                                for (JsonElement fe : bObj.getAsJsonArray("featured_four_stars")) {
                                    fourStars.add(fe.getAsString());
                                }
                            }

                            List<String> fourStarsRu = new ArrayList<>();
                            if (bObj.has("featured_four_stars_ru") && bObj.get("featured_four_stars_ru").isJsonArray()) {
                                for (JsonElement fe : bObj.getAsJsonArray("featured_four_stars_ru")) {
                                    fourStarsRu.add(fe.getAsString());
                                }
                            }

                            List<String> fourStarsEn = new ArrayList<>();
                            if (bObj.has("featured_four_stars_en") && bObj.get("featured_four_stars_en").isJsonArray()) {
                                for (JsonElement fe : bObj.getAsJsonArray("featured_four_stars_en")) {
                                    fourStarsEn.add(fe.getAsString());
                                }
                            }

                            String bannerImageUrl = bObj.has("banner_image_url") ? bObj.get("banner_image_url").getAsString() : null;

                            bannerList.add(new BannerScheduleItem(
                                    bObj.has("id") ? bObj.get("id").getAsString() : "",
                                    gameType,
                                    bObj.has("version") ? bObj.get("version").getAsString() : "",
                                    bObj.has("phase") ? bObj.get("phase").getAsInt() : 1,
                                    bObj.has("start_date") ? bObj.get("start_date").getAsString() : "",
                                    bObj.has("end_date") ? bObj.get("end_date").getAsString() : "",
                                    bObj.has("status") ? bObj.get("status").getAsString() : "PAST",
                                    fiveStars,
                                    fourStars,
                                    !fiveStarsRu.isEmpty() ? fiveStarsRu : fiveStars,
                                    !fiveStarsEn.isEmpty() ? fiveStarsEn : fiveStars,
                                    !fourStarsRu.isEmpty() ? fourStarsRu : fourStars,
                                    !fourStarsEn.isEmpty() ? fourStarsEn : fourStars,
                                    bannerImageUrl
                            ));
                        }
                    }
                    bannerCache.put(gameType, bannerList);
                }
            }
        } catch (Exception e) {
            AppLogger.e("BannerScheduleRepository", "Failed to parse banners schedule JSON", e);
        }
    }
}
