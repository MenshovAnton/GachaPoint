package ru.menshovanton.gachapoint.ui.fragment.banners;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;
import ru.menshovanton.gachapoint.domain.models.CharacterRerunInfo;
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

    private View cardSearch;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private View emptySearchState;
    private TextWatcher searchTextWatcher;

    private ChipGroup chipGroupScheduleFilters;
    private ChipGroup chipGroupRerunFilters;

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

        cardSearch = root.findViewById(R.id.card_banners_search);
        etSearch = root.findViewById(R.id.et_banners_search);
        btnClearSearch = root.findViewById(R.id.btn_clear_banners_search);
        emptySearchState = root.findViewById(R.id.ll_empty_search);
        chipGroupScheduleFilters = root.findViewById(R.id.chip_group_schedule_filters);
        chipGroupRerunFilters = root.findViewById(R.id.chip_group_rerun_filters);

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
        setupSearch();
        setupFilterChips();

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

    private void setupSearch() {
        if (etSearch == null) return;
        searchTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                if (viewModel != null) {
                    viewModel.setSearchQuery(s != null ? s.toString() : "");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etSearch.addTextChangedListener(searchTextWatcher);

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(requireContext(), v);
                etSearch.setText("");
            });
        }
    }

    private void updateEmptyState() {
        if (emptySearchState == null || rvContent == null) return;
        boolean hasQuery = viewModel != null && !viewModel.getSearchQuery().isEmpty();
        boolean isEmpty = false;
        if (viewModel != null) {
            if (viewModel.getDisplayMode().getValue() == BannersViewModel.DisplayMode.SCHEDULE) {
                List<BannerScheduleItem> list = viewModel.getBannersList().getValue();
                isEmpty = list == null || list.isEmpty();
            } else {
                List<CharacterRerunInfo> list = viewModel.getRerunList().getValue();
                isEmpty = list == null || list.isEmpty();
            }
        }
        if (hasQuery && isEmpty) {
            emptySearchState.setVisibility(View.VISIBLE);
            rvContent.setVisibility(View.GONE);
        } else {
            emptySearchState.setVisibility(View.GONE);
            rvContent.setVisibility(View.VISIBLE);
        }
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
                    if (chipGroupScheduleFilters != null) chipGroupScheduleFilters.setVisibility(View.VISIBLE);
                    if (chipGroupRerunFilters != null) chipGroupRerunFilters.setVisibility(View.GONE);
                    updateEmptyState();
                } else if (checkedId == R.id.btn_tab_reruns) {
                    viewModel.setDisplayMode(BannersViewModel.DisplayMode.RERUNS);
                    rerunAdapter.submitList(viewModel.getRerunList().getValue());
                    rvContent.setAdapter(rerunAdapter);
                    if (chipGroupScheduleFilters != null) chipGroupScheduleFilters.setVisibility(View.GONE);
                    if (chipGroupRerunFilters != null) chipGroupRerunFilters.setVisibility(View.VISIBLE);
                    updateEmptyState();
                }
            }
        });
    }

    private void setupFilterChips() {
        if (chipGroupScheduleFilters != null) {
            chipGroupScheduleFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty() || viewModel == null) return;
                HapticHelper.performClickHaptic(requireContext(), null);
                int id = checkedIds.get(0);
                if (id == R.id.chip_schedule_all) {
                    viewModel.setScheduleFilter(BannersViewModel.ScheduleFilter.ALL);
                } else if (id == R.id.chip_schedule_current) {
                    viewModel.setScheduleFilter(BannersViewModel.ScheduleFilter.CURRENT);
                } else if (id == R.id.chip_schedule_future) {
                    viewModel.setScheduleFilter(BannersViewModel.ScheduleFilter.FUTURE);
                } else if (id == R.id.chip_schedule_past) {
                    viewModel.setScheduleFilter(BannersViewModel.ScheduleFilter.PAST);
                }
            });
        }
        if (chipGroupRerunFilters != null) {
            chipGroupRerunFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty() || viewModel == null) return;
                HapticHelper.performClickHaptic(requireContext(), null);
                int id = checkedIds.get(0);
                if (id == R.id.chip_rerun_all) {
                    viewModel.setRerunFilter(BannersViewModel.RerunFilter.ALL);
                } else if (id == R.id.chip_rerun_active) {
                    viewModel.setRerunFilter(BannersViewModel.RerunFilter.ACTIVE);
                } else if (id == R.id.chip_rerun_overdue) {
                    viewModel.setRerunFilter(BannersViewModel.RerunFilter.OVERDUE);
                } else if (id == R.id.chip_rerun_never) {
                    viewModel.setRerunFilter(BannersViewModel.RerunFilter.NEVER);
                }
            });
        }
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

        viewModel.getBannersList().observe(getViewLifecycleOwner(), banners -> {
            scheduleAdapter.submitList(banners);
            updateEmptyState();
        });

        viewModel.getRerunList().observe(getViewLifecycleOwner(), reruns -> {
            rerunAdapter.submitList(reruns);
            updateEmptyState();
        });

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
        if (etSearch != null && searchTextWatcher != null) {
            etSearch.removeTextChangedListener(searchTextWatcher);
            searchTextWatcher = null;
        }
        etSearch = null;
        btnClearSearch = null;
        emptySearchState = null;
        cardSearch = null;
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
        chipGroupScheduleFilters = null;
        chipGroupRerunFilters = null;
    }
}
