package ru.menshovanton.gachapoint.helpers;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.time.LocalDate;

import ru.menshovanton.gachapoint.Calendar;
import ru.menshovanton.gachapoint.Statistic;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.fragments.TrackerFragment;

public class CalendarHelper {
    public MainActivity mainActivity;
    public Calendar calendar;

    public int toDayOfMonth;
    public int toDayOfYear;
    public int year;
    public int missesDays;
    public int claimsDays;
    public int subsCount;

    public final int WISHES_COST = 160;
    public final int PRIMOGEMS_PER_DAY = 90;
    public final int SUMMARY_CLAIM = 2700;

    public CalendarHelper(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        missesDays = 0;
        claimsDays = 0;
        subsCount = 0;

        toDayOfMonth = LocalDate.now().getDayOfMonth();
        toDayOfYear = LocalDate.now().getDayOfYear();
        year = LocalDate.now().getYear();

        calendar = new Calendar(mainActivity);

        if (calendar.getSubDaysRemaining(toDayOfYear) == 0) { subsCount = 0; }
        else if (calendar.getSubDaysRemaining(toDayOfYear) <= 30) { subsCount = 1; }
        else if (calendar.getSubDaysRemaining(toDayOfYear) <= 60) { subsCount = 2;}
        else if (calendar.getSubDaysRemaining(toDayOfYear) <= 90) { subsCount = 3; }
        else if (calendar.getSubDaysRemaining(toDayOfYear) <= 120) { subsCount = 4; }
        else if (calendar.getSubDaysRemaining(toDayOfYear) <= 150) { subsCount = 5; }
        else if (calendar.getSubDaysRemaining(toDayOfYear) <= 180) { subsCount = 6; }
    }

    public void drawCalendarView() {
        int margin = 0;
        int topMargin = 0;
        int j = 0;
        int line = 1;

        int daysOfYearForMonth = Calendar.getDaysOfYearForMonth(TrackerFragment.selectedMonth);

        for (int i = 0; i < Calendar.calendarSize; i++) {
            if (calendar.datesArray[i].status == 0
                    && calendar.datesArray[i].subDaysRemaining > 0
                    && calendar.datesArray[i].dayOfYear <= toDayOfYear - 1) {
                missesDays++;
            }

            if (calendar.datesArray[i].status == 1
                    && calendar.datesArray[i].subDaysRemaining > 0
                    && calendar.datesArray[i].dayOfYear <= toDayOfYear - 1) {
                claimsDays++;
            }

            if (i >= daysOfYearForMonth
                    && i < daysOfYearForMonth + Calendar.getDaysOfMonth(TrackerFragment.selectedMonth)) {
                if (j == 7) {
                    topMargin = topMargin + 140;
                    margin = 0;
                    j = 0;
                }
                if (calendar.datesArray[i].dayOfWeek > 1 && line == 1) {
                    margin = calendar.datesArray[i].dayOfWeek * 140 - 140;
                    j = calendar.datesArray[i].dayOfWeek - 1;
                }

                TrackerFragment fragment = (TrackerFragment) mainActivity
                        .getSupportFragmentManager()
                        .findFragmentByTag(mainActivity.TRACKER_TAG);

                if (fragment != null && fragment.isVisible()) {
                    fragment.createView(calendar.datesArray[i],
                        calendar.datesCellsLabelsArray[i],
                        calendar.datesCellsBackgroundArray[i],
                        margin,
                        topMargin);
                }

                margin += 140;
                j++;
                line++;
            }
        }

        if (missesDays > 0) {
            missesDays++;
        }

        if (calendar.datesArray[toDayOfYear - 1].status == 1) {
            claimsDays++;
        }
    }

    public void removeCalendarView(ConstraintLayout constraintLayout) {
        for (TextView textView : calendar.datesCellsLabelsArray) {
            constraintLayout.removeView(textView);
        }
        for (ImageView imageView : calendar.datesCellsBackgroundArray) {
            constraintLayout.removeView(imageView);
        }
    }

    public void calculateMissesAndClaims() {
        for (int i = 0; i < Calendar.calendarSize; i++) {
            if (calendar.datesArray[i].status == 0 && calendar.datesArray[i].subDaysRemaining > 0 && calendar.datesArray[i].dayOfYear <= toDayOfYear - 1) {
                missesDays++;
            }

            if (calendar.datesArray[i].status == 1 && calendar.datesArray[i].subDaysRemaining > 0 && calendar.datesArray[i].dayOfYear <= toDayOfYear - 1) {
                claimsDays++;
            }
        }

        if (missesDays > 0) {
            missesDays++;
        }

        if (calendar.datesArray[toDayOfYear - 1].status == 1) {
            claimsDays++;
        }
    }

    private Statistic calculateStatistics() {
        int missedPrimogemsCount = missesDays * PRIMOGEMS_PER_DAY;
        int claimPrimogemsCount = claimsDays * PRIMOGEMS_PER_DAY;

        int laterPrimogemsCount = SUMMARY_CLAIM * subsCount - claimPrimogemsCount - missedPrimogemsCount;
        int laterWishesCount = laterPrimogemsCount / WISHES_COST;

        int missedWishesCount = missedPrimogemsCount / WISHES_COST;
        int claimWishesCount = claimPrimogemsCount / WISHES_COST;

        return new Statistic(
                missedPrimogemsCount,
                claimPrimogemsCount,
                laterPrimogemsCount,
                missedWishesCount,
                claimWishesCount,
                laterWishesCount
        );
    }

    public Statistic getStatistic() {
        return calculateStatistics();
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

    public void update() {
        int length;
        if (calendar.getSubDaysRemaining(toDayOfYear) > 30) {
            length = calendar.datesArray[toDayOfYear - 1].subDaysRemaining;
        } else {
            length = 30;
        }

        calculateStatistics();
        DateHelper.writeDB(mainActivity.getApplicationContext(), calendar.datesArray, LocalDate.now().getDayOfYear() - 1, length);
    }
}
