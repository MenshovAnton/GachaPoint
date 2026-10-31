package ru.menshovanton.gachapoint.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import ru.menshovanton.gachapoint.domain.enums.GameType;

public final class DataSyncConfig {

    private DataSyncConfig() {}

    public static final String PROMOCODES_REMOTE_URL =
            "https://raw.githubusercontent.com/menshovanton/GachaPoint-Data/main/promocodes.json";
    public static final String BANNERS_REMOTE_URL =
            "https://raw.githubusercontent.com/menshovanton/GachaPoint-Data/main/banners_schedule.json";
    public static final String CATALOG_REMOTE_URL =
            "https://raw.githubusercontent.com/menshovanton/GachaPoint-Data/main/game_catalog.json";

    public static final String ASSET_PROMOCODES = "data/promocodes.json";
    public static final String ASSET_BANNERS = "data/banners_schedule.json";
    public static final String ASSET_CATALOG = "data/game_catalog.json";

    public static final String CACHE_PROMOCODES = "promocodes_v1_cache.json";
    public static final String CACHE_BANNERS = "banners_v1_cache.json";
    public static final String CACHE_CATALOG = "catalog_v1_cache.json";

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,30}$");

    public static boolean isValidCode(@Nullable String code) {
        if (code == null) return false;
        return CODE_PATTERN.matcher(code.trim()).matches();
    }

    public static String sanitizeCode(@NonNull String code) {
        return code.trim().toUpperCase();
    }

    @Nullable
    public static String getOfficialRedeemUrl(@NonNull GameType gameType, @NonNull String rawCode) {
        if (!isValidCode(rawCode)) {
            return null;
        }
        String cleanCode = sanitizeCode(rawCode);

        switch (gameType) {
            case GENSHIN:
                return "https://genshin.hoyoverse.com/ru/gift?code=" + cleanCode;
            case HSR:
                return "https://hsr.hoyoverse.com/gift?code=" + cleanCode;
            case ZZZ:
                return "https://zenless.hoyoverse.com/redemption?code=" + cleanCode;
            case HI3:
                return "https://act.hoyoverse.com/puzzle/bh3/pz_u6h7mQo34g/index.html?cdkey=" + cleanCode;
            default:
                return null;
        }
    }
}
