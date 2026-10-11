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
            if (onComplete != null) onComplete.run();
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
                    } else {
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
        int totalPrimogems = activeSub.count * 30 * primogemsPerDay;
        activeSub.wait = Math.max(0, totalPrimogems - activeSub.claimed - activeSub.missed);
    }

    private void fetchDatesInRange(LocalDate start, LocalDate end, GameType gameType, DatabaseRepository.Callback<List<Date>> callback) {
        if (start.getYear() == end.getYear()) {
            calendar.getDaysRange(start.getYear(), start.getDayOfYear(), end.getDayOfYear(), gameType, callback);
        } else {
            calendar.getDaysRange(start.getYear(), start.getDayOfYear(), start.lengthOfYear(), gameType, firstYearDates -> calendar.getDaysRange(end.getYear(), 1, end.getDayOfYear(), gameType, secondYearDates -> {
                List<Date> combined = new ArrayList<>(
                        (firstYearDates != null ? firstYearDates.size() : 0) +
                        (secondYearDates != null ? secondYearDates.size() : 0)
                );
                if (firstYearDates != null) combined.addAll(firstYearDates);
                if (secondYearDates != null) combined.addAll(secondYearDates);
                if (callback != null) callback.onResult(combined);
            }));
        }
    }

    public void getStatistic(GameType gameType, int year, DatabaseRepository.Callback<Statistic> callback) {
        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();

        databaseRepository.getActiveSubscription(gameType, todayEpoch, activeSub -> {
            if (activeSub == null) {
                subsCount = 0;
                if (callback != null) {
                    callback.onResult(new Statistic(0, 0, 0, 0, 0, 0));
                }
                return;
            }

            subsCount = activeSub.count;
            LocalDate startDate = LocalDate.ofEpochDay(activeSub.startDate);

            fetchDatesInRange(startDate, today, gameType, chainDates -> {
                int[] counts = countClaimsAndMisses(chainDates, todayEpoch);
                int claims = counts[0];
                int misses = counts[1];

                applySubscriptionProgress(activeSub, gameType, claims, misses);

                databaseRepository.updateSubscription(activeSub, () -> {
                    checkAndAddWishFromClaims(gameType, claims);

                    int wishesCost = gameType.getPullCost();
                    int primogemsPerDay = gameType.getCurrencyPerDay();
                    int summaryClaim = primogemsPerDay * 30;

                    int missedPrimogemsCount = misses * primogemsPerDay;
                    int claimPrimogemsCount = claims * primogemsPerDay;
                    int totalPromogemsInActiveSubs = summaryClaim * activeSub.count;

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
        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();

        databaseRepository.getActiveSubscription(gameType, todayEpoch, activeSub -> {
            if (activeSub == null) {
                LocalDate endDate = today.plusDays(29);
                SubscriptionEntity newSub = new SubscriptionEntity(
                        gameType.getCode(),
                        1,
                        todayEpoch,
                        endDate.toEpochDay(),
                        gameType.getCurrencyPerDay(),
                        0,
                        gameType.getCurrencyPerDay() * 29
                );

                calendar.ensureYearInitialized(today.getYear(), () ->
                        calendar.ensureYearInitialized(endDate.getYear(), () ->
                                databaseRepository.insertSubscription(newSub, id -> {
                                    subsCount = 1;
                                    setDayStatus(today.getYear(), today.getDayOfYear(), gameType, DayState.STATUS_RECEIVED, () -> {
                                        if (callback != null) callback.onResult(true);
                                    });
                                })
                        )
                );
            } else {
                if (activeSub.count >= gameType.getMaxSubs()) {
                    if (callback != null) callback.onResult(false);
                    return;
                }

                activeSub.count++;
                LocalDate curEnd = LocalDate.ofEpochDay(activeSub.endDate);
                LocalDate newEnd = curEnd.plusDays(30);
                activeSub.endDate = newEnd.toEpochDay();
                activeSub.wait += gameType.getCurrencyPerDay() * 30;

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
                LocalDate newEnd = curEnd.minusDays(30);
                activeSub.endDate = newEnd.toEpochDay();
                activeSub.wait = Math.max(0, activeSub.wait - gameType.getCurrencyPerDay() * 30);

                databaseRepository.updateSubscription(activeSub, () -> {
                    subsCount = activeSub.count;
                    if (callback != null) callback.onResult(true);
                });
            } else {
                LocalDate subStartDate = LocalDate.ofEpochDay(activeSub.startDate);
                databaseRepository.clearCalendarStatusesInRange(gameType, subStartDate, today, () ->
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

    public void setDayStatus(int year, int dayOfYear, GameType gameType, int status, Runnable onComplete) {
        calendar.getDay(year, dayOfYear, gameType, date -> {
            int rem = date != null ? date.subDaysRemaining : 0;

            calendar.updateDay(year, dayOfYear, gameType, status, rem, () ->
                    calculateMissesAndClaims(gameType, year, () -> {
                if (onComplete != null) onComplete.run();
            }));
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

    public void getDaySubDaysRemaining(int year, int dayOfYear, GameType gameType, DatabaseRepository.Callback<Integer> callback) {
        LocalDate date = LocalDate.ofYearDay(year, dayOfYear);
        long dateEpoch = date.toEpochDay();

        databaseRepository.getActiveSubscription(gameType, dateEpoch, activeSub -> {
            if (activeSub != null) {
                int rem = (int) (activeSub.endDate - dateEpoch + 1);
                if (callback != null) callback.onResult(Math.max(0, rem));
            } else {
                if (callback != null) callback.onResult(0);
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

        int span = (totalDays > 0) ? totalDays : 180;
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