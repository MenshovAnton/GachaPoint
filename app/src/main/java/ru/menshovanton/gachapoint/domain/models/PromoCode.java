package ru.menshovanton.gachapoint.domain.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class PromoCode {
    private final String id;
    private final int gameType;
    private final String code;
    private final String rewardsDescription;
    private final String rewardsDescriptionRu;
    private final String rewardsDescriptionEn;
    private final List<String> rewardTypes;
    private final String status; // STREAM, NEW, PERMANENT, ACTIVE
    @Nullable
    private final String expiresAt;
    @Nullable
    private final String createdAt;
    private final boolean isStream;
    private final boolean isActive;
    private boolean isRedeemed;
    private boolean isExpiredByUser;

    public PromoCode(
            @NonNull String id,
            int gameType,
            @NonNull String code,
            @NonNull String rewardsDescription,
            @Nullable String rewardsDescriptionRu,
            @Nullable String rewardsDescriptionEn,
            @Nullable List<String> rewardTypes,
            @NonNull String status,
            @Nullable String expiresAt,
            @Nullable String createdAt,
            boolean isStream,
            boolean isActive,
            boolean isRedeemed,
            boolean isExpiredByUser
    ) {
        this.id = id;
        this.gameType = gameType;
        this.code = code;
        this.rewardsDescription = rewardsDescription;
        this.rewardsDescriptionRu = rewardsDescriptionRu != null ? rewardsDescriptionRu : rewardsDescription;
        this.rewardsDescriptionEn = rewardsDescriptionEn != null ? rewardsDescriptionEn : rewardsDescription;
        this.rewardTypes = rewardTypes != null ? rewardTypes : Collections.emptyList();
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.isStream = isStream;
        this.isActive = isActive;
        this.isRedeemed = isRedeemed;
        this.isExpiredByUser = isExpiredByUser;
    }

    public PromoCode(
            @NonNull String id,
            int gameType,
            @NonNull String code,
            @NonNull String rewardsDescription,
            @Nullable String rewardsDescriptionRu,
            @Nullable String rewardsDescriptionEn,
            @Nullable List<String> rewardTypes,
            @NonNull String status,
            @Nullable String expiresAt,
            boolean isStream,
            boolean isActive,
            boolean isRedeemed
    ) {
        this(id, gameType, code, rewardsDescription, rewardsDescriptionRu, rewardsDescriptionEn,
                rewardTypes, status, expiresAt, null, isStream, isActive, isRedeemed, false);
    }

    public PromoCode(
            @NonNull String id,
            int gameType,
            @NonNull String code,
            @NonNull String rewardsDescription,
            @Nullable List<String> rewardTypes,
            @NonNull String status,
            @Nullable String expiresAt,
            boolean isStream,
            boolean isActive,
            boolean isRedeemed
    ) {
        this(id, gameType, code, rewardsDescription, rewardsDescription, rewardsDescription,
                rewardTypes, status, expiresAt, null, isStream, isActive, isRedeemed, false);
    }

    @NonNull
    public String getId() { return id; }
    public int getGameType() { return gameType; }
    @NonNull
    public String getCode() { return code; }
    @NonNull
    public String getRewardsDescription() { return rewardsDescription; }

    @NonNull
    public String getRewardsDescription(@Nullable Context context) {
        if (context == null) return rewardsDescription;
        try {
            String lang = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
            if ("ru".equalsIgnoreCase(lang) && rewardsDescriptionRu != null && !rewardsDescriptionRu.isEmpty()) {
                return rewardsDescriptionRu;
            } else if (!"ru".equalsIgnoreCase(lang) && rewardsDescriptionEn != null && !rewardsDescriptionEn.isEmpty()) {
                return rewardsDescriptionEn;
            }
        } catch (Exception ignored) {}
        return rewardsDescription;
    }

    @NonNull
    public List<String> getRewardTypes() { return rewardTypes; }
    @NonNull
    public String getStatus() { return status; }
    @Nullable
    public String getExpiresAt() { return expiresAt; }
    @Nullable
    public String getCreatedAt() { return createdAt; }
    public boolean isStream() { return isStream; }

    public boolean isPermanent() {
        if ("PERMANENT".equalsIgnoreCase(status)) {
            return true;
        }
        String clean = code.trim().toUpperCase(java.util.Locale.ROOT);
        return clean.equals("GENSHINGIFT")
                || clean.equals("STARRAILGIFT")
                || clean.equals("ZENLESSGIFT")
                || clean.equals("ZZZTVCM")
                || clean.equals("HONKAIGIFT")
                || clean.equals("CHEERSCAPTAIN")
                || clean.equals("CREATIONNYMPH")
                || clean.equals("FAREWELL")
                || clean.equals("IFYOUAREREADINGTHIS")
                || clean.equals("OMEGA");
    }

    public boolean isNew() {
        if (isExpired() || isRedeemed() || isPermanent()) {
            return false;
        }
        if ("NEW".equalsIgnoreCase(status)) {
            return true;
        }
        if (createdAt != null && !createdAt.trim().isEmpty()) {
            try {
                Instant created = Instant.parse(createdAt.trim());
                long days = Duration.between(created, Instant.now()).toDays();
                return days <= 7;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public boolean isExpired() {
        if (isExpiredByUser) {
            return true;
        }
        if (isPermanent()) {
            return false;
        }
        if (!isActive || "EXPIRED".equalsIgnoreCase(status)) {
            return true;
        }

        // Hard invariant: Livestream codes expire within 28 hours of broadcast/creation,
        // regardless of any erroneous future date in expiresAt.
        if (isStream() || "STREAM".equalsIgnoreCase(status)) {
            if (createdAt != null && !createdAt.trim().isEmpty()) {
                try {
                    Instant created = Instant.parse(createdAt.trim());
                    long hours = Duration.between(created, Instant.now()).toHours();
                    if (hours >= 28) {
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (expiresAt != null && !expiresAt.trim().isEmpty()) {
            try {
                Instant expiry = Instant.parse(expiresAt.trim());
                return Instant.now().isAfter(expiry);
            } catch (Exception ignored) {
                try {
                    LocalDate expiryDate = LocalDate.parse(expiresAt.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                    return LocalDate.now().isAfter(expiryDate);
                } catch (Exception ignored2) {}
            }
        }

        return isStream() || "STREAM".equalsIgnoreCase(status);
    }

    public long getRemainingHours() {
        if (expiresAt == null || expiresAt.trim().isEmpty() || isExpired()) {
            return -1;
        }
        try {
            Instant expiry = Instant.parse(expiresAt.trim());
            Duration diff = Duration.between(Instant.now(), expiry);
            return diff.isNegative() ? 0 : diff.toHours();
        } catch (Exception ignored) {
            return -1;
        }
    }

    public boolean isActive() {
        return isActive && !isExpired();
    }

    public boolean isRedeemed() { return isRedeemed; }
    public void setRedeemed(boolean redeemed) { isRedeemed = redeemed; }

    public boolean isExpiredByUser() { return isExpiredByUser; }
    public void setExpiredByUser(boolean expiredByUser) { this.isExpiredByUser = expiredByUser; }
}
