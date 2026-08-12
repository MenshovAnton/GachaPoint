package ru.menshovanton.gachapoint.helpers;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.db.AppDatabase;
import ru.menshovanton.gachapoint.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.db.entities.WishEntity;
import ru.menshovanton.gachapoint.enums.GameType;
import ru.menshovanton.gachapoint.models.Date;
import ru.menshovanton.gachapoint.models.Wish;

public class DatabaseHelper {

    public static final String DATABASE_NAME = AppDatabase.DATABASE_NAME;
    private final AppDatabase db;

    public DatabaseHelper(Context context) {
        this.db = AppDatabase.getInstance(context);
    }

    public List<Wish> getWishesByBanner(GameType gameType, String bannerType) {
        List<WishEntity> entities = db.wishDao().getWishes(gameType.getCode(), bannerType);
        List<Wish> wishes = new ArrayList<>(entities.size());
        for (WishEntity entity : entities) {
            wishes.add(entity.toWishModel());
        }
        return wishes;
    }

    public void addWishes(String dateTime, String dropType, String dropRare, int count, GameType gameType, String bannerType, boolean isResetPity) {
        db.wishDao().addWishesBatch(dateTime, dropType, dropRare, count, gameType.getCode(), bannerType, isResetPity);
    }

    public void updateWish(int id, String dateTime, String dropType, String dropRare, GameType gameType, String bannerType, boolean isResetPity) {
        db.wishDao().updateWishFields(id, dateTime, dropType, dropRare, bannerType, isResetPity);
    }

    public boolean hasDataForYear(int year) {
        return db.calendarDao().getYearEntriesCount(year) > 0;
    }

    public void saveCalendarEntities(List<CalendarEntity> entities) {
        db.calendarDao().insertOrUpdateBatch(entities);
    }

    public List<Date> getMonthCalendarData(int year, int month, GameType gameType) {
        List<CalendarEntity> entities = db.calendarDao().getCalendarForMonth(year, month);
        List<Date> dates = new ArrayList<>(entities.size());
        for (CalendarEntity entity : entities) {
            dates.add(entity.toDateModel(gameType));
        }
        return dates;
    }

    public Date getDayCalendarData(int year, int dayOfYear, GameType gameType) {
        CalendarEntity entity = db.calendarDao().getDay(year, dayOfYear);
        return entity != null ? entity.toDateModel(gameType) : null;
    }

    public List<Date> getDaysRangeData(int year, int startDay, int endDay, GameType gameType) {
        List<CalendarEntity> entities = db.calendarDao().getDaysRange(year, startDay, endDay);
        List<Date> dates = new ArrayList<>(entities.size());
        for (CalendarEntity entity : entities) {
            dates.add(entity.toDateModel(gameType));
        }
        return dates;
    }

    public void updateCalendarDay(int year, int dayOfYear, GameType gameType, int status, int subDaysRemaining) {
        CalendarEntity entity = db.calendarDao().getDay(year, dayOfYear);
        if (entity != null) {
            entity.updateForGame(gameType, status, subDaysRemaining);
            db.calendarDao().insertOrUpdate(entity);
        }
    }

    public AppDatabase getDb() {
        return db;
    }
}