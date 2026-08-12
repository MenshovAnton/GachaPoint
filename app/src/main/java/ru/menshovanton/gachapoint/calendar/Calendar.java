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
        ensureYearInitialized(LocalDate.now().getYear());
    }

    public void ensureYearInitialized(int year) {
        if (!dbHelper.hasDataForYear(year)) {
            generateAndSaveYear(year);
        }
    }

    private void generateAndSaveYear(int year) {
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

        dbHelper.saveCalendarEntities(entities);
    }

    public List<Date> getMonthDates(int year, int month, GameType gameType) {
        ensureYearInitialized(year);
        return dbHelper.getMonthCalendarData(year, month, gameType);
    }

    public Date getDay(int year, int dayOfYear, GameType gameType) {
        ensureYearInitialized(year);
        return dbHelper.getDayCalendarData(year, dayOfYear, gameType);
    }

    public void updateDay(int year, int dayOfYear, GameType gameType, int status, int subDaysRemaining) {
        dbHelper.updateCalendarDay(year, dayOfYear, gameType, status, subDaysRemaining);
    }

    public List<Date> getDaysRange(int year, int startDay, int endDay, GameType gameType) {
        ensureYearInitialized(year);
        return dbHelper.getDaysRangeData(year, startDay, endDay, gameType);
    }
}