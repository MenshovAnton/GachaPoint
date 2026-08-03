package ru.menshovanton.gachapoint.fragments;

import static ru.menshovanton.gachapoint.Calendar.splashScreen;

import android.os.Bundle;
import android.widget.HorizontalScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import ru.menshovanton.gachapoint.Statistic;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.helpers.PiggyBankHelper;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;
import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.helpers.CalendarHelper;

public class HomeFragment extends Fragment {
    MainActivity mainActivity;

    MaterialButton genshinImpact;
    MaterialButton honkaiStarRail;
    MaterialButton zenlessZoneZero;

    TextView subsCounterView;

    HorizontalScrollView scrollView;

    PreferencesHelper settings;
    CalendarHelper calendarHelper;
    PiggyBankHelper piggyBankHelper;

    public HomeFragment() {}
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = MainActivity.mainActivity;
        settings = new PreferencesHelper(mainActivity);
        calendarHelper = new CalendarHelper(splashScreen);
        piggyBankHelper = new PiggyBankHelper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        genshinImpact = view.findViewById(R.id.genshinImpact);
        honkaiStarRail = view.findViewById(R.id.honkai);
        zenlessZoneZero = view.findViewById(R.id.zenless);
        subsCounterView = view.findViewById(R.id.subsCountHome);
        scrollView = view.findViewById(R.id.gameTypeChangerHome);

        scrollView.post(() -> scrollView.scrollTo(MainActivity.subTypeScrollX, 0));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        genshinImpact.setOnClickListener(this::onGenshinClick);
        honkaiStarRail.setOnClickListener(this::onHonkaiClick);
        zenlessZoneZero.setOnClickListener(this::onZenlessClick);

        scrollView.setOnScrollChangeListener(this::onScrollChange);

        ImageView gemIcon = view.findViewById(R.id.gemIconHome);
        TextView subsCountTitle = view.findViewById(R.id.subsCountHeaderHome);
        ImageView wishIconStats = view.findViewById(R.id.wishIconHome);
        ImageView wishIconPiggyBank = view.findViewById(R.id.wishIconHomeBank);

        calendarHelper.calculate();
        setStatistics();
        subsCounterView.setText(String.valueOf(calendarHelper.subsCount));

        switch (MainActivity.subType) {
            case 0:
                gemIcon.setImageResource(R.drawable.primogem);
                wishIconStats.setImageResource(R.drawable.intertwined_fate);
                wishIconPiggyBank.setImageResource(R.drawable.intertwined_fate);
                subsCountTitle.setText(R.string.blessing_of_the_welkin_moon_count_header);
                changeCheckedTab(genshinImpact);
                break;
            case 1:
                gemIcon.setImageResource(R.drawable.stellar_jade);
                wishIconStats.setImageResource(R.drawable.star_rail_special_pass);
                wishIconPiggyBank.setImageResource(R.drawable.star_rail_special_pass);
                subsCountTitle.setText(R.string.star_rail_special_pass_count_header);
                changeCheckedTab(honkaiStarRail);
                break;
            case 2:
                gemIcon.setImageResource(R.drawable.polychrome);
                wishIconStats.setImageResource(R.drawable.encrypted_master_tape);
                wishIconPiggyBank.setImageResource(R.drawable.encrypted_master_tape);
                subsCountTitle.setText(R.string.inter_knot_member_count_header);
                changeCheckedTab(zenlessZoneZero);
                break;
        }
    }

    public void onGenshinClick(View view) {
        MainActivity.subType = 0;
        changeCheckedTab(genshinImpact);
        mainActivity.updateFragment(HomeFragment.newInstance());
    }

    public void onHonkaiClick(View view) {
        MainActivity.subType = 1;
        changeCheckedTab(honkaiStarRail);
        mainActivity.updateFragment(HomeFragment.newInstance());
    }

    public void onZenlessClick(View view) {
        MainActivity.subType = 2;
        changeCheckedTab(zenlessZoneZero);
        mainActivity.updateFragment(HomeFragment.newInstance());
    }

    private void changeCheckedTab(MaterialButton view) {
        genshinImpact.setStrokeColorResource(R.color.accent);
        honkaiStarRail.setStrokeColorResource(R.color.accent);
        zenlessZoneZero.setStrokeColorResource(R.color.accent);

        view.setStrokeColorResource(R.color.check);
    }

    private void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        MainActivity.subTypeScrollX = scrollX;
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
        TextView claimPrimogems = getView().findViewById(R.id.cliamsGemsCounterHome);
        claimPrimogems.setText(claimPrimogemsText);

        TextView missedPrimogems = getView().findViewById(R.id.missGemsCounterHome);
        missedPrimogems.setText(missedPrimogemsText);

        TextView claimWishes = getView().findViewById(R.id.claimWishesCounterHome);
        claimWishes.setText(claimWishesText);

        TextView missedWishes = getView().findViewById(R.id.missWishesCounterHome);
        missedWishes.setText(missedWishesText);

        TextView laterPrimogems = getView().findViewById(R.id.laterGemsCounterHome);
        if (laterPrimogemsCount > 0 && calendarHelper.claimsDays > 0)
        {   laterPrimogemsText = String.valueOf(laterPrimogemsCount);   }
        laterPrimogems.setText(laterPrimogemsText);

        TextView laterWishes = getView().findViewById(R.id.laterWishesCounterHome);
        if (laterPrimogemsCount > 0 && calendarHelper.claimsDays > 0)
        {   laterWishesText = String.valueOf(laterWishesCount); }
        laterWishes.setText(laterWishesText);

        int progress = piggyBankHelper.getProgress();
        int target = piggyBankHelper.getTarget();

        String text = progress + "/" + target;

        TextView piggyBank = getView().findViewById(R.id.piggyBankCounterHome);
        ProgressBar progressBar = getView().findViewById(R.id.piggyBankProgressHome);
        progressBar.setMax(target);
        progressBar.setProgress(progress);
        piggyBank.setText(text);

    }
}