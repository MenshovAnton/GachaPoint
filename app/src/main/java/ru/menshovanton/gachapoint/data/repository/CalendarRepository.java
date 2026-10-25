package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.calendar.Calendar;
import ru.menshovanton.gachapoint.data.db.entities.CalendarGameStatusEntity;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.DayState;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Statistic;

public class CalendarRepository {
    private final Calendar calendar;
    private final PiggyBankRepository piggyBankRepository;
    private final DatabaseRepository databaseRepository;
    private final Preferences preferences;

    private int subsCount = 0;

    public CalendarRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.databaseRepository = new DatabaseRepository(appContext);
        this.calendar = new Calendar(this.databaseRepository);
        this.piggyBankRepository = new PiggyBankRepository(appContext);
        this.preferences = new Preferences(appContext);
    }

    public CalendarRepository(
            Calendar calendar,
            PiggyBankRepository piggyBankRepository,
            DatabaseRepository databaseRepository,
            Preferences preferences
    ) {
        this.calendar = calendar;
        this.piggyBankRepository = piggyBankRepository;
        this.databaseRepository = databaseRepository;
        this.preferences = preferences;
    }

    public void init(GameType gameType, int year, Runnable onComplete) {
        LocalDate now = LocalDate.now();
        long todayEpoch = now.toEpochDay();

        databaseRepository.getActiveSubscription(gameType, todayEpoch, activeSub -> {
            subsCount = (activeSub != null) ? activeSub.count : 0;
            syncDailiesProgress(gameType, onComplete);
        });
    }

    public void getMonthDates(int year, int month, GameType gameType, DatabaseRepository.Callback<List<Date>> callback) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        LocalDate endOfMonth = startOfMonth.plusDays(daysInMonth - 1);

        calendar.getMonthDates(year, month, gameType, dates -> {
            if (dates == null) {
                if (callback != null) callback.onResult(null);
                return;
            }

            databaseRepository.getSubscriptionsInRange(gameType, startOfMonth.toEpochDay(), endOfMonth.toEpochDay(), subs -> {
                for (Date date : dates) {
                    LocalDate dateObj = LocalDate.of(date.year, date.month, date.dayOfMonth);
                    long dateEpoch = dateObj.toEpochDay();

                    SubscriptionEntity matchingSub = null;
                    if (subs != null) {
                        for (SubscriptionEntity sub : subs) {
                            if (dateEpoch >= sub.startDate && dateEpoch <= sub.endDate) {
                                matchingSub = sub;
                                break;
                            }
                        }
                    }

                    if (matchingSub != null) {
                        date.subDaysRemaining = (int) (matchingSub.endDate - dateEpoch + 1);
                    } else if (date.subDaysRemaining <= 0) {
                        date.subDaysRemaining = 0;
                    }
                }

                if (callback != null) {
                    callback.onResult(dates);
                }
            });
        });
    }

    public void calculateMissesAndClaims(GameType gameType, int year, Runnable onComplete) {
        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();

        databaseRepository.getActiveSubscription(gameType, todayEpoch, activeSub -> {
            if (activeSub == null) {
                subsCount = 0;
                if (onComplete != null) onComplete.run();
                return;
            }

            subsCount = activeSub.count;
            LocalDate startDate = LocalDate.ofEpochDay(activeSub.startDate);

            if (today.isBefore(startDate)) {
                applySubscriptionProgress(activeSub, gameType, 0, 0);
                databaseRepository.updateSubscription(activeSub, () -> {
                    if (onComplete != null) onComplete.run();
                });
                return;
            }

            fetchDatesInRange(startDate, today, gameType, chainDates -> {
                int[] counts = countClaimsAndMisses(chainDates, todayEpoch);
                int claims = counts[0];
                int misses = counts[1];

                applySubscriptionProgress(activeSub, gameType, claims, misses);

                databaseRepository.updateSubscription(activeSub, () -> {
                    checkAndAddWishFromClaims(gameType, claims);
                    if (onComplete != null) onComplete.run();
                });
            });
        });
    }

    private int[] countClaimsAndMisses(List<Date> dates, long todayEpoch) {
        int claims = 0;
        int misses = 0;
        if (dates != null) {
            for (Date date : dates) {
                long dEpoch = LocalDate.of(date.year, date.month, date.dayOfMonth).toEpochDay();
                boolean isClaimed = (date.status == DayState.STATUS_RECEIVED || date.status == DayState.STATUS_OLD_RECEIVED);
                if (dEpoch < todayEpoch) {
                    if (isClaimed) {
                        claims++;
                    } else {
                        misses++;
                    }
                } else if (dEpoch == todayEpoch && isClaimed) {
                    claims++;
                }
            }
        }
        return new int[]{claims, misses};
    }

    private void applySubscriptionProgress(SubscriptionEntity activeSub, GameType gameType, int claims, int misses) {
        int primogemsPerDay = gameType.getCurrencyPerDay();
        activeSub.claimed = claims * primogemsPerDay;
        activeSub.missed = misses * primogemsPerDay;
        int totalPrimogems = activeSub.count * gameType.getSubscriptionDurationDays() * primogemsPerDay;
        activeSub.wait = Math.max(0, totalPrimogems - activeSub.claimed - activeSub.missed);
    }

    private void fetchDatesInRange(LocalDate start, LocalDate end, GameType gameType, DatabaseRepository.Callback<List<Date>> callback) {
        if (start.getYear() == end.getYear()) {
            calendar.getDaysRange(start.getYear(), start.getDayOfYear(), end.getDayOfYear(), gameType, callback);
        } else {
            fetchYearsRange(start.getYear(), end.getYear(), start, end, gameType, new ArrayList<>(), callback);
        }
    }

    private void fetchYearsRange(int currentYear, int endYear, LocalDate start, LocalDate end, GameType gameType, List<Date> accumulator, DatabaseRepository.Callback<List<Date>> callback) {
        if (currentYear > endYear) {
            if (callback != null) callback.onResult(accumulator);
            return;
        }
        int startDay = (currentYear == start.getYear()) ? start.getDayOfYear() : 1;
        int endDay = (currentYear == endYear) ? end.getDayOfYear() : LocalDate.of(currentYear, 1, 1).lengthOfYear();
        calendar.getDaysRange(currentYear, startDay, endDay, gameType, dates -> {
            if (dates != null) {
                accumulator.addAll(dates);
            }
            fetchYearsRange(currentYear + 1, endYear, start, end, gameType, accumulator, callback);
        });
    }

    public void getStatistic(GameType gameType, int year, DatabaseRepository.Callback<Statistic> callback) {
        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();

        databaseRepository.getActiveSubscription(gameType, todayEpoch, activeSub -> {
            if (activeSub == null || todayEpoch > activeSub.endDate) {
                subsCount = 0;
                if (callback != null) {
                    callback.onResult(new Statistic(0, 0, 0, 0, 0, 0));
                }
                return;
            }

            subsCount = activeSub.count;
            LocalDate startDate = LocalDate.ofEpochDay(activeSub.startDate);
            LocalDate endDate = LocalDate.ofEpochDay(activeSub.endDate);

            int wishesCost = gameType.getPullCost();
            int primogemsPerDay = gameType.getCurrencyPerDay();
            int totalPromogemsInActiveSubs = primogemsPerDay * gameType.getSubscriptionDurationDays() * activeSub.count;

            if (today.isBefore(startDate)) {
                Statistic statistic = new Statistic(
                        0,
                        0,
                        totalPromogemsInActiveSubs,
                        0,
                        0,
                        totalPromogemsInActiveSubs / wishesCost
                );
                if (callback != null) callback.onResult(statistic);
                return;
            }

            LocalDate countEnd = today.isAfter(endDate) ? endDate : today;

            fetchDatesInRange(startDate, countEnd, gameType, chainDates -> {
                int[] counts = countClaimsAndMisses(chainDates, todayEpoch);
                int claims = counts[0];
                int misses = counts[1];

                applySubscriptionProgress(activeSub, gameType, claims, misses);

                databaseRepository.updateSubscription(activeSub, () -> {
                    checkAndAddWishFromClaims(gameType, claims);

                    int missedPrimogemsCount = misses * primogemsPerDay;
                    int claimPrimogemsCount = claims * primogemsPerDay;
                    int laterPrimogemsCount = Math.max(0, totalPromogemsInActiveSubs - claimPrimogemsCount - missedPrimogemsCount);

                    Statistic statistic = new Statistic(
                            missedPrimogemsCount,
                            claimPrimogemsCount,
                            laterPrimogemsCount,
                            missedPrimogemsCount / wishesCost,
                            claimPrimogemsCount / wishesCost,
                            laterPrimogemsCount / wishesCost
                    );

                    if (callback != null) {
                        callback.onResult(statistic);
                    }
                });
            });
        });
    }

    public void addSubscription(GameType gameType, DatabaseRepository.Callback<Boolean> callback) {
        addSubscription(gameType, 1, LocalDate.now(), callback);
    }

    public void addSubscriptionFromDate(GameType gameType, LocalDate startDate, DatabaseRepository.Callback<Boolean> callback) {
        addSubscription(gameType, 1, startDate, callback);
    }

    public void addSubscription(GameType gameType, int countToAdd, LocalDate startDate, DatabaseRepository.Callback<Boolean> callback) {
        if (countToAdd <= 0 || gameType == null) {
            if (callback != null) callback.onResult(false);
            return;
        }

        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();

        databaseRepository.getActiveSubscription(gameType, todayEpoch, activeSub -> {
            if (activeSub == null || todayEpoch > activeSub.endDate) {
                int totalCount = Math.min(countToAdd, gameType.getMaxSubs());
                LocalDate actualStart = (startDate != null) ? startDate : today;
                LocalDate endDate = actualStart.plusDays((long) totalCount * gameType.getSubscriptionDurationDays() - 1);
                int currencyPerDay = gameType.getCurrencyPerDay();

                SubscriptionEntity newSub = new SubscriptionEntity(
                        gameType.getCode(),
                        totalCount,
                        actualStart.toEpochDay(),
                        endDate.toEpochDay(),
                        0,
                        0,
                        totalCount * gameType.getSubscriptionDurationDays() * currencyPerDay
                );

                calendar.ensureYearInitialized(actualStart.getYear(), () ->
                        calendar.ensureYearInitialized(endDate.getYear(), () ->
                                databaseRepository.insertSubscription(newSub, id -> {
                                    subsCount = totalCount;
                                    calculateMissesAndClaims(gameType, actualStart.getYear(), () -> {
                                        if (callback != null) callback.onResult(true);
                                    });
                                })
                        )
                );
            } else {
                int newCount = Math.min(activeSub.count + countToAdd, gameType.getMaxSubs());
                int added = newCount - activeSub.count;
                if (added <= 0) {
                    if (callback != null) callback.onResult(false);
                    return;
                }

                activeSub.count = newCount;
                LocalDate curEnd = LocalDate.ofEpochDay(activeSub.endDate);
                LocalDate newEnd = curEnd.plusDays((long) added * gameType.getSubscriptionDurationDays());
                activeSub.endDate = newEnd.toEpochDay();
                activeSub.wait = (int) (activeSub.wait + (long) added * gameType.getSubscriptionDurationDays() * gameType.getCurrencyPerDay());

                calendar.ensureYearInitialized(newEnd.getYear(), () ->
                        databaseRepository.updateSubscription(activeSub, () -> {
                            subsCount = activeSub.count;
                            if (callback != null) callback.onResult(true);
                        })
                );
            }
        });
    }

    public void deleteSubscription(GameType gameType, DatabaseRepository.Callback<Boolean> callback) {
        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();

        databaseRepository.getActiveSubscription(gameType, todayEpoch, activeSub -> {
            if (activeSub == null) {
                if (callback != null) callback.onResult(false);
                return;
            }

            if (activeSub.count > 1) {
                activeSub.count--;
                LocalDate curEnd = LocalDate.ofEpochDay(activeSub.endDate);
                LocalDate newEnd = curEnd.minusDays(gameType.getSubscriptionDurationDays());
                activeSub.endDate = newEnd.toEpochDay();
                activeSub.wait = Math.max(0, activeSub.wait - gameType.getCurrencyPerDay() * gameType.getSubscriptionDurationDays());

                databaseRepository.updateSubscription(activeSub, () -> {
                    subsCount = activeSub.count;
                    if (callback != null) callback.onResult(true);
                });
            } else {
                LocalDate subStartDate = LocalDate.ofEpochDay(activeSub.startDate);
                LocalDate subEndDate = LocalDate.ofEpochDay(activeSub.endDate);
                databaseRepository.clearCalendarStatusesInRange(gameType, subStartDate, subEndDate, () ->
                        databaseRepository.deleteSubscription(activeSub, () -> {
                    subsCount = 0;
                    if (callback != null) callback.onResult(true);
                }));
            }
        });
    }

    public void getDayStatus(int year, int dayOfYear, GameType gameType, DatabaseRepository.Callback<Integer> callback) {
        calendar.getDay(year, dayOfYear, gameType, date -> {
            if (callback != null) callback.onResult(date != null ? date.status : DayState.STATUS_MISSED);
        });
    }

    public void getDayDailyStatus(int year, int dayOfYear, GameType gameType, DatabaseRepository.Callback<Integer> callback) {
        calendar.getDay(year, dayOfYear, gameType, date -> {
            if (callback != null) callback.onResult(date != null ? date.dailyStatus : 0);
        });
    }

    public void setDayStatus(int year, int dayOfYear, GameType gameType, int status, Runnable onComplete) {
        LocalDate dateObj = LocalDate.ofYearDay(year, dayOfYear);
        long epoch = dateObj.toEpochDay();

        databaseRepository.getSubscriptionsInRange(gameType, epoch, epoch, subs -> {
            int subDaysRemaining = 0;
            if (subs != null && !subs.isEmpty()) {
                SubscriptionEntity sub = subs.get(0);
                subDaysRemaining = (int) (sub.endDate - epoch + 1);
            }

            final int finalRem = subDaysRemaining;
            calendar.getDay(year, dayOfYear, gameType, date -> {
                int remToUse = finalRem > 0 ? finalRem : (date != null ? date.subDaysRemaining : 0);

                calendar.updateDay(year, dayOfYear, gameType, status, remToUse, () ->
                        calculateMissesAndClaims(gameType, year, () -> {
                    if (onComplete != null) onComplete.run();
                }));
            });
        });
    }

    public void setDayDailyStatus(int year, int dayOfYear, GameType gameType, int dailyStatus, Runnable onComplete) {
        calendar.ensureYearInitialized(year, () ->
                databaseRepository.setCalendarDayDailyStatus(year, dayOfYear, gameType, dailyStatus, () ->
                        syncDailiesProgress(gameType, onComplete)
                )
        );
    }

    public void setDaysDailyStatusBatch(List<LocalDate> dates, GameType gameType, int dailyStatus, Runnable onComplete) {
        if (dates == null || dates.isEmpty() || gameType == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        List<Integer> years = new ArrayList<>(dates.size());
        List<Integer> daysOfYear = new ArrayList<>(dates.size());
        for (LocalDate d : dates) {
            years.add(d.getYear());
            daysOfYear.add(d.getDayOfYear());
        }
        databaseRepository.setCalendarDaysDailyStatusBatch(years, daysOfYear, gameType, dailyStatus, () ->
                syncDailiesProgress(gameType, onComplete)
        );
    }

    public void setDaysSubscriptionStatusBatch(List<LocalDate> dates, GameType gameType, int status, Runnable onComplete) {
        if (dates == null || dates.isEmpty() || gameType == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        List<Integer> years = new ArrayList<>(dates.size());
        List<Integer> daysOfYear = new ArrayList<>(dates.size());
        for (LocalDate d : dates) {
            years.add(d.getYear());
            daysOfYear.add(d.getDayOfYear());
        }
        databaseRepository.setCalendarDaysSubscriptionStatusBatch(years, daysOfYear, gameType, status, () ->
                calculateMissesAndClaims(gameType, dates.get(0).getYear(), onComplete));
    }

    public void getDailiesStatistic(GameType gameType, int year, int month, DatabaseRepository.Callback<Statistic> callback) {
        getMonthDates(year, month, gameType, dates -> {
            if (dates == null) {
                if (callback != null) callback.onResult(new Statistic(0, 0, 0, 0, 0, 0));
                return;
            }

            LocalDate today = LocalDate.now();
            long todayEpoch = today.toEpochDay();
            int completedDays = 0;
            int missedDays = 0;
            int laterDays = 0;

            for (Date d : dates) {
                LocalDate dDate = LocalDate.of(d.year, d.month, d.dayOfMonth);
                long dEpoch = dDate.toEpochDay();
                if (d.dailyStatus == 1) {
                    completedDays++;
                } else if (dEpoch < todayEpoch) {
                    missedDays++;
                } else {
                    laterDays++;
                }
            }

            int dailyReward = gameType != null ? gameType.getDailyReward() : GameType.GENSHIN.getDailyReward();
            int pullCost = gameType != null ? gameType.getPullCost() : GameType.GENSHIN.getPullCost();
            int claimGems = completedDays * dailyReward;
            int missedGems = missedDays * dailyReward;
            int laterGems = laterDays * dailyReward;

            Statistic stat = new Statistic(
                    missedGems,
                    claimGems,
                    laterGems,
                    missedGems / pullCost,
                    claimGems / pullCost,
                    laterGems / pullCost
            );

            if (callback != null) {
                callback.onResult(stat);
            }
        });
    }

    private void checkAndAddWishFromClaims(GameType gameType, int claims) {
        int primogemsPerDay = gameType.getCurrencyPerDay();
        int wishesCost = gameType.getPullCost();

        int totalGems = claims * primogemsPerDay;
        int totalWishes = totalGems / wishesCost;

        int currentProgress = piggyBankRepository.getSubsProgress(gameType);
        if (totalWishes > currentProgress) {
            piggyBankRepository.saveSubsProgress(gameType, totalWishes);
        }
    }

    public void syncDailiesProgress(GameType gameType, Runnable onComplete) {
        if (gameType == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        databaseRepository.getCompletedDailiesCount(gameType, count -> {
            int dailyReward = gameType.getDailyReward();
            int pullCost = gameType.getPullCost();
            int totalGems = (count != null ? count : 0) * dailyReward;
            int totalWishes = totalGems / pullCost;
            piggyBankRepository.saveDailiesProgress(gameType, totalWishes);
            if (onComplete != null) onComplete.run();
        });
    }

    public void getDaySubDaysRemaining(int year, int dayOfYear, GameType gameType, DatabaseRepository.Callback<Integer> callback) {
        LocalDate date = LocalDate.ofYearDay(year, dayOfYear);
        long dateEpoch = date.toEpochDay();

        databaseRepository.getSubscriptionsInRange(gameType, dateEpoch, dateEpoch, subs -> {
            if (subs != null && !subs.isEmpty()) {
                SubscriptionEntity sub = subs.get(0);
                int rem = (int) (sub.endDate - dateEpoch + 1);
                if (callback != null) callback.onResult(Math.max(0, rem));
            } else {
                calendar.getDay(year, dayOfYear, gameType, day -> {
                    int rem = day != null ? day.subDaysRemaining : 0;
                    if (callback != null) callback.onResult(Math.max(0, rem));
                });
            }
        });
    }

    public void setDaySubDaysRemaining(int year, int dayOfYear, GameType gameType, int value, Runnable onComplete) {
        calendar.getDay(year, dayOfYear, gameType, date -> {
            int status = date != null ? date.status : DayState.STATUS_MISSED;
            calendar.updateDay(year, dayOfYear, gameType, status, value, onComplete);
        });
    }

    public void updateSubscribeDays(int startYear, int startDayOfYear, GameType gameType, int todayStatus, int totalDays) {
        updateSubscribeDays(startYear, startDayOfYear, gameType, todayStatus, totalDays, null);
    }

    public void updateSubscribeDays(int startYear, int startDayOfYear, GameType gameType, int todayStatus, int totalDays, Runnable onComplete) {
        if (gameType == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        int span = (totalDays > 0) ? totalDays : (gameType.getMaxSubs() * gameType.getSubscriptionDurationDays());
        LocalDate startDate = LocalDate.ofYearDay(startYear, startDayOfYear);
        LocalDate endDate = startDate.plusDays(span - 1);

        fetchDatesInRange(startDate, endDate, gameType, all -> {
            if (all == null || all.isEmpty()) {
                if (onComplete != null) onComplete.run();
                return;
            }

            int gameTypeCode = gameType.getCode();
            List<CalendarGameStatusEntity> batchList = new ArrayList<>(all.size());
            int i = 0;
            for (Date date : all) {
                int targetSubDays = Math.max(0, totalDays - i);
                int statusToSet;
                if (totalDays == 0) {
                    statusToSet = 0;
                } else {
                    statusToSet = (i == 0)
                            ? todayStatus
                            : (targetSubDays > 0 ? date.status : 0);
                }
                batchList.add(new CalendarGameStatusEntity(date.year, date.dayOfYear, gameTypeCode, statusToSet, targetSubDays));
                i++;
            }

            if (!batchList.isEmpty()) {
                databaseRepository.updateSubscribeDaysBatch(batchList, onComplete);
            } else {
                if (onComplete != null) onComplete.run();
            }
        });
    }


    public int getSubsCount() { return subsCount; }
}