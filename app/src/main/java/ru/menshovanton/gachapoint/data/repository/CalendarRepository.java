package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.List;

import ru.menshovanton.gachapoint.calendar.Calendar;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Statistic;

public class CalendarRepository {
    private final Calendar calendar;
    private final PiggyBankRepository piggyBankRepository;

    private int missesDays = 0;
    private int claimsDays = 0;
    private int subsCount = 0;

    public CalendarRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.calendar = new Calendar(appContext);
        this.piggyBankRepository = new PiggyBankRepository(appContext);
    }

    public void init(GameType gameType, int year, Runnable onComplete) {
        LocalDate now = LocalDate.now();
        calendar.getDay(now.getYear(), now.getDayOfYear(), gameType, todayDate -> {
            int daysRemaining = todayDate != null ? todayDate.subDaysRemaining : 0;
            subsCount = (daysRemaining <= 0 || daysRemaining > 180) ? 0 : (daysRemaining + 29) / 30;
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void getMonthDates(int year, int month, GameType gameType, DatabaseRepository.Callback<List<Date>> callback) {
        calendar.getMonthDates(year, month, gameType, callback);
    }

    public void calculateMissesAndClaims(GameType gameType, int year, Runnable onComplete) {
        missesDays = 0;
        claimsDays = 0;
        LocalDate now = LocalDate.now();
        int todayOfYear = now.getDayOfYear();

        calendar.getDay(year, todayOfYear, gameType, today -> {
            int subDaysRemaining = today != null ? today.subDaysRemaining : 0;

            if (subDaysRemaining <= 0 && (today == null || today.status == 0)) {
                if (onComplete != null) onComplete.run();
                return;
            }

            int startDayOfYear = Math.max(1, todayOfYear - (180 - subDaysRemaining));

            calendar.getDaysRange(year, startDayOfYear, todayOfYear, gameType, currentPeriodDays -> {
                for (Date date : currentPeriodDays) {
                    if (date.subDaysRemaining > 0 || (date.dayOfYear == todayOfYear && subDaysRemaining > 0)) {
                        if (date.status == 0 && date.dayOfYear < todayOfYear) {
                            missesDays++;
                        } else if (date.status == 1) {
                            claimsDays++;
                        }
                    }
                }
                if (onComplete != null) onComplete.run();
            });
        });
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

    public enum UpdateSubscribeDaysActions { Add, Delete }

    public void updateSubscribeDays(int year, int dayOfYear, GameType gameType, UpdateSubscribeDaysActions action, int totalDays, Runnable onComplete) {
        AppDatabase.getExecutor().execute(() -> {
            LocalDate currentDate = LocalDate.ofYearDay(year, dayOfYear);

            calendar.getDay(year, dayOfYear, gameType, todayDate -> {
                int newStatus;
                if (action == UpdateSubscribeDaysActions.Add) {
                    newStatus = 1;
                } else {
                    newStatus = (totalDays > 0 && todayDate != null) ? todayDate.status : 0;
                }

                calendar.updateDay(year, dayOfYear, gameType, newStatus, totalDays, null);

                for (int i = 1; i <= 180; i++) {
                    LocalDate targetDate = currentDate.plusDays(i);
                    int targetSubDays = Math.max(0, totalDays - i);

                    calendar.updateDay(targetDate.getYear(), targetDate.getDayOfYear(), gameType, 0, targetSubDays, null);
                }

                if (onComplete != null) {
                    AppDatabase.postToMain(onComplete);
                }
            });
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
                    checkAndAddWishFromClaim(gameType);
                }
                if (onComplete != null) onComplete.run();
            });
        });
    }

    private void checkAndAddWishFromClaim(GameType gameType) {
        int primogemsPerDay = 90;
        int wishesCost = 160;

        int currentClaimedGems = claimsDays * primogemsPerDay;
        int previousClaimedGems = (claimsDays - 1) * primogemsPerDay;

        int currentWishes = currentClaimedGems / wishesCost;
        int previousWishes = previousClaimedGems / wishesCost;

        int newWishes = currentWishes - previousWishes;
        if (newWishes > 0) {
            piggyBankRepository.addSubsProgress(gameType, newWishes);
        }
    }

    public void getDaySubDaysRemaining(int year, int dayOfYear, GameType gameType, DatabaseRepository.Callback<Integer> callback) {
        calendar.getDay(year, dayOfYear, gameType, date -> {
            if (callback != null) callback.onResult(date != null ? date.subDaysRemaining : 0);
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