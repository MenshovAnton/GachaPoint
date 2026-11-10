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

import java.util.List;

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
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout srlBanners;
    private View cardOfflineNotice;
    private PillsAdapter pillsAdapter;

    private final ru.menshovanton.gachapoint.data.local.JsonDataLoader.OnDataSyncResultListener syncResultListener = (cacheFileName, status) -> {
        if (!ru.menshovanton.gachapoint.data.local.DataSyncConfig.CACHE_BANNERS.equals(cacheFileName)) return;
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (getContext() == null) return;
            if (srlBanners != null) {
                srlBanners.setRefreshing(false);
            }
            switch (status) {
                case OFFLINE:
                case ERROR:
                    if (cardOfflineNotice != null) cardOfflineNotice.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                case NOT_MODIFIED:
                    if (cardOfflineNotice != null) cardOfflineNotice.setVisibility(View.GONE);
                    break;
            }
        });
    };

    private List<GameType> getEnabledGames() {
        if (getActivity() instanceof ru.menshovanton.gachapoint.ui.main.MainActivityView) {
            return ((ru.menshovanton.gachapoint.ui.main.MainActivityView) getActivity()).getEnabledGames();
        }
        return new ru.menshovanton.gachapoint.data.local.Preferences(requireContext()).getEnabledGames();
    }

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
        srlBanners = root.findViewById(R.id.srl_banners);
        cardOfflineNotice = root.findViewById(R.id.card_offline_notice);

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

        List<GameType> enabledGames = getEnabledGames();
        Bundle args = getArguments();
        GameType targetGame = null;
        if (args != null && args.containsKey("game_type")) {
            int gCode = args.getInt("game_type", -1);
            if (gCode >= 0) {
                targetGame = GameType.fromCode(gCode);
            }
        }
        if (targetGame == null || !enabledGames.contains(targetGame)) {
            targetGame = !enabledGames.isEmpty() ? enabledGames.get(0) : GameType.GENSHIN;
        }
        viewModel.selectGameType(targetGame);

        setupGamePills();

        scheduleAdapter = new BannersScheduleAdapter();
        rerunAdapter = new CharacterRerunAdapter();
        rvContent.setAdapter(scheduleAdapter);

        setupModeToggle();

        ru.menshovanton.gachapoint.data.local.JsonDataLoader.addSyncResultListener(syncResultListener);

        if (srlBanners != null) {
            srlBanners.setColorSchemeResources(R.color.check);
            srlBanners.setProgressBackgroundColorSchemeResource(R.color.card_bg);
            srlBanners.setOnRefreshListener(() -> viewModel.loadData(true));
        }

        observeViewModel();

        GameType current = viewModel.getSelectedGameType().getValue();
        if (current == null) current = GameType.GENSHIN;
        ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository.getInstance(requireContext())
                .markBannersAsViewed(current);
        updateMainBadge();
        updatePillsBadges();
    }

    private void updateMainBadge() {
        if (getActivity() instanceof ru.menshovanton.gachapoint.ui.main.MainActivityView) {
            ((ru.menshovanton.gachapoint.ui.main.MainActivityView) getActivity()).updatePromocodesBadge();
        }
    }

    private void updatePillsBadges() {
        ru.menshovanton.gachapoint.data.local.NetworkExecutor.execute(() -> {
            if (getContext() == null) return;
            ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository repo =
                    ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository.getInstance(requireContext());
            java.util.Set<GameType> badgeGames = new java.util.HashSet<>();
            for (GameType g : getEnabledGames()) {
                if (repo.getUnviewedBannersCount(g) > 0) {
                    badgeGames.add(g);
                }
            }
            ru.menshovanton.gachapoint.data.db.AppDatabase.postToMain(() -> {
                if (pillsAdapter != null) {
                    pillsAdapter.setBadgeGames(badgeGames);
                }
            });
        });
    }

    private void updatePillsVisibility() {
        if (rvGameTypes == null) return;
        List<GameType> enabled = getEnabledGames();
        rvGameTypes.setVisibility(enabled.size() > 1 ? View.VISIBLE : View.GONE);
    }

    private void setupGamePills() {
        List<GameType> enabledGames = getEnabledGames();
        GameType current = viewModel.getSelectedGameType().getValue();
        if (current == null || !enabledGames.contains(current)) {
            current = !enabledGames.isEmpty() ? enabledGames.get(0) : GameType.GENSHIN;
            viewModel.selectGameType(current);
        }

        ru.menshovanton.gachapoint.ui.main.MainActivityView mainActivity =
                getActivity() instanceof ru.menshovanton.gachapoint.ui.main.MainActivityView
                        ? (ru.menshovanton.gachapoint.ui.main.MainActivityView) getActivity()
                        : null;

        pillsAdapter = new PillsAdapter(
                enabledGames,
                current,
                (gameType, position) -> {
                    HapticHelper.performClickHaptic(requireContext(), null);
                    viewModel.selectGameType(gameType);
                    ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository.getInstance(requireContext())
                            .markBannersAsViewed(gameType);
                    updateMainBadge();
                    updatePillsBadges();
                },
                () -> ru.menshovanton.gachapoint.ui.main.GamesConfigBottomSheetDialog.show(requireContext(), () -> {
                    if (mainActivity != null) mainActivity.refreshGamesConfig();
                })
        );
        rvGameTypes.setAdapter(pillsAdapter);
        updatePillsBadges();
        updatePillsVisibility();
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
        ru.menshovanton.gachapoint.ui.main.MainActivityView mainActivity =
                getActivity() instanceof ru.menshovanton.gachapoint.ui.main.MainActivityView
                        ? (ru.menshovanton.gachapoint.ui.main.MainActivityView) getActivity()
                        : null;
        if (mainActivity != null) {
            mainActivity.getMainActivityViewModel().getEnabledGamesLiveData().observe(getViewLifecycleOwner(), enabled -> {
                if (enabled == null || enabled.isEmpty()) return;
                GameType current = viewModel.getSelectedGameType().getValue();
                if (current == null || !enabled.contains(current)) {
                    current = enabled.get(0);
                    viewModel.selectGameType(current);
                }
                if (pillsAdapter != null) {
                    pillsAdapter.updateData(enabled, current);
                }
                updatePillsBadges();
                updatePillsVisibility();
            });
        }

        viewModel.getBannersList().observe(getViewLifecycleOwner(), banners ->
                scheduleAdapter.submitList(banners));

        viewModel.getRerunList().observe(getViewLifecycleOwner(), reruns ->
                rerunAdapter.submitList(reruns));

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (srlBanners != null) {
                srlBanners.setRefreshing(Boolean.TRUE.equals(loading));
            }
        });
    }

    @Override
    public void onDestroyView() {
        ru.menshovanton.gachapoint.data.local.JsonDataLoader.removeSyncResultListener(syncResultListener);
        super.onDestroyView();
        if (srlBanners != null) {
            srlBanners.setOnRefreshListener(null);
            srlBanners = null;
        }
        cardOfflineNotice = null;
        if (rvContent != null) {
            rvContent.setAdapter(null);
            rvContent = null;
        }
        if (rvGameTypes != null) {
            rvGameTypes.setAdapter(null);
            rvGameTypes = null;
        }
        pillsAdapter = null;
        scheduleAdapter = null;
        rerunAdapter = null;
        toggleBannerMode = null;
    }
}
