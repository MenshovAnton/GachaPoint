package ru.menshovanton.gachapoint.data.repository;

import android.content.Context;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.ui.journal.piggybank.PiggyBankHelper;
import ru.menshovanton.gachapoint.ui.main.MainActivity;
import ru.menshovanton.gachapoint.calendar.Calendar;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.domain.enums.DayState;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.tracker.TrackerFragment;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Statistic;

public class CalendarRepository {
    private final MainActivity mainActivity;
    private final Calendar calendar;
    private final PiggyBankHelper piggyBankHelper;

    private final int toDayOfYear;
    private int year;
    private int missesDays = 0;
    private int claimsDays = 0;
    private int subsCount;

    public CalendarRepository(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.piggyBankHelper = new PiggyBankHelper(mainActivity);

        LocalDate now = LocalDate.now();
        toDayOfYear = now.getDayOfYear();
        year = now.getYear();

        calendar = new Calendar(mainActivity);
    }

    // Первичная асинхронная инициализация количества подписок на сегодня
    public void init(Runnable onComplete) {
        calendar.getDay(year, toDayOfYear, mainActivity.getSubType(), todayDate -> {
            int daysRemaining = todayDate != null ? todayDate.subDaysRemaining : 0;
            subsCount = (daysRemaining <= 0 || daysRemaining > 180) ? 0 : (daysRemaining + 29) / 30;
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void renderCalendar(TrackerFragment fragment) {
        if (fragment == null || !fragment.isAdded() || !fragment.isVisible()) return;

        int selectedMonth = fragment.getSelectedMonth();
        LocalDate today = LocalDate.now();
        Context context = mainActivity.getApplicationContext();

        int firstDayOfWeek = LocalDate.of(year, selectedMonth, 1).getDayOfWeek().getValue();
        int offset = firstDayOfWeek - 1;
        int daysInMonth = YearMonth.of(year, selectedMonth).lengthOfMonth();

        calendar.getMonthDates(year, selectedMonth, mainActivity.getSubType(), monthDates -> {
            if (!fragment.isAdded() || !fragment.isVisible() || fragment.getView() == null) return;

            GridLayout gridLayout = fragment.getCalendarGrid();
            List<TextView> cellsPool = fragment.getCellViewsPool();

            if (gridLayout == null || cellsPool == null) return;

            TransitionSet transitionSet = new TransitionSet();
            transitionSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
            transitionSet.addTransition(new Fade().setDuration(250));
            transitionSet.addTransition(new ChangeBounds().setDuration(250));
            TransitionManager.beginDelayedTransition(gridLayout, transitionSet);

            for (int i = 0; i < 42; i++) {
                TextView cell = cellsPool.get(i);

                if (i < offset || i >= (offset + daysInMonth)) {
                    cell.setVisibility(View.GONE);
                    cell.setText("");
                    cell.setBackground(null);
                } else {
                    int dayIndexInMonth = i - offset;
                    if (dayIndexInMonth < monthDates.size()) {
                        Date dateObj = monthDates.get(dayIndexInMonth);

                        cell.setVisibility(View.VISIBLE);
                        cell.setText(String.valueOf(dateObj.dayOfMonth));

                        if (dateObj.dayOfMonth == today.getDayOfMonth()
                                && dateObj.month == today.getMonthValue()
                                && dateObj.year == today.getYear()) {
                            cell.setBackgroundResource(R.drawable.calendar_cell_today);
                        } else {
                            cell.setBackgroundResource(R.drawable.calendar_cell);
                        }

                        DayState dayState = DayState.from(dateObj, selectedMonth, toDayOfYear);
                        int textColor = ContextCompat.getColor(context, dayState.getColorResId());
                        cell.setTextColor(textColor);
                    }
                }
            }
        });
    }

    public void adjustCellSizes(TrackerFragment fragment) {
        if (fragment == null || !fragment.isAdded() || !fragment.isVisible()) return;

        GridLayout gridLayout = fragment.getCalendarGrid();
        List<TextView> cellsPool = fragment.getCellViewsPool();
        if (gridLayout == null || cellsPool == null) return;

        gridLayout.post(() -> {
            if (!fragment.isAdded() || fragment.getView() == null) return;

            int gridWidth = gridLayout.getWidth();
            if (gridWidth == 0) return;

            Context context = gridLayout.getContext();
            float density = context.getResources().getDisplayMetrics().density;
            int marginPx = (int) (4 * density);

            int cellSidePx = (gridWidth - (marginPx * 2 * 7)) / 7;

            for (int i = 0; i < 42; i++) {
                TextView cell = cellsPool.get(i);
                GridLayout.LayoutParams params = (GridLayout.LayoutParams) cell.getLayoutParams();

                if (params != null) {
                    params.width = cellSidePx;
                    params.height = cellSidePx;
                    params.setMargins(marginPx, marginPx, marginPx, marginPx);
                    cell.setLayoutParams(params);
                }
            }
        });
    }

    public void calculateMissesAndClaims(Runnable onComplete) {
        missesDays = 0;
        claimsDays = 0;

        if (toDayOfYear > 1) {
            calendar.getDaysRange(year, 1, toDayOfYear - 1, mainActivity.getSubType(), pastDays -> {
                for (Date date : pastDays) {
                    if (date.status == 0 && date.subDaysRemaining > 0) {
                        missesDays++;
                    }
                    if (date.status == 1 && date.subDaysRemaining > 0) {
                        claimsDays++;
                    }
                }
                calendar.getDay(year, toDayOfYear, mainActivity.getSubType(), today -> {
                    if (today != null && today.status == 1) {
                        claimsDays++;
                    }
                    if (onComplete != null) onComplete.run();
                });
            });
        } else {
            calendar.getDay(year, toDayOfYear, mainActivity.getSubType(), today -> {
                if (today != null && today.status == 1) {
                    claimsDays++;
                }
                if (onComplete != null) onComplete.run();
            });
        }
    }

    public void getStatistic(DatabaseRepository.Callback<Statistic> callback) {
        calculateMissesAndClaims(() -> {
            int wishesCost = 160;
            int primogemsPerDay = 90;
            int summaryClaim = 2700;

            int missedPrimogemsCount = missesDays * primogemsPerDay;
            int claimPrimogemsCount = claimsDays * primogemsPerDay;
            int laterPrimogemsCount = summaryClaim * subsCount - claimPrimogemsCount - missedPrimogemsCount;
            int claimWishesCount = claimPrimogemsCount / wishesCost;

            piggyBankHelper.updateSubsProgress(claimWishesCount);

            Statistic statistic = new Statistic(
                    missedPrimogemsCount,
                    claimPrimogemsCount,
                    laterPrimogemsCount,
                    missedPrimogemsCount / wishesCost,
                    claimWishesCount,
                    laterPrimogemsCount / wishesCost
            );

            if (callback != null) {
                callback.onResult(statistic);
            }
        });
    }

    public enum UpdateSubscribeDaysActions {
        Add,
        Delete
    }

    public void updateSubscribeDays(UpdateSubscribeDaysActions action, Runnable onComplete) {
        GameType gameType = mainActivity.getSubType();

        getDaySubDaysRemaining(toDayOfYear, remaining -> {
            LocalDate currentDate = LocalDate.ofYearDay(year, toDayOfYear);

            AppDatabase.getExecutor().execute(() -> {
                if (action == UpdateSubscribeDaysActions.Add) {
                    for (int i = 1; i < remaining; i++) {
                        LocalDate targetDate = currentDate.plusDays(i);
                        int targetYear = targetDate.getYear();
                        int targetDay = targetDate.getDayOfYear();
                        int targetSubDays = remaining - i;

                        calendar.updateDay(targetYear, targetDay, gameType, 0, targetSubDays, null);
                    }
                } else if (action == UpdateSubscribeDaysActions.Delete) {
                    int daysToClear = 30;
                    for (int i = 1; i <= daysToClear; i++) {
                        LocalDate targetDate = currentDate.plusDays(i);
                        int targetYear = targetDate.getYear();
                        int targetDay = targetDate.getDayOfYear();
                        int targetSubDays = remaining > 0 ? Math.max(0, remaining - i) : 0;

                        calendar.updateDay(targetYear, targetDay, gameType, 0, targetSubDays, null);
                    }
                }

                if (onComplete != null) {
                    AppDatabase.postToMain(onComplete);
                }
            });
        });
    }

    public void getDayStatus(int dayOfYear, DatabaseRepository.Callback<Integer> callback) {
        calendar.getDay(year, dayOfYear, mainActivity.getSubType(), date -> {
            if (callback != null) {
                callback.onResult(date != null ? date.status : 0);
            }
        });
    }

    public void setDayStatus(int dayOfYear, int status, Runnable onComplete) {
        calendar.getDay(year, dayOfYear, mainActivity.getSubType(), date -> {
            int rem = date != null ? date.subDaysRemaining : 0;
            calendar.updateDay(year, dayOfYear, mainActivity.getSubType(), status, rem, onComplete);
        });
    }

    public void getDaySubDaysRemaining(int dayOfYear, DatabaseRepository.Callback<Integer> callback) {
        calendar.getDay(year, dayOfYear, mainActivity.getSubType(), date -> {
            if (callback != null) {
                callback.onResult(date != null ? date.subDaysRemaining : 0);
            }
        });
    }

    public void setDaySubDaysRemaining(int dayOfYear, int value, Runnable onComplete) {
        calendar.getDay(year, dayOfYear, mainActivity.getSubType(), date -> {
            int status = date != null ? date.status : 0;
            calendar.updateDay(year, dayOfYear, mainActivity.getSubType(), status, value, onComplete);
        });
    }

    public void addDaySubDaysRemaining(int dayOfYear, int value, Runnable onComplete) {
        getDaySubDaysRemaining(dayOfYear, rem -> setDaySubDaysRemaining(dayOfYear, rem + value, onComplete));
    }

    public void subtractDaySubDaysRemaining(int dayOfYear, int value, Runnable onComplete) {
        getDaySubDaysRemaining(dayOfYear, rem -> setDaySubDaysRemaining(dayOfYear, rem - value, onComplete));
    }

    public int getYear() {
        return year;
    }

    public void addYear() {
        year++;
    }

    public void subtractYear() {
        year--;
    }

    public int getSubsCount() {
        return subsCount;
    }

    public void setSubsCount(int value) {
        subsCount = value;
    }

    public void addSub() {
        subsCount++;
    }

    public void delSub() {
        subsCount--;
    }

    public int getClaimsDays() {
        return claimsDays;
    }

    public void setClaimsDays(int value) {
        claimsDays = value;
    }

    public void addClaimDay() {
        claimsDays++;
    }

    public void subtractClaimDay() {
        claimsDays--;
    }

    public void setMissesDays(int value) {
        missesDays = value;
    }
}