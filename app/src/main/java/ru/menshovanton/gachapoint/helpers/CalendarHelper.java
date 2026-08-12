package ru.menshovanton.gachapoint.helpers;

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
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.calendar.Calendar;
import ru.menshovanton.gachapoint.enums.DayState;
import ru.menshovanton.gachapoint.enums.GameType;
import ru.menshovanton.gachapoint.fragments.TrackerFragment;
import ru.menshovanton.gachapoint.models.Date;
import ru.menshovanton.gachapoint.models.Statistic;

public class CalendarHelper {
    private final MainActivity mainActivity;
    private final Calendar calendar;
    private final PiggyBankHelper piggyBankHelper;

    private final int toDayOfYear;
    private int year;
    private int missesDays = 0;
    private int claimsDays = 0;
    private int subsCount;

    public CalendarHelper(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.piggyBankHelper = new PiggyBankHelper(mainActivity);

        LocalDate now = LocalDate.now();
        toDayOfYear = now.getDayOfYear();
        year = now.getYear();

        calendar = new Calendar(mainActivity);

        Date todayDate = calendar.getDay(year, toDayOfYear, mainActivity.getSubType());
        int daysRemaining = todayDate != null ? todayDate.subDaysRemaining : 0;
        subsCount = (daysRemaining <= 0 || daysRemaining > 180) ? 0 : (daysRemaining + 29) / 30;
    }

    public void renderCalendar(TrackerFragment fragment) {
        if (fragment == null || !fragment.isVisible()) return;

        GridLayout gridLayout = fragment.getCalendarGrid();
        List<TextView> cellsPool = fragment.getCellViewsPool();

        int selectedMonth = fragment.getSelectedMonth();
        LocalDate today = LocalDate.now();
        Context context = mainActivity.getApplicationContext();

        int firstDayOfWeek = LocalDate.of(year, selectedMonth, 1).getDayOfWeek().getValue();
        int offset = firstDayOfWeek - 1;
        int daysInMonth = YearMonth.of(year, selectedMonth).lengthOfMonth();

        List<Date> monthDates = calendar.getMonthDates(year, selectedMonth, mainActivity.getSubType());

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
    }

    public void adjustCellSizes(TrackerFragment fragment) {
        if (fragment == null || !fragment.isVisible()) return;

        GridLayout gridLayout = fragment.getCalendarGrid();
        List<TextView> cellsPool = fragment.getCellViewsPool();

        gridLayout.post(() -> {
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

    public void calculateMissesAndClaims() {
        missesDays = 0;
        claimsDays = 0;

        if (toDayOfYear > 1) {
            List<Date> pastDays = calendar.getDaysRange(year, 1, toDayOfYear - 1, mainActivity.getSubType());
            for (Date date : pastDays) {
                if (date.status == 0 && date.subDaysRemaining > 0) {
                    missesDays++;
                }
                if (date.status == 1 && date.subDaysRemaining > 0) {
                    claimsDays++;
                }
            }
        }

        Date today = calendar.getDay(year, toDayOfYear, mainActivity.getSubType());
        if (today != null && today.status == 1) {
            claimsDays++;
        }
    }

    public Statistic getStatistic() {
        calculateMissesAndClaims();

        int wishesCost = 160;
        int primogemsPerDay = 90;
        int summaryClaim = 2700;

        int missedPrimogemsCount = missesDays * primogemsPerDay;
        int claimPrimogemsCount = claimsDays * primogemsPerDay;
        int laterPrimogemsCount = summaryClaim * subsCount - claimPrimogemsCount - missedPrimogemsCount;
        int claimWishesCount = claimPrimogemsCount / wishesCost;

        piggyBankHelper.updateSubsProgress(claimWishesCount);

        return new Statistic(
                missedPrimogemsCount,
                claimPrimogemsCount,
                laterPrimogemsCount,
                missedPrimogemsCount / wishesCost,
                claimWishesCount,
                laterPrimogemsCount / wishesCost
        );
    }

    public enum UpdateSubscribeDaysActions {
        Add,
        Delete
    }

    public void updateSubscribeDays(UpdateSubscribeDaysActions action) {
        GameType gameType = mainActivity.getSubType();
        int remaining = getDaySubDaysRemaining(toDayOfYear);

        LocalDate currentDate = LocalDate.ofYearDay(year, toDayOfYear);

        if (action == UpdateSubscribeDaysActions.Add) {
            for (int i = 1; i < remaining; i++) {
                LocalDate targetDate = currentDate.plusDays(i);

                int targetYear = targetDate.getYear();
                int targetDay = targetDate.getDayOfYear();
                int targetSubDays = remaining - i;

                Date existingDate = calendar.getDay(targetYear, targetDay, gameType);
                int currentStatus = existingDate != null ? existingDate.status : 0;

                calendar.updateDay(targetYear, targetDay, gameType, currentStatus, targetSubDays);
            }
        } else if (action == UpdateSubscribeDaysActions.Delete) {
            int daysToClear = 30;
            for (int i = 1; i <= daysToClear; i++) {
                LocalDate targetDate = currentDate.plusDays(i);

                int targetYear = targetDate.getYear();
                int targetDay = targetDate.getDayOfYear();
                int targetSubDays = remaining > 0 ? Math.max(0, remaining - i) : 0;

                Date existingDate = calendar.getDay(targetYear, targetDay, gameType);
                int currentStatus = existingDate != null ? existingDate.status : 0;

                calendar.updateDay(targetYear, targetDay, gameType, currentStatus, targetSubDays);
            }
        }
    }

    public int getDayStatus(int dayOfYear) {
        Date date = calendar.getDay(year, dayOfYear, mainActivity.getSubType());
        return date != null ? date.status : 0;
    }

    public void setDayStatus(int dayOfYear, int status) {
        Date date = calendar.getDay(year, dayOfYear, mainActivity.getSubType());
        int rem = date != null ? date.subDaysRemaining : 0;
        calendar.updateDay(year, dayOfYear, mainActivity.getSubType(), status, rem);
    }

    public int getDaySubDaysRemaining(int dayOfYear) {
        Date date = calendar.getDay(year, dayOfYear, mainActivity.getSubType());
        return date != null ? date.subDaysRemaining : 0;
    }

    public void setDaySubDaysRemaining(int dayOfYear, int value) {
        Date date = calendar.getDay(year, dayOfYear, mainActivity.getSubType());
        int status = date != null ? date.status : 0;
        calendar.updateDay(year, dayOfYear, mainActivity.getSubType(), status, value);
    }

    public void addDaySubDaysRemaining(int dayOfYear, int value) {
        setDaySubDaysRemaining(dayOfYear, getDaySubDaysRemaining(dayOfYear) + value);
    }

    public void subtractDaySubDaysRemaining(int dayOfYear, int value) {
        setDaySubDaysRemaining(dayOfYear, getDaySubDaysRemaining(dayOfYear) - value);
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