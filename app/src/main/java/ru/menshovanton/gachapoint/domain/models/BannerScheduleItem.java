package ru.menshovanton.gachapoint.domain.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class BannerScheduleItem {
    private final String id;
    private final int gameType;
    private final String version;
    private final int phase;
    private final String startDate;
    private final String endDate;
    private final String status; // CURRENT, FUTURE, PAST
    private final List<String> featuredFiveStars;
    private final List<String> featuredFourStars;
    private final List<String> featuredFiveStarsRu;
    private final List<String> featuredFiveStarsEn;
    private final List<String> featuredFourStarsRu;
    private final List<String> featuredFourStarsEn;
    private final String bannerImageUrl;

    public BannerScheduleItem(
            @NonNull String id,
            int gameType,
            @NonNull String version,
            int phase,
            @NonNull String startDate,
            @NonNull String endDate,
            @NonNull String status,
            @Nullable List<String> featuredFiveStars,
            @Nullable List<String> featuredFourStars,
            @Nullable List<String> featuredFiveStarsRu,
            @Nullable List<String> featuredFiveStarsEn,
            @Nullable List<String> featuredFourStarsRu,
            @Nullable List<String> featuredFourStarsEn,
            @Nullable String bannerImageUrl
    ) {
        this.id = id;
        this.gameType = gameType;
        this.version = version;
        this.phase = phase;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.featuredFiveStars = featuredFiveStars != null ? featuredFiveStars : Collections.emptyList();
        this.featuredFourStars = featuredFourStars != null ? featuredFourStars : Collections.emptyList();
        this.featuredFiveStarsRu = featuredFiveStarsRu != null ? featuredFiveStarsRu : this.featuredFiveStars;
        this.featuredFiveStarsEn = featuredFiveStarsEn != null ? featuredFiveStarsEn : this.featuredFiveStars;
        this.featuredFourStarsRu = featuredFourStarsRu != null ? featuredFourStarsRu : this.featuredFourStars;
        this.featuredFourStarsEn = featuredFourStarsEn != null ? featuredFourStarsEn : this.featuredFourStars;
        this.bannerImageUrl = bannerImageUrl;
    }

    public BannerScheduleItem(
            @NonNull String id,
            int gameType,
            @NonNull String version,
            int phase,
            @NonNull String startDate,
            @NonNull String endDate,
            @NonNull String status,
            @Nullable List<String> featuredFiveStars,
            @Nullable List<String> featuredFourStars
    ) {
        this(id, gameType, version, phase, startDate, endDate, status,
                featuredFiveStars, featuredFourStars, featuredFiveStars, featuredFiveStars,
                featuredFourStars, featuredFourStars, null);
    }

    @NonNull
    public String getId() { return id; }
    public int getGameType() { return gameType; }
    @NonNull
    public String getVersion() { return version; }
    public int getPhase() { return phase; }
    @NonNull
    public String getStartDate() { return startDate; }
    @NonNull
    public String getEndDate() { return endDate; }
    @NonNull
    public String getStatus() { return status; }

    @NonNull
    public List<String> getFeaturedFiveStars() { return featuredFiveStars; }
    @NonNull
    public List<String> getFeaturedFiveStarsRu() { return featuredFiveStarsRu; }
    @NonNull
    public List<String> getFeaturedFiveStarsEn() { return featuredFiveStarsEn; }

    @NonNull
    public List<String> getFeaturedFiveStars(@Nullable Context context) {
        if (context == null) return featuredFiveStars;
        try {
            String lang = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
            if ("ru".equalsIgnoreCase(lang) && !featuredFiveStarsRu.isEmpty()) {
                return featuredFiveStarsRu;
            } else if (!"ru".equalsIgnoreCase(lang) && !featuredFiveStarsEn.isEmpty()) {
                return featuredFiveStarsEn;
            }
        } catch (Exception ignored) {}
        return featuredFiveStars;
    }

    @NonNull
    public List<String> getFeaturedFourStars() { return featuredFourStars; }
    @NonNull
    public List<String> getFeaturedFourStarsRu() { return featuredFourStarsRu; }
    @NonNull
    public List<String> getFeaturedFourStarsEn() { return featuredFourStarsEn; }

    @NonNull
    public List<String> getFeaturedFourStars(@Nullable Context context) {
        if (context == null) return featuredFourStars;
        try {
            String lang = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
            if ("ru".equalsIgnoreCase(lang) && !featuredFourStarsRu.isEmpty()) {
                return featuredFourStarsRu;
            } else if (!"ru".equalsIgnoreCase(lang) && !featuredFourStarsEn.isEmpty()) {
                return featuredFourStarsEn;
            }
        } catch (Exception ignored) {}
        return featuredFourStars;
    }

    @Nullable
    public String getBannerImageUrl() { return bannerImageUrl; }
}
