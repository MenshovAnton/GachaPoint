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

import ru.menshovanton.gachapoint.Calendar;
import ru.menshovanton.gachapoint.models.Date;
import ru.menshovanton.gachapoint.models.DayState;
import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.models.Statistic;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.fragments.TrackerFragment;

public class CalendarHelper {
    private final MainActivity mainActivity;
    private final Calendar calendar;
    private final PiggyBankHelper piggyBankHelper;
    private final DateHelper dateHelper;

    private final int toDayOfYear;
    private int year;
    private int missesDays = 0;
    private int claimsDays = 0;
    private int subsCount;

    private final int WISHES_COST = 160;
    private final int PRIMOGEMS_PER_DAY = 90;
    private final int SUMMARY_CLAIM = 2700;

    public CalendarHelper(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        piggyBankHelper = new PiggyBankHelper(mainActivity);
        dateHelper = new DateHelper(mainActivity);

        toDayOfYear = LocalDate.now().getDayOfYear();
        year = LocalDate.now().getYear();

        calendar = new Calendar(mainActivity, mainActivity);

        int daysRemaining = calendar.getSubDaysRemaining(toDayOfYear);
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

        int yearOffset = (year - today.getYear()) * 365;
        int startDayOfYearInMonth = LocalDate.of(year, selectedMonth, 1).getDayOfYear();

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
                int globalIndex = (startDayOfYearInMonth - 1) + dayIndexInMonth + yearOffset;

                if (globalIndex >= 0 && globalIndex < calendar.datesArray.length) {
                    Date dateObj = calendar.datesArray[globalIndex];

                    cell.setVisibility(View.VISIBLE);
                    cell.setText(String.valueOf(dateObj.dayOfMonth));

                    if (dateObj.dayOfMonth == today.getDayOfMonth()
                            && dateObj.month == today.getMonthValue()
                            && dateObj.year == today.getYear()) {
                        cell.setBackgroundResource(R.drawable.background_date_today);
                    } else {
                        cell.setBackgroundResource(R.drawable.background_date);
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

    public void calculateStatistics() {
        int missedPrimogemsCount = missesDays * PRIMOGEMS_PER_DAY;
        int claimPrimogemsCount = claimsDays * PRIMOGEMS_PER_DAY;

        int laterPrimogemsCount = SUMMARY_CLAIM * subsCount - claimPrimogemsCount - missedPrimogemsCount;
        int laterWishesCount = laterPrimogemsCount / WISHES_COST;

        int missedWishesCount = missedPrimogemsCount / WISHES_COST;
        int claimWishesCount = claimPrimogemsCount / WISHES_COST;

        piggyBankHelper.updateSubsProgress(claimWishesCount);
    }

    public void calculateMissesAndClaims() {
        missesDays = 0;
        claimsDays = 0;

        for (int i = 0; i < calendar.datesArray.length; i++) {
            Date date = calendar.datesArray[i];
            if (date.status == 0 && date.subDaysRemaining > 0 && date.dayOfYear <= toDayOfYear - 1) {
                missesDays++;
            }
            if (date.status == 1 && date.subDaysRemaining > 0 && date.dayOfYear <= toDayOfYear - 1) {
                claimsDays++;
            }
        }

        if (missesDays > 0) missesDays++;
        if (toDayOfYear <= calendar.datesArray.length && calendar.datesArray[toDayOfYear - 1].status == 1) {
            claimsDays++;
        }
    }

    public Statistic getStatistic() {
        calculateMissesAndClaims();

        int missedPrimogemsCount = missesDays * PRIMOGEMS_PER_DAY;
        int claimPrimogemsCount = claimsDays * PRIMOGEMS_PER_DAY;
        int laterPrimogemsCount = SUMMARY_CLAIM * subsCount - claimPrimogemsCount - missedPrimogemsCount;
        int claimWishesCount = claimPrimogemsCount / WISHES_COST;

        piggyBankHelper.updateSubsProgress(claimWishesCount);

        return new Statistic(
                missedPrimogemsCount,
                claimPrimogemsCount,
                laterPrimogemsCount,
                missedPrimogemsCount / WISHES_COST,
                claimWishesCount,
                laterPrimogemsCount / WISHES_COST
        );
    }

    public void update() {
        int length;
        if (calendar.getSubDaysRemaining(toDayOfYear) > 30) {
            length = calendar.datesArray[toDayOfYear - 1].subDaysRemaining;
        } else {
            length = 30;
        }

        calculateStatistics();
        dateHelper.writeDB(mainActivity.getApplicationContext(), calendar.datesArray, LocalDate.now().getDayOfYear() - 1, length);
    }

    public enum UpdateSubscribeDaysActions {
        Add,
        Delete
    }

    public void updateSubscribeDays(UpdateSubscribeDaysActions action) {
        switch (action) {
            case Add:
                for (int i = 0; i < calendar.getSubDaysRemaining(toDayOfYear); i++) {
                    calendar.datesArray[toDayOfYear + i].subDaysRemaining = calendar.datesArray[toDayOfYear + i - 1].subDaysRemaining - 1;
                }
                break;
            case Delete:
                if (calendar.getSubDaysRemaining(toDayOfYear) > 30) {
                    for (int i = 0; i < calendar.getSubDaysRemaining(toDayOfYear); i++) {
                        calendar.datesArray[toDayOfYear + i].subDaysRemaining = calendar.datesArray[toDayOfYear + i - 1].subDaysRemaining - 1;
                    }
                } else {
                    for (int i = 30; i >= 0; i--) {
                        calendar.datesArray[toDayOfYear + i].subDaysRemaining = 0;
                    }
                }
                break;
        }
    }

    public int getDayStatus(int id) {
        return calendar.datesArray[id].status;
    }

    public void setDayStatus(int id, int status) {
        calendar.datesArray[id].status = status;
    }

    public int getDaySubDaysRemaining(int id) {
        return calendar.datesArray[id].subDaysRemaining;
    }

    public void setDaySubDaysRemaining(int id, int value) {
        calendar.datesArray[id].subDaysRemaining = value;
    }

    public void addDaySubDaysRemaining(int id, int value) {
        calendar.datesArray[id].subDaysRemaining += value;
    }

    public void subtractDaySubDaysRemaining(int id, int value) {
        calendar.datesArray[id].subDaysRemaining -= value;
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