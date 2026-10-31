package ru.menshovanton.gachapoint.domain.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CatalogItem {
    private final String name;
    private final String nameRu;
    private final String nameEn;
    private final String rarity;
    private final String type;
    private final boolean isStandard;
    private final String iconUrl;

    public CatalogItem(@NonNull String name, @Nullable String nameRu, @Nullable String nameEn,
                       @NonNull String rarity, @NonNull String type, boolean isStandard, @Nullable String iconUrl) {
        this.name = name;
        this.nameRu = nameRu != null ? nameRu : name;
        this.nameEn = nameEn != null ? nameEn : name;
        this.rarity = rarity;
        this.type = type;
        this.isStandard = isStandard;
        this.iconUrl = iconUrl;
    }

    public CatalogItem(@NonNull String name, @NonNull String rarity, @NonNull String type, boolean isStandard) {
        this(name, name, name, rarity, type, isStandard, null);
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getName(@Nullable Context context) {
        if (context == null) return name;
        try {
            String lang = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
            if ("ru".equalsIgnoreCase(lang) && nameRu != null && !nameRu.isEmpty()) {
                return nameRu;
            } else if (!"ru".equalsIgnoreCase(lang) && nameEn != null && !nameEn.isEmpty()) {
                return nameEn;
            }
        } catch (Exception ignored) {}
        return name;
    }

    @NonNull
    public String getNameRu() {
        return nameRu != null ? nameRu : name;
    }

    @NonNull
    public String getNameEn() {
        return nameEn != null ? nameEn : name;
    }

    @Nullable
    public String getIconUrl() {
        return iconUrl;
    }

    @NonNull
    public String getRarity() {
        return rarity;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public boolean isStandard() {
        return isStandard;
    }

    public boolean isFiveStar() {
        return "5★".equals(rarity);
    }

    public boolean isFourStar() {
        return "4★".equals(rarity);
    }
}
