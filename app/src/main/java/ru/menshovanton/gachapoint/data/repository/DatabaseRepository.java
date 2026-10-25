package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabaseCorruptException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.data.db.entities.GameEntity;
import ru.menshovanton.gachapoint.data.db.entities.PullEntity;
import ru.menshovanton.gachapoint.data.db.entities.SavingsEntity;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.data.local.AppLogger;
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

    private <T> void queryAsyncSafe(Callable<T> query, Callback<T> callback, T fallback) {
        AppDatabase.getExecutor().execute(() -> {
            T result = fallback;
            try {
                result = query.call();
            } catch (SQLiteDatabaseCorruptException e) {
                AppLogger.e("DatabaseRepository", "CRITICAL: Database corrupted during query!", e);
                handleDatabaseCorruption();
            } catch (OutOfMemoryError oom) {
                AppLogger.e("DatabaseRepository", "CRITICAL: OutOfMemoryError in DB query!", oom);
                System.gc();
            } catch (Throwable t) {
                AppLogger.e("DatabaseRepository", "Unexpected DB error in query", t);
            } finally {
                final T finalResult = result;
                if (callback != null) {
                    AppDatabase.postToMain(() -> callback.onResult(finalResult));
                }
            }
        });
    }

    private void executeAsyncSafe(Runnable task, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            try {
                task.run();
            } catch (SQLiteDatabaseCorruptException e) {
                AppLogger.e("DatabaseRepository", "CRITICAL: Database corrupted during write!", e);
                handleDatabaseCorruption();
            } catch (OutOfMemoryError oom) {
                AppLogger.e("DatabaseRepository", "CRITICAL: OutOfMemoryError in DB write!", oom);
                System.gc();
            } catch (Throwable t) {
                AppLogger.e("DatabaseRepository", "Unexpected DB error in write", t);
            } finally {
                if (onComplete != null) {
                    AppDatabase.postToMain(onComplete);
                }
            }
        });
    }

    private void handleDatabaseCorruption() {
        try {
            AppDatabase.destroyInstance();
        } catch (Exception e) {
            AppLogger.e("DatabaseRepository", "Error destroying database instance after corruption", e);
        }
    }

    public void getPullsByBanner(GameType gameType, String bannerType, Callback<List<Pull>> callback) {
        queryAsyncSafe(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            List<PullEntity> entities = getDb().pullDao().getPulls(code, bannerType);
            List<Pull> pulls = new ArrayList<>(entities.size());
            for (PullEntity entity : entities) {
                pulls.add(entity.toWishModel());
            }
            return pulls;
        }, callback, Collections.emptyList());
    }

    public void getAllPullsForGame(GameType gameType, Callback<List<Pull>> callback) {
        queryAsyncSafe(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            List<PullEntity> entities = getDb().pullDao().getAllPullsForGame(code);
            List<Pull> pulls = new ArrayList<>(entities.size());
            for (PullEntity entity : entities) {
                pulls.add(entity.toWishModel());
            }
            return pulls;
        }, callback, Collections.emptyList());
    }

    public void addPulls(String dateTime, String dropType, String dropRare, int count,
                         GameType gameType, String bannerType, boolean isResetPity, PityStatus pityStatus, Runnable onComplete) {
        executeAsyncSafe(() -> {
            int gameCode = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            String statusStr = pityStatus != null ? pityStatus.name() : "NONE";
            List<PullEntity> entities = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                boolean currentReset = isResetPity && (i == count - 1);
                String currentStatus = (i == count - 1) ? statusStr : "NONE";
                entities.add(new PullEntity(gameCode, dateTime, dropRare, dropType, bannerType, currentReset, currentStatus));
            }
            getDb().pullDao().insertAll(entities);
        }, onComplete);
    }

    public void addPulls(String dateTime, String dropType, String dropRare, int count,
                         GameType gameType, String bannerType, boolean isResetPity, Runnable onComplete) {
        addPulls(dateTime, dropType, dropRare, count, gameType, bannerType, isResetPity, PityStatus.NONE, onComplete);
    }

    public void updatePull(int id, String dateTime, String dropType, String dropRare,
                           GameType gameType, String bannerType, boolean isResetPity, PityStatus pityStatus, Runnable onComplete) {
        executeAsyncSafe(() -> {
            String statusStr = pityStatus != null ? pityStatus.name() : "NONE";
            getDb().pullDao().updatePullFields(id, dateTime, dropType, dropRare, bannerType, isResetPity, statusStr);
        }, onComplete);
    }

    public void updatePull(int id, String dateTime, String dropType, String dropRare,
                           GameType gameType, String bannerType, boolean isResetPity, Runnable onComplete) {
        updatePull(id, dateTime, dropType, dropRare, gameType, bannerType, isResetPity, PityStatus.NONE, onComplete);
    }

    public void deletePull(int id, Runnable onComplete) {
        executeAsyncSafe(() -> getDb().pullDao().deletePull(id), onComplete);
    }

    public void deletePullsBatch(List<Integer> ids, Runnable onComplete) {
        if (ids == null || ids.isEmpty()) {
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
            return;
        }
        executeAsyncSafe(() -> getDb().pullDao().deletePullsBatch(ids), onComplete);
    }

    public void hasDataForYear(int year, Callback<Boolean> callback) {
        queryAsyncSafe(() -> getDb().calendarDao().getYearEntriesCount(year) > 0, callback, false);
    }

    public void saveCalendarEntities(List<CalendarEntity> entities, Runnable onComplete) {
        executeAsyncSafe(() -> getDb().calendarDao().insertCalendarDays(entities), onComplete);
    }

    public void getMonthCalendarData(int year, int month, GameType gameType, Callback<List<Date>> callback) {
        queryAsyncSafe(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            return getDb().calendarDao().getCalendarForMonth(year, month, code);
        }, callback, Collections.emptyList());
    }

    public void getDayCalendarData(int year, int dayOfYear, GameType gameType, Callback<Date> callback) {
        queryAsyncSafe(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            return getDb().calendarDao().getDay(year, dayOfYear, code);
        }, callback, null);
    }

    public void getDaysRangeData(int year, int startDay, int endDay, GameType gameType, Callback<List<Date>> callback) {
        queryAsyncSafe(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            return getDb().calendarDao().getDaysRange(year, startDay, endDay, code);
        }, callback, Collections.emptyList());
    }

    public void updateCalendarDay(int year, int dayOfYear, GameType gameType, int status, int subDaysRemaining, Runnable onComplete) {
        setCalendarDaySubscriptionStatus(year, dayOfYear, gameType, status, subDaysRemaining, onComplete);
    }

    public void setCalendarDayDailyStatus(int year, int dayOfYear, GameType gameType, int dailyStatus, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null) {
                CalendarGameStatusEntity entity = getDb().calendarDao().getGameStatus(year, dayOfYear, gameType.getCode());
                if (entity == null) {
                    entity = new CalendarGameStatusEntity(year, dayOfYear, gameType.getCode(), 0, 0, dailyStatus);
                } else {
                    entity.dailyStatus = dailyStatus;
                }
                getDb().calendarDao().insertOrUpdateGameStatus(entity);
            }
        }, onComplete);
    }

    public void setCalendarDaySubscriptionStatus(int year, int dayOfYear, GameType gameType, int status, int subDaysRemaining, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null) {
                CalendarGameStatusEntity entity = getDb().calendarDao().getGameStatus(year, dayOfYear, gameType.getCode());
                if (entity == null) {
                    entity = new CalendarGameStatusEntity(year, dayOfYear, gameType.getCode(), status, subDaysRemaining, 0);
                } else {
                    entity.status = status;
                    entity.subDaysRemaining = subDaysRemaining;
                }
                getDb().calendarDao().insertOrUpdateGameStatus(entity);
            }
        }, onComplete);
    }

    public void updateSubscribeDaysBatch(List<CalendarGameStatusEntity> statusEntities, Runnable onComplete) {
        executeAsyncSafe(() -> getDb().calendarDao().updateSubscribeDaysTransaction(statusEntities), onComplete);
    }

    public void setCalendarDaysDailyStatusBatch(List<Integer> years, List<Integer> daysOfYear, GameType gameType, int dailyStatus, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null && years != null && daysOfYear != null && years.size() == daysOfYear.size()) {
                int gameTypeCode = gameType.getCode();
                List<CalendarGameStatusEntity> entities = new ArrayList<>(years.size());
                for (int i = 0; i < years.size(); i++) {
                    int y = years.get(i);
                    int d = daysOfYear.get(i);
                    CalendarGameStatusEntity entity = getDb().calendarDao().getGameStatus(y, d, gameTypeCode);
                    if (entity == null) {
                        entity = new CalendarGameStatusEntity(y, d, gameTypeCode, 0, 0, dailyStatus);
                    } else {
                        entity.dailyStatus = dailyStatus;
                    }
                    entities.add(entity);
                }
                getDb().calendarDao().insertOrUpdateGameStatusBatch(entities);
            }
        }, onComplete);
    }

    public void setCalendarDaysSubscriptionStatusBatch(List<Integer> years, List<Integer> daysOfYear, GameType gameType, int status, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null && years != null && daysOfYear != null && years.size() == daysOfYear.size()) {
                int gameTypeCode = gameType.getCode();
                List<CalendarGameStatusEntity> entities = new ArrayList<>(years.size());
                for (int i = 0; i < years.size(); i++) {
                    int y = years.get(i);
                    int d = daysOfYear.get(i);
                    CalendarGameStatusEntity entity = getDb().calendarDao().getGameStatus(y, d, gameTypeCode);
                    if (entity == null) {
                        entity = new CalendarGameStatusEntity(y, d, gameTypeCode, status, 0, 0);
                    } else {
                        entity.status = status;
                    }
                    entities.add(entity);
                }
                getDb().calendarDao().insertOrUpdateGameStatusBatch(entities);
            }
        }, onComplete);
    }

    public void clearCalendarStatusesInRange(GameType gameType, LocalDate startDate, LocalDate endDate, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null && startDate != null && endDate != null) {
                int gameCode = gameType.getCode();
                int startYear = startDate.getYear();
                int endYear = endDate.getYear();

                if (startYear == endYear) {
                    getDb().calendarDao().resetStatusesInRangeSameYear(gameCode, startYear, startDate.getDayOfYear(), endDate.getDayOfYear());
                } else {
                    getDb().calendarDao().resetStatusesInRangeSameYear(gameCode, startYear, startDate.getDayOfYear(), startDate.lengthOfYear());
                    for (int y = startYear + 1; y < endYear; y++) {
                        getDb().calendarDao().resetStatusesInRangeSameYear(gameCode, y, 1, LocalDate.of(y, 1, 1).lengthOfYear());
                    }
                    getDb().calendarDao().resetStatusesInRangeSameYear(gameCode, endYear, 1, endDate.getDayOfYear());
                }
            }
        }, onComplete);
    }

    public void getActiveSubscription(GameType gameType, long epochDay, Callback<SubscriptionEntity> callback) {
        queryAsyncSafe(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            return getDb().subscriptionDao().getActiveOrUpcomingSubscription(code, epochDay);
        }, callback, null);
    }

    public void cleanupOldSubscriptions(GameType gameType, int keepId, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null) {
                getDb().subscriptionDao().cleanupOldSubscriptionsForGame(gameType.getCode(), keepId);
            }
        }, onComplete);
    }

    public void getSubscriptionsCountForGame(GameType gameType, Callback<Integer> callback) {
        queryAsyncSafe(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            return getDb().subscriptionDao().getSubscriptionsCountForGame(code);
        }, callback, 0);
    }

    public void getSubscriptionsInRange(GameType gameType, long startEpoch, long endEpoch, Callback<List<SubscriptionEntity>> callback) {
        queryAsyncSafe(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            return getDb().subscriptionDao().getSubscriptionsInRange(code, startEpoch, endEpoch);
        }, callback, Collections.emptyList());
    }

    public void getAllSubscriptions(GameType gameType, Callback<List<SubscriptionEntity>> callback) {
        queryAsyncSafe(() -> {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            return getDb().subscriptionDao().getAllSubscriptionsForGame(code);
        }, callback, Collections.emptyList());
    }

    public void insertSubscription(SubscriptionEntity entity, Callback<Long> callback) {
        queryAsyncSafe(() -> getDb().subscriptionDao().insert(entity), callback, -1L);
    }

    public void updateSubscription(SubscriptionEntity entity, Runnable onComplete) {
        executeAsyncSafe(() -> getDb().subscriptionDao().update(entity), onComplete);
    }

    public void deleteSubscription(SubscriptionEntity entity, Runnable onComplete) {
        executeAsyncSafe(() -> getDb().subscriptionDao().delete(entity), onComplete);
    }

    public void deleteSubscriptionById(int id, Runnable onComplete) {
        executeAsyncSafe(() -> getDb().subscriptionDao().deleteById(id), onComplete);
    }

    public void syncGames(Runnable onComplete) {
        executeAsyncSafe(() -> getDb().syncGames(), onComplete);
    }

    public void getAllGames(Callback<List<GameEntity>> callback) {
        queryAsyncSafe(() -> getDb().gameDao().getAllGames(), callback, Collections.emptyList());
    }

    public void getCompletedDailiesCount(GameType gameType, Callback<Integer> callback) {
        queryAsyncSafe(() -> {
            if (gameType != null) {
                return getDb().calendarDao().getCompletedDailiesCount(gameType.getCode());
            }
            return 0;
        }, callback, 0);
    }

    public void getSavings(GameType gameType, Callback<SavingsEntity> callback) {
        queryAsyncSafe(() -> {
            if (gameType != null) {
                return getDb().savingsDao().getSavings(gameType.getCode());
            }
            return null;
        }, callback, null);
    }

    public void getAllSavings(Callback<List<SavingsEntity>> callback) {
        queryAsyncSafe(() -> getDb().savingsDao().getAllSavings(), callback, Collections.emptyList());
    }

    public void saveSavingsTarget(GameType gameType, int target, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null) {
                getDb().savingsDao().updateTarget(gameType.getCode(), target);
            }
        }, onComplete);
    }

    public void saveSavingsManualProgress(GameType gameType, int progress, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null) {
                getDb().savingsDao().updateManualProgress(gameType.getCode(), progress);
            }
        }, onComplete);
    }

    public void saveSavingsSubsProgress(GameType gameType, int progress, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null) {
                getDb().savingsDao().updateSubsProgress(gameType.getCode(), progress);
            }
        }, onComplete);
    }

    public void saveSavingsDailiesProgress(GameType gameType, int progress, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null) {
                getDb().savingsDao().updateDailiesProgress(gameType.getCode(), progress);
            }
        }, onComplete);
    }

    public void resetSavings(GameType gameType, Runnable onComplete) {
        executeAsyncSafe(() -> {
            if (gameType != null) {
                getDb().savingsDao().reset(gameType.getCode());
            }
        }, onComplete);
    }
}
