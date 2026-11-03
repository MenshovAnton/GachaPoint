package ru.menshovanton.gachapoint.domain.enums;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ru.menshovanton.gachapoint.R;

public enum Rarity {
    LEGENDARY(5, R.color.rare_legendary),
    EPIC(4, R.color.rare_epic),
    COMMON(3, R.color.text);

    private final int numericTier;
    @ColorRes
    private final int colorResId;

    Rarity(int numericTier, @ColorRes int colorResId) {
        this.numericTier = numericTier;
        this.colorResId = colorResId;
    }

    public int getNumericTier() {
        return numericTier;
    }

    @ColorRes
    public int getColorResId() {
        return colorResId;
    }

    public RareType toRareType() {
        switch (this) {
            case LEGENDARY:
                return RareType.LEGENDARY;
            case EPIC:
                return RareType.EPIC;
            default:
                return RareType.DEFAULT;
        }
    }

    /**
     * Returns authentic game display rank string:
     * - Genshin & HSR: 5★, 4★, 3★
     * - ZZZ & HI3: S, A, B
     */
    @NonNull
    public String getDisplayRank(@Nullable GameType gameType, @NonNull Context context) {
        boolean usesRanks = (gameType == GameType.ZZZ || gameType == GameType.HI3);
        switch (this) {
            case LEGENDARY:
                return usesRanks ? "S" : context.getString(R.string.five_star);
            case EPIC:
                return usesRanks ? "A" : context.getString(R.string.four_star);
            case COMMON:
            default:
                return usesRanks ? "B" : context.getString(R.string.three_star);
        }
    }

    /**
     * Returns localized label for pity counters:
     * - Genshin/HSR: "5★", "4★"
     * - ZZZ/HI3: "S-ранг", "A-ранг"
     */
    @NonNull
    public String getPityRankLabel(@Nullable GameType gameType, @NonNull Context context) {
        boolean usesRanks = (gameType == GameType.ZZZ || gameType == GameType.HI3);
        switch (this) {
            case LEGENDARY:
                return usesRanks ? "S" : context.getString(R.string.five_star);
            case EPIC:
                return usesRanks ? "A" : context.getString(R.string.four_star);
            default:
                return usesRanks ? "B" : context.getString(R.string.three_star);
        }
    }

    @NonNull
    public static Rarity from(@Nullable String raw) {
        return from(raw, null);
    }

    @NonNull
    public static Rarity from(@Nullable String raw, @Nullable GameType gameType) {
        if (raw == null || raw.trim().isEmpty()) {
            return COMMON;
        }

        String clean = raw.trim().toUpperCase(java.util.Locale.ROOT);

        if (clean.contains("5") || clean.startsWith("S") || clean.equals("LEGENDARY") || clean.contains("S-RANK") || clean.contains("S-РАНГ")) {
            return LEGENDARY;
        } else if (clean.contains("4") || clean.startsWith("A") || clean.equals("EPIC") || clean.contains("A-RANK") || clean.contains("A-РАНГ")) {
            return EPIC;
        } else {
            return COMMON;
        }
    }
}
