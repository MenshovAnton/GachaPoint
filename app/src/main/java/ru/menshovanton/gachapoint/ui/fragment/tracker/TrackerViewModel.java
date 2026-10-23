package ru.menshovanton.gachapoint.ui.fragment.tracker;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.repository.CalendarRepository;
import ru.menshovanton.gachapoint.domain.enums.DayState;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.enums.TrackerMode;
import ru.menshovanton.gachapoint.domain.models.Date;
import ru.menshovanton.gachapoint.domain.models.Statistic;
import ru.menshovanton.gachapoint.ui.event.SingleLiveEvent;
import ru.menshovanton.gachapoint.ui.fragment.tracker.model.CalendarCellUiModel;

public class TrackerViewModel extends AndroidViewModel {

    private final CalendarRepository calendarRepository;

    private GameType currentGameType = GameType.GENSHIN;
    private TrackerMode currentMode = TrackerMode.SUBSCRIPTION;
    private int selectedMonth;
    private int selectedYear;

    private final MutableLiveData<GameType> gameTypeLiveData = new MutableLiveData<>(GameType.GENSHIN);
    private final MutableLiveData<TrackerMode> trackerModeLiveData = new MutableLiveData<>(TrackerMode.SUBSCRIPTION);
    private final MutableLiveData<Integer> selectedMonthLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedYearLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> subsCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<int[]> dailiesProgressLiveData = new MutableLiveData<>(new int[]{0, YearMonth.now().lengthOfMonth()});
    private final MutableLiveData<Statistic> statisticLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<CalendarCellUiModel>> calendarCellsLiveData = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isSelectionModeLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Set<LocalDate>> selectedDatesLiveData = new MutableLiveData<>(new HashSet<>());

    private final SingleLiveEvent<Integer> toastMessageEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> vibrateEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> openSubAddingDialogEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> openSubDeletingDialogEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<CalendarCellUiModel> openDayDetailsEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<LocalDate> openAddSubscriptionDialogEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> playSoundEvent = new SingleLiveEvent<>();

    public TrackerViewModel(@NonNull Application application) {
        super(application);
        ru.menshovanton.gachapoint.di.AppContainer container = ((ru.menshovanton.gachapoint.App) application).getAppContainer();
        this.calendarRepository = container.getCalendarRepository();

        LocalDate now = LocalDate.now();
        this.selectedMonth = now.getMonthValue();
        this.selectedYear = now.getYear();

        this.selectedMonthLiveData.setValue(selectedMonth);
        this.selectedYearLiveData.setValue(selectedYear);
    }

    public LiveData<GameType> getGameTypeLiveData() { return gameTypeLiveData; }
    public LiveData<TrackerMode> getTrackerModeLiveData() { return trackerModeLiveData; }
    public LiveData<Integer> getSelectedMonthLiveData() { return selectedMonthLiveData; }
    public LiveData<Integer> getSelectedYearLiveData() { return selectedYearLiveData; }
    public LiveData<Integer> getSubsCountLiveData() { return subsCountLiveData; }
    public LiveData<int[]> getDailiesProgressLiveData() { return dailiesProgressLiveData; }
    public LiveData<Statistic> getStatisticLiveData() { return statisticLiveData; }
    public LiveData<List<CalendarCellUiModel>> getCalendarCellsLiveData() { return calendarCellsLiveData; }
    public LiveData<Boolean> getIsSelectionModeLiveData() { return isSelectionModeLiveData; }
    public LiveData<Set<LocalDate>> getSelectedDatesLiveData() { return selectedDatesLiveData; }
    public boolean isSelectionMode() { return Boolean.TRUE.equals(isSelectionModeLiveData.getValue()); }
    public LiveData<Integer> getToastMessageEvent() { return toastMessageEvent; }
    public LiveData<Void> getVibrateEvent() { return vibrateEvent; }
    public SingleLiveEvent<Void> getOpenSubAddingDialogEvent() { return openSubAddingDialogEvent; }
    public SingleLiveEvent<Void> getOpenSubDeletingDialogEvent() { return openSubDeletingDialogEvent; }
    public SingleLiveEvent<CalendarCellUiModel> getOpenDayDetailsEvent() { return openDayDetailsEvent; }
    public SingleLiveEvent<LocalDate> getOpenAddSubscriptionDialogEvent() { return openAddSubscriptionDialogEvent; }
    public LiveData<Integer> getPlaySoundEvent() { return playSoundEvent; }

    public void showAddSubscriptionDialog(LocalDate startDate) {
        openAddSubscriptionDialogEvent.setValue(startDate);
    }

    public void setGameType(GameType gameType) {
        if (this.currentGameType != gameType) {
            exitSelectionMode();
            this.currentGameType = gameType;
            this.gameTypeLiveData.setValue(gameType);
            refreshData();
        }
    }

    public void setGameTypeSilently(GameType gameType) {
        if (this.currentGameType != gameType) {
            exitSelectionMode();
            this.currentGameType = gameType;
            this.gameTypeLiveData.setValue(gameType);
        }
    }

    public GameType getCurrentGameType() {
        return currentGameType;
    }

    public TrackerMode getCurrentMode() {
        return currentMode;
    }

    public void setTrackerMode(TrackerMode mode) {
        if (this.currentMode != mode) {
            exitSelectionMode();
            this.currentMode = mode;
            this.trackerModeLiveData.setValue(mode);
            loadCalendarGrid();
            loadStatistics();
        }
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
            int dailiesCompleted = 0;

            if (monthDates != null) {
                for (Date d : monthDates) {
                    if (d.dailyStatus == 1) dailiesCompleted++;
                }
            }
            dailiesProgressLiveData.setValue(new int[]{dailiesCompleted, daysInMonth});

            for (int i = 0; i < 42; i++) {
                if (i < offset || i >= (offset + daysInMonth)) {
                    cells.add(CalendarCellUiModel.empty());
                } else {
                    int dayIndexInMonth = i - offset;
                    if (monthDates != null && dayIndexInMonth < monthDates.size()) {
                        Date dateObj = monthDates.get(dayIndexInMonth);
                        boolean isToday = (dateObj.dayOfMonth == today.getDayOfMonth()
                                && dateObj.month == today.getMonthValue()
                                && dateObj.year == today.getYear());

                        boolean isPast = dateObj.year < today.getYear()
                                || (dateObj.year == today.getYear() && dateObj.dayOfYear < today.getDayOfYear());

                        int bg;
                        int textColor;

                        if (currentMode == TrackerMode.SUBSCRIPTION) {
                            boolean isClaimed = (dateObj.status == DayState.STATUS_RECEIVED || dateObj.status == DayState.STATUS_OLD_RECEIVED);
                            if (isClaimed) {
                                bg = isToday ? R.drawable.bg_calendar_cell_today_checked : R.drawable.bg_calendar_cell_checked;
                                textColor = R.color.white;
                            } else if (isPast && dateObj.subDaysRemaining > 0) {
                                bg = R.drawable.bg_calendar_cell_missed;
                                textColor = R.color.white;
                            } else if (isToday) {
                                bg = R.drawable.bg_calendar_cell_today;
                                textColor = dateObj.subDaysRemaining > 0 ? R.color.check : R.color.text;
                            } else if (dateObj.subDaysRemaining > 0) {
                                bg = R.drawable.bg_calendar_cell_active;
                                textColor = R.color.text;
                            } else {
                                bg = 0;
                                textColor = R.color.text;
                            }
                        } else {
                            if (dateObj.dailyStatus == 1) {
                                bg = isToday ? R.drawable.bg_calendar_cell_today_checked : R.drawable.bg_calendar_cell_checked;
                                textColor = R.color.white;
                            } else if (isPast) {
                                bg = R.drawable.bg_calendar_cell_missed;
                                textColor = R.color.white;
                            } else if (isToday) {
                                bg = R.drawable.bg_calendar_cell_today;
                                textColor = R.color.check;
                            } else {
                                bg = 0;
                                textColor = R.color.text;
                            }
                        }

                        cells.add(new CalendarCellUiModel(
                                dateObj.dayOfMonth,
                                dateObj.dayOfYear,
                                dateObj.month,
                                dateObj.year,
                                true,
                                isToday,
                                dateObj.status,
                                dateObj.subDaysRemaining,
                                dateObj.dailyStatus,
                                bg,
                                textColor
                        ));
                    } else {
                        cells.add(CalendarCellUiModel.empty());
                    }
                }
            }
            calendarCellsLiveData.setValue(cells);
        });
    }

    private void loadStatistics() {
        if (currentMode == TrackerMode.SUBSCRIPTION) {
            calendarRepository.getStatistic(currentGameType, selectedYear, statisticLiveData::setValue);
        } else {
            calendarRepository.getDailiesStatistic(currentGameType, selectedYear, selectedMonth, statisticLiveData::setValue);
        }
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
        exitSelectionMode();
        selectedMonthLiveData.setValue(selectedMonth);
        selectedYearLiveData.setValue(selectedYear);
        loadCalendarGrid();
        loadStatistics();
    }

    public void enterSelectionMode(@Nullable LocalDate initialDate) {
        isSelectionModeLiveData.setValue(true);
        Set<LocalDate> set = new HashSet<>();
        if (initialDate != null) {
            set.add(initialDate);
        }
        selectedDatesLiveData.setValue(set);
    }

    public void exitSelectionMode() {
        if (Boolean.TRUE.equals(isSelectionModeLiveData.getValue())) {
            isSelectionModeLiveData.setValue(false);
        }
        Set<LocalDate> current = selectedDatesLiveData.getValue();
        if (current != null && !current.isEmpty()) {
            selectedDatesLiveData.setValue(new HashSet<>());
        }
    }

    public void toggleDateSelection(LocalDate date) {
        if (date == null) return;
        vibrateEvent.call();
        Set<LocalDate> current = selectedDatesLiveData.getValue();
        Set<LocalDate> updated = (current != null) ? new HashSet<>(current) : new HashSet<>();
        if (updated.contains(date)) {
            updated.remove(date);
        } else {
            updated.add(date);
        }
        selectedDatesLiveData.setValue(updated);
    }

    public void selectAllCurrentMonth(List<CalendarCellUiModel> cells) {
        vibrateEvent.call();
        if (cells == null) return;
        List<LocalDate> visibleDates = new ArrayList<>();
        for (CalendarCellUiModel cell : cells) {
            if (cell.isVisible) {
                visibleDates.add(LocalDate.of(cell.year, cell.month, cell.dayOfMonth));
            }
        }
        Set<LocalDate> current = selectedDatesLiveData.getValue();
        if (current != null && current.containsAll(visibleDates) && !visibleDates.isEmpty()) {
            selectedDatesLiveData.setValue(new HashSet<>());
        } else {
            selectedDatesLiveData.setValue(new HashSet<>(visibleDates));
        }
    }

    public void markSelectedDays(boolean isReceived) {
        Set<LocalDate> selected = selectedDatesLiveData.getValue();
        if (selected == null || selected.isEmpty()) {
            toastMessageEvent.setValue(R.string.multi_select_no_selection);
            return;
        }

        vibrateEvent.call();
        List<LocalDate> datesToUpdate = new ArrayList<>(selected);

        if (currentMode == TrackerMode.SUBSCRIPTION) {
            List<CalendarCellUiModel> currentCells = calendarCellsLiveData.getValue();
            List<LocalDate> eligibleDates = new ArrayList<>();
            if (currentCells != null) {
                for (CalendarCellUiModel cell : currentCells) {
                    if (cell.isVisible && cell.subDaysRemaining > 0) {
                        LocalDate cellDate = LocalDate.of(cell.year, cell.month, cell.dayOfMonth);
                        if (selected.contains(cellDate)) {
                            eligibleDates.add(cellDate);
                        }
                    }
                }
            }

            if (eligibleDates.isEmpty()) {
                toastMessageEvent.setValue(R.string.active_subs_none_in_selection);
                return;
            }

            int status = isReceived ? DayState.STATUS_RECEIVED : DayState.STATUS_MISSED;
            calendarRepository.setDaysSubscriptionStatusBatch(eligibleDates, currentGameType, status, () -> {
                if (isReceived) {
                    playSoundEvent.setValue(R.raw.success);
                }
                toastMessageEvent.setValue(R.string.multi_select_marked_success);
                exitSelectionMode();
                refreshData();
            });
        } else {
            int dailyStatus = isReceived ? 1 : 0;
            calendarRepository.setDaysDailyStatusBatch(datesToUpdate, currentGameType, dailyStatus, () -> {
                if (isReceived) {
                    playSoundEvent.setValue(R.raw.success);
                }
                toastMessageEvent.setValue(R.string.multi_select_marked_success);
                exitSelectionMode();
                refreshData();
            });
        }
    }

    public void onCellClick(CalendarCellUiModel cell) {
        if (cell != null && cell.isVisible) {
            vibrateEvent.call();
            openDayDetailsEvent.setValue(cell);
        }
    }

    public void onCheckClick() {
        vibrateEvent.call();
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int todayDay = today.getDayOfYear();

        if (currentMode == TrackerMode.SUBSCRIPTION) {
            calendarRepository.getDayStatus(currentYear, todayDay, currentGameType, status -> {
                if (status == 1) {
                    toastMessageEvent.setValue(R.string.already_checked);
                } else {
                    calendarRepository.getDaySubDaysRemaining(currentYear, todayDay, currentGameType, remaining -> {
                        if (remaining == 0) {
                            openSubAddingDialogEvent.call();
                        } else {
                            playSoundEvent.setValue(R.raw.success);
                            performCheck(currentYear, todayDay, R.string.check_today);
                        }
                    });
                }
            });
        } else {
            calendarRepository.getMonthDates(currentYear, today.getMonthValue(), currentGameType, dates -> {
                Date todayDate = null;
                if (dates != null) {
                    for (Date d : dates) {
                        if (d.dayOfYear == todayDay) {
                            todayDate = d;
                            break;
                        }
                    }
                }
                if (todayDate != null && todayDate.dailyStatus == 1) {
                    toastMessageEvent.setValue(R.string.dailies_already_completed);
                } else {
                    playSoundEvent.setValue(R.raw.success);
                    calendarRepository.setDayDailyStatus(currentYear, todayDay, currentGameType, 1, () -> {
                        toastMessageEvent.setValue(R.string.mark_dailies_today);
                        refreshData();
                    });
                }
            });
        }
    }

    public void toggleDailyStatus(int year, int dayOfYear, boolean isCompleted) {
        vibrateEvent.call();
        if (isCompleted) {
            playSoundEvent.setValue(R.raw.success);
        }
        calendarRepository.setDayDailyStatus(year, dayOfYear, currentGameType, isCompleted ? 1 : 0, this::refreshData);
    }

    public void toggleSubStatus(int year, int dayOfYear, boolean isReceived) {
        vibrateEvent.call();
        if (isReceived) {
            playSoundEvent.setValue(R.raw.success);
        }
        calendarRepository.setDayStatus(year, dayOfYear, currentGameType, isReceived ? DayState.STATUS_RECEIVED : DayState.STATUS_MISSED, this::refreshData);
    }

    public void addSubscriptionChain(int count, LocalDate startDate) {
        if (calendarRepository.getSubsCount() >= currentGameType.getMaxSubs()) {
            toastMessageEvent.setValue(R.string.subs_limit);
            return;
        }

        calendarRepository.addSubscription(currentGameType, count, startDate, success -> {
            if (Boolean.TRUE.equals(success)) {
                playSoundEvent.setValue(R.raw.success);
                toastMessageEvent.setValue(R.string.add_sub);
                refreshData();
            } else {
                toastMessageEvent.setValue(R.string.subs_limit);
            }
        });
    }

    private void performCheck(int year, int dayOfYear, @StringRes int messageRes) {
        calendarRepository.setDayStatus(year, dayOfYear, currentGameType, 1, () -> {
            toastMessageEvent.setValue(messageRes);
            refreshData();
        });
    }

    public void onAddClick() {
        if (calendarRepository.getSubsCount() < currentGameType.getMaxSubs()) {
            openAddSubscriptionDialogEvent.setValue(LocalDate.now());
        } else {
            toastMessageEvent.setValue(R.string.subs_limit);
        }
    }

    public void onDelClick() {
        openSubDeletingDialogEvent.call();
    }

    public void deletingSub() {
        calendarRepository.deleteSubscription(currentGameType, success -> {
            if (Boolean.TRUE.equals(success)) {
                toastMessageEvent.setValue(R.string.del_sub);
                refreshData();
            } else {
                toastMessageEvent.setValue(R.string.active_subs_null);
            }
        });
    }

    public void recoveryMissDay() {
        LocalDate yesterdayDate = LocalDate.now().minusDays(1);
        int yesterdayYear = yesterdayDate.getYear();
        int yesterdayDay = yesterdayDate.getDayOfYear();

        calendarRepository.getDayStatus(yesterdayYear, yesterdayDay, currentGameType, status -> {
            if (status == 1) {
                toastMessageEvent.setValue(R.string.not_miss_day);
            } else {
                calendarRepository.getDaySubDaysRemaining(yesterdayYear, yesterdayDay, currentGameType, remaining -> {
                    if (remaining == 0) {
                        toastMessageEvent.setValue(R.string.active_subs_null);
                    } else {
                        playSoundEvent.setValue(R.raw.success);
                        performCheck(yesterdayYear, yesterdayDay, R.string.check_today);
                    }
                });
            }
        });
    }

    public void onCancelCheck() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int todayDay = today.getDayOfYear();

        calendarRepository.getDayStatus(currentYear, todayDay, currentGameType, status -> {
            if (status == 0) {
                toastMessageEvent.setValue(R.string.not_check_today);
            } else {
                calendarRepository.getDaySubDaysRemaining(currentYear, todayDay, currentGameType, remaining -> {
                    if (remaining == 0) {
                        toastMessageEvent.setValue(R.string.active_subs_null);
                    } else {
                        cancelCheckInternal(currentYear, todayDay, () -> {
                            toastMessageEvent.setValue(R.string.cancel_check_today);
                            refreshData();
                        });
                    }
                });
            }
        });
    }

    private void cancelCheckInternal(int year, int dayOfYear, Runnable onComplete) {
        calendarRepository.setDayStatus(year, dayOfYear, currentGameType, 0, () -> {
            if (onComplete != null) onComplete.run();
        });
    }
}