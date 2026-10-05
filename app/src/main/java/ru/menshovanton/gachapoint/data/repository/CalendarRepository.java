package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.calendar.Calendar;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Statistic;

public class CalendarRepository {
    private final Calendar calendar;
    private final PiggyBankRepository piggyBankRepository;
    private final DatabaseRepository databaseRepository;
    private final Preferences preferences;

    private int missesDays = 0;
    private int claimsDays = 0;
    private int subsCount = 0;

    public CalendarRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.calendar = new Calendar(appContext);
        this.piggyBankRepository = new PiggyBankRepository(appContext);
        this.databaseRepository = new DatabaseRepository(appContext);
        this.preferences = new Preferences(appContext);
    }

    public void init(GameType gameType, int year, Runnable onComplete) {
        LocalDate now = LocalDate.now();
        long todayEpoch = now.toEpochDay();

        databaseRepository.getActiveSubscription(gameType, todayEpoch, activeSub -> {
            if (activeSub != null) {
                subsCount = activeSub.count;
                if (onComplete != null) onComplete.run();
            } else {
                boolean needMigration = preferences.getBooleanPreference(gameType.getName() + Preferences.NEED_MIGRATION);

                if (!needMigration) {
                    subsCount = 0;
                    if (onComplete != null) onComplete.run();
                    return;
                }

                databaseRepository.getSubscriptionsCountForGame(gameType, countInDb -> {
                    if (countInDb > 0) {
                        preferences.saveBooleanPreference(gameType.getName() + Preferences.NEED_MIGRATION, false);
                        subsCount = 0;
                        if (onComplete != null) onComplete.run();
                        return;
                    }
                    calendar.getDay(now.getYear(), now.getDayOfYear(), gameType, todayDate -> {
                        if (todayDate != null && todayDate.subDaysRemaining > 0) {
                            int rem = todayDate.subDaysRemaining;
                            int count = (int) Math.ceil(rem / 30.0);
                            int totalDays = count * 30;
                            int daysPassed = Math.max(0, totalDays - rem);
                            LocalDate startDate = now.minusDays(daysPassed);
                            LocalDate endDate = now.plusDays(rem - 1);

                            SubscriptionEntity migrated = new SubscriptionEntity(
                                    gameType.getCode(),
                                    count,
                                    startDate.toEpochDay(),
                                    endDate.toEpochDay(),
                                    0,
                                    0,
                                    rem * 90
                            );
                            databaseRepository.insertSubscription(migrated, id -> {
                                subsCount = count;
                                calculateMissesAndClaims(gameType, year, () -> {
                                    if (onComplete != null) onComplete.run();
                                });
                            });

                            preferences.saveBooleanPreference(gameType.getName() + Preferences.NEED_MIGRATION, false);
                        } else {
                            subsCount = 0;
                            if (onComplete != null) onComplete.run();
                        }
                    });
                });
            }
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
        missesDays = 0;
        claimsDays = 0;
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
                int claims = 0;
                int misses = 0;

                for (Date date : chainDates) {
                    LocalDate d = LocalDate.of(date.year, date.month, date.dayOfMonth);
                    long dEpoch = d.toEpochDay();

                    if (dEpoch < todayEpoch) {
                        if (date.status == 1 || date.status == 3) {
                            claims++;
                        } else {
                            misses++;
                        }
                    } else if (dEpoch == todayEpoch) {
                        if (date.status == 1 || date.status == 3) {
                            claims++;
                        }
                    }
                }

                missesDays = misses;
                claimsDays = claims;

                int primogemsPerDay = 90;
                activeSub.claimed = claimsDays * primogemsPerDay;
                activeSub.missed = missesDays * primogemsPerDay;
                int totalPrimogems = activeSub.count * 30 * primogemsPerDay;
                activeSub.wait = Math.max(0, totalPrimogems - activeSub.claimed - activeSub.missed);

                databaseRepository.updateSubscription(activeSub, () -> {
                    if (onComplete != null) onComplete.run();
                });
            });
        });
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
        calculateMissesAndClaims(gameType, year, () -> {
            int wishesCost = 160;
            int primogemsPerDay = 90;
            int summaryClaim = 2700;

            int missedPrimogemsCount = missesDays * primogemsPerDay;
            int claimPrimogemsCount = claimsDays * primogemsPerDay;
            int totalPromogemsInActiveSubs = summaryClaim * subsCount;

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
                        90,
                        0,
                        29 * 90
                );

                calendar.ensureYearInitialized(today.getYear(), () ->
                        calendar.ensureYearInitialized(endDate.getYear(), () ->
                                databaseRepository.insertSubscription(newSub, id -> {
                                    subsCount = 1;
                                    setDayStatus(today.getYear(), today.getDayOfYear(), gameType, 1, () -> {
                                        if (callback != null) callback.onResult(true);
                                    });
                                })
                        )
                );
            } else {
                if (activeSub.count >= 6) {
                    if (callback != null) callback.onResult(false);
                    return;
                }

                activeSub.count++;
                LocalDate curEnd = LocalDate.ofEpochDay(activeSub.endDate);
                LocalDate newEnd = curEnd.plusDays(30);
                activeSub.endDate = newEnd.toEpochDay();
                activeSub.wait += 30 * 90;

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
                activeSub.wait = Math.max(0, activeSub.wait - 30 * 90);

                databaseRepository.updateSubscription(activeSub, () -> {
                    subsCount = activeSub.count;
                    if (callback != null) callback.onResult(true);
                });
            } else {
                databaseRepository.deleteSubscription(activeSub, () -> {
                    subsCount = 0;
                    if (callback != null) callback.onResult(true);
                });
            }
        });
    }

    public void getDayStatus(int year, int dayOfYear, GameType gameType, DatabaseRepository.Callback<Integer> callback) {
        calendar.getDay(year, dayOfYear, gameType, date -> {
            if (callback != null) callback.onResult(date != null ? date.status : 0);
        });
    }

    public void setDayStatus(int year, int dayOfYear, GameType gameType, int status, Runnable onComplete) {
        calendar.getDay(year, dayOfYear, gameType, date -> {
            int oldStatus = date != null ? date.status : 0;
            int rem = date != null ? date.subDaysRemaining : 0;

            calendar.updateDay(year, dayOfYear, gameType, status, rem, () -> {
                if (oldStatus == 0 && status == 1) {
                    claimsDays++;
                    checkAndAddWishFromClaim(gameType);
                } else if (oldStatus == 1 && status == 0) {
                    claimsDays = Math.max(0, claimsDays - 1);
                    checkAndAddWishFromClaim(gameType);
                }
                if (onComplete != null) onComplete.run();
            });
        });
    }

    private void checkAndAddWishFromClaim(GameType gameType) {
        int primogemsPerDay = 90;
        int wishesCost = 160;

        int totalGems = claimsDays * primogemsPerDay;
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
            int status = date != null ? date.status : 0;
            calendar.updateDay(year, dayOfYear, gameType, status, value, onComplete);
        });
    }

    public int getSubsCount() { return subsCount; }
    public void setSubsCount(int value) { subsCount = value; }
    public void addSub() { subsCount++; }
    public void delSub() { subsCount--; }
    public int getClaimsDays() { return claimsDays; }
    public void setClaimsDays(int value) { claimsDays = value; }
    public void addClaimDay() { claimsDays++; }
    public void subtractClaimDay() { claimsDays--; }
    public void setMissesDays(int value) { missesDays = value; }
}