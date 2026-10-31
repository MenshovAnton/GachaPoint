package ru.menshovanton.gachapoint.data.db.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "promocode_statuses")
public class PromocodeStatusEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "code_id")
    public String codeId = "";

    @ColumnInfo(name = "is_redeemed", defaultValue = "0")
    public boolean isRedeemed;

    @ColumnInfo(name = "redeemed_at", defaultValue = "0")
    public long redeemedAt;

    @ColumnInfo(name = "is_notified", defaultValue = "0")
    public boolean isNotified;

    @ColumnInfo(name = "is_expired_by_user", defaultValue = "0")
    public boolean isExpiredByUser;

    public PromocodeStatusEntity() {}

    @Ignore
    public PromocodeStatusEntity(@NonNull String codeId, boolean isRedeemed, long redeemedAt, boolean isNotified) {
        this(codeId, isRedeemed, redeemedAt, isNotified, false);
    }

    @Ignore
    public PromocodeStatusEntity(@NonNull String codeId, boolean isRedeemed, long redeemedAt, boolean isNotified, boolean isExpiredByUser) {
        this.codeId = codeId;
        this.isRedeemed = isRedeemed;
        this.redeemedAt = redeemedAt;
        this.isNotified = isNotified;
        this.isExpiredByUser = isExpiredByUser;
    }
}
