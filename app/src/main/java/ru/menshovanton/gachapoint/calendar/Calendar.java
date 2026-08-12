package ru.menshovanton.gachapoint.calendar;

import android.content.Context;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.enums.GameType;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.models.Date;

public class Calendar {

    private final DatabaseHelper dbHelper;

    public Calendar(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public void ensureYearInitialized(int year, Runnable onComplete) {
        dbHelper.hasDataForYear(year, hasData -> {
            if (!hasData) {
                generateAndSaveYear(year, onComplete);
            } else {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private void generateAndSaveYear(int year, Runnable onComplete) {
        boolean isLeap = LocalDate.of(year, 1, 1).isLeapYear();
        int daysInYear = isLeap ? 366 : 365;

        List<CalendarEntity> entities = new ArrayList<>(daysInYear);

        for (int dayOfYear = 1; dayOfYear <= daysInYear; dayOfYear++) {
            LocalDate currentDate = LocalDate.ofYearDay(year, dayOfYear);

            CalendarEntity entity = new CalendarEntity();
            entity.day = currentDate.getDayOfMonth();
            entity.dayOfYear = dayOfYear;
            entity.dayOfWeek = currentDate.getDayOfWeek().getValue();
            entity.month = currentDate.getMonthValue();
            entity.year = year;

            entities.add(entity);
        }

        dbHelper.saveCalendarEntities(entities, onComplete);
    }

    public void getMonthDates(int year, int month, GameType gameType, DatabaseHelper.Callback<List<Date>> callback) {
        ensureYearInitialized(year, () -> dbHelper.getMonthCalendarData(year, month, gameType, callback));
    }

    public void getDay(int year, int dayOfYear, GameType gameType, DatabaseHelper.Callback<Date> callback) {
        ensureYearInitialized(year, () -> dbHelper.getDayCalendarData(year, dayOfYear, gameType, callback));
    }

    public void updateDay(int year, int dayOfYear, GameType gameType, int status, int subDaysRemaining, Runnable onComplete) {
        dbHelper.updateCalendarDay(year, dayOfYear, gameType, status, subDaysRemaining, onComplete);
    }

    public void getDaysRange(int year, int startDay, int endDay, GameType gameType, DatabaseHelper.Callback<List<Date>> callback) {
        ensureYearInitialized(year, () -> dbHelper.getDaysRangeData(year, startDay, endDay, gameType, callback));
    }
}