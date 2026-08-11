package ru.menshovanton.gachapoint.helpers;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.menshovanton.gachapoint.db.AppDatabase;
import ru.menshovanton.gachapoint.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.db.entities.WishEntity;
import ru.menshovanton.gachapoint.models.Date;
import ru.menshovanton.gachapoint.models.Wish;

public class DatabaseHelper {

    public static final String DATABASE_NAME = AppDatabase.DATABASE_NAME;
    private final AppDatabase db;

    public DatabaseHelper(Context context) {
        this.db = AppDatabase.getInstance(context);
    }


    public List<Wish> getWishesByBanner(int subType, String bannerType) {
        List<WishEntity> entities = db.wishDao().getWishes(subType, bannerType);
        List<Wish> wishes = new ArrayList<>(entities.size());
        for (WishEntity entity : entities) {
            wishes.add(entity.toWishModel());
        }
        return wishes;
    }

    public void addWishes(String dateTime, String dropType, String dropRare, int count, int subType, String bannerType, boolean isResetPity) {
        db.wishDao().addWishesBatch(dateTime, dropType, dropRare, count, subType, bannerType, isResetPity);
    }

    public void updateWish(int id, String dateTime, String dropType, String dropRare, int subType, String bannerType, boolean isResetPity) {
        db.wishDao().updateWishFields(id, dateTime, dropType, dropRare, bannerType, isResetPity);
    }

    public Date[] getAllCalendarData(int subType) {
        List<CalendarEntity> entities = db.calendarDao().getAllCalendarEntries();
        if (entities == null || entities.isEmpty()) {
            return null;
        }

        int size = entities.size();
        Date[] result = new Date[size];
        for (int i = 0; i < size; i++) {
            result[i] = entities.get(i).toDateModel(subType);
        }
        return result;
    }

    public void saveCalendarBatch(Date[] dateArray, int start, int count, int subType) {
        if (dateArray == null || count <= 0) return;

        int end = Math.min(start + count, dateArray.length);
        List<CalendarEntity> existingEntities = db.calendarDao().getCalendarRange(start, end - 1);

        Map<Integer, CalendarEntity> entityMap = new HashMap<>();
        for (CalendarEntity e : existingEntities) {
            entityMap.put(e.id, e);
        }

        List<CalendarEntity> entitiesToSave = new ArrayList<>(end - start);

        for (int i = start; i < end; i++) {
            Date date = dateArray[i];
            CalendarEntity entity = entityMap.get(i);

            if (entity == null) {
                entity = new CalendarEntity();
                entity.id = i;
            }

            entity.updateForGame(subType, date);
            entitiesToSave.add(entity);
        }

        db.calendarDao().insertOrUpdateBatch(entitiesToSave);
    }

    public AppDatabase getDb() {
        return db;
    }
}