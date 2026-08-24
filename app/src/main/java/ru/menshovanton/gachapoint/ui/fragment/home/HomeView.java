package ru.menshovanton.gachapoint.ui.fragment.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Statistic;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class HomeView extends Fragment {

    private MainActivityView mainActivityView;
    private HomeViewModel viewModel;

    private TextView subsCounterView;
    private TextView piggyBankCounter;
    private ProgressBar piggyBankProgress;

    private TextView claimPrimogems, missedPrimogems, claimWishes, missedWishes, laterPrimogems, laterWishes;
    private ImageView gemIcon, wishIconStats, wishIconPiggyBank;
    private TextView subsCountTitle;

    private PillsAdapter pillsAdapter;
    private LinearLayoutManager layoutManager;

    private int lastProgress = 0;
    private int lastTarget = 100;

    public HomeView() {}

    public static HomeView newInstance() {
        return new HomeView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityView = (MainActivityView) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        subsCounterView = view.findViewById(R.id.tv_sub_counter);
        piggyBankCounter = view.findViewById(R.id.tv_saved_counter);
        piggyBankProgress = view.findViewById(R.id.pb_save_progress);

        claimPrimogems = view.findViewById(R.id.tv_received_gems);
        missedPrimogems = view.findViewById(R.id.tv_omitted_gems);
        claimWishes = view.findViewById(R.id.tv_received_pulls);
        missedWishes = view.findViewById(R.id.tv_omitted_pulls);
        laterPrimogems = view.findViewById(R.id.tv_upcoming_gems);
        laterWishes = view.findViewById(R.id.tv_upcoming_pulls);

        gemIcon = view.findViewById(R.id.iv_gem);
        subsCountTitle = view.findViewById(R.id.tv_sub_counter_label);
        wishIconStats = view.findViewById(R.id.iv_pull);
        wishIconPiggyBank = view.findViewById(R.id.iv_saved_pulls);

        initGameTypePills(view);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        observeViewModel();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        observeViewModel();
    }

    private void initGameTypePills(View view) {
        RecyclerView gameTypeChanger = view.findViewById(R.id.rv_game_types);

        List<String> categories = Arrays.asList(
                getString(R.string.genshin),
                getString(R.string.hsr),
                getString(R.string.zzz)
        );

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        gameTypeChanger.setLayoutManager(layoutManager);

        pillsAdapter = new PillsAdapter(categories, (item, position) -> {
            GameType selected = GameType.fromCode(position);
            if (mainActivityView != null) mainActivityView.setSubType(selected);
            viewModel.setGameType(selected);
        });

        gameTypeChanger.setAdapter(pillsAdapter);
        if (mainActivityView != null) {
            selectAndScrollIfNeeded(mainActivityView.getSubType().getCode());
        }
    }

    private void observeViewModel() {
        viewModel.getGameTypeLiveData().observe(getViewLifecycleOwner(), this::updateGameTheme);
        viewModel.getSubsCountLiveData().observe(getViewLifecycleOwner(), count -> subsCounterView.setText(String.valueOf(count)));

        viewModel.getPiggyProgressLiveData().observe(getViewLifecycleOwner(), progress -> updatePiggyProgress(progress, null));
        viewModel.getPiggyTargetLiveData().observe(getViewLifecycleOwner(), target -> updatePiggyProgress(null, target));

        viewModel.getStatisticLiveData().observe(getViewLifecycleOwner(), this::updateStatisticsUi);
    }

    @SuppressLint("SetTextI18n")
    private void updatePiggyProgress(@Nullable Integer progress, @Nullable Integer target) {
        if (target != null) lastTarget = target;
        if (progress != null) lastProgress = progress;

        if (piggyBankProgress != null && piggyBankCounter != null) {
            piggyBankProgress.setMax(lastTarget);
            piggyBankProgress.setProgress(lastProgress);
            piggyBankCounter.setText(lastProgress + "/" + lastTarget);
        }
    }

    private void updateStatisticsUi(Statistic statistic) {
        if (statistic == null) return;

        claimPrimogems.setText(String.valueOf(statistic.claimGems));
        missedPrimogems.setText(String.valueOf(statistic.missedGems));
        claimWishes.setText(String.valueOf(statistic.claimWishes));
        missedWishes.setText(String.valueOf(statistic.missedWishes));

        laterPrimogems.setText(statistic.laterGems > 0 ? String.valueOf(statistic.laterGems) : "0");
        laterWishes.setText(statistic.laterWishes > 0 ? String.valueOf(statistic.laterWishes) : "0");
    }

    private void updateGameTheme(GameType gameType) {
        selectAndScrollIfNeeded(gameType.getCode());

        switch (gameType) {
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

    private void selectAndScrollIfNeeded(int targetPosition) {
        if (pillsAdapter == null || layoutManager == null) return;
        pillsAdapter.setSelectedPosition(targetPosition);

        int firstCompletelyVisible = layoutManager.findFirstCompletelyVisibleItemPosition();
        int lastCompletelyVisible = layoutManager.findLastCompletelyVisibleItemPosition();

        if (targetPosition < firstCompletelyVisible || targetPosition > lastCompletelyVisible) {
            int offsetPx = (int) (16 * getResources().getDisplayMetrics().density);
            layoutManager.scrollToPositionWithOffset(targetPosition, offsetPx);
        }
    }
}