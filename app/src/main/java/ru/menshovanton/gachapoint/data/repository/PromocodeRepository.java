package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.db.dao.PromocodeDao;
import ru.menshovanton.gachapoint.data.db.entities.PromocodeStatusEntity;
import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.data.local.DataSyncConfig;
import ru.menshovanton.gachapoint.data.local.JsonDataLoader;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.PromoCode;

public class PromocodeRepository {

    private static volatile PromocodeRepository INSTANCE;

    private final Context context;
    private final PromocodeDao promocodeDao;
    private final Gson gson;

    private PromocodeRepository(Context context) {
        this.context = context.getApplicationContext();
        this.promocodeDao = AppDatabase.getInstance(context).promocodeDao();
        this.gson = new Gson();
    }

    public static PromocodeRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PromocodeRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PromocodeRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public List<PromoCode> getPromoCodes(GameType gameType, boolean forceRefresh) {
        String json = JsonDataLoader.loadJsonData(
                context,
                DataSyncConfig.PROMOCODES_REMOTE_URL,
                DataSyncConfig.CACHE_PROMOCODES,
                DataSyncConfig.ASSET_PROMOCODES,
                forceRefresh
        );

        List<PromoCode> result = parsePromoCodes(json, gameType != null ? gameType.getId() : -1);

        try {
            List<PromocodeStatusEntity> statuses = promocodeDao.getAllStatusesSync();
            Map<String, PromocodeStatusEntity> statusMap = new HashMap<>();
            for (PromocodeStatusEntity s : statuses) {
                statusMap.put(s.codeId, s);
            }

            for (PromoCode code : result) {
                PromocodeStatusEntity st = statusMap.get(code.getId());
                if (st != null) {
                    code.setRedeemed(st.isRedeemed);
                    code.setExpiredByUser(st.isExpiredByUser);
                }
            }
        } catch (Exception e) {
            AppLogger.e("PromocodeRepository", "Error syncing promocode statuses from DB", e);
        }

        return result;
    }

    public void setRedeemed(@NonNull String codeId, boolean isRedeemed) {
        AppDatabase.getExecutor().execute(() -> {
            try {
                PromocodeStatusEntity entity = promocodeDao.getStatusSync(codeId);
                long now = System.currentTimeMillis();
                if (entity == null) {
                    entity = new PromocodeStatusEntity(codeId, isRedeemed, now, false);
                    promocodeDao.insertOrUpdate(entity);
                } else {
                    promocodeDao.updateRedeemed(codeId, isRedeemed, now);
                }
            } catch (Exception e) {
                AppLogger.e("PromocodeRepository", "Error saving redeemed status for " + codeId, e);
            }
        });
    }

    public void setExpiredByUser(@NonNull String codeId, boolean isExpiredByUser) {
        AppDatabase.getExecutor().execute(() -> {
            try {
                PromocodeStatusEntity entity = promocodeDao.getStatusSync(codeId);
                if (entity == null) {
                    entity = new PromocodeStatusEntity(codeId, false, 0, false, isExpiredByUser);
                    promocodeDao.insertOrUpdate(entity);
                } else {
                    promocodeDao.updateExpiredByUser(codeId, isExpiredByUser);
                }
            } catch (Exception e) {
                AppLogger.e("PromocodeRepository", "Error saving expired_by_user status for " + codeId, e);
            }
        });
    }

    public List<PromoCode> getUnnotifiedStreamCodes() {
        String json = JsonDataLoader.loadJsonData(
                context,
                DataSyncConfig.PROMOCODES_REMOTE_URL,
                DataSyncConfig.CACHE_PROMOCODES,
                DataSyncConfig.ASSET_PROMOCODES,
                true
        );

        List<PromoCode> allCodes = parsePromoCodes(json, -1);
        List<PromoCode> streamCodes = new ArrayList<>();

        try {
            List<PromocodeStatusEntity> statuses = promocodeDao.getAllStatusesSync();
            Map<String, PromocodeStatusEntity> statusMap = new HashMap<>();
            for (PromocodeStatusEntity s : statuses) {
                statusMap.put(s.codeId, s);
            }

            for (PromoCode code : allCodes) {
                if (code.isStream() && code.isActive()) {
                    PromocodeStatusEntity st = statusMap.get(code.getId());
                    boolean redeemed = st != null && st.isRedeemed;
                    boolean notified = st != null && st.isNotified;
                    boolean expiredByUser = st != null && st.isExpiredByUser;
                    if (!redeemed && !notified && !expiredByUser) {
                        streamCodes.add(code);
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.e("PromocodeRepository", "Error checking stream codes", e);
        }

        return streamCodes;
    }

    public void markAsNotified(@NonNull String codeId) {
        AppDatabase.getExecutor().execute(() -> {
            try {
                PromocodeStatusEntity entity = promocodeDao.getStatusSync(codeId);
                if (entity == null) {
                    entity = new PromocodeStatusEntity(codeId, false, 0, true);
                    promocodeDao.insertOrUpdate(entity);
                } else {
                    promocodeDao.markAsNotified(codeId);
                }
            } catch (Exception e) {
                AppLogger.e("PromocodeRepository", "Error marking code as notified", e);
            }
        });
    }

    public int getUnviewedPromoCodesCount() {
        Preferences prefs = new Preferences(context);
        Set<String> viewed = prefs.getViewedPromocodes();

        String json = JsonDataLoader.loadJsonData(
                context,
                DataSyncConfig.PROMOCODES_REMOTE_URL,
                DataSyncConfig.CACHE_PROMOCODES,
                DataSyncConfig.ASSET_PROMOCODES,
                false
        );

        List<PromoCode> allCodes = parsePromoCodes(json, -1);
        int unviewedCount = 0;

        try {
            List<PromocodeStatusEntity> statuses = promocodeDao.getAllStatusesSync();
            Map<String, PromocodeStatusEntity> statusMap = new HashMap<>();
            for (PromocodeStatusEntity s : statuses) {
                statusMap.put(s.codeId, s);
            }

            for (PromoCode code : allCodes) {
                if (code.isActive() && !code.isPermanent()) {
                    PromocodeStatusEntity st = statusMap.get(code.getId());
                    boolean isRedeemed = st != null && st.isRedeemed;
                    boolean isExpiredByUser = st != null && st.isExpiredByUser;
                    if (!isRedeemed && !isExpiredByUser && !viewed.contains(code.getId()) && !viewed.contains(code.getCode())) {
                        unviewedCount++;
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.e("PromocodeRepository", "Error calculating unviewed promocodes count", e);
        }

        return unviewedCount;
    }

    public void markAllPromocodesAsViewed() {
        AppDatabase.getExecutor().execute(() -> {
            try {
                String json = JsonDataLoader.loadJsonData(
                        context,
                        DataSyncConfig.PROMOCODES_REMOTE_URL,
                        DataSyncConfig.CACHE_PROMOCODES,
                        DataSyncConfig.ASSET_PROMOCODES,
                        false
                );
                List<PromoCode> allCodes = parsePromoCodes(json, -1);
                Set<String> ids = new HashSet<>();
                for (PromoCode code : allCodes) {
                    ids.add(code.getId());
                    ids.add(code.getCode());
                }
                new Preferences(context).markPromocodesAsViewed(ids);
            } catch (Exception e) {
                AppLogger.e("PromocodeRepository", "Error marking all promocodes as viewed", e);
            }
        });
    }

    @NonNull
    private List<PromoCode> parsePromoCodes(String json, int targetGameType) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<PromoCode> list = new ArrayList<>();
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root != null && root.has("codes")) {
                JsonArray array = root.getAsJsonArray("codes");
                for (JsonElement elem : array) {
                    if (!elem.isJsonObject()) continue;
                    JsonObject obj = elem.getAsJsonObject();

                    int gameType = obj.has("game_type") ? obj.get("game_type").getAsInt() : 0;
                    if (targetGameType != -1 && gameType != targetGameType) {
                        continue;
                    }

                    String id = obj.has("id") ? obj.get("id").getAsString() : "";
                    String rawCode = obj.has("code") ? obj.get("code").getAsString() : "";
                    if (!DataSyncConfig.isValidCode(rawCode)) {
                        continue;
                    }
                    String code = DataSyncConfig.sanitizeCode(rawCode);

                    String rewardsRu = obj.has("rewards_description_ru") ? obj.get("rewards_description_ru").getAsString() : "";
                    String rewardsEn = obj.has("rewards_description_en") ? obj.get("rewards_description_en").getAsString() : "";
                    String rewards = !rewardsRu.isEmpty() ? rewardsRu : rewardsEn;
                    String status = obj.has("status") ? obj.get("status").getAsString() : "NEW";
                    String expiresAt = obj.has("expires_at") && !obj.get("expires_at").isJsonNull()
                            ? obj.get("expires_at").getAsString() : null;
                    String createdAt = obj.has("created_at") && !obj.get("created_at").isJsonNull()
                            ? obj.get("created_at").getAsString() : null;
                    boolean isStream = obj.has("is_stream") && obj.get("is_stream").getAsBoolean();
                    boolean isActive = !obj.has("is_active") || obj.get("is_active").getAsBoolean();

                    List<String> rewardTypes = new ArrayList<>();
                    if (obj.has("reward_types") && obj.get("reward_types").isJsonArray()) {
                        for (JsonElement t : obj.getAsJsonArray("reward_types")) {
                            rewardTypes.add(t.getAsString());
                        }
                    }

                    list.add(new PromoCode(id, gameType, code, rewards, rewardsRu, rewardsEn, rewardTypes, status, expiresAt, createdAt, isStream, isActive, false, false));
                }
            }
        } catch (Exception e) {
            AppLogger.e("PromocodeRepository", "Failed to parse promocodes JSON", e);
        }
        return list;
    }
}
