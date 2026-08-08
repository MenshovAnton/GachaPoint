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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import ru.menshovanton.gachapoint.CalendarGrid;
import ru.menshovanton.gachapoint.Statistic;
import ru.menshovanton.gachapoint.helpers.CalendarHelper;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.Notification;
import ru.menshovanton.gachapoint.helpers.PiggyBankHelper;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;
import ru.menshovanton.gachapoint.R;

public class TrackerFragment extends Fragment {

    MainActivity mainActivity;
    PiggyBankHelper piggyBankHelper;
    TextView subsCounterView;
    ConstraintLayout constraintLayout;
    HorizontalScrollView subTypesLine;

    ImageView previousMonthButton;
    ImageView nextMonthButton;

    Button checkButton;
    ImageButton moreButton;

    MaterialButton blessingOfTheWelkinMoonSelectButton;
    MaterialButton starRailSpecialPassSelectButton;
    MaterialButton interKnotMembershipSelectButton;

    TextView tagMonday;
    TextView tagTuesday;
    TextView tagWednesday;
    TextView tagThursday;
    TextView tagFriday;
    TextView tagSaturday;
    TextView tagSunday;


    public TrackerFragment instance;
    PreferencesHelper settings;

    View view;

    public static int selectedMonth;
    public static int selectedYear;

    public static CalendarHelper calendarHelper;
    DatabaseHelper dbHelper;

    CalendarGrid calendarGrid;

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
        piggyBankHelper = new PiggyBankHelper(mainActivity);
        instance = this;
        settings = new PreferencesHelper(Objects.requireNonNull(mainActivity));

        String toDayMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru"));
        toDayMonth.substring(0, 1).toUpperCase();

        selectedMonth = LocalDate.now().getMonth().getValue();
        selectedYear = LocalDate.now().getYear();

        calendarHelper = new CalendarHelper(mainActivity);
        dbHelper = new DatabaseHelper(getContext());

        Notification.subsCount = calendarHelper.subsCount;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_tracker, container, false);

        subsCounterView = view.findViewById(R.id.subsCount);
        calendarGrid = view.findViewById(R.id.calendarGrid);
        checkButton = view.findViewById(R.id.checkButton);
        moreButton = view.findViewById(R.id.moreButton);
        previousMonthButton = view.findViewById(R.id.previousMonth);
        nextMonthButton = view.findViewById(R.id.nextMonth);
        blessingOfTheWelkinMoonSelectButton = view.findViewById(R.id.blessingOfTheWelkinMoonSelectButton);
        starRailSpecialPassSelectButton = view.findViewById(R.id.starRailSpecialPassSelectButton);
        interKnotMembershipSelectButton = view.findViewById(R.id.interKnotMembershipSelectButton);
        subTypesLine = view.findViewById(R.id.subTypesLine);
        tagMonday = view.findViewById(R.id.tagMonday);
        tagTuesday = view.findViewById(R.id.tagTuesday);
        tagWednesday = view.findViewById(R.id.tagWednesday);
        tagThursday = view.findViewById(R.id.tagThursday);
        tagFriday = view.findViewById(R.id.tagFriday);
        tagSaturday = view.findViewById(R.id.tagSaturday);
        tagSunday = view.findViewById(R.id.tagSunday);

        subTypesLine.post(() -> subTypesLine.scrollTo(MainActivity.subTypeScrollX, 0));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subsCounterView.setText(String.valueOf(calendarHelper.subsCount));

        initCalendarGrid();
        calendarHelper.adjustCellSizes(this);
        calendarHelper.renderCalendar(this);

        setHeader(selectedMonth, calendarHelper.year);
        setStatistics();

        checkButton.setOnClickListener(this::onCheckClick);
        moreButton.setOnClickListener(this::showMoreMenu);
        previousMonthButton.setOnClickListener(this::onPreviousMonthClick);
        nextMonthButton.setOnClickListener(this::onNextMonthClick);
        blessingOfTheWelkinMoonSelectButton.setOnClickListener(this::onMoonClick);
        starRailSpecialPassSelectButton.setOnClickListener(this::onPassClick);
        interKnotMembershipSelectButton.setOnClickListener(this::onInterknotClick);

        subTypesLine.setOnScrollChangeListener(this::onScrollSubTypes);

        ImageView gemIcon = view.findViewById(R.id.gemIcon);
        TextView subsCountTitle = view.findViewById(R.id.subsCountHeader);
        ImageView wishIcon = view.findViewById(R.id.wishIcon);

        switch (MainActivity.subType) {
            case 0:
                gemIcon.setImageResource(R.drawable.primogem);
                wishIcon.setImageResource(R.drawable.intertwined_fate);
                subsCountTitle.setText(R.string.blessing_of_the_welkin_moon_count_header);
                changeCheckedTab(blessingOfTheWelkinMoonSelectButton);
                break;
            case 1:
                gemIcon.setImageResource(R.drawable.stellar_jade);
                wishIcon.setImageResource(R.drawable.star_rail_special_pass);
                subsCountTitle.setText(R.string.star_rail_special_pass_count_header);
                changeCheckedTab(starRailSpecialPassSelectButton);
                break;
            case 2:
                gemIcon.setImageResource(R.drawable.polychrome);
                wishIcon.setImageResource(R.drawable.encrypted_master_tape);
                subsCountTitle.setText(R.string.inter_knot_member_count_header);
                changeCheckedTab(interKnotMembershipSelectButton);
                break;
        }

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
        if (laterPrimogemsCount > 0 && calendarHelper.claimsDays > 0)
        {   laterPrimogemsText = String.valueOf(laterPrimogemsCount);   }
        laterPrimogems.setText(laterPrimogemsText);

        TextView laterWishes = getView().findViewById(R.id.laterWishesCounter);
        if (laterPrimogemsCount > 0 && calendarHelper.claimsDays > 0)
        {   laterWishesText = String.valueOf(laterWishesCount); }
        laterWishes.setText(laterWishesText);
    }

    public void updateCalendar() {
        calendarHelper.update();
        setStatistics();
        selectedMonth = LocalDate.now().getMonth().getValue();
        calendarHelper.renderCalendar(this);
        setHeader(selectedMonth, calendarHelper.year);
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
        if (calendarHelper.subsCount <= 6) {
            if (calendarHelper.calendar.getSubDaysRemaining(calendarHelper.toDayOfYear) == 0)
            {
                calendarHelper.subsCount = 1;
                for (int i = 0; i < calendarHelper.toDayOfYear - 1; i++) {
                    if (calendarHelper.calendar.datesArray[i].status == 1) {
                        calendarHelper.calendar.datesArray[i].status = 3;
                    }
                    if (calendarHelper.calendar.datesArray[i].status == 0 && calendarHelper.calendar.datesArray[i].subDaysRemaining > 0) {
                        calendarHelper.calendar.datesArray[i].status = 2;
                    }
                }

                calendarHelper.missesDays = 0;
                calendarHelper.claimsDays = 0;

                calendarHelper.calendar.datesArray[calendarHelper.toDayOfYear - 1].subDaysRemaining = 30;
            }
            else
            {
                calendarHelper.subsCount++;
                calendarHelper.calendar.datesArray[calendarHelper.toDayOfYear - 1].subDaysRemaining += 30;
            }

            calendarHelper.updateSubscribeDays(CalendarHelper.UpdateSubscribeDaysActions.Add);
            Toast.makeText(mainActivity, getString(R.string.add_sub), Toast.LENGTH_SHORT).show();
            check();
            updateCalendar();

            Notification.subsCount = calendarHelper.subsCount;

            mainActivity.updateFragmentWithoutAnimation(TrackerFragment.newInstance(), mainActivity.TRACKER_TAG);
        }
        else
        {   Toast.makeText(mainActivity, getString(R.string.subs_limit), Toast.LENGTH_SHORT).show(); }
        subsCounterView.setText(String.valueOf(calendarHelper.subsCount));
    }

    public void onDelClick() {
        if (calendarHelper.calendar.datesArray[calendarHelper.toDayOfYear - 1].subDaysRemaining == 30) {
            if (calendarHelper.subsCount > 0) {
                cancelCheck();

                calendarHelper.subsCount--;
                calendarHelper.calendar.datesArray[calendarHelper.toDayOfYear - 1].subDaysRemaining -= 30;

                calendarHelper.updateSubscribeDays(CalendarHelper.UpdateSubscribeDaysActions.Delete);
                updateCalendar();
                Toast.makeText(mainActivity, getString(R.string.del_sub), Toast.LENGTH_SHORT).show();

                Notification.subsCount = calendarHelper.subsCount;
            }
            else
            {   Toast.makeText(mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show(); }
            subsCounterView.setText(String.valueOf(calendarHelper.subsCount));
        } else {
            Toast.makeText(mainActivity, getString(R.string.impossible_cancel_sub), Toast.LENGTH_SHORT).show();
        }
    }

    public void check() {
        calendarHelper.calendar.datesArray[calendarHelper.toDayOfYear - 1].status = 1;
        calendarHelper.claimsDays++;
        updateCalendar();
    }

    public void onCheckClick(View view)
    {
        if (calendarHelper.calendar.getStatus(calendarHelper.toDayOfYear) == 1)
        {   Toast.makeText(getActivity(), getString(R.string.already_cheked), Toast.LENGTH_SHORT).show();  }
        else if (calendarHelper.calendar.getSubDaysRemaining(calendarHelper.toDayOfYear) == 0)
        {   Toast.makeText(getActivity(), getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();    }
        else
        {
            check();
            Toast.makeText(getActivity(), getString(R.string.check_today), Toast.LENGTH_SHORT).show();
        }

        Vibrator vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    public void onPreviousMonthClick(View view) {
        previousMonth();
    }

    public void onNextMonthClick(View view) {
        nextMonth();
    }

    private void onMoonClick(View view) {
        MainActivity.subType = 0;
        changeCheckedTab(blessingOfTheWelkinMoonSelectButton);
        mainActivity.updateFragmentWithoutAnimation(TrackerFragment.newInstance(), mainActivity.TRACKER_TAG);
    }

    private void onPassClick(View view) {
        MainActivity.subType = 1;
        changeCheckedTab(starRailSpecialPassSelectButton);
        mainActivity.updateFragmentWithoutAnimation(TrackerFragment.newInstance(), mainActivity.TRACKER_TAG);
    }

    private void onInterknotClick(View view) {
        MainActivity.subType = 2;
        changeCheckedTab(interKnotMembershipSelectButton);
        mainActivity.updateFragmentWithoutAnimation(TrackerFragment.newInstance(), mainActivity.TRACKER_TAG);
    }

    private void changeCheckedTab(MaterialButton view) {
        blessingOfTheWelkinMoonSelectButton.setStrokeColorResource(R.color.accent);
        starRailSpecialPassSelectButton.setStrokeColorResource(R.color.accent);
        interKnotMembershipSelectButton.setStrokeColorResource(R.color.accent);

        view.setStrokeColorResource(R.color.check);
    }

    private void onScrollSubTypes(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        MainActivity.subTypeScrollX = scrollX;
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
        if (calendarHelper.calendar.getStatus(calendarHelper.toDayOfYear - 1) == 1)
        {   Toast.makeText(mainActivity, getString(R.string.not_miss_day), Toast.LENGTH_SHORT).show();  }
        else if (calendarHelper.calendar.getSubDaysRemaining(calendarHelper.toDayOfYear - 1) == 0)
        {   Toast.makeText(mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();    }
        else
        {
            calendarHelper.calendar.datesArray[calendarHelper.toDayOfYear - 2].status = 1;
            Toast.makeText(mainActivity, getString(R.string.check_today), Toast.LENGTH_SHORT).show();
            calendarHelper.claimsDays++;
        }

        updateCalendar();
    }

    public void cancelCheck() {
        calendarHelper.calendar.datesArray[calendarHelper.toDayOfYear - 1].status = 0;
        calendarHelper.claimsDays--;
    }

    public void onCancelCheck() {
        if (calendarHelper.calendar.getStatus(calendarHelper.toDayOfYear) == 0)
        {   Toast.makeText(mainActivity, getString(R.string.not_check_today), Toast.LENGTH_SHORT).show();  }
        else if (calendarHelper.calendar.getSubDaysRemaining(calendarHelper.toDayOfYear) == 0)
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
            calendarHelper.year++;
        } else {
            selectedMonth++;
        }
        calendarHelper.renderCalendar(this);
        setHeader(selectedMonth, selectedYear);
    }

    private void previousMonth() {
        if (selectedMonth == 1) {
            selectedMonth = 12;
            calendarHelper.year--;
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
}