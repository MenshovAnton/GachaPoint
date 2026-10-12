package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.data.db.entities.GameEntity;
import ru.menshovanton.gachapoint.data.db.entities.PullEntity;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.enums.PityStatus;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Pull;

public class DatabaseRepository {

    public interface Callback<T> {
        void onResult(T result);
    }

    private final Context context;

    public DatabaseRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private AppDatabase getDb() {
        return AppDatabase.getInstance(context);
    }


    public void getPullsByBanner(GameType gameType, String bannerType, Callback<List<Pull>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<PullEntity> entities = getDb().pullDao().getPulls(gameType.getCode(), bannerType);
            List<Pull> pulls = new ArrayList<>(entities.size());
            for (PullEntity entity : entities) {
                pulls.add(entity.toWishModel());
            }
            AppDatabase.postToMain(() -> callback.onResult(pulls));
        });
    }

    public void addPulls(String dateTime, String dropType, String dropRare, int count,
                         GameType gameType, String bannerType, boolean isResetPity, PityStatus pityStatus, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            String statusStr = pityStatus != null ? pityStatus.name() : "NONE";
            List<PullEntity> entities = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                boolean currentReset = isResetPity && (i == count - 1);
                String currentStatus = (i == count - 1) ? statusStr : "NONE";
                entities.add(new PullEntity(gameType.getCode(), dateTime, dropRare, dropType, bannerType, currentReset, currentStatus));
            }
            getDb().pullDao().insertAll(entities);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void addPulls(String dateTime, String dropType, String dropRare, int count,
                         GameType gameType, String bannerType, boolean isResetPity, Runnable onComplete) {
        addPulls(dateTime, dropType, dropRare, count, gameType, bannerType, isResetPity, PityStatus.NONE, onComplete);
    }

    public void updatePull(int id, String dateTime, String dropType, String dropRare,
                           GameType gameType, String bannerType, boolean isResetPity, PityStatus pityStatus, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            String statusStr = pityStatus != null ? pityStatus.name() : "NONE";
            getDb().pullDao().updatePullFields(id, dateTime, dropType, dropRare, bannerType, isResetPity, statusStr);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void updatePull(int id, String dateTime, String dropType, String dropRare,
                           GameType gameType, String bannerType, boolean isResetPity, Runnable onComplete) {
        updatePull(id, dateTime, dropType, dropRare, gameType, bannerType, isResetPity, PityStatus.NONE, onComplete);
    }

    public void deletePull(int id, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            getDb().pullDao().deletePull(id);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void deletePullsBatch(List<Integer> ids, Runnable onComplete) {
        if (ids == null || ids.isEmpty()) {
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
            return;
        }
        AppDatabase.getExecutor().execute(() -> {
            getDb().pullDao().deletePullsBatch(ids);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }


    public void hasDataForYear(int year, Callback<Boolean> callback) {
        AppDatabase.getExecutor().execute(() -> {
            boolean hasData = getDb().calendarDao().getYearEntriesCount(year) > 0;
            AppDatabase.postToMain(() -> callback.onResult(hasData));
        });
    }

    public void saveCalendarEntities(List<CalendarEntity> entities, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            getDb().calendarDao().insertCalendarDays(entities);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void getMonthCalendarData(int year, int month, GameType gameType, Callback<List<Date>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            List<Date> dates = getDb().calendarDao().getCalendarForMonth(year, month, code);
            AppDatabase.postToMain(() -> callback.onResult(dates));
        });
    }

    public void getDayCalendarData(int year, int dayOfYear, GameType gameType, Callback<Date> callback) {
        AppDatabase.getExecutor().execute(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            Date date = getDb().calendarDao().getDay(year, dayOfYear, code);
            AppDatabase.postToMain(() -> callback.onResult(date));
        });
    }

    public void getDaysRangeData(int year, int startDay, int endDay, GameType gameType, Callback<List<Date>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            List<Date> dates = getDb().calendarDao().getDaysRange(year, startDay, endDay, code);
            AppDatabase.postToMain(() -> callback.onResult(dates));
        });
    }

    public void updateCalendarDay(int year, int dayOfYear, GameType gameType, int status, int subDaysRemaining, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            if (gameType != null) {
                getDb().calendarDao().insertOrUpdateGameStatus(
                        new CalendarGameStatusEntity(year, dayOfYear, gameType.getCode(), status, subDaysRemaining)
                );
            }
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void updateSubscribeDaysBatch(List<CalendarGameStatusEntity> statusEntities, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            getDb().calendarDao().updateSubscribeDaysTransaction(statusEntities);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void clearCalendarStatusesInRange(GameType gameType, java.time.LocalDate startDate, java.time.LocalDate endDate, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            if (gameType != null && startDate != null && endDate != null) {
                int gameCode = gameType.getCode();
                int startYear = startDate.getYear();
                int endYear = endDate.getYear();

                if (startYear == endYear) {
                    getDb().calendarDao().resetStatusesInRangeSameYear(gameCode, startYear, startDate.getDayOfYear(), endDate.getDayOfYear());
                } else {
                    getDb().calendarDao().resetStatusesInRangeSameYear(gameCode, startYear, startDate.getDayOfYear(), startDate.lengthOfYear());
                    for (int y = startYear + 1; y < endYear; y++) {
                        getDb().calendarDao().resetStatusesInRangeSameYear(gameCode, y, 1, java.time.LocalDate.of(y, 1, 1).lengthOfYear());
                    }
                    getDb().calendarDao().resetStatusesInRangeSameYear(gameCode, endYear, 1, endDate.getDayOfYear());
                }
            }
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void getActiveSubscription(GameType gameType, long epochDay, Callback<SubscriptionEntity> callback) {
        AppDatabase.getExecutor().execute(() -> {
            SubscriptionEntity entity = getDb().subscriptionDao().getActiveSubscription(gameType.getCode(), epochDay);
            AppDatabase.postToMain(() -> callback.onResult(entity));
        });
    }

    public void getSubscriptionsCountForGame(GameType gameType, Callback<Integer> callback) {
        AppDatabase.getExecutor().execute(() -> {
            int count = getDb().subscriptionDao().getSubscriptionsCountForGame(gameType.getCode());
            AppDatabase.postToMain(() -> callback.onResult(count));
        });
    }

    public void getSubscriptionsInRange(GameType gameType, long startEpoch, long endEpoch, Callback<List<SubscriptionEntity>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<SubscriptionEntity> list = getDb().subscriptionDao().getSubscriptionsInRange(gameType.getCode(), startEpoch, endEpoch);
            AppDatabase.postToMain(() -> callback.onResult(list));
        });
    }

    public void getAllSubscriptions(GameType gameType, Callback<List<SubscriptionEntity>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<SubscriptionEntity> list = getDb().subscriptionDao().getAllSubscriptionsForGame(gameType.getCode());
            AppDatabase.postToMain(() -> callback.onResult(list));
        });
    }

    public void insertSubscription(SubscriptionEntity entity, Callback<Long> callback) {
        AppDatabase.getExecutor().execute(() -> {
            long id = getDb().subscriptionDao().insert(entity);
            if (callback != null) {
                AppDatabase.postToMain(() -> callback.onResult(id));
            }
        });
    }

    public void updateSubscription(SubscriptionEntity entity, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            getDb().subscriptionDao().update(entity);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void deleteSubscription(SubscriptionEntity entity, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            getDb().subscriptionDao().delete(entity);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void deleteSubscriptionById(int id, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            getDb().subscriptionDao().deleteById(id);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void syncGames(Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            getDb().syncGames();
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void getAllGames(Callback<List<GameEntity>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<GameEntity> games = getDb().gameDao().getAllGames();
            if (callback != null) {
                AppDatabase.postToMain(() -> callback.onResult(games));
            }
        });
    }
}