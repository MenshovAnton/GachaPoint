package ru.menshovanton.gachapoint.ui.fragment.banners;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Arrays;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.common.HapticHelper;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class BannersView extends Fragment {

    private BannersViewModel viewModel;
    private BannersScheduleAdapter scheduleAdapter;
    private CharacterRerunAdapter rerunAdapter;

    private RecyclerView rvGameTypes;
    private MaterialButtonToggleGroup toggleBannerMode;
    private RecyclerView rvContent;
    private ImageButton btnRefresh;

    private static final GameType[] SUPPORTED_GAMES = {
            GameType.GENSHIN,
            GameType.HSR,
            GameType.ZZZ,
            GameType.HI3
    };

    public BannersView() {}

    public static BannersView newInstance() {
        return new BannersView();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_banners, container, false);

        rvGameTypes = root.findViewById(R.id.rv_game_types);
        toggleBannerMode = root.findViewById(R.id.toggle_banner_mode);
        rvContent = root.findViewById(R.id.rv_banners_content);
        btnRefresh = root.findViewById(R.id.btn_refresh);

        ImageButton btnBack = root.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(requireContext(), v);
                NavHostFragment.findNavController(this).popBackStack();
            });
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BannersViewModel.class);

        setupGamePills();

        scheduleAdapter = new BannersScheduleAdapter();
        rerunAdapter = new CharacterRerunAdapter();
        rvContent.setAdapter(scheduleAdapter);

        setupModeToggle();

        btnRefresh.setOnClickListener(v -> {
            HapticHelper.performClickHaptic(requireContext(), v);
            btnRefresh.animate().rotationBy(360f).setDuration(400).start();
            viewModel.loadData(true);
        });

        observeViewModel();
    }

    private void setupGamePills() {
        GameType current = viewModel.getSelectedGameType().getValue();
        if (current == null) current = GameType.GENSHIN;

        PillsAdapter pillsAdapter = new PillsAdapter(
                Arrays.asList(SUPPORTED_GAMES),
                current,
                (gameType, position) -> {
                    HapticHelper.performClickHaptic(requireContext(), null);
                    viewModel.selectGameType(gameType);
                },
                null
        );
        rvGameTypes.setAdapter(pillsAdapter);
    }

    private void setupModeToggle() {
        toggleBannerMode.check(R.id.btn_tab_schedule);
        toggleBannerMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                HapticHelper.performClickHaptic(requireContext(), null);
                if (checkedId == R.id.btn_tab_schedule) {
                    viewModel.setDisplayMode(BannersViewModel.DisplayMode.SCHEDULE);
                    scheduleAdapter.submitList(viewModel.getBannersList().getValue());
                    rvContent.setAdapter(scheduleAdapter);
                } else if (checkedId == R.id.btn_tab_reruns) {
                    viewModel.setDisplayMode(BannersViewModel.DisplayMode.RERUNS);
                    rerunAdapter.submitList(viewModel.getRerunList().getValue());
                    rvContent.setAdapter(rerunAdapter);
                }
            }
        });
    }

    private void observeViewModel() {
        viewModel.getBannersList().observe(getViewLifecycleOwner(), banners ->
                scheduleAdapter.submitList(banners));

        viewModel.getRerunList().observe(getViewLifecycleOwner(), reruns ->
                rerunAdapter.submitList(reruns));

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                btnRefresh.setEnabled(false);
                btnRefresh.setAlpha(0.6f);
            } else {
                btnRefresh.setEnabled(true);
                btnRefresh.setAlpha(1.0f);
            }
        });
    }
}
