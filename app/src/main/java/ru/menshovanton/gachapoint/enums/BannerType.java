package ru.menshovanton.gachapoint.enums;

import android.content.Context;

import androidx.annotation.StringRes;

import ru.menshovanton.gachapoint.R;

public enum BannerType {
    EVENT("BANNER_EVENT", R.string.type_event),
    SPECIAL("BANNER_SPECIAL", R.string.type_spec),
    STANDARD("BANNER_STANDARD", R.string.type_std),
    BEGINNER("BANNER_BEGINNER", R.string.type_novie);

    private final String dbKey;
    private final int labelResId;

    BannerType(String dbKey, @StringRes int labelResId) {
        this.dbKey = dbKey;
        this.labelResId = labelResId;
    }

    public String getDbKey() {
        return dbKey;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public static String getKeyByLabel(Context context, String label) {
        if (label != null) {
            for (BannerType type : values()) {
                if (context.getString(type.labelResId).equalsIgnoreCase(label)) {
                    return type.dbKey;
                }
            }
        }
        return EVENT.dbKey;
    }

    public static String getLabelByKey(Context context, String key) {
        if (key != null) {
            for (BannerType type : values()) {
                if (type.dbKey.equals(key)) {
                    return context.getString(type.labelResId);
                }
            }
        }
        return context.getString(EVENT.labelResId);
    }
}
