package ru.menshovanton.gachapoint.ui.fragment.tracker;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.repository.CalendarRepository;
import ru.menshovanton.gachapoint.domain.enums.DayState;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Statistic;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;
import ru.menshovanton.gachapoint.ui.fragment.tracker.model.CalendarCellUiModel;

public class TrackerViewModel extends AndroidViewModel {

    private final CalendarRepository calendarRepository;

    private GameType currentGameType = GameType.GENSHIN;
    private int selectedMonth;
    private int selectedYear;

    private final MutableLiveData<GameType> gameTypeLiveData = new MutableLiveData<>(GameType.GENSHIN);
    private final MutableLiveData<Integer> selectedMonthLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedYearLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> subsCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Statistic> statisticLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<CalendarCellUiModel>> calendarCellsLiveData = new MutableLiveData<>();

    private final SingleLiveEvent<Integer> toastMessageEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> vibrateEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> openQuestionDialogEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> playSoundEvent = new SingleLiveEvent<>();

    private final MutableLiveData<List<Date>> monthDates = new MutableLiveData<>();

    private final int currentYear = LocalDate.now().getYear();
    private final int currentMonth = LocalDate.now().getMonthValue();

    public TrackerViewModel(@NonNull Application application) {
        super(application);
        this.calendarRepository = new CalendarRepository(application);

        LocalDate now = LocalDate.now();
        this.selectedMonth = now.getMonthValue();
        this.selectedYear = now.getYear();

        this.selectedMonthLiveData.setValue(selectedMonth);
        this.selectedYearLiveData.setValue(selectedYear);
    }

    public LiveData<List<Date>> getMonthDates() {
        return monthDates;
    }

    public LiveData<GameType> getGameTypeLiveData() { return gameTypeLiveData; }
    public LiveData<Integer> getSelectedMonthLiveData() { return selectedMonthLiveData; }
    public LiveData<Integer> getSelectedYearLiveData() { return selectedYearLiveData; }
    public LiveData<Integer> getSubsCountLiveData() { return subsCountLiveData; }
    public LiveData<Statistic> getStatisticLiveData() { return statisticLiveData; }
    public LiveData<List<CalendarCellUiModel>> getCalendarCellsLiveData() { return calendarCellsLiveData; }
    public LiveData<Integer> getToastMessageEvent() { return toastMessageEvent; }
    public LiveData<Void> getVibrateEvent() { return vibrateEvent; }
    public SingleLiveEvent<Void> getOpenQuestionDialogEvent() { return openQuestionDialogEvent; }
    public LiveData<Integer> getPlaySoundEvent() { return playSoundEvent; }

    public void setGameType(GameType gameType) {
        if (this.currentGameType != gameType) {
            this.currentGameType = gameType;
            this.gameTypeLiveData.setValue(gameType);
            refreshData();
        }
    }

    public GameType getCurrentGameType() {
        return currentGameType;
    }

    public void refreshData() {
        calendarRepository.init(currentGameType, selectedYear, () -> {
            int count = calendarRepository.getSubsCount();
            subsCountLiveData.setValue(count);

            loadCalendarGrid();
            loadStatistics();
        });
    }

    private void loadCalendarGrid() {
        LocalDate today = LocalDate.now();

        int firstDayOfWeek = LocalDate.of(selectedYear, selectedMonth, 1).getDayOfWeek().getValue();
        int offset = firstDayOfWeek - 1;
        int daysInMonth = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth();

        calendarRepository.getMonthDates(selectedYear, selectedMonth, currentGameType, monthDates -> {
            List<CalendarCellUiModel> cells = new ArrayList<>(42);

            for (int i = 0; i < 42; i++) {
                if (i < offset || i >= (offset + daysInMonth)) {
                    cells.add(CalendarCellUiModel.empty());
                } else {
                    int dayIndexInMonth = i - offset;
                    if (dayIndexInMonth < monthDates.size()) {
                        Date dateObj = monthDates.get(dayIndexInMonth);

                        int bg = (dateObj.dayOfMonth == today.getDayOfMonth()
                                && dateObj.month == today.getMonthValue()
                                && dateObj.year == today.getYear())
                                ? R.drawable.calendar_cell_today
                                : R.drawable.calendar_cell;

                        DayState dayState = DayState.from(dateObj, selectedMonth, today.getDayOfYear());
                        cells.add(new CalendarCellUiModel(dateObj.dayOfMonth, true, bg, dayState.getColorResId()));
                    } else {
                        cells.add(CalendarCellUiModel.empty());
                    }
                }
            }
            calendarCellsLiveData.setValue(cells);
        });
    }

    private void loadStatistics() {
        calendarRepository.getStatistic(currentGameType, selectedYear, statisticLiveData::setValue);
    }

    public void nextMonth() {
        if (selectedMonth == 12) {
            selectedMonth = 1;
            selectedYear++;
        } else {
            selectedMonth++;
        }
        updateDateState();
    }

    public void previousMonth() {
        if (selectedMonth == 1) {
            selectedMonth = 12;
            selectedYear--;
        } else {
            selectedMonth--;
        }
        updateDateState();
    }

    private void updateDateState() {
        selectedMonthLiveData.setValue(selectedMonth);
        selectedYearLiveData.setValue(selectedYear);
        loadCalendarGrid();
    }

    public void onCheckClick() {
        vibrateEvent.call();
        int today = LocalDate.now().getDayOfYear();

        calendarRepository.getDayStatus(selectedYear, today, currentGameType, status -> {
            if (status == 1) {
                toastMessageEvent.setValue(R.string.already_checked);
            } else {
                calendarRepository.getDaySubDaysRemaining(selectedYear, today, currentGameType, remaining -> {
                    if (remaining == 0) {
                        openQuestionDialogEvent.call();
                    } else {
                        playSoundEvent.setValue(R.raw.success);
                        performCheck(today, R.string.check_today);
                    }
                });
            }
        });
    }

    private void performCheck(int dayOfYear, @StringRes int messageRes) {
        calendarRepository.setDayStatus(selectedYear, dayOfYear, currentGameType, 1, () -> {
            calendarRepository.addClaimDay();
            toastMessageEvent.setValue(messageRes);
            refreshData();
        });
    }

    public void onAddClick() {
        int today = todayOfYear();
        int currentYear = LocalDate.now().getYear();

        if (calendarRepository.getSubsCount() < 6) {
            playSoundEvent.setValue(R.raw.success);
            calendarRepository.getDaySubDaysRemaining(currentYear, today, currentGameType, remaining -> {
                if (remaining == 0) {
                    calendarRepository.setSubsCount(1);
                    calendarRepository.setMissesDays(0);
                    calendarRepository.setClaimsDays(0);
                    executeSubAction(today, 30, CalendarRepository.UpdateSubscribeDaysActions.Add);
                } else {
                    calendarRepository.addSub();
                    executeSubAction(today, remaining + 30, CalendarRepository.UpdateSubscribeDaysActions.Add);
                }
            });
        } else {
            toastMessageEvent.setValue(R.string.subs_limit);
        }
    }

    private void executeSubAction(int dayOfYear, int subDays, CalendarRepository.UpdateSubscribeDaysActions action) {
        int currentYear = LocalDate.now().getYear();
        calendarRepository.updateSubscribeDays(currentYear, dayOfYear, currentGameType, action, subDays, () -> {
            toastMessageEvent.setValue(action == CalendarRepository.UpdateSubscribeDaysActions.Add ? R.string.add_sub : R.string.del_sub);
            refreshData();
        });
    }

    public void onDelClick() {
        int today = todayOfYear();
        int currentYear = LocalDate.now().getYear();

        calendarRepository.getDaySubDaysRemaining(currentYear, today, currentGameType, remaining -> {
            if (remaining > 0) {
                int newRemaining = Math.max(0, remaining - 30);

                calendarRepository.delSub();
                executeSubAction(today, newRemaining, CalendarRepository.UpdateSubscribeDaysActions.Delete);
            } else {
                toastMessageEvent.setValue(R.string.active_subs_null);
            }
        });
    }

    public void recoveryMissDay() {
        int yesterday = todayOfYear() - 1;
        if (yesterday >= 1) {
            calendarRepository.getDayStatus(selectedYear, yesterday, currentGameType, status -> {
                if (status == 1) {
                    toastMessageEvent.setValue(R.string.not_miss_day);
                } else {
                    calendarRepository.getDaySubDaysRemaining(selectedYear, yesterday, currentGameType, remaining -> {
                        if (remaining == 0) {
                            toastMessageEvent.setValue(R.string.active_subs_null);
                        } else {
                            performCheck(yesterday, R.string.check_today);
                        }
                    });
                }
            });
        }
    }

    public void onCancelCheck() {
        int today = todayOfYear();
        calendarRepository.getDayStatus(selectedYear, today, currentGameType, status -> {
            if (status == 0) {
                toastMessageEvent.setValue(R.string.not_check_today);
            } else {
                calendarRepository.getDaySubDaysRemaining(selectedYear, today, currentGameType, remaining -> {
                    if (remaining == 0) {
                        toastMessageEvent.setValue(R.string.active_subs_null);
                    } else {
                        cancelCheckInternal(today, () -> {
                            toastMessageEvent.setValue(R.string.cancel_check_today);
                            refreshData();
                        });
                    }
                });
            }
        });
    }

    private void cancelCheckInternal(int dayOfYear, Runnable onComplete) {
        calendarRepository.setDayStatus(selectedYear, dayOfYear, currentGameType, 0, () -> {
            calendarRepository.subtractClaimDay();
            if (onComplete != null) onComplete.run();
        });
    }

    private int todayOfYear() {
        return LocalDate.now().getDayOfYear();
    }
}