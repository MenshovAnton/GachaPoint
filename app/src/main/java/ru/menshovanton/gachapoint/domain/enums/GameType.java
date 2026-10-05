package ru.menshovanton.gachapoint.domain.enums;

public enum GameType {
    GENSHIN(0),
    HSR(1),
    ZZZ(2);

    private final int code;

    GameType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        switch (this) {
            case HSR:
                return "HSR";
            case ZZZ:
                return "ZZZ";
            default:
                return "Genshin";
        }
    }

    public static GameType fromCode(int code) {
        for (GameType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return GENSHIN;
    }
}