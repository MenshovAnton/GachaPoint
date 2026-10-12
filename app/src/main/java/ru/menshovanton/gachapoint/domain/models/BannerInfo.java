package ru.menshovanton.gachapoint.domain.models;

import androidx.annotation.StringRes;

import ru.menshovanton.gachapoint.domain.enums.BannerType;

public class BannerInfo {
    private final BannerType type;
    @StringRes private final int labelResId;
    private final int maxPity5Star;
    private final int softPityStart5Star;
    private final int maxPity4Star;
    private final boolean hasFiftyFifty;

    public BannerInfo(BannerType type, @StringRes int labelResId, int maxPity5Star, int softPityStart5Star, int maxPity4Star, boolean hasFiftyFifty) {
        this.type = type;
        this.labelResId = labelResId;
        this.maxPity5Star = maxPity5Star;
        this.softPityStart5Star = softPityStart5Star;
        this.maxPity4Star = maxPity4Star;
        this.hasFiftyFifty = hasFiftyFifty;
    }

    public BannerInfo(BannerType type, @StringRes int labelResId) {
        this(type, labelResId, 90, 74, 10, type == BannerType.EVENT);
    }

    public BannerType getType() {
        return type;
    }

    @StringRes
    public int getLabelResId() {
        return labelResId;
    }

    public String getDbKey() {
        return type.getDbKey();
    }

    public int getMaxPity5Star() {
        return maxPity5Star;
    }

    public int getSoftPityStart5Star() {
        return softPityStart5Star;
    }

    public int getMaxPity4Star() {
        return maxPity4Star;
    }

    public boolean hasFiftyFifty() {
        return hasFiftyFifty;
    }
}
