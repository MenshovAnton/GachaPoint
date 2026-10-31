package ru.menshovanton.gachapoint.domain.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CharacterRerunInfo {
    private final String characterName;
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
        this.characterName = characterName;
        this.gameType = gameType;
        this.daysSinceLastRerun = daysSinceLastRerun;
        this.lastVersion = lastVersion;
        this.lastBannerEndDate = lastBannerEndDate;
    }

    @NonNull
    public String getCharacterName() { return characterName; }
    public int getGameType() { return gameType; }
    public int getDaysSinceLastRerun() { return daysSinceLastRerun; }
    @Nullable
    public String getLastVersion() { return lastVersion; }
    @Nullable
    public String getLastBannerEndDate() { return lastBannerEndDate; }
}
