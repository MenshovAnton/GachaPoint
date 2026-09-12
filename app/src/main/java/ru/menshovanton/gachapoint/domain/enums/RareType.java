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
        if (context.getString(R.string.four_star).equalsIgnoreCase(rarity)) {
            return EPIC;
        } else if (context.getString(R.string.five_star).equalsIgnoreCase(rarity)) {
            return LEGENDARY;
        } else {
            return DEFAULT;
        }
    }
}
