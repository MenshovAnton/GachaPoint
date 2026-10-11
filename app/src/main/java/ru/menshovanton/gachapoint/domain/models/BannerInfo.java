package ru.menshovanton.gachapoint.domain.models;

import androidx.annotation.StringRes;

import ru.menshovanton.gachapoint.domain.enums.BannerType;

public class BannerInfo {
    private final BannerType type;
    @StringRes private final int labelResId;

    public BannerInfo(BannerType type, @StringRes int labelResId) {
        this.type = type;
        this.labelResId = labelResId;
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
}
