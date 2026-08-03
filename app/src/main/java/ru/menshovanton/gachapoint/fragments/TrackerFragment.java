package ru.menshovanton.gachapoint.fragments;

import static androidx.core.content.ContextCompat.getColor;
import static ru.menshovanton.gachapoint.Calendar.splashScreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;

import android.os.Vibrator;
import android.widget.*;
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

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import ru.menshovanton.gachapoint.Calendar;
import ru.menshovanton.gachapoint.Statistic;
import ru.menshovanton.gachapoint.helpers.CalendarHelper;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.Date;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.Notification;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;
import ru.menshovanton.gachapoint.R;

public class TrackerFragment extends Fragment {

    MainActivity mainActivity;
    TextView subsCounterView;
    static ConstraintLayout constraintLayout;
    @SuppressLint("StaticFieldLeak")
    static TableLayout tableLayout;
    HorizontalScrollView subTypesLine;

    ImageView previousMonthButton;
    ImageView nextMonthButton;

    Button checkButton;
    @SuppressLint("StaticFieldLeak")
    public static Button addButton;
    ImageButton moreButton;

    MaterialButton blessingOfTheWelkinMoonSelectButton;
    MaterialButton starRailSpecialPassSelectButton;
    MaterialButton interKnotMembershipSelectButton;

    @SuppressLint("StaticFieldLeak")
    public static TextView tagMonday;
    @SuppressLint("StaticFieldLeak")
    public static TextView tagTuesday;
    @SuppressLint("StaticFieldLeak")
    public static TextView tagWednesday;
    @SuppressLint("StaticFieldLeak")
    public static TextView tagThursday;
    @SuppressLint("StaticFieldLeak")
    public static TextView tagFriday;
    @SuppressLint("StaticFieldLeak")
    public static TextView tagSaturday;
    @SuppressLint("StaticFieldLeak")
    public static TextView tagSunday;


    @SuppressLint("StaticFieldLeak")
    public static TrackerFragment instance;
    PreferencesHelper settings;

    @SuppressLint("StaticFieldLeak")
    static View view;

    public static int selectedMonth;
    public static int selectedYear;

    public static CalendarHelper calendarHelper;


    public TrackerFragment() {}

    public static TrackerFragment newInstance() {
        return new TrackerFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = MainActivity.mainActivity;
        instance = this;
        settings = new PreferencesHelper(mainActivity);

        String toDayMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru"));
        toDayMonth.substring(0, 1).toUpperCase();

        selectedMonth = LocalDate.now().getMonth().getValue();
        selectedYear = LocalDate.now().getYear();

        calendarHelper = new CalendarHelper(splashScreen);

        Notification.subsCount = calendarHelper.subsCount;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_tracker, container, false);

        subsCounterView = view.findViewById(R.id.subsCount);
        constraintLayout = view.findViewById(R.id.calendarArea);
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

        calendarHelper.update();
        calendarHelper.drawCalendar();

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
    }

    public static void createView(Date date, TextView textView, ImageView imageView, int leftMargin, int topMargin) {
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(120, 120);
        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.topToBottom = R.id.calendarFiller;
        layoutParams.leftMargin = leftMargin;
        layoutParams.rightMargin = 0;
        layoutParams.topMargin = topMargin;
        layoutParams.horizontalBias = 0;
        layoutParams.verticalBias = 0;

        textView.setText(String.valueOf(date.dayOfMonth));
        textView.setTextSize(20);
        Typeface typeface = ResourcesCompat.getFont(MainActivity.context, R.font.genshin_font);
        textView.setTypeface(typeface);
        textView.setTextColor(getColor(MainActivity.context, R.color.white));
        textView.setGravity(Gravity.CENTER);

        if (date.dayOfYear == calendarHelper.toDayOfYear && selectedMonth == LocalDate.now().getMonth().getValue() && selectedYear == LocalDate.now().getYear()) {
            imageView.setImageResource(R.drawable.background_date_today);
        } else {
            imageView.setImageResource(R.drawable.background_date);
        }

        if (date.status == 0 && date.subDaysRemaining > 0) {
            textView.setTextColor(getColor(MainActivity.context, R.color.check));
        }

        if ((date.status == 1 || date.status == 3) && date.month == selectedMonth) {
            textView.setTextColor(getColor(MainActivity.context, R.color.checked));
        }

        if ((date.status == 0 || date.status == 2) && date.id < calendarHelper.toDayOfYear && date.subDaysRemaining != 0
                && date.month == selectedMonth) {
            textView.setTextColor(getColor(MainActivity.context, R.color.missed));
        }

        constraintLayout.addView(imageView, layoutParams);
        constraintLayout.addView(textView, layoutParams);
    }

    public void setStatistics() {
        Statistic statistic = calendarHelper.getStatistic();

        int laterPrimogemsCount = statistic.laterGemsCount;
        int laterWishesCount = statistic.laterWishesCount;

        String laterPrimogemsText = "0";
        String laterWishesText = "0";
        String claimPrimogemsText = String.valueOf(statistic.claimGemsCount);
        String missedPrimogemsText = String.valueOf(statistic.missedGemsCount);
        String claimWishesText = String.valueOf(statistic.missedWishesCount);
        String missedWishesText = String.valueOf(statistic.missedWishesCount);

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
        calendarHelper.removeCalendar(constraintLayout);
        calendarHelper.drawCalendar();
        setHeader(selectedMonth, calendarHelper.year);
    }

    @SuppressLint("SetTextI18n")
    public void setHeader(int month, int year) {
        assert getView() != null;
        TextView monthHeader = getView().findViewById(R.id.monthHeader);
        Month monthObj = Month.of(month);

        TextView yearHeader = getView().findViewById(R.id.yearHeader);

        Locale locale = Locale.forLanguageTag("ru");
        String printMonth = monthObj.getDisplayName(TextStyle.FULL_STANDALONE, locale);

        monthHeader.setText(printMonth.substring(0, 1).toUpperCase() + printMonth.substring(1));
        yearHeader.setText(String.valueOf(year));
    }

    public void onAddClick(View view)
    {
        if (calendarHelper.subsCount <= 6) {
            if (CalendarHelper.calendar.getSubDaysRemaining(calendarHelper.toDayOfYear) == 0)
            {
                calendarHelper.subsCount = 1;
                for (int i = 0; i < calendarHelper.toDayOfYear - 1; i++) {
                    if (CalendarHelper.calendar.dateArray[i].status == 1) {
                        CalendarHelper.calendar.dateArray[i].status = 3;
                    }
                    if (CalendarHelper.calendar.dateArray[i].status == 0 && CalendarHelper.calendar.dateArray[i].subDaysRemaining > 0) {
                        CalendarHelper.calendar.dateArray[i].status = 2;
                    }
                }

                calendarHelper.missesDays = 0;
                calendarHelper.claimsDays = 0;

                CalendarHelper.calendar.dateArray[calendarHelper.toDayOfYear - 1].subDaysRemaining = 30;
            }
            else
            {
                calendarHelper.subsCount++;
                CalendarHelper.calendar.dateArray[calendarHelper.toDayOfYear - 1].subDaysRemaining += 30;
            }

            calendarHelper.updateSubscribes(CalendarHelper.UpdActions.Add);
            Toast.makeText(MainActivity.mainActivity, getString(R.string.add_sub), Toast.LENGTH_SHORT).show();
            check();
            updateCalendar();

            Notification.subsCount = calendarHelper.subsCount;

            mainActivity.updateFragment(TrackerFragment.newInstance());
        }
        else
        {   Toast.makeText(MainActivity.mainActivity, getString(R.string.subs_limit), Toast.LENGTH_SHORT).show(); }
        subsCounterView.setText(String.valueOf(calendarHelper.subsCount));
        mainActivity.closeCalendarMenu();
    }

    public void onDelClick(View view) {
        if (CalendarHelper.calendar.dateArray[calendarHelper.toDayOfYear - 1].subDaysRemaining == 30) {
            if (calendarHelper.subsCount > 0) {
                cancelCheck();

                calendarHelper.subsCount--;
                CalendarHelper.calendar.dateArray[calendarHelper.toDayOfYear - 1].subDaysRemaining -= 30;

                calendarHelper.updateSubscribes(CalendarHelper.UpdActions.Delete);
                updateCalendar();
                Toast.makeText(MainActivity.mainActivity, getString(R.string.del_sub), Toast.LENGTH_SHORT).show();

                Notification.subsCount = calendarHelper.subsCount;
            }
            else
            {   Toast.makeText(MainActivity.mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show(); }
            subsCounterView.setText(String.valueOf(calendarHelper.subsCount));
        } else {
            Toast.makeText(MainActivity.mainActivity, getString(R.string.impossible_cancel_sub), Toast.LENGTH_SHORT).show();
        }
        mainActivity.closeCalendarMenu();
    }

    public void check() {
        CalendarHelper.calendar.dateArray[calendarHelper.toDayOfYear - 1].status = 1;
        calendarHelper.claimsDays++;
        updateCalendar();
    }

    public void onCheckClick(View view)
    {
        if (CalendarHelper.calendar.getStatus(calendarHelper.toDayOfYear) == 1)
        {   Toast.makeText(getActivity(), getString(R.string.already_cheked), Toast.LENGTH_SHORT).show();  }
        else if (CalendarHelper.calendar.getSubDaysRemaining(calendarHelper.toDayOfYear) == 0)
        {   Toast.makeText(getActivity(), getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();    }
        else
        {
            check();
            Toast.makeText(getActivity(), getString(R.string.check_today), Toast.LENGTH_SHORT).show();
        }

        Vibrator vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(10);
    }

    public void onPreviousMonthClick(View view) {
        if (selectedMonth != 1) {
            selectedMonth--;
        } else {
            if (selectedYear > CalendarHelper.calendar.dateArray[0].year) {
                selectedMonth = 12;
                selectedYear--;
            }
        }
        calendarHelper.removeCalendar(constraintLayout);
        calendarHelper.drawCalendar();
        setHeader(selectedMonth, selectedYear);
    }

    public void onNextMonthClick(View view) {
        if (selectedMonth != 12) {
            selectedMonth++;
        } else {
            if (selectedYear < CalendarHelper.calendar.dateArray[Calendar.calendarSize - 1].year) {
                selectedMonth = 1;
                selectedYear++;
            }
        }
        calendarHelper.removeCalendar(constraintLayout);
        calendarHelper.drawCalendar();
        setHeader(selectedMonth, selectedYear);
    }

    private void onMoonClick(View view) {
        MainActivity.subType = 0;
        changeCheckedTab(blessingOfTheWelkinMoonSelectButton);
        mainActivity.updateFragment(TrackerFragment.newInstance());
    }

    private void onPassClick(View view) {
        MainActivity.subType = 1;
        changeCheckedTab(starRailSpecialPassSelectButton);
        mainActivity.updateFragment(TrackerFragment.newInstance());
    }

    private void onInterknotClick(View view) {
        MainActivity.subType = 2;
        changeCheckedTab(interKnotMembershipSelectButton);
        mainActivity.updateFragment(TrackerFragment.newInstance());
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
        mainActivity.showCalendarMenu();
    }

    public void recoveryMissDay(View view) {
        if (CalendarHelper.calendar.getStatus(calendarHelper.toDayOfYear - 1) == 1)
        {   Toast.makeText(MainActivity.mainActivity, getString(R.string.not_miss_day), Toast.LENGTH_SHORT).show();  }
        else if (CalendarHelper.calendar.getSubDaysRemaining(calendarHelper.toDayOfYear - 1) == 0)
        {   Toast.makeText(MainActivity.mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();    }
        else
        {
            CalendarHelper.calendar.dateArray[calendarHelper.toDayOfYear - 2].status = 1;
            Toast.makeText(MainActivity.mainActivity, getString(R.string.check_today), Toast.LENGTH_SHORT).show();
            calendarHelper.claimsDays++;
        }

        updateCalendar();
        mainActivity.closeCalendarMenu();
    }

    public void cancelCheck() {
        CalendarHelper.calendar.dateArray[calendarHelper.toDayOfYear - 1].status = 0;
        calendarHelper.claimsDays--;
    }

    public void onCancelCheck(View view){
        if (CalendarHelper.calendar.getStatus(calendarHelper.toDayOfYear) == 0)
        {   Toast.makeText(MainActivity.mainActivity, getString(R.string.not_check_today), Toast.LENGTH_SHORT).show();  }
        else if (CalendarHelper.calendar.getSubDaysRemaining(calendarHelper.toDayOfYear) == 0)
        {   Toast.makeText(MainActivity.mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();    }
        else
        {
            cancelCheck();
            Toast.makeText(MainActivity.mainActivity, getString(R.string.cancel_check_today), Toast.LENGTH_SHORT).show();
        }

        updateCalendar();
        mainActivity.closeCalendarMenu();
    }

    public void createDataBaseBackup(View view) {
        DatabaseHelper.createExport(view);
        mainActivity.closeCalendarMenu();
    }
}