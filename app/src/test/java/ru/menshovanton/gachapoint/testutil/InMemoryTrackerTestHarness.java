package ru.menshovanton.gachapoint.testutil;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ru.menshovanton.gachapoint.calendar.Calendar;
import ru.menshovanton.gachapoint.data.db.entities.CalendarEntity;
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.data.db.entities.SavingsEntity;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.data.repository.CalendarRepository;
import ru.menshovanton.gachapoint.data.repository.DatabaseRepository;
import ru.menshovanton.gachapoint.data.repository.PiggyBankRepository;
import ru.menshovanton.gachapoint.domain.enums.DayState;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Date;

public class InMemoryTrackerTestHarness {

    public final FakeContext context;
    public final Preferences preferences;
    public final FakeDatabaseRepository databaseRepository;
    public final PiggyBankRepository piggyBankRepository;
    public final Calendar calendar;
    public final CalendarRepository calendarRepository;

    public InMemoryTrackerTestHarness() {
        this.context = new FakeContext();
        this.preferences = new Preferences(context);
        this.databaseRepository = new FakeDatabaseRepository(context);
        this.piggyBankRepository = new PiggyBankRepository(context, databaseRepository, preferences);
        this.calendar = new Calendar(databaseRepository);
        this.calendarRepository = new CalendarRepository(calendar, piggyBankRepository, databaseRepository, preferences);
    }

    public static class FakeContext extends ContextWrapper {
        private final InMemorySharedPreferences prefs = new InMemorySharedPreferences();

        public FakeContext() {
            super(null);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return prefs;
        }
    }

    public static class InMemorySharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(values);
        }

        @Nullable
        @Override
        public String getString(String key, @Nullable String defValue) {
            Object val = values.get(key);
            return val instanceof String ? (String) val : defValue;
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
            Object val = values.get(key);
            return val instanceof Set ? new HashSet<>((Set<String>) val) : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object val = values.get(key);
            return val instanceof Integer ? (Integer) val : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object val = values.get(key);
            return val instanceof Long ? (Long) val : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object val = values.get(key);
            return val instanceof Float ? (Float) val : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object val = values.get(key);
            return val instanceof Boolean ? (Boolean) val : defValue;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new InMemoryEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        private class InMemoryEditor implements Editor {
            private final Map<String, Object> pending = new HashMap<>();
            private final Set<String> removals = new HashSet<>();
            private boolean clearAll = false;

            @Override
            public Editor putString(String key, @Nullable String value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, @Nullable Set<String> valuesSet) {
                pending.put(key, valuesSet != null ? new HashSet<>(valuesSet) : null);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                removals.add(key);
                return this;
            }

            @Override
            public Editor clear() {
                clearAll = true;
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                if (clearAll) {
                    values.clear();
                }
                for (String key : removals) {
                    values.remove(key);
                }
                for (Map.Entry<String, Object> entry : pending.entrySet()) {
                    if (entry.getValue() == null) {
                        values.remove(entry.getKey());
                    } else {
                        values.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    public static class FakeDatabaseRepository extends DatabaseRepository {
        private final Map<Integer, List<CalendarEntity>> calendarYears = new HashMap<>();
        private final Map<String, CalendarGameStatusEntity> gameStatuses = new HashMap<>();
        private final List<SubscriptionEntity> subscriptions = new ArrayList<>();
        private final Map<Integer, SavingsEntity> savingsMap = new HashMap<>();
        private int nextSubId = 1;

        public FakeDatabaseRepository(Context context) {
            super(context);
            for (GameType g : GameType.values()) {
                savingsMap.put(g.getCode(), new SavingsEntity(g.getCode(), 0, 0, 0, 0, 0));
            }
        }

        private String statusKey(int year, int dayOfYear, int gameCode) {
            return year + "_" + dayOfYear + "_" + gameCode;
        }

        @Override
        public void hasDataForYear(int year, Callback<Boolean> callback) {
            if (callback != null) callback.onResult(calendarYears.containsKey(year));
        }

        @Override
        public void saveCalendarEntities(List<CalendarEntity> entities, Runnable onComplete) {
            if (entities != null && !entities.isEmpty()) {
                int year = entities.get(0).year;
                calendarYears.put(year, new ArrayList<>(entities));
            }
            if (onComplete != null) onComplete.run();
        }

        private Date toDate(CalendarEntity c, int gameCode) {
            CalendarGameStatusEntity s = gameStatuses.get(statusKey(c.year, c.dayOfYear, gameCode));
            int status = s != null ? s.status : 0;
            int subRem = s != null ? s.subDaysRemaining : 0;
            int daily = s != null ? s.dailyStatus : 0;
            return new Date(c.day, c.dayOfYear, c.dayOfWeek, status, subRem, daily, c.month, c.year);
        }

        @Override
        public void getMonthCalendarData(int year, int month, GameType gameType, Callback<List<Date>> callback) {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            List<CalendarEntity> yearList = calendarYears.getOrDefault(year, Collections.emptyList());
            List<Date> result = new ArrayList<>();
            assert yearList != null;
            for (CalendarEntity c : yearList) {
                if (c.month == month) {
                    result.add(toDate(c, code));
                }
            }
            if (callback != null) callback.onResult(result);
        }

        @Override
        public void getDayCalendarData(int year, int dayOfYear, GameType gameType, Callback<Date> callback) {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            List<CalendarEntity> yearList = calendarYears.getOrDefault(year, Collections.emptyList());
            Date found = null;
            assert yearList != null;
            for (CalendarEntity c : yearList) {
                if (c.dayOfYear == dayOfYear) {
                    found = toDate(c, code);
                    break;
                }
            }
            if (callback != null) callback.onResult(found);
        }

        @Override
        public void getDaysRangeData(int year, int startDay, int endDay, GameType gameType, Callback<List<Date>> callback) {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            List<CalendarEntity> yearList = calendarYears.getOrDefault(year, Collections.emptyList());
            List<Date> result = new ArrayList<>();
            assert yearList != null;
            for (CalendarEntity c : yearList) {
                if (c.dayOfYear >= startDay && c.dayOfYear <= endDay) {
                    result.add(toDate(c, code));
                }
            }
            if (callback != null) callback.onResult(result);
        }

        @Override
        public void setCalendarDayDailyStatus(int year, int dayOfYear, GameType gameType, int dailyStatus, Runnable onComplete) {
            if (gameType != null) {
                String key = statusKey(year, dayOfYear, gameType.getCode());
                CalendarGameStatusEntity entity = gameStatuses.get(key);
                if (entity == null) {
                    entity = new CalendarGameStatusEntity(year, dayOfYear, gameType.getCode(), 0, 0, dailyStatus);
                } else {
                    entity.dailyStatus = dailyStatus;
                }
                gameStatuses.put(key, entity);
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void setCalendarDaySubscriptionStatus(int year, int dayOfYear, GameType gameType, int status, int subDaysRemaining, Runnable onComplete) {
            if (gameType != null) {
                String key = statusKey(year, dayOfYear, gameType.getCode());
                CalendarGameStatusEntity entity = gameStatuses.get(key);
                if (entity == null) {
                    entity = new CalendarGameStatusEntity(year, dayOfYear, gameType.getCode(), status, subDaysRemaining, 0);
                } else {
                    entity.status = status;
                    entity.subDaysRemaining = subDaysRemaining;
                }
                gameStatuses.put(key, entity);
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void setCalendarDaysDailyStatusBatch(List<Integer> years, List<Integer> daysOfYear, GameType gameType, int dailyStatus, Runnable onComplete) {
            if (gameType != null && years != null && daysOfYear != null && years.size() == daysOfYear.size()) {
                for (int i = 0; i < years.size(); i++) {
                    setCalendarDayDailyStatus(years.get(i), daysOfYear.get(i), gameType, dailyStatus, null);
                }
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void setCalendarDaysSubscriptionStatusBatch(List<Integer> years, List<Integer> daysOfYear, GameType gameType, int status, Runnable onComplete) {
            if (gameType != null && years != null && daysOfYear != null && years.size() == daysOfYear.size()) {
                for (int i = 0; i < years.size(); i++) {
                    int y = years.get(i);
                    int d = daysOfYear.get(i);
                    long epoch = LocalDate.ofYearDay(y, d).toEpochDay();
                    int rem = 0;
                    for (SubscriptionEntity sub : subscriptions) {
                        if (sub.gameType == gameType.getCode() && !(sub.endDate < epoch || sub.startDate > epoch)) {
                            rem = Math.max(0, (int) (sub.endDate - epoch + 1));
                            break;
                        }
                    }
                    setCalendarDaySubscriptionStatus(y, d, gameType, status, rem, null);
                }
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void clearCalendarStatusesInRange(GameType gameType, LocalDate startDate, LocalDate endDate, Runnable onComplete) {
            if (gameType != null && startDate != null && endDate != null) {
                long startEpoch = startDate.toEpochDay();
                long endEpoch = endDate.toEpochDay();
                for (CalendarGameStatusEntity entity : gameStatuses.values()) {
                    if (entity.gameType == gameType.getCode()) {
                        long e = LocalDate.ofYearDay(entity.year, entity.dayOfYear).toEpochDay();
                        if (e >= startEpoch && e <= endEpoch) {
                            entity.status = 0;
                            entity.subDaysRemaining = 0;
                        }
                    }
                }
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void getActiveSubscription(GameType gameType, long epochDay, Callback<SubscriptionEntity> callback) {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            SubscriptionEntity best = null;
            for (SubscriptionEntity sub : subscriptions) {
                if (sub.gameType == code && sub.endDate >= epochDay) {
                    if (best == null || sub.id > best.id) {
                        best = sub;
                    }
                }
            }
            if (callback != null) callback.onResult(best);
        }

        @Override
        public void getSubscriptionsInRange(GameType gameType, long startEpoch, long endEpoch, Callback<List<SubscriptionEntity>> callback) {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            List<SubscriptionEntity> result = new ArrayList<>();
            for (SubscriptionEntity sub : subscriptions) {
                if (sub.gameType == code && !(sub.endDate < startEpoch || sub.startDate > endEpoch)) {
                    result.add(sub);
                }
            }
            result.sort((a, b) -> Long.compare(a.startDate, b.startDate));
            if (callback != null) callback.onResult(result);
        }

        @Override
        public void getAllSubscriptions(GameType gameType, Callback<List<SubscriptionEntity>> callback) {
            int code = gameType != null ? gameType.getCode() : GameType.GENSHIN.getCode();
            List<SubscriptionEntity> result = new ArrayList<>();
            for (SubscriptionEntity sub : subscriptions) {
                if (sub.gameType == code) {
                    result.add(sub);
                }
            }
            if (callback != null) callback.onResult(result);
        }

        @Override
        public void insertSubscription(SubscriptionEntity entity, Callback<Long> callback) {
            entity.id = nextSubId++;
            subscriptions.add(entity);
            if (callback != null) callback.onResult((long) entity.id);
        }

        @Override
        public void updateSubscription(SubscriptionEntity entity, Runnable onComplete) {
            for (int i = 0; i < subscriptions.size(); i++) {
                if (subscriptions.get(i).id == entity.id) {
                    subscriptions.set(i, entity);
                    break;
                }
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void deleteSubscription(SubscriptionEntity entity, Runnable onComplete) {
            subscriptions.removeIf(s -> s.id == entity.id);
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void getCompletedDailiesCount(GameType gameType, Callback<Integer> callback) {
            int count = 0;
            if (gameType != null) {
                for (CalendarGameStatusEntity e : gameStatuses.values()) {
                    if (e.gameType == gameType.getCode() && e.dailyStatus == 1) {
                        count++;
                    }
                }
            }
            if (callback != null) callback.onResult(count);
        }

        @Override
        public void getClaimedSubscriptionsCount(GameType gameType, Callback<Integer> callback) {
            int count = 0;
            if (gameType != null) {
                for (CalendarGameStatusEntity e : gameStatuses.values()) {
                    if (e.gameType == gameType.getCode()
                            && (e.status == DayState.STATUS_RECEIVED || e.status == DayState.STATUS_OLD_RECEIVED)
                            && e.subDaysRemaining > 0) {
                        count++;
                    }
                }
            }
            if (callback != null) callback.onResult(count);
        }

        @Override
        public void getAllSavings(Callback<List<SavingsEntity>> callback) {
            if (callback != null) callback.onResult(new ArrayList<>(savingsMap.values()));
        }

        @Override
        public void saveSavingsTarget(GameType gameType, int target, Runnable onComplete) {
            if (gameType != null) {
                Objects.requireNonNull(savingsMap.get(gameType.getCode())).target = target;
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void saveSavingsManualProgress(GameType gameType, int progress, Runnable onComplete) {
            if (gameType != null) {
                Objects.requireNonNull(savingsMap.get(gameType.getCode())).manualProgress = progress;
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void saveSavingsManualGems(GameType gameType, int gems, Runnable onComplete) {
            if (gameType != null) {
                Objects.requireNonNull(savingsMap.get(gameType.getCode())).manualGems = gems;
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void saveSavingsManual(GameType gameType, int progress, int gems, Runnable onComplete) {
            if (gameType != null) {
                SavingsEntity s = savingsMap.get(gameType.getCode());
                assert s != null;
                s.manualProgress = progress;
                s.manualGems = gems;
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void saveSavingsSubsProgress(GameType gameType, int progress, Runnable onComplete) {
            if (gameType != null) {
                Objects.requireNonNull(savingsMap.get(gameType.getCode())).subsProgress = progress;
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void saveSavingsDailiesProgress(GameType gameType, int progress, Runnable onComplete) {
            if (gameType != null) {
                Objects.requireNonNull(savingsMap.get(gameType.getCode())).dailiesProgress = progress;
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void saveAllSavingsProgress(GameType gameType, int manual, int subs, int dailies, int manualGems, Runnable onComplete) {
            if (gameType != null) {
                SavingsEntity s = savingsMap.get(gameType.getCode());
                assert s != null;
                s.manualProgress = manual;
                s.subsProgress = subs;
                s.dailiesProgress = dailies;
                s.manualGems = manualGems;
            }
            if (onComplete != null) onComplete.run();
        }

        @Override
        public void resetSavings(GameType gameType, Runnable onComplete) {
            if (gameType != null) {
                SavingsEntity s = savingsMap.get(gameType.getCode());
                assert s != null;
                s.target = 0;
                s.manualProgress = 0;
                s.manualGems = 0;
                s.subsProgress = 0;
                s.dailiesProgress = 0;
            }
            if (onComplete != null) onComplete.run();
        }
    }
}
