package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.data.db.entities.WishEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Wish;

public class DatabaseRepository {

    public interface Callback<T> {
        void onResult(T result);
    }

    public static final String DATABASE_NAME = AppDatabase.DATABASE_NAME;
    private final AppDatabase db;

    public DatabaseRepository(Context context) {
        this.db = AppDatabase.getInstance(context);
    }

    // --- WISHES ---

    public void getWishesByBanner(GameType gameType, String bannerType, Callback<List<Wish>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<WishEntity> entities = db.wishDao().getWishes(gameType.getCode(), bannerType);
            List<Wish> wishes = new ArrayList<>(entities.size());
            for (WishEntity entity : entities) {
                wishes.add(entity.toWishModel());
            }
            AppDatabase.postToMain(() -> callback.onResult(wishes));
        });
    }

    public void addWishes(String dateTime, String dropType, String dropRare, int count,
                          GameType gameType, String bannerType, boolean isResetPity, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            db.wishDao().addWishesBatch(dateTime, dropType, dropRare, count, gameType.getCode(), bannerType, isResetPity);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void updateWish(int id, String dateTime, String dropType, String dropRare,
                           GameType gameType, String bannerType, boolean isResetPity, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            db.wishDao().updateWishFields(id, dateTime, dropType, dropRare, bannerType, isResetPity);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    // --- CALENDAR ---

    public void hasDataForYear(int year, Callback<Boolean> callback) {
        AppDatabase.getExecutor().execute(() -> {
            boolean hasData = db.calendarDao().getYearEntriesCount(year) > 0;
            AppDatabase.postToMain(() -> callback.onResult(hasData));
        });
    }

    public void saveCalendarEntities(List<CalendarEntity> entities, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            db.calendarDao().insertOrUpdateBatch(entities);
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public void getMonthCalendarData(int year, int month, GameType gameType, Callback<List<Date>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<CalendarEntity> entities = db.calendarDao().getCalendarForMonth(year, month);
            List<Date> dates = new ArrayList<>(entities.size());
            for (CalendarEntity entity : entities) {
                dates.add(entity.toDateModel(gameType));
            }
            AppDatabase.postToMain(() -> callback.onResult(dates));
        });
    }

    public void getDayCalendarData(int year, int dayOfYear, GameType gameType, Callback<Date> callback) {
        AppDatabase.getExecutor().execute(() -> {
            CalendarEntity entity = db.calendarDao().getDay(year, dayOfYear);
            Date date = entity != null ? entity.toDateModel(gameType) : null;
            AppDatabase.postToMain(() -> callback.onResult(date));
        });
    }

    public void getDaysRangeData(int year, int startDay, int endDay, GameType gameType, Callback<List<Date>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<CalendarEntity> entities = db.calendarDao().getDaysRange(year, startDay, endDay);
            List<Date> dates = new ArrayList<>(entities.size());
            for (CalendarEntity entity : entities) {
                dates.add(entity.toDateModel(gameType));
            }
            AppDatabase.postToMain(() -> callback.onResult(dates));
        });
    }

    public void updateCalendarDay(int year, int dayOfYear, GameType gameType, int status, int subDaysRemaining, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            CalendarEntity entity = db.calendarDao().getDay(year, dayOfYear);
            if (entity != null) {
                entity.updateForGame(gameType, status, subDaysRemaining);
                db.calendarDao().insertOrUpdate(entity);
            }
            if (onComplete != null) {
                AppDatabase.postToMain(onComplete);
            }
        });
    }

    public AppDatabase getDb() {
        return db;
    }
}