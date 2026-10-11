package ru.menshovanton.gachapoint.domain.enums;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.models.BannerInfo;

public enum GameType {
    GENSHIN(
            0,
            "Genshin",
            90,
            160,
            6,
            R.string.genshin,
            R.drawable.icon_primogem,
            R.drawable.icon_intertwined_fate,
            R.string.blessing_of_the_welkin_moon_count_header,
            Arrays.asList(
                    new BannerInfo(BannerType.EVENT, R.string.banner_type_genshin_event),
                    new BannerInfo(BannerType.STANDARD, R.string.banner_type_genshin_std),
                    new BannerInfo(BannerType.WEAPON, R.string.banner_type_genshin_weapon),
                    new BannerInfo(BannerType.BEGINNER, R.string.banner_type_genshin_novice),
                    new BannerInfo(BannerType.CHRONICLE, R.string.banner_type_genshin_chronicled),
                    new BannerInfo(BannerType.SPECIAL, R.string.banner_type_spec)
            )
    ),
    HSR(
            1,
            "HSR",
            90,
            160,
            6,
            R.string.hsr,
            R.drawable.icon_stellar_jade,
            R.drawable.icon_star_rail_special_pass,
            R.string.star_rail_special_pass_count_header,
            Arrays.asList(
                    new BannerInfo(BannerType.EVENT, R.string.banner_type_hsr_event),
                    new BannerInfo(BannerType.WEAPON, R.string.banner_type_hsr_weapon),
                    new BannerInfo(BannerType.STANDARD, R.string.banner_type_hsr_std),
                    new BannerInfo(BannerType.BEGINNER, R.string.banner_type_hsr_novice),
                    new BannerInfo(BannerType.CHRONICLE, R.string.banner_type_hsr_chronicled),
                    new BannerInfo(BannerType.SPECIAL, R.string.banner_type_spec)
            )
    ),
    ZZZ(
            2,
            "ZZZ",
            90,
            160,
            6,
            R.string.zzz,
            R.drawable.icon_polychrome,
            R.drawable.icon_encrypted_master_tape,
            R.string.inter_knot_member_count_header,
            Arrays.asList(
                    new BannerInfo(BannerType.EVENT, R.string.banner_type_zzz_event),
                    new BannerInfo(BannerType.WEAPON, R.string.banner_type_zzz_weapon),
                    new BannerInfo(BannerType.STANDARD, R.string.banner_type_zzz_std),
                    new BannerInfo(BannerType.BEGINNER, R.string.banner_type_zzz_novice),
                    new BannerInfo(BannerType.BANGBOO, R.string.banner_type_zzz_bangboo),
                    new BannerInfo(BannerType.SPECIAL, R.string.banner_type_spec)
            )
    );

    private final int code;
    private final String name;
    private final int currencyPerDay;
    private final int pullCost;
    private final int maxSubs;
    @StringRes private final int nameResId;
    @DrawableRes private final int currencyIconResId;
    @DrawableRes private final int pullIconResId;
    @StringRes private final int subTitleResId;
    private final List<BannerInfo> banners;

    GameType(int code, String name, int currencyPerDay, int pullCost, int maxSubs, @StringRes int nameResId,
            @DrawableRes int currencyIconResId, @DrawableRes int pullIconResId, @StringRes int subTitleResId, List<BannerInfo> banners) {
        this.code = code;
        this.name = name;
        this.currencyPerDay = currencyPerDay;
        this.pullCost = pullCost;
        this.maxSubs = maxSubs;
        this.nameResId = nameResId;
        this.currencyIconResId = currencyIconResId;
        this.pullIconResId = pullIconResId;
        this.subTitleResId = subTitleResId;
        this.banners = Collections.unmodifiableList(banners);
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static GameType fromCode(int code) {
        for (GameType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return GENSHIN;
    }

    public int getCurrencyPerDay() {
        return currencyPerDay;
    }

    public int getPullCost() {
        return pullCost;
    }

    public int getMaxSubs() {
        return maxSubs;
    }

    @StringRes
    public int getNameResId() {
        return nameResId;
    }

    @DrawableRes
    public int getCurrencyIconResId() {
        return currencyIconResId;
    }

    @DrawableRes
    public int getPullIconResId() {
        return pullIconResId;
    }

    @StringRes
    public int getSubTitleResId() {
        return subTitleResId;
    }

    public List<BannerInfo> getBanners() {
        return banners;
    }

    public String getBannerLabel(Context context, String dbKey) {
        if (dbKey != null) {
            for (BannerInfo banner : banners) {
                if (banner.getDbKey().equals(dbKey)) {
                    return context.getString(banner.getLabelResId());
                }
            }
        }
        return context.getString(R.string.banner_type_spec);
    }

    public String getBannerKeyByLabel(Context context, String label) {
        if (label != null) {
            for (BannerInfo banner : banners) {
                if (context.getString(banner.getLabelResId()).equalsIgnoreCase(label)) {
                    return banner.getDbKey();
                }
            }
        }
        return BannerType.SPECIAL.getDbKey();
    }
}