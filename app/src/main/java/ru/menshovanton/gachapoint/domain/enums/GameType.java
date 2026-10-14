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
            60,
            30,
            77,
            R.string.genshin,
            R.drawable.icon_primogem,
            R.drawable.icon_intertwined_fate,
            R.string.blessing_of_the_welkin_moon_count_header,
            R.string.sub_name_genshin,
            Arrays.asList(
                    new BannerInfo(BannerType.EVENT, R.string.banner_type_genshin_event, 90, 74, 10, true),
                    new BannerInfo(BannerType.STANDARD, R.string.banner_type_genshin_std, 90, 74, 10, false),
                    new BannerInfo(BannerType.WEAPON, R.string.banner_type_genshin_weapon, 80, 63, 10, true),
                    new BannerInfo(BannerType.BEGINNER, R.string.banner_type_genshin_novice, 20, 15, 10, false),
                    new BannerInfo(BannerType.CHRONICLE, R.string.banner_type_genshin_chronicled, 90, 74, 10, true),
                    new BannerInfo(BannerType.SPECIAL, R.string.banner_type_spec, 90, 74, 10, false)
            )
    ),
    HSR(
            1,
            "HSR",
            90,
            160,
            6,
            60,
            30,
            77,
            R.string.hsr,
            R.drawable.icon_stellar_jade,
            R.drawable.icon_star_rail_special_pass,
            R.string.star_rail_special_pass_count_header,
            R.string.sub_name_hsr,
            Arrays.asList(
                    new BannerInfo(BannerType.EVENT, R.string.banner_type_hsr_event, 90, 74, 10, true),
                    new BannerInfo(BannerType.WEAPON, R.string.banner_type_hsr_weapon, 80, 65, 10, true),
                    new BannerInfo(BannerType.STANDARD, R.string.banner_type_hsr_std, 90, 74, 10, false),
                    new BannerInfo(BannerType.BEGINNER, R.string.banner_type_hsr_novice, 50, 45, 10, false),
                    new BannerInfo(BannerType.CHRONICLE, R.string.banner_type_hsr_chronicled, 90, 74, 10, true),
                    new BannerInfo(BannerType.SPECIAL, R.string.banner_type_spec, 90, 74, 10, false)
            )
    ),
    ZZZ(
            2,
            "ZZZ",
            90,
            160,
            6,
            60,
            30,
            77,
            R.string.zzz,
            R.drawable.icon_polychrome,
            R.drawable.icon_encrypted_master_tape,
            R.string.inter_knot_member_count_header,
            R.string.sub_name_zzz,
            Arrays.asList(
                    new BannerInfo(BannerType.EVENT, R.string.banner_type_zzz_event, 90, 74, 10, true),
                    new BannerInfo(BannerType.WEAPON, R.string.banner_type_zzz_weapon, 80, 65, 10, true),
                    new BannerInfo(BannerType.STANDARD, R.string.banner_type_zzz_std, 90, 74, 10, false),
                    new BannerInfo(BannerType.BEGINNER, R.string.banner_type_zzz_novice, 50, 45, 10, false),
                    new BannerInfo(BannerType.BANGBOO, R.string.banner_type_zzz_bangboo, 80, 65, 10, false),
                    new BannerInfo(BannerType.SPECIAL, R.string.banner_type_spec, 90, 74, 10, false)
            )
    );

    private final int code;
    private final String name;
    private final int currencyPerDay;
    private final int pullCost;
    private final int maxSubs;
    private final int dailyReward;
    private final int subscriptionDurationDays;
    private final int defaultPity;
    @StringRes private final int nameResId;
    @DrawableRes private final int currencyIconResId;
    @DrawableRes private final int pullIconResId;
    @StringRes private final int subTitleResId;
    @StringRes private final int subscriptionNameResId;
    private final List<BannerInfo> banners;

    GameType(int code, String name, int currencyPerDay, int pullCost, int maxSubs,
            int dailyReward, int subscriptionDurationDays, int defaultPity,
            @StringRes int nameResId, @DrawableRes int currencyIconResId, @DrawableRes int pullIconResId,
            @StringRes int subTitleResId, @StringRes int subscriptionNameResId, List<BannerInfo> banners) {
        this.code = code;
        this.name = name;
        this.currencyPerDay = currencyPerDay;
        this.pullCost = pullCost;
        this.maxSubs = maxSubs;
        this.dailyReward = dailyReward;
        this.subscriptionDurationDays = subscriptionDurationDays;
        this.defaultPity = defaultPity;
        this.nameResId = nameResId;
        this.currencyIconResId = currencyIconResId;
        this.pullIconResId = pullIconResId;
        this.subTitleResId = subTitleResId;
        this.subscriptionNameResId = subscriptionNameResId;
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

    public int getDailyReward() {
        return dailyReward;
    }

    public int getSubscriptionDurationDays() {
        return subscriptionDurationDays;
    }

    public int getDefaultPity() {
        return defaultPity;
    }

    public int getSubscriptionTotalCurrency() {
        return currencyPerDay * subscriptionDurationDays;
    }

    public String getPiggyBankTargetKey() {
        if (this == GENSHIN) return "Piggy Bank target Genshin Impact";
        return "Piggy Bank target " + name;
    }

    public String getPiggyBankManualProgressKey() {
        if (this == GENSHIN) return "Piggy Bank manual progress Genshin Impact";
        return "Piggy Bank manual progress " + name;
    }

    public String getPiggyBankSubsProgressKey() {
        if (this == GENSHIN) return "Piggy Bank subs progress Genshin Impact";
        return "Piggy Bank subs progress " + name;
    }

    public String getPiggyBankDailiesProgressKey() {
        if (this == GENSHIN) return "Piggy Bank dailies progress Genshin Impact";
        return "Piggy Bank dailies progress " + name;
    }

    public String getNeedMigrationKey() {
        return name + " need migration";
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

    @StringRes
    public int getSubscriptionNameResId() {
        return subscriptionNameResId;
    }

    public List<BannerInfo> getBanners() {
        return banners;
    }

    public BannerInfo getBannerInfo(String dbKey) {
        if (dbKey != null) {
            for (BannerInfo banner : banners) {
                if (banner.getDbKey().equals(dbKey)) {
                    return banner;
                }
            }
        }
        return banners.isEmpty() ? null : banners.get(0);
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