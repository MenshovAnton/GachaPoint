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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private FloatingActionButton fabRefresh;
    private android.animation.ObjectAnimator refreshAnimator;
    private boolean isManualRefresh = false;

    private final ru.menshovanton.gachapoint.data.local.JsonDataLoader.OnDataSyncResultListener syncResultListener = (cacheFileName, status) -> {
        if (!ru.menshovanton.gachapoint.data.local.DataSyncConfig.CACHE_BANNERS.equals(cacheFileName)) return;
        if (!isManualRefresh) return;
        isManualRefresh = false;
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (getContext() == null) return;
            switch (status) {
                case OFFLINE:
                    android.widget.Toast.makeText(getContext(), R.string.sync_status_offline, android.widget.Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    android.widget.Toast.makeText(getContext(), R.string.sync_status_error, android.widget.Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    android.widget.Toast.makeText(getContext(), R.string.sync_status_success, android.widget.Toast.LENGTH_SHORT).show();
                    break;
                case NOT_MODIFIED:
                    android.widget.Toast.makeText(getContext(), R.string.sync_status_up_to_date, android.widget.Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    };

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
        fabRefresh = root.findViewById(R.id.fab_refresh);

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

        ru.menshovanton.gachapoint.data.local.JsonDataLoader.addSyncResultListener(syncResultListener);

        fabRefresh.setOnClickListener(v -> {
            HapticHelper.performClickHaptic(requireContext(), v);
            isManualRefresh = true;
            viewModel.loadData(true);
        });

        observeViewModel();

        ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository.getInstance(requireContext())
                .markAllBannersAsViewed();
        if (getActivity() instanceof ru.menshovanton.gachapoint.ui.main.MainActivityView) {
            ((ru.menshovanton.gachapoint.ui.main.MainActivityView) getActivity()).updatePromocodesBadge();
        }
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
                if (fabRefresh != null) {
                    fabRefresh.setEnabled(false);
                    if (refreshAnimator == null) {
                        refreshAnimator = android.animation.ObjectAnimator.ofFloat(fabRefresh, View.ROTATION, 0f, 360f);
                        refreshAnimator.setDuration(700);
                        refreshAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                        refreshAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
                    }
                    if (!refreshAnimator.isStarted()) {
                        refreshAnimator.start();
                    }
                }
            } else {
                if (refreshAnimator != null) {
                    refreshAnimator.cancel();
                }
                if (fabRefresh != null) {
                    fabRefresh.setEnabled(true);
                    fabRefresh.setRotation(0f);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        ru.menshovanton.gachapoint.data.local.JsonDataLoader.removeSyncResultListener(syncResultListener);
        if (refreshAnimator != null) {
            refreshAnimator.cancel();
            refreshAnimator = null;
        }
        super.onDestroyView();
        if (rvContent != null) {
            rvContent.setAdapter(null);
            rvContent = null;
        }
        if (rvGameTypes != null) {
            rvGameTypes.setAdapter(null);
            rvGameTypes = null;
        }
        scheduleAdapter = null;
        rerunAdapter = null;
        toggleBannerMode = null;
        fabRefresh = null;
    }
}
