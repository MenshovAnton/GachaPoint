package ru.menshovanton.gachapoint.domain.enums;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    @NonNull
    public static RareType from(@Nullable String rarity, @Nullable Context context) {
        return Rarity.from(rarity).toRareType();
    }

    @NonNull
    public static RareType from(@Nullable String rarity, @Nullable GameType gameType, @Nullable Context context) {
        return Rarity.from(rarity, gameType).toRareType();
    }
}
