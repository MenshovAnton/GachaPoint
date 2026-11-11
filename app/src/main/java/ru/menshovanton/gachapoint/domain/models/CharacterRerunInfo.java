package ru.menshovanton.gachapoint.domain.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CharacterRerunInfo {
    private final String characterNameRu;
    private final String characterNameEn;
    private final int gameType;
    private final int daysSinceLastRerun;
    @Nullable
    private final String lastVersion;
    @Nullable
    private final String lastBannerEndDate;

    public CharacterRerunInfo(
            @NonNull String characterName,
            int gameType,
            int daysSinceLastRerun,
            @Nullable String lastVersion,
            @Nullable String lastBannerEndDate
    ) {
        this(characterName, characterName, gameType, daysSinceLastRerun, lastVersion, lastBannerEndDate);
    }

    public CharacterRerunInfo(
            @NonNull String characterNameRu,
            @NonNull String characterNameEn,
            int gameType,
            int daysSinceLastRerun,
            @Nullable String lastVersion,
            @Nullable String lastBannerEndDate
    ) {
        this.characterNameRu = characterNameRu;
        this.characterNameEn = characterNameEn;
        this.gameType = gameType;
        this.daysSinceLastRerun = daysSinceLastRerun;
        this.lastVersion = lastVersion;
        this.lastBannerEndDate = lastBannerEndDate;
    }

    @NonNull
    public String getCharacterName() {
        return !characterNameRu.isEmpty() ? characterNameRu : characterNameEn;
    }

    @NonNull
    public String getCharacterName(@NonNull Context context) {
        String lang = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        if ("ru".equalsIgnoreCase(lang)) {
            return !characterNameRu.isEmpty() ? characterNameRu : characterNameEn;
        }
        return !characterNameEn.isEmpty() ? characterNameEn : characterNameRu;
    }

    @NonNull
    public String getCharacterNameRu() { return characterNameRu; }
    @NonNull
    public String getCharacterNameEn() { return characterNameEn; }
    public int getGameType() { return gameType; }
    public int getDaysSinceLastRerun() { return daysSinceLastRerun; }
    @Nullable
    public String getLastVersion() { return lastVersion; }
    @Nullable
    public String getLastBannerEndDate() { return lastBannerEndDate; }
}
