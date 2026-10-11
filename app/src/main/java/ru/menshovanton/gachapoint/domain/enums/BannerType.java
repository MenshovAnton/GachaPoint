package ru.menshovanton.gachapoint.domain.enums;

public enum BannerType {
    EVENT("BANNER_EVENT"),
    WEAPON("BANNER_WEAPON"),
    STANDARD("BANNER_STANDARD"),
    BEGINNER("BANNER_BEGINNER"),
    CHRONICLE("BANNER_CHRONICLE"),
    BANGBOO("BANNER_BANGBOO"),
    SPECIAL("BANNER_SPECIAL");

    private final String dbKey;

    BannerType(String dbKey) {
        this.dbKey = dbKey;
    }

    public String getDbKey() {
        return dbKey;
    }
}
