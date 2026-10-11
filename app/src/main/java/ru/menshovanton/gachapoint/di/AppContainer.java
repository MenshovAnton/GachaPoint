package ru.menshovanton.gachapoint.di;

import android.content.Context;

import androidx.annotation.NonNull;

import ru.menshovanton.gachapoint.calendar.Calendar;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.data.repository.CalendarRepository;
import ru.menshovanton.gachapoint.data.repository.DatabaseRepository;
import ru.menshovanton.gachapoint.data.repository.PiggyBankRepository;

public class AppContainer {

    private final Preferences preferences;
    private final DatabaseRepository databaseRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final Calendar calendar;
    private final CalendarRepository calendarRepository;

    public AppContainer(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.preferences = new Preferences(appContext);
        this.databaseRepository = new DatabaseRepository(appContext);
        this.piggyBankRepository = new PiggyBankRepository(appContext);
        this.calendar = new Calendar(this.databaseRepository);
        this.calendarRepository = new CalendarRepository(
                this.calendar,
                this.piggyBankRepository,
                this.databaseRepository,
                this.preferences
        );
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public DatabaseRepository getDatabaseRepository() {
        return databaseRepository;
    }

    public PiggyBankRepository getPiggyBankRepository() {
        return piggyBankRepository;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public CalendarRepository getCalendarRepository() {
        return calendarRepository;
    }
}
