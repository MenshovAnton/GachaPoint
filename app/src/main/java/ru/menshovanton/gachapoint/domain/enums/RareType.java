package ru.menshovanton.gachapoint.domain.enums;

import android.content.Context;

import androidx.annotation.ColorRes;

import ru.menshovanton.gachapoint.R;

public enum RareType {
    LEGENDARY(R.color.rare_legendary),
    EPIC(R.color.rare_epic),
    DEFAULT(R.color.text);

    @ColorRes
    private final int colorResId;

    RareType(@ColorRes int colorResId) {
        this.colorResId = colorResId;
    }

    public int getColorResId() {
        return colorResId;
    }

    public static RareType from(String rarity, Context context) {
        if (rarity == null) {
            return DEFAULT;
        }
        String clean = rarity.trim();
        if (context.getString(R.string.four_star).equalsIgnoreCase(clean)
                || "A".equalsIgnoreCase(clean) || "A-Rank".equalsIgnoreCase(clean) || "A-ранг".equalsIgnoreCase(clean)) {
            return EPIC;
        } else if (context.getString(R.string.five_star).equalsIgnoreCase(clean)
                || "S".equalsIgnoreCase(clean) || "S-Rank".equalsIgnoreCase(clean) || "S-ранг".equalsIgnoreCase(clean)) {
            return LEGENDARY;
        } else {
            return DEFAULT;
        }
    }
}
