package ru.menshovanton.gachapoint.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
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

import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.main.MainActivity;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;
import ru.menshovanton.gachapoint.ui.journal.piggybank.PiggyBankHelper;
import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.repository.CalendarRepository;

public class HomeFragment extends Fragment {
    private MainActivity mainActivity;

    private TextView subsCounterView;

    private CalendarRepository calendarRepository;
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
        calendarRepository = new CalendarRepository(mainActivity);
        piggyBankHelper = new PiggyBankHelper(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        subsCounterView = view.findViewById(R.id.subsCountHome);

        RecyclerView gameTypeChanger = view.findViewById(R.id.gameTypeChangerHome);

        List<String> categories = Arrays.asList(
                getString(R.string.genshin),
                getString(R.string.hsr),
                getString(R.string.zzz)
        );

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        gameTypeChanger.setLayoutManager(layoutManager);

        pillsAdapter = new PillsAdapter(categories, (item, position) -> {
            mainActivity.setSubType(GameType.fromCode(position));
            if (getView() != null) {
                refresh(getView());
            }
        });

        gameTypeChanger.setAdapter(pillsAdapter);
        selectAndScrollIfNeeded(mainActivity.getSubType().getCode());

        refresh(view);
        return view;
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

        updateGameTheme(view);

        updatePiggyBank(view);

        calendarRepository.init(() -> {
            if (!isAdded() || getView() == null) return;

            subsCounterView.setText(String.valueOf(calendarRepository.getSubsCount()));
            setStatistics();
        });
    }

    @SuppressLint("SetTextI18n")
    private void updatePiggyBank(View currentView) {
        int progress = piggyBankHelper.getProgress();
        int target = piggyBankHelper.getTarget();

        TextView piggyBank = currentView.findViewById(R.id.piggyBankCounterHome);
        ProgressBar progressBar = currentView.findViewById(R.id.piggyBankProgressHome);

        if (progressBar != null && piggyBank != null) {
            progressBar.setMax(target);
            progressBar.setProgress(progress);
            piggyBank.setText(progress + "/" + target);
        }
    }

    public void setStatistics() {
        if (!isAdded() || getView() == null) return;

        calendarRepository.getStatistic(statistic -> {
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
            TextView claimPrimogems = currentView.findViewById(R.id.cliamsGemsCounterHome);
            TextView missedPrimogems = currentView.findViewById(R.id.missGemsCounterHome);
            TextView claimWishes = currentView.findViewById(R.id.claimWishesCounterHome);
            TextView missedWishes = currentView.findViewById(R.id.missWishesCounterHome);
            TextView laterPrimogems = currentView.findViewById(R.id.laterGemsCounterHome);
            TextView laterWishes = currentView.findViewById(R.id.laterWishesCounterHome);

            claimPrimogems.setText(claimPrimogemsText);
            missedPrimogems.setText(missedPrimogemsText);
            claimWishes.setText(claimWishesText);
            missedWishes.setText(missedWishesText);

            if (laterPrimogemsCount > 0 && calendarRepository.getClaimsDays() > 0) {
                laterPrimogemsText = String.valueOf(laterPrimogemsCount);
            }
            laterPrimogems.setText(laterPrimogemsText);

            if (laterPrimogemsCount > 0 && calendarRepository.getClaimsDays() > 0) {
                laterWishesText = String.valueOf(laterWishesCount);
            }
            laterWishes.setText(laterWishesText);
        });
    }

    private void updateGameTheme(View view) {
        ImageView gemIcon = view.findViewById(R.id.gemIconHome);
        TextView subsCountTitle = view.findViewById(R.id.subsCountHeaderHome);
        ImageView wishIconStats = view.findViewById(R.id.wishIconHome);
        ImageView wishIconPiggyBank = view.findViewById(R.id.wishIconHomeBank);

        switch (mainActivity.getSubType()) {
            case GENSHIN:
                gemIcon.setImageResource(R.drawable.icon_primogem);
                wishIconStats.setImageResource(R.drawable.icon_intertwined_fate);
                wishIconPiggyBank.setImageResource(R.drawable.icon_intertwined_fate);
                subsCountTitle.setText(R.string.blessing_of_the_welkin_moon_count_header);
                break;
            case HSR:
                gemIcon.setImageResource(R.drawable.icon_stellar_jade);
                wishIconStats.setImageResource(R.drawable.icon_star_rail_special_pass);
                wishIconPiggyBank.setImageResource(R.drawable.icon_star_rail_special_pass);
                subsCountTitle.setText(R.string.star_rail_special_pass_count_header);
                break;
            case ZZZ:
                gemIcon.setImageResource(R.drawable.icon_polychrome);
                wishIconStats.setImageResource(R.drawable.icon_encrypted_master_tape);
                wishIconPiggyBank.setImageResource(R.drawable.icon_encrypted_master_tape);
                subsCountTitle.setText(R.string.inter_knot_member_count_header);
                break;
        }
    }
}