package ru.menshovanton.gachapoint.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import android.os.LocaleList;
import android.os.Vibrator;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import ru.menshovanton.gachapoint.calendar.CalendarGrid;
import ru.menshovanton.gachapoint.db.AppDatabase;
import ru.menshovanton.gachapoint.enums.GameType;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.adapters.PillsAdapter;
import ru.menshovanton.gachapoint.helpers.CalendarHelper;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.utils.Notification;
import ru.menshovanton.gachapoint.R;

public class TrackerFragment extends Fragment {

    private MainActivity mainActivity;
    private TextView subsCounterView;

    private Button checkButton;
    private ImageButton moreButton;

    private View view;

    private int selectedMonth;
    private int selectedYear;

    private CalendarHelper calendarHelper;

    private CalendarGrid calendarGrid;

    private PillsAdapter pillsAdapter;
    private LinearLayoutManager layoutManager;

    private int toDayOfYear;

    private final ActivityResultLauncher<String> exportDbLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
                if (uri != null) {
                    writeDatabaseToUri(uri);
                }
            });

    private final List<TextView> cellViewsPool = new ArrayList<>();

    public TrackerFragment() {}

    public static TrackerFragment newInstance() {
        return new TrackerFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        calendarHelper = new CalendarHelper(mainActivity);

        toDayOfYear = LocalDate.now().getDayOfYear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_tracker, container, false);

        subsCounterView = view.findViewById(R.id.subsCount);
        calendarGrid = view.findViewById(R.id.calendarGrid);
        checkButton = view.findViewById(R.id.checkButton);
        moreButton = view.findViewById(R.id.moreButton);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initCalendarGrid();

        checkButton.setOnClickListener(this::onCheckClick);
        moreButton.setOnClickListener(this::showMoreMenu);

        calendarGrid.setOnSwipeListener(new CalendarGrid.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                nextMonth();
            }

            @Override
            public void onSwipeRight() {
                previousMonth();
            }
        });

        RecyclerView gameTypeChanger = view.findViewById(R.id.gameTypeChangerTracker);

        List<String> categories = Arrays.asList(
                getString(R.string.genshin),
                getString(R.string.hsr),
                getString(R.string.zzz)
        );

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        gameTypeChanger.setLayoutManager(layoutManager);

        pillsAdapter = new PillsAdapter(categories, (item, position) -> {
            mainActivity.setSubType(GameType.fromCode(position));
            refresh(view);
        });

        gameTypeChanger.setAdapter(pillsAdapter);
        selectAndScrollIfNeeded(mainActivity.getSubType().getCode());

        refresh(view);
    }

    private void selectAndScrollIfNeeded(int targetPosition) {
        if (pillsAdapter == null || layoutManager == null) return;

        pillsAdapter.setSelectedPosition(targetPosition);

        int firstCompletelyVisible = layoutManager.findFirstCompletelyVisibleItemPosition();
        int lastCompletelyVisible = layoutManager.findLastCompletelyVisibleItemPosition();

        boolean isNotFullyVisible = targetPosition < firstCompletelyVisible || targetPosition > lastCompletelyVisible;

        if (isNotFullyVisible) {
            int offsetPx = (int) (16 * getResources().getDisplayMetrics().density);
            layoutManager.scrollToPositionWithOffset(targetPosition, offsetPx);
        }
    }

    void refresh(View view) {
        if (!isAdded() || view == null) return;

        // 1. Убрано пересоздание `calendarHelper = new CalendarHelper(...)`

        // 2. Мгновенно обновляем месяц, год, иконки и темы в основном потоке UI
        selectedMonth = LocalDate.now().getMonth().getValue();
        selectedYear = calendarHelper.getYear();
        setHeader(selectedMonth, selectedYear);

        ImageView gemIcon = view.findViewById(R.id.gemIcon);
        TextView subsCountTitle = view.findViewById(R.id.subsCountHeader);
        ImageView wishIcon = view.findViewById(R.id.wishIcon);

        switch (mainActivity.getSubType()) {
            case GENSHIN:
                gemIcon.setImageResource(R.drawable.icon_primogem);
                wishIcon.setImageResource(R.drawable.icon_intertwined_fate);
                subsCountTitle.setText(R.string.blessing_of_the_welkin_moon_count_header);
                break;
            case HSR:
                gemIcon.setImageResource(R.drawable.icon_stellar_jade);
                wishIcon.setImageResource(R.drawable.icon_star_rail_special_pass);
                subsCountTitle.setText(R.string.star_rail_special_pass_count_header);
                break;
            case ZZZ:
                gemIcon.setImageResource(R.drawable.icon_polychrome);
                wishIcon.setImageResource(R.drawable.icon_encrypted_master_tape);
                subsCountTitle.setText(R.string.inter_knot_member_count_header);
                break;
        }

        calendarHelper.adjustCellSizes(this);

        // 3. Асинхронно запрашиваем данные из БД и обновляем сетку и статистику
        calendarHelper.init(() -> {
            if (!isAdded() || getView() == null) return;

            Notification.subsCount = calendarHelper.getSubsCount();
            subsCounterView.setText(String.valueOf(calendarHelper.getSubsCount()));

            calendarHelper.renderCalendar(this);
            setStatistics();
        });
    }

    public void setStatistics() {
        if (!isAdded() || getView() == null) return;

        calendarHelper.getStatistic(statistic -> {
            if (!isAdded() || getView() == null) return;

            int laterPrimogemsCount = statistic.laterGems;
            int laterWishesCount = statistic.laterWishes;

            String laterPrimogemsText = "0";
            String laterWishesText = "0";
            String claimPrimogemsText = String.valueOf(statistic.claimGems);
            String missedPrimogemsText = String.valueOf(statistic.missedGems);
            String claimWishesText = String.valueOf(statistic.claimWishes);
            String missedWishesText = String.valueOf(statistic.missedWishes);

            View currentView = getView();
            TextView claimPrimogems = currentView.findViewById(R.id.cliamsGemsCounter);
            claimPrimogems.setText(claimPrimogemsText);

            TextView missedPrimogems = currentView.findViewById(R.id.missGemsCounter);
            missedPrimogems.setText(missedPrimogemsText);

            TextView claimWishes = currentView.findViewById(R.id.claimWishesCounter);
            claimWishes.setText(claimWishesText);

            TextView missedWishes = currentView.findViewById(R.id.missWishesCounter);
            missedWishes.setText(missedWishesText);

            TextView laterPrimogems = currentView.findViewById(R.id.laterGemsCounter);
            if (laterPrimogemsCount > 0 && calendarHelper.getClaimsDays() > 0) {
                laterPrimogemsText = String.valueOf(laterPrimogemsCount);
            }
            laterPrimogems.setText(laterPrimogemsText);

            TextView laterWishes = currentView.findViewById(R.id.laterWishesCounter);
            if (laterPrimogemsCount > 0 && calendarHelper.getClaimsDays() > 0) {
                laterWishesText = String.valueOf(laterWishesCount);
            }
            laterWishes.setText(laterWishesText);
        });
    }

    public void updateCalendar() {
        setStatistics();
        calendarHelper.renderCalendar(this);
        setHeader(selectedMonth, calendarHelper.getYear());
    }

    @SuppressLint("SetTextI18n")
    public void setHeader(int month, int year) {
        if (!isAdded() || getView() == null) return;

        TextView monthHeader = getView().findViewById(R.id.monthHeader);
        Month monthObj = Month.of(month);

        TextView yearHeader = getView().findViewById(R.id.yearHeader);

        Locale locale = LocaleList.getDefault().get(0);
        String printMonth = monthObj.getDisplayName(TextStyle.FULL_STANDALONE, locale);

        monthHeader.setText(printMonth.substring(0, 1).toUpperCase() + printMonth.substring(1));
        yearHeader.setText(String.valueOf(year));
    }

    public void onAddClick() {
        if (calendarHelper.getSubsCount() <= 6) {
            calendarHelper.getDaySubDaysRemaining(toDayOfYear, remaining -> {
                if (remaining == 0) {
                    calendarHelper.setSubsCount(1);
                    calendarHelper.setMissesDays(0);
                    calendarHelper.setClaimsDays(0);

                    calendarHelper.setDaySubDaysRemaining(toDayOfYear, 30, () -> calendarHelper.updateSubscribeDays(CalendarHelper.UpdateSubscribeDaysActions.Add, () -> {
                        if (!isAdded()) return;
                        Toast.makeText(mainActivity, getString(R.string.add_sub), Toast.LENGTH_SHORT).show();
                        check();
                        Notification.subsCount = calendarHelper.getSubsCount();
                        refresh(view);
                    }));
                } else {
                    calendarHelper.addSub();
                    calendarHelper.addDaySubDaysRemaining(toDayOfYear, 30, () -> calendarHelper.updateSubscribeDays(CalendarHelper.UpdateSubscribeDaysActions.Add, () -> {
                        if (!isAdded()) return;
                        Toast.makeText(mainActivity, getString(R.string.add_sub), Toast.LENGTH_SHORT).show();
                        check();
                        Notification.subsCount = calendarHelper.getSubsCount();
                        refresh(view);
                    }));
                }
            });
        } else {
            Toast.makeText(mainActivity, getString(R.string.subs_limit), Toast.LENGTH_SHORT).show();
        }
    }

    public void onDelClick() {
        calendarHelper.getDaySubDaysRemaining(toDayOfYear, remaining -> {
            if (remaining == 30) {
                if (calendarHelper.getSubsCount() > 0) {
                    cancelCheck(() -> {
                        calendarHelper.delSub();
                        calendarHelper.subtractDaySubDaysRemaining(toDayOfYear, 30, () -> calendarHelper.updateSubscribeDays(CalendarHelper.UpdateSubscribeDaysActions.Delete, () -> {
                            if (!isAdded()) return;
                            updateCalendar();
                            Toast.makeText(mainActivity, getString(R.string.del_sub), Toast.LENGTH_SHORT).show();
                            Notification.subsCount = calendarHelper.getSubsCount();
                            subsCounterView.setText(String.valueOf(calendarHelper.getSubsCount()));
                        }));
                    });
                } else {
                    Toast.makeText(mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mainActivity, getString(R.string.impossible_cancel_sub), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void check() {
        calendarHelper.setDayStatus(toDayOfYear, 1, () -> {
            calendarHelper.addClaimDay();
            updateCalendar();
        });
    }

    public void onCheckClick(View view) {
        calendarHelper.getDayStatus(toDayOfYear, status -> {
            if (status == 1) {
                Toast.makeText(getActivity(), getString(R.string.already_cheked), Toast.LENGTH_SHORT).show();
            } else {
                calendarHelper.getDaySubDaysRemaining(toDayOfYear, remaining -> {
                    if (remaining == 0) {
                        Toast.makeText(getActivity(), getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();
                    } else {
                        check();
                        Toast.makeText(getActivity(), getString(R.string.check_today), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        Vibrator vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(100);
        }
    }

    private void showMoreMenu(View view) {
        ((MainActivity) requireActivity()).showCalendarMenu(new MainActivity.OnCalendarMenuClickListener() {
            @Override public void onAdd() { onAddClick(); }
            @Override public void onDel() { onDelClick(); }
            @Override public void onExport() { exportDatabase(); }
            @Override public void onRecovery() { recoveryMissDay(); }
            @Override public void onCancel() { onCancelCheck(); }
        });
    }

    public void recoveryMissDay() {
        int yesterday = toDayOfYear - 1;
        if (yesterday >= 1) {
            calendarHelper.getDayStatus(yesterday, status -> {
                if (status == 1) {
                    Toast.makeText(mainActivity, getString(R.string.not_miss_day), Toast.LENGTH_SHORT).show();
                } else {
                    calendarHelper.getDaySubDaysRemaining(yesterday, remaining -> {
                        if (remaining == 0) {
                            Toast.makeText(mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();
                        } else {
                            calendarHelper.setDayStatus(yesterday, 1, () -> {
                                Toast.makeText(mainActivity, getString(R.string.check_today), Toast.LENGTH_SHORT).show();
                                calendarHelper.addClaimDay();
                                updateCalendar();
                            });
                        }
                    });
                }
            });
        } else {
            updateCalendar();
        }
    }

    public void cancelCheck(Runnable onComplete) {
        calendarHelper.setDayStatus(toDayOfYear, 0, () -> {
            calendarHelper.subtractClaimDay();
            if (onComplete != null) onComplete.run();
        });
    }

    public void cancelCheck() {
        cancelCheck(this::updateCalendar);
    }

    public void onCancelCheck() {
        calendarHelper.getDayStatus(toDayOfYear, status -> {
            if (status == 0) {
                Toast.makeText(mainActivity, getString(R.string.not_check_today), Toast.LENGTH_SHORT).show();
            } else {
                calendarHelper.getDaySubDaysRemaining(toDayOfYear, remaining -> {
                    if (remaining == 0) {
                        Toast.makeText(mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();
                    } else {
                        cancelCheck(() -> {
                            Toast.makeText(mainActivity, getString(R.string.cancel_check_today), Toast.LENGTH_SHORT).show();
                            updateCalendar();
                        });
                    }
                });
            }
        });
    }

    private void initCalendarGrid() {
        if (!cellViewsPool.isEmpty() && calendarGrid.getChildCount() == 42) {
            return;
        }

        calendarGrid.removeAllViews();
        cellViewsPool.clear();

        float density = getResources().getDisplayMetrics().density;
        int cellHeightInPx = (int) (48 * density);
        int marginInPx = (int) (4 * density);

        Typeface customTypeface = ResourcesCompat.getFont(requireContext(), R.font.genshin_font);

        for (int i = 0; i < 42; i++) {
            TextView dayView = new TextView(getContext());

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = cellHeightInPx;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

            params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
            dayView.setLayoutParams(params);

            dayView.setGravity(Gravity.CENTER);
            dayView.setTextSize(18f);

            if (customTypeface != null) {
                dayView.setTypeface(customTypeface);
            }

            calendarGrid.addView(dayView);
            cellViewsPool.add(dayView);
        }
    }

    public List<TextView> getCellViewsPool() {
        return cellViewsPool;
    }

    public GridLayout getCalendarGrid() {
        return calendarGrid;
    }

    private void nextMonth() {
        if (selectedMonth == 12) {
            selectedMonth = 1;
            calendarHelper.addYear();
        } else {
            selectedMonth++;
        }
        selectedYear = calendarHelper.getYear();
        calendarHelper.renderCalendar(this);
        setHeader(selectedMonth, selectedYear);
    }

    private void previousMonth() {
        if (selectedMonth == 1) {
            selectedMonth = 12;
            calendarHelper.subtractYear();
        } else {
            selectedMonth--;
        }
        selectedYear = calendarHelper.getYear();
        calendarHelper.renderCalendar(this);
        setHeader(selectedMonth, selectedYear);
    }

    public void exportDatabase() {
        exportDbLauncher.launch(DatabaseHelper.DATABASE_NAME);
    }

    private void writeDatabaseToUri(Uri targetUri) {
        if (!isAdded()) return;

        Context context = requireContext();

        try {
            AppDatabase.getInstance(context)
                    .getOpenHelper()
                    .getWritableDatabase()
                    .query("PRAGMA wal_checkpoint(TRUNCATE)")
                    .close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File dbFile = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);

        if (!dbFile.exists()) {
            Toast.makeText(context, R.string.db_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = context.getContentResolver().openOutputStream(targetUri)) {

            if (out == null) {
                Toast.makeText(context, R.string.db_export_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();

            Toast.makeText(context, R.string.db_export_successful, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.db_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public int getSelectedMonth() {
        return selectedMonth;
    }

    public int getSelectedYear() {
        return selectedYear;
    }
}