package ru.menshovanton.gachapoint.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ru.menshovanton.gachapoint.models.Statistic;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.adapters.PillsAdapter;
import ru.menshovanton.gachapoint.helpers.PiggyBankHelper;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;
import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.helpers.CalendarHelper;

public class HomeFragment extends Fragment {
    private MainActivity mainActivity;

    private TextView subsCounterView;

    private CalendarHelper calendarHelper;
    private PiggyBankHelper piggyBankHelper;

    private PillsAdapter pillsAdapter;

    private LinearLayoutManager layoutManager;

    public HomeFragment() {}
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        PreferencesHelper settings = new PreferencesHelper(Objects.requireNonNull(mainActivity));
        calendarHelper = new CalendarHelper(mainActivity);
        piggyBankHelper = new PiggyBankHelper(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        subsCounterView = view.findViewById(R.id.subsCountHome);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView gameTypeChanger = view.findViewById(R.id.gameTypeChangerHome);

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
        calendarHelper.calculateMissesAndClaims();
        setStatistics();
        subsCounterView.setText(String.valueOf(calendarHelper.getSubsCount()));

        ImageView gemIcon = view.findViewById(R.id.gemIconHome);
        TextView subsCountTitle = view.findViewById(R.id.subsCountHeaderHome);
        ImageView wishIconStats = view.findViewById(R.id.wishIconHome);
        ImageView wishIconPiggyBank = view.findViewById(R.id.wishIconHomeBank);

        switch (mainActivity.getSubType()) {
            case 0:
                gemIcon.setImageResource(R.drawable.icon_primogem);
                wishIconStats.setImageResource(R.drawable.icon_intertwined_fate);
                wishIconPiggyBank.setImageResource(R.drawable.icon_intertwined_fate);
                subsCountTitle.setText(R.string.blessing_of_the_welkin_moon_count_header);
                break;
            case 1:
                gemIcon.setImageResource(R.drawable.icon_stellar_jade);
                wishIconStats.setImageResource(R.drawable.icon_star_rail_special_pass);
                wishIconPiggyBank.setImageResource(R.drawable.icon_star_rail_special_pass);
                subsCountTitle.setText(R.string.star_rail_special_pass_count_header);
                break;
            case 2:
                gemIcon.setImageResource(R.drawable.icon_polychrome);
                wishIconStats.setImageResource(R.drawable.icon_encrypted_master_tape);
                wishIconPiggyBank.setImageResource(R.drawable.icon_encrypted_master_tape);
                subsCountTitle.setText(R.string.inter_knot_member_count_header);
                break;
        }
    }

    public void setStatistics() {
        calendarHelper = new CalendarHelper(mainActivity);
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
        TextView claimPrimogems = getView().findViewById(R.id.cliamsGemsCounterHome);
        claimPrimogems.setText(claimPrimogemsText);

        TextView missedPrimogems = getView().findViewById(R.id.missGemsCounterHome);
        missedPrimogems.setText(missedPrimogemsText);

        TextView claimWishes = getView().findViewById(R.id.claimWishesCounterHome);
        claimWishes.setText(claimWishesText);

        TextView missedWishes = getView().findViewById(R.id.missWishesCounterHome);
        missedWishes.setText(missedWishesText);

        TextView laterPrimogems = getView().findViewById(R.id.laterGemsCounterHome);
        if (laterPrimogemsCount > 0 && calendarHelper.getClaimsDays() > 0)
        {   laterPrimogemsText = String.valueOf(laterPrimogemsCount);   }
        laterPrimogems.setText(laterPrimogemsText);

        TextView laterWishes = getView().findViewById(R.id.laterWishesCounterHome);
        if (laterPrimogemsCount > 0 && calendarHelper.getClaimsDays() > 0)
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