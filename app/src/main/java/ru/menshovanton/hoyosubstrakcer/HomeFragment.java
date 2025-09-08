package ru.menshovanton.hoyosubstrakcer;

import static androidx.core.content.ContextCompat.getColor;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

public class HomeFragment extends Fragment {

    MainActivity mainActivity;
    TextView subsCounterView;
    static ConstraintLayout constraintLayout;
    HorizontalScrollView scrollView;

    ImageView previousMonthButton;
    ImageView nextMonthButton;

    Button checkButton;
    Button addButton;

    MaterialButton blessingOfTheWelkinMoonSelectButton;
    MaterialButton starRailSpecialPassSelectButton;
    MaterialButton interKnotMembershipSelectButton;

    public static Calendar calendar;
    private static HomeFragment instance;

    public int toDayOfMonth;
    public static int toDayOfYear;
    public static int year;
    public static int missesDays;
    public static int claimsDays;
    public static int selectedMonth;
    public static int selectedYear;
    public static int subsCount;

    public final int WISHES_COST = 160;
    public final int PRIMOGEMS_PER_DAY = 90;
    public final int SUMMARY_CLAIM = 2700;

    public HomeFragment() {}

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = MainActivity.mainActivity;
        instance = this;

        missesDays = 0;
        claimsDays = 0;
        subsCount = 0;

        String toDayMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru"));
        toDayMonth.substring(0, 1).toUpperCase();

        selectedMonth = LocalDate.now().getMonth().getValue();
        selectedYear = LocalDate.now().getYear();
        toDayOfMonth = LocalDate.now().getDayOfMonth();
        toDayOfYear = LocalDate.now().getDayOfYear();
        year = LocalDate.now().getYear();

        calendar = new Calendar(MainActivity.context, MainActivity.mainActivity);

        if (calendar.dateArray[toDayOfYear - 1].subDaysRemaining == 0) { subsCount = 0; }
        else if (calendar.dateArray[toDayOfYear - 1].subDaysRemaining <= 30) { subsCount = 1; }
        else if (calendar.dateArray[toDayOfYear - 1].subDaysRemaining <= 60) { subsCount = 2;}
        else if (calendar.dateArray[toDayOfYear - 1].subDaysRemaining <= 90) { subsCount = 3; }
        else if (calendar.dateArray[toDayOfYear - 1].subDaysRemaining <= 120) { subsCount = 4; }
        else if (calendar.dateArray[toDayOfYear - 1].subDaysRemaining <= 150) { subsCount = 5; }
        else if (calendar.dateArray[toDayOfYear - 1].subDaysRemaining <= 180) { subsCount = 6; }

        Notification.subsCount = subsCount;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        subsCounterView = view.findViewById(R.id.subsCount);
        constraintLayout = view.findViewById(R.id.constraint);
        checkButton = view.findViewById(R.id.checkDayButton);
        addButton = view.findViewById(R.id.addSubButton);
        previousMonthButton = view.findViewById(R.id.previousMonth);
        nextMonthButton = view.findViewById(R.id.nextMonth);
        blessingOfTheWelkinMoonSelectButton = view.findViewById(R.id.blessingOfTheWelkinMoonSelectButton);
        starRailSpecialPassSelectButton = view.findViewById(R.id.starRailSpecialPassSelectButton);
        interKnotMembershipSelectButton = view.findViewById(R.id.interKnotMembershipSelectButton);
        scrollView = view.findViewById(R.id.subTypes);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subsCounterView.setText(String.valueOf(subsCount));

        calendar.updateCalendar();
        calendar.drawCalendar();

        setHeader(selectedMonth, year);
        calculateStats();

        checkButton.setOnClickListener(this::onCheckClick);
        addButton.setOnClickListener(this::onAddClick);
        previousMonthButton.setOnClickListener(this::onPreviousMonthClick);
        nextMonthButton.setOnClickListener(this::onNextMonthClick);
        blessingOfTheWelkinMoonSelectButton.setOnClickListener(this::onMoonClick);
        starRailSpecialPassSelectButton.setOnClickListener(this::onPassClick);
        interKnotMembershipSelectButton.setOnClickListener(this::onInterknotClick);

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

    public static void createView(Date date, TextView textView, ImageView imageView, int leftMargin, int rightMargin, int topMargin) {
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(120, 120);
        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.leftMargin = leftMargin;
        layoutParams.rightMargin = rightMargin;
        layoutParams.topMargin = topMargin;

        textView.setText(String.valueOf(date.dayOfMonth));
        textView.setTextSize(24);
        Typeface typeface = ResourcesCompat.getFont(MainActivity.context, R.font.genshin_font);
        textView.setTypeface(typeface);
        textView.setTextColor(getColor(MainActivity.context, R.color.white));
        textView.setGravity(Gravity.CENTER);

        if (date.dayOfYear == toDayOfYear && selectedMonth == LocalDate.now().getMonth().getValue() && selectedYear == LocalDate.now().getYear()) {
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

        if ((date.status == 0 || date.status == 2) && date.id < toDayOfYear && date.subDaysRemaining != 0
                && date.month == selectedMonth) {
            textView.setTextColor(getColor(MainActivity.context, R.color.missed));
        }

        constraintLayout.addView(imageView, layoutParams);
        constraintLayout.addView(textView, layoutParams);
    }

    public void calculateStats() {
        int missedPrimogemsCount = missesDays * PRIMOGEMS_PER_DAY;
        int claimPrimogemsCount = claimsDays * PRIMOGEMS_PER_DAY;
        int laterPrimogemsCount = SUMMARY_CLAIM * subsCount - claimPrimogemsCount - missedPrimogemsCount;
        int laterWishesCount = laterPrimogemsCount / WISHES_COST;

        String laterPrimogemsText = "0";
        String laterWishesText = "0";
        String claimPrimogemsText = String.valueOf(claimPrimogemsCount);
        String missedPrimogemsText = String.valueOf(missedPrimogemsCount);
        String claimWishesText = String.valueOf(claimPrimogemsCount / WISHES_COST);
        String missedWishesText = String.valueOf(missedPrimogemsCount / WISHES_COST);

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
        if (laterPrimogemsCount > 0 && claimsDays > 0)
        {   laterPrimogemsText = String.valueOf(laterPrimogemsCount);   }
        laterPrimogems.setText(laterPrimogemsText);

        TextView laterWishes = getView().findViewById(R.id.laterWishesCounter);
        if (laterPrimogemsCount > 0 && claimsDays > 0)
        {   laterWishesText = String.valueOf(laterWishesCount); }
        laterWishes.setText(laterWishesText);
    }

    public void updateSubscribes() {
        for (int i = 0; i < calendar.dateArray[toDayOfYear - 1].subDaysRemaining; i++) {
            calendar.dateArray[toDayOfYear + i].subDaysRemaining = calendar.dateArray[toDayOfYear + i - 1].subDaysRemaining - 1;
        }
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
        if (subsCount <= 6) {
            if (calendar.dateArray[toDayOfYear - 1].subDaysRemaining == 0)
            {
                subsCount = 1;
                for (int i = 0; i < toDayOfYear - 1; i++) {
                    if (calendar.dateArray[i].status == 1) {
                        calendar.dateArray[i].status = 3;
                    }
                    if (calendar.dateArray[i].status == 0 && calendar.dateArray[i].subDaysRemaining > 0) {
                        calendar.dateArray[i].status = 2;
                    }
                }

                missesDays = 0;
                claimsDays = 0;

                calendar.dateArray[toDayOfYear - 1].subDaysRemaining = 30;
            }
            else
            {
                subsCount++;
                calendar.dateArray[toDayOfYear - 1].subDaysRemaining += 30;
            }

            updateSubscribes();
            Toast.makeText(MainActivity.mainActivity, getString(R.string.add_sub), Toast.LENGTH_SHORT).show();
            onCheckClick(view);

            Notification.subsCount = subsCount;

            mainActivity.updateLayout(HomeFragment.newInstance());
        }
        else
        {   Toast.makeText(MainActivity.mainActivity, getString(R.string.subs_limit), Toast.LENGTH_SHORT).show(); }
        subsCounterView.setText(String.valueOf(subsCount));
    }

    public void onCheckClick(View view)
    {
        if (calendar.dateArray[toDayOfYear - 1].status == 1)
        {   Toast.makeText(MainActivity.mainActivity, getString(R.string.already_cheked), Toast.LENGTH_SHORT).show();  }
        else if (calendar.dateArray[toDayOfYear - 1].subDaysRemaining == 0)
        {   Toast.makeText(MainActivity.mainActivity, getString(R.string.active_subs_null), Toast.LENGTH_SHORT).show();    }
        else
        {
            calendar.dateArray[toDayOfYear - 1].status = 1;
            Toast.makeText(MainActivity.mainActivity, getString(R.string.check_today), Toast.LENGTH_SHORT).show();
            claimsDays++;
        }

        calculateStats();
        DataManager.writeDB(MainActivity.context, calendar.dateArray, LocalDate.now().getDayOfYear() - 1, calendar.dateArray[toDayOfYear - 1].subDaysRemaining);
        selectedMonth = LocalDate.now().getMonth().getValue();
        calendar.removeCalendar(constraintLayout);
        calendar.drawCalendar();
        setHeader(selectedMonth, year);
    }

    public void onPreviousMonthClick(View view) {
        if (selectedMonth != 1) {
            selectedMonth--;
        } else {
            if (selectedYear > calendar.dateArray[0].year) {
                selectedMonth = 12;
                selectedYear--;
            }
        }
        calendar.removeCalendar(constraintLayout);
        calendar.drawCalendar();
        setHeader(selectedMonth, selectedYear);
    }

    public void onNextMonthClick(View view) {
        if (selectedMonth != 12) {
            selectedMonth++;
        } else {
            if (selectedYear < calendar.dateArray[Calendar.init_days - 1].year) {
                selectedMonth = 1;
                selectedYear++;
            }
        }
        calendar.removeCalendar(constraintLayout);
        calendar.drawCalendar();
        setHeader(selectedMonth, selectedYear);
    }

    public void onMoonClick(View view) {
        MainActivity.subType = 0;
        changeCheckedTab(blessingOfTheWelkinMoonSelectButton);
        mainActivity.updateLayout(HomeFragment.newInstance());
    }

    public void onPassClick(View view) {
        MainActivity.subType = 1;
        changeCheckedTab(starRailSpecialPassSelectButton);
        mainActivity.updateLayout(HomeFragment.newInstance());
    }

    public void onInterknotClick(View view) {
        MainActivity.subType = 2;
        changeCheckedTab(interKnotMembershipSelectButton);
        mainActivity.updateLayout(HomeFragment.newInstance());
    }

    private void changeCheckedTab(MaterialButton view) {
        blessingOfTheWelkinMoonSelectButton.setStrokeColorResource(R.color.accent);
        starRailSpecialPassSelectButton.setStrokeColorResource(R.color.accent);
        interKnotMembershipSelectButton.setStrokeColorResource(R.color.accent);

        view.setStrokeColorResource(R.color.check);
    }

    public static void update() {
        calendar.removeCalendar(constraintLayout);
        calendar.updateCalendar();
        calendar.drawCalendar();
        instance.calculateStats();
    }
}