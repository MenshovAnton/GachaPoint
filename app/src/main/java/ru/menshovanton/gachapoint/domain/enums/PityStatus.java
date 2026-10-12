package ru.menshovanton.gachapoint.domain.enums;

import androidx.annotation.StringRes;

import ru.menshovanton.gachapoint.R;

public enum PityStatus {
    NONE(R.string.pity_status_none),
    WIN_50_50(R.string.pity_status_win),
    LOSS_50_50(R.string.pity_status_loss),
    GUARANTEED(R.string.pity_status_guaranteed);

    @StringRes
    private final int labelResId;

    PityStatus(@StringRes int labelResId) {
        this.labelResId = labelResId;
    }

    @StringRes
    public int getLabelResId() {
        return labelResId;
    }

    public static PityStatus fromName(String name) {
        if (name == null) return NONE;
        for (PityStatus status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return NONE;
    }
}
