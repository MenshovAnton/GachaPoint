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

import ru.menshovanton.gachapoint.CalendarGrid;
import ru.menshovanton.gachapoint.Statistic;
import ru.menshovanton.gachapoint.adapters.PillsAdapter;
import ru.menshovanton.gachapoint.helpers.CalendarHelper;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.Notification;
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

        Notification.subsCount = calendarHelper.getSubsCount();

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
            mainActivity.setSubType(position);
            refresh(view);
        });

        gameTypeChanger.setAdapter(pillsAdapter);
        selectAndScrollIfNeeded(mainActivity.getSubType());

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
        calendarHelper = new CalendarHelper(mainActivity);

        String toDayMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru"));
        toDayMonth.substring(0, 1).toUpperCase();

        selectedMonth = LocalDate.now().getMonth().getValue();
        selectedYear = LocalDate.now().getYear();

        initCalendarGrid();
        calendarHelper.adjustCellSizes(this);
        calendarHelper.renderCalendar(this);

        setHeader(selectedMonth, calendarHelper.getYear());
        setStatistics();

        subsCounterView.setText(String.valueOf(calendarHelper.getSubsCount()));

        ImageView gemIcon = view.findViewById(R.id.gemIcon);
        TextView subsCountTitle = view.findViewById(R.id.subsCountHeader);
        ImageView wishIcon = view.findViewById(R.id.wishIcon);

        switch (mainActivity.getSubType()) {
            case 0:
                gemIcon.setImageResource(R.drawable.primogem);
                wishIcon.setImageResource(R.drawable.intertwined_fate);
                subsCountTitle.setText(R.string.blessing_of_the_welkin_moon_count_header);
                break;
            case 1:
                gemIcon.setImageResource(R.drawable.stellar_jade);
                wishIcon.setImageResource(R.drawable.star_rail_special_pass);
                subsCountTitle.setText(R.string.star_rail_special_pass_count_header);
                break;
            case 2:
                gemIcon.setImageResource(R.drawable.polychrome);
                wishIcon.setImageResource(R.drawable.encrypted_master_tape);
                subsCountTitle.setText(R.string.inter_knot_member_count_header);
                break;
        }
    }

    public void setStatistics() {
        Statistic statistic = calendarHelper.getStatistic();

        int laterPrimogemsCount = statistic.laterGems;
        int laterWishesCount = statistic.laterWishes;

        String laterPrimogemsText = "0";
        String laterWishesText = "0";
        String claimPrimogemsText = String.valueOf(statistic.claimGems);
        String missedPrimogemsText = String.valueOf(statistic.missedGems);
        String claimWishesText = String.valueOf(statistic.claimWishes);
        String missedWishesText = String.valueOf(statistic.missedWishes);

        assert getView() != null;
        TextView claimPrimogems = getView().findViewById(R.id.cliamsGemsCounter);
        claimPrimogems.setText(claimPrimogemsText);

        TextView missedPrimogems = getView().findViewById(R.id.missGemsCounter);
        missedPrimogems.setText(missedPrimogemsText);

        TextView claimWishes = getView().findViewById(R.id.claimWishesCounter);
        claimWishes.setText(claimWishesText);

        TextView missedWishes = getView().findViewById(R.id.missWishesCounter);
        missedWishes.setText(missedWishesText);

        TextView laterPrimogems = getView().findViewById(R.id.laterGemsCounter);
        if (laterPrimogemsCount > 0 && calendarHelper.getClaimsDays() > 0)
        {   laterPrimogemsText = String.valueOf(laterPrimogemsCount);   }
        laterPrimogems.setText(laterPrimogemsText);

        TextView laterWishes = getView().findViewById(R.id.laterWishesCounter);
        if (laterPrimogemsCount > 0 && calendarHelper.getClaimsDays() > 0)
        {   laterWishesText = String.valueOf(laterWishesCount); }
        laterWishes.setText(laterWishesText);
    }

    public void updateCalendar() {
        calendarHelper.update();
        setStatistics();
        selectedMonth = LocalDate.now().getMonth().getValue();
        calendarHelper.renderCalendar(this);
        setHeader(selectedMonth, calendarHelper.getYear());
    }

    @SuppressLint("SetTextI18n")
    public void setHeader(int month, int year) {
        assert getView() != null;
        TextView monthHeader = getView().findViewById(R.id.monthHeader);
        Month monthObj = Month.of(month);

        TextView yearHeader = getView().findViewById(R.id.yearHeader);

        Locale locale = LocaleList.getDefault().get(0);
        String printMonth = monthObj.getDisplayName(TextStyle.FULL_STANDALONE, locale);

        monthHeader.setText(printMonth.substring(0, 1).toUpperCase() + printMonth.substring(1));
        yearHeader.setText(String.valueOf(year));
    }

    public void onAddClick()
    {
        if (calendarHelper.getSubsCount() <= 6) {
            if (calendarHelper.getDaySubDaysRemaining(toDayOfYear - 1) == 0)
            {
                calendarHelper.setSubsCount(1);
                for (int i = 0; i < toDayOfYear - 1; i++) {
                    int dayStatus = calendarHelper.getDayStatus(i);
                    if (dayStatus == 1) {
                        calendarHelper.setDayStatus(i, 3);
                    }
                    if (dayStatus == 0 && calendarHelper.getDaySubDaysRemaining(i) > 0) {
                        calendarHelper.setDayStatus(i, 2);
                    }
                }

                calendarHelper.setMissesDays(0);
                calendarHelper.setClaimsDays(0);

                calendarHelper.setDaySubDaysRemaining(toDayOfYear - 1, 30);
            }
            else
            {
                calendarHelper.addSub();
                calendarHelper.addDaySubDaysRemaining(toDayOfYear - 1, 30);
            }

            calendarHelper.updateSubscribeDays(CalendarHelper.UpdateSubscribeDaysActions.Add);
            Toast.makeText(mainActivity, getString(R.string.add_sub), Toast.LENGTH_SHORT).show();
            check();
            updateCalendar();

            Notification.subsCount = calendarHelper.getSubsCount();

            refresh(view);
        }
        else
        {   Toast.makeText(mainActivity, getString(R.string.subs_limit), Toast.LENGTH_SHORT).show(); }
        subsCounterView.setText(String.valueOf(calendarHelper.getSubsCount()));
    }

    public void onDelClick() {
        if (calendarHelper.getDaySubDaysRemaining(toDayOfYear - 1) == 30) {
            if (calendarHelper.getSubsCount() > 0) {
                cancelCheck();

                calendarHelper.delSub();
                calendarHelper.subtractDaySubDaysRemaining(toDayOfYear - 1, 30);

                calendarHelper.updateSubscribeDays(CalendarHelper.UpdateSubscribeDaysActions.Delete);
                updateCalendar();
                Toast.makeText(mainActivity, getString(R.string.del_sub), Toast.LENGTH_SHORT).show();

                Notification.subsCount = calendarHelper.getSubsCount();
            }
            else
            {   Toast.makeText(mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show(); }
            subsCounterView.setText(String.valueOf(calendarHelper.getSubsCount()));
        } else {
            Toast.makeText(mainActivity, getString(R.string.impossible_cancel_sub), Toast.LENGTH_SHORT).show();
        }
    }

    public void check() {
        calendarHelper.setDayStatus(toDayOfYear - 1, 1);
        calendarHelper.addClaimDay();
        updateCalendar();
    }

    public void onCheckClick(View view)
    {
        if (calendarHelper.getDayStatus(toDayOfYear - 1) == 1)
        {   Toast.makeText(getActivity(), getString(R.string.already_cheked), Toast.LENGTH_SHORT).show();  }
        else if (calendarHelper.getDaySubDaysRemaining(toDayOfYear - 1) == 0)
        {   Toast.makeText(getActivity(), getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();    }
        else
        {
            check();
            Toast.makeText(getActivity(), getString(R.string.check_today), Toast.LENGTH_SHORT).show();
        }

        Vibrator vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(100);
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
        if (calendarHelper.getDayStatus(toDayOfYear - 2) == 1)
        {   Toast.makeText(mainActivity, getString(R.string.not_miss_day), Toast.LENGTH_SHORT).show();  }
        else if (calendarHelper.getDaySubDaysRemaining(toDayOfYear - 2) == 0)
        {   Toast.makeText(mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();    }
        else
        {
            calendarHelper.setDayStatus(toDayOfYear - 2, 1);
            Toast.makeText(mainActivity, getString(R.string.check_today), Toast.LENGTH_SHORT).show();
            calendarHelper.addClaimDay();
        }

        updateCalendar();
    }

    public void cancelCheck() {
        calendarHelper.setDayStatus(toDayOfYear - 1, 0);
        calendarHelper.subtractClaimDay();
    }

    public void onCancelCheck() {
        if (calendarHelper.getDayStatus(toDayOfYear - 1) == 0)
        {   Toast.makeText(mainActivity, getString(R.string.not_check_today), Toast.LENGTH_SHORT).show();  }
        else if (calendarHelper.getDaySubDaysRemaining(toDayOfYear - 1) == 0)
        {   Toast.makeText(mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();    }
        else
        {
            cancelCheck();
            Toast.makeText(mainActivity, getString(R.string.cancel_check_today), Toast.LENGTH_SHORT).show();
        }

        updateCalendar();
    }

    private void initCalendarGrid() {
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
        calendarHelper.renderCalendar(this);
        setHeader(selectedMonth, selectedYear);
    }

    public void exportDatabase() {
        exportDbLauncher.launch(DatabaseHelper.DATABASE_NAME);
    }

    private void writeDatabaseToUri(Uri targetUri) {
        if (!isAdded()) return;

        File dbFile = requireContext().getDatabasePath(DatabaseHelper.DATABASE_NAME);

        if (!dbFile.exists()) {
            Toast.makeText(requireContext(), R.string.db_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = requireContext().getContentResolver().openOutputStream(targetUri)) {

            if (out == null) {
                Toast.makeText(requireContext(), R.string.db_export_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();

            Toast.makeText(requireContext(), R.string.db_export_successful, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), R.string.db_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public int getSelectedMonth() {
        return selectedMonth;
    }
}