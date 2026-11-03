package ru.menshovanton.gachapoint.ui.fragment.journal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Toast;
import ru.menshovanton.gachapoint.domain.enums.RareType;
import ru.menshovanton.gachapoint.domain.enums.Rarity;
import ru.menshovanton.gachapoint.ui.common.AvatarImageLoader;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AutoCompleteTextView;
import ru.menshovanton.gachapoint.ui.common.NoFilterArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import com.bumptech.glide.Glide;
import android.widget.ImageView;
import android.widget.LinearLayout;
import ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository;
import ru.menshovanton.gachapoint.data.repository.CatalogRepository;
import ru.menshovanton.gachapoint.domain.enums.BannerType;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;
import ru.menshovanton.gachapoint.domain.models.CatalogItem;
import ru.menshovanton.gachapoint.ui.common.HapticHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.enums.PityStatus;
import ru.menshovanton.gachapoint.domain.models.BannerInfo;
import ru.menshovanton.gachapoint.domain.models.Pull;
import ru.menshovanton.gachapoint.ui.animation.CarouselAnimator;
import ru.menshovanton.gachapoint.ui.animation.NumberCountAnimator;
import ru.menshovanton.gachapoint.ui.main.GamesConfigBottomSheetDialog;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class JournalView extends Fragment {

    private JournalViewModel viewModel;
    private MainActivityView mainActivityView;

    private AppBarLayout appbarLayout;
    private PillsAdapter pillsAdapter;
    private LinearLayoutManager gameTypesLayoutManager;
    private RecyclerView rvGameTypes;

    private FloatingActionButton btnAdd;
    private FloatingActionButton btnScrollTop;
    private RecyclerView.OnScrollListener scrollListener;
    private View tilBannerSelector;
    private View llSelectionBar;
    private TextView tvSelectionCount;
    private ImageButton btnCancelSelection;
    private ImageButton btnSelectAll;
    private ImageButton btnDeleteSelected;
    private AutoCompleteTextView bannerSelector;
    private RecyclerView recyclerView;
    private View emptyView;
    private View emptyContentView;
    private View cardBannerDashboard;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private MaterialButton btnEmptyAddOne;
    private MaterialButton btnEmptyAddTen;
    private LinearProgressIndicator progressBar;

    private TextView tvPity5StarCounter;
    private TextView tvLabel5Star;
    private LinearProgressIndicator progressPity5Star;
    private MaterialCardView badgePityStatus;
    private TextView tvPityStatusBadge;
    private TextView tvPity4StarCounter;
    private TextView tvLabel4Star;
    private LinearProgressIndicator progressPity4Star;

    private PullsAdapter pullsAdapter;
    private String lastLoadedBannerKey = null;
    private boolean isFirstLaunch = true;

    public JournalView() {}

    public static JournalView newInstance() {
        return new JournalView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityView = (MainActivityView) getActivity();
        viewModel = new ViewModelProvider(this).get(JournalViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivityView = (MainActivityView) getActivity();
        View view = inflater.inflate(R.layout.fragment_journal, container, false);

        appbarLayout = view.findViewById(R.id.appbar_journal);
        cardBannerDashboard = view.findViewById(R.id.card_banner_dashboard);
        rvGameTypes = view.findViewById(R.id.rv_game_types);
        btnAdd = view.findViewById(R.id.btn_add);
        btnScrollTop = view.findViewById(R.id.btn_scroll_top);
        tilBannerSelector = view.findViewById(R.id.til_banner_selector);
        llSelectionBar = view.findViewById(R.id.ll_selection_bar);
        tvSelectionCount = view.findViewById(R.id.tv_selection_count);
        btnCancelSelection = view.findViewById(R.id.btn_cancel_selection);
        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnDeleteSelected = view.findViewById(R.id.btn_delete_selected);
        bannerSelector = view.findViewById(R.id.mac_banner_selector);
        recyclerView = view.findViewById(R.id.rv_pulls_list);
        emptyView = view.findViewById(R.id.ll_empty_state);
        emptyContentView = view.findViewById(R.id.ll_empty_content);
        tvEmptyTitle = view.findViewById(R.id.tv_empty_title);
        tvEmptyDesc = view.findViewById(R.id.tv_empty_desc);
        btnEmptyAddOne = view.findViewById(R.id.btn_empty_add_one);
        btnEmptyAddTen = view.findViewById(R.id.btn_empty_add_ten);
        progressBar = view.findViewById(R.id.progress_loading);

        tvPity5StarCounter = view.findViewById(R.id.tv_pity_5star_counter);
        tvLabel5Star = view.findViewById(R.id.tv_label_5star);
        progressPity5Star = view.findViewById(R.id.progress_pity_5star);
        badgePityStatus = view.findViewById(R.id.badge_pity_status);
        tvPityStatusBadge = view.findViewById(R.id.tv_pity_status_badge);
        tvPity4StarCounter = view.findViewById(R.id.tv_pity_4star_counter);
        tvLabel4Star = view.findViewById(R.id.tv_label_4star);
        progressPity4Star = view.findViewById(R.id.progress_pity_4star);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        setupGameTypeSelector();
        setupRecyclerView();
        setupBannerSelector();
        setupSelectionBar();
        observeViewModel();

        btnAdd.setOnClickListener(this::showMoreMenu);
        btnScrollTop.setOnClickListener(v -> scrollToTop());

        if (cardBannerDashboard != null) {
            cardBannerDashboard.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(requireContext(), v);
                NavHostFragment.findNavController(this).navigate(R.id.action_journal_to_banners);
            });
        }

        if (btnEmptyAddOne != null) {
            btnEmptyAddOne.setOnClickListener(v -> viewModel.saveOneWish());
        }
        if (btnEmptyAddTen != null) {
            btnEmptyAddTen.setOnClickListener(v -> showWishDialog(null, getString(R.string.five_star), true));
        }

        if (appbarLayout != null && emptyView != null) {
            appbarLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (emptyView != null && appbarLayout != null) {
                    int scrollRange = appbarLayout.getTotalScrollRange();
                    if (emptyView.getPaddingBottom() != scrollRange) {
                        emptyView.setPadding(0, 0, 0, scrollRange);
                    }
                }
            });
        }
    }

    private void setupGameTypeSelector() {
        gameTypesLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvGameTypes.setLayoutManager(gameTypesLayoutManager);

        GameType currentType = (mainActivityView != null && mainActivityView.getSubType() != null)
                ? mainActivityView.getSubType()
                : GameType.GENSHIN;

        List<GameType> enabledGames = mainActivityView != null ? mainActivityView.getEnabledGames() : Collections.singletonList(GameType.GENSHIN);

        viewModel.setGameType(currentType);

        pillsAdapter = new PillsAdapter(
                enabledGames,
                currentType,
                (selectedGame, position) -> {
                    if (mainActivityView != null) {
                        mainActivityView.setSubType(selectedGame);
                    }
                    if (appbarLayout != null) {
                        appbarLayout.setExpanded(true, true);
                    }
                    if (btnScrollTop != null && btnScrollTop.getVisibility() == View.VISIBLE) {
                        btnScrollTop.hide();
                    }
                    viewModel.setGameType(selectedGame);
                    updateBannerSelector(selectedGame);
                    smoothScrollToCenter(position);
                },
                () -> GamesConfigBottomSheetDialog.show(requireContext(), () -> {
                    if (mainActivityView != null) mainActivityView.refreshGamesConfig();
                })
        );

        CarouselAnimator.setupItemAnimator(rvGameTypes);
        rvGameTypes.setAdapter(pillsAdapter);
        updatePillsVisibility();
    }

    private void updatePillsVisibility() {
        if (rvGameTypes == null || mainActivityView == null) return;
        List<GameType> enabled = mainActivityView.getEnabledGames();
        ViewGroup root = (getView() instanceof ViewGroup) ? (ViewGroup) getView() : null;
        boolean isMultiGame = enabled.size() > 1;

        CarouselAnimator.setCarouselVisibility(rvGameTypes, root, isMultiGame);

        if (!isMultiGame) {
            GameType monoGame = !enabled.isEmpty() ? enabled.get(0) : GameType.GENSHIN;
            if (mainActivityView.getSubType() != monoGame) {
                mainActivityView.setSubType(monoGame);
            }
            if (viewModel != null && viewModel.getCurrentGameType() != monoGame) {
                viewModel.setGameType(monoGame);
                updateBannerSelector(monoGame);
            }
        } else {
            if (pillsAdapter != null) {
                pillsAdapter.updateData(enabled, mainActivityView.getSubType());
                int pos = pillsAdapter.getPositionForGame(mainActivityView.getSubType());
                if (pos != -1) {
                    centerPositionInstantly(pos);
                }
            }
        }
    }

    private void setupSelectionBar() {
        btnCancelSelection.setOnClickListener(v -> pullsAdapter.clearSelection());

        btnSelectAll.setOnClickListener(v -> pullsAdapter.selectAll());

        btnDeleteSelected.setOnClickListener(v -> {
            Set<Integer> selectedIds = pullsAdapter.getSelectedPullIds();
            if (selectedIds.isEmpty()) return;

            Context contextThemeWrapper = new ContextThemeWrapper(requireContext(), R.style.Dialog_GachaPoint_AlertDialog);
            androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(contextThemeWrapper, R.style.Dialog_GachaPoint_AlertDialog)
                    .setTitle(R.string.delete_batch_confirm_title)
                    .setMessage(getString(R.string.delete_batch_confirm_message, selectedIds.size()))
                    .setPositiveButton(R.string.delete, (d, which) -> {
                        pullsAdapter.clearSelection();
                        viewModel.deleteWishesBatch(new ArrayList<>(selectedIds));
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();

            dialog.setOnShowListener(d -> {
                Button btnPositive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                if (btnPositive != null) {
                    btnPositive.setTextColor(ContextCompat.getColor(requireContext(), R.color.missed));
                }
            });
            dialog.show();
        });
    }

    private void updateSelectionUi(int selectedCount) {
        if (selectedCount > 0) {
            if (llSelectionBar.getVisibility() != View.VISIBLE) {
                tilBannerSelector.animate().cancel();
                llSelectionBar.animate().cancel();
                tilBannerSelector.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                    tilBannerSelector.setVisibility(View.GONE);
                    llSelectionBar.setAlpha(0f);
                    llSelectionBar.setVisibility(View.VISIBLE);
                    llSelectionBar.animate().alpha(1f).setDuration(150).start();
                }).start();
                btnAdd.hide();
            }
            tvSelectionCount.setText(getString(R.string.selected_count, selectedCount));
        } else {
            if (llSelectionBar.getVisibility() == View.VISIBLE) {
                tilBannerSelector.animate().cancel();
                llSelectionBar.animate().cancel();
                llSelectionBar.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                    llSelectionBar.setVisibility(View.GONE);
                    tilBannerSelector.setAlpha(0f);
                    tilBannerSelector.setVisibility(View.VISIBLE);
                    tilBannerSelector.animate().alpha(1f).setDuration(150).start();
                }).start();
                btnAdd.show();
            } else {
                tilBannerSelector.setVisibility(View.VISIBLE);
                tilBannerSelector.setAlpha(1f);
                llSelectionBar.setVisibility(View.GONE);
                btnAdd.show();
            }
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setItemAnimator(new SlideInItemAnimator());
        android.view.animation.LayoutAnimationController animationController =
                android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down);
        recyclerView.setLayoutAnimation(animationController);

        pullsAdapter = new PullsAdapter();
        pullsAdapter.setOnItemClickListener(this::showPullDetailsDialog);
        pullsAdapter.setOnItemLongClickListener(pull -> {
            triggerHapticFeedback();
            pullsAdapter.startSelection(pull);
        });
        pullsAdapter.setOnSelectionChangedListener(this::updateSelectionUi);
        recyclerView.setAdapter(pullsAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                if (pullsAdapter.isSelectionMode()) {
                    return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
                }
                return 0;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from != RecyclerView.NO_POSITION && to != RecyclerView.NO_POSITION) {
                    pullsAdapter.onItemMove(from, to);
                    return true;
                }
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    recyclerView.setNestedScrollingEnabled(false);
                    triggerHapticFeedback();
                    if (viewHolder != null) {
                        viewHolder.itemView.setAlpha(0.85f);
                        viewHolder.itemView.setScaleX(1.02f);
                        viewHolder.itemView.setScaleY(1.02f);
                    }
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    recyclerView.setNestedScrollingEnabled(true);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(rv, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setScaleX(1.0f);
                viewHolder.itemView.setScaleY(1.0f);
                recyclerView.setNestedScrollingEnabled(true);
                viewModel.onPullsReordered(pullsAdapter.getCurrentPullsList());
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
        pullsAdapter.setOnStartDragListener(itemTouchHelper::startDrag);

        scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null && btnScrollTop != null) {
                    int firstVisible = lm.findFirstVisibleItemPosition();
                    if (firstVisible >= 8) {
                        if (btnScrollTop.getVisibility() != View.VISIBLE) {
                            btnScrollTop.show();
                        }
                    } else {
                        if (btnScrollTop.getVisibility() == View.VISIBLE) {
                            btnScrollTop.hide();
                        }
                    }
                }
            }
        };
        recyclerView.addOnScrollListener(scrollListener);
    }

    public void scrollToTop() {
        if (recyclerView != null) {
            LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (lm != null) {
                int firstVisible = lm.findFirstVisibleItemPosition();
                if (firstVisible > 25) {
                    recyclerView.scrollToPosition(15);
                }
                recyclerView.smoothScrollToPosition(0);
            }
        }
        if (appbarLayout != null) {
            appbarLayout.setExpanded(true, true);
        }
        if (btnScrollTop != null && btnScrollTop.getVisibility() == View.VISIBLE) {
            btnScrollTop.hide();
        }
    }

    private void triggerHapticFeedback() {
        if (getView() != null) {
            getView().performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePillsVisibility();
        if (mainActivityView != null) {
            mainActivityView.setOnBottomNavReselectedListener(itemId -> {
                if (itemId == R.id.nav_journal) {
                    scrollToTop();
                }
            });
            if (mainActivityView.getSubType() != null) {
                GameType current = mainActivityView.getSubType();
                if (viewModel != null && viewModel.getCurrentGameType() != current) {
                    viewModel.setGameType(current);
                    updateBannerSelector(current);
                }
            }
        }
        if (!isFirstLaunch && viewModel != null) {
            viewModel.refreshData();
        }
        isFirstLaunch = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mainActivityView != null) {
            mainActivityView.setOnBottomNavReselectedListener(null);
        }
    }

    @Override
    public void onDestroyView() {
        if (bannerSelector != null) {
            bannerSelector.dismissDropDown();
            bannerSelector.setAdapter(null);
            bannerSelector = null;
        }
        if (recyclerView != null) {
            if (scrollListener != null) {
                recyclerView.removeOnScrollListener(scrollListener);
                scrollListener = null;
            }
            recyclerView.setAdapter(null);
            recyclerView = null;
        }
        if (rvGameTypes != null) {
            rvGameTypes.setAdapter(null);
            rvGameTypes = null;
        }
        mainActivityView = null;
        pillsAdapter = null;
        gameTypesLayoutManager = null;
        appbarLayout = null;
        cardBannerDashboard = null;
        btnAdd = null;
        btnScrollTop = null;
        tilBannerSelector = null;
        llSelectionBar = null;
        tvSelectionCount = null;
        btnCancelSelection = null;
        btnSelectAll = null;
        btnDeleteSelected = null;
        emptyView = null;
        emptyContentView = null;
        tvEmptyTitle = null;
        tvEmptyDesc = null;
        btnEmptyAddOne = null;
        btnEmptyAddTen = null;
        progressBar = null;
        tvPity5StarCounter = null;
        tvLabel5Star = null;
        progressPity5Star = null;
        badgePityStatus = null;
        tvPityStatusBadge = null;
        tvPity4StarCounter = null;
        tvLabel4Star = null;
        progressPity4Star = null;
        pullsAdapter = null;
        super.onDestroyView();
    }

    private void setupBannerSelector() {
        updateBannerSelector(viewModel.getCurrentGameType());

        bannerSelector.setOnItemClickListener((parent, itemView, position, id) -> {
            if (bannerSelector.getAdapter() instanceof NoFilterArrayAdapter) {
                ((NoFilterArrayAdapter<?>) bannerSelector.getAdapter()).setSelectedPosition(position);
            }
            String selectedLabel = (String) parent.getItemAtPosition(position);
            String bannerKey = viewModel.getCurrentGameType().getBannerKeyByLabel(requireContext(), selectedLabel);
            if (appbarLayout != null) {
                appbarLayout.setExpanded(true, true);
            }
            if (btnScrollTop != null && btnScrollTop.getVisibility() == View.VISIBLE) {
                btnScrollTop.hide();
            }
            viewModel.setBannerType(bannerKey);
            updateDashboardLimits();
            bannerSelector.dismissDropDown();
            bannerSelector.clearFocus();
        });
    }

    private void updateBannerSelector(GameType gameType) {
        if (gameType == null) return;
        List<BannerInfo> banners = gameType.getBanners();
        List<String> bannerLabels = new ArrayList<>(banners.size());
        for (BannerInfo banner : banners) {
            bannerLabels.add(getString(banner.getLabelResId()));
        }
        NoFilterArrayAdapter<String> adapter = new NoFilterArrayAdapter<>(requireContext(), R.layout.item_dropdown_option, bannerLabels);
        String currentKey = viewModel.getCurrentBannerType();
        String currentLabel = gameType.getBannerLabel(requireContext(), currentKey);
        adapter.setSelectedItem(currentLabel);
        bannerSelector.setAdapter(adapter);

        bannerSelector.setText(currentLabel, false);
        updateDashboardLimits();
        updateEmptyState(gameType);
    }

    private void updateEmptyState(GameType gameType) {
        if (gameType == null) return;
        if (tvEmptyTitle != null) {
            tvEmptyTitle.setText(gameType.getEmptyJournalTitleResId());
        }
        if (tvEmptyDesc != null) {
            tvEmptyDesc.setText(gameType.getEmptyJournalDescResId());
        }
        if (btnEmptyAddOne != null) {
            btnEmptyAddOne.setText(gameType.getAddOnePullResId());
        }
        if (btnEmptyAddTen != null) {
            btnEmptyAddTen.setText(gameType.getAddTenPullsResId());
        }
    }

    private void updateDashboardLimits() {
        BannerInfo bannerInfo = viewModel.getCurrentBannerInfo();
        if (bannerInfo != null) {
            progressPity5Star.setMax(bannerInfo.getMaxPity5Star());
            progressPity4Star.setMax(bannerInfo.getMaxPity4Star());
        }
        updatePityTexts();
    }

    @SuppressLint("SetTextI18n")
    private void updatePityTexts() {
        BannerInfo bannerInfo = viewModel.getCurrentBannerInfo();
        GameType gameType = viewModel.getCurrentGameType();
        if (tvLabel5Star != null) {
            boolean usesRanks = (gameType == GameType.ZZZ || gameType == GameType.HI3);
            tvLabel5Star.setText(usesRanks ? R.string.pity_s_rank_title : R.string.pity_5star_title);
        }
        if (tvLabel4Star != null) {
            boolean usesRanks = (gameType == GameType.ZZZ || gameType == GameType.HI3);
            tvLabel4Star.setText(usesRanks ? R.string.pity_a_rank_title : R.string.pity_4star_title);
        }

        int max5 = bannerInfo != null ? bannerInfo.getMaxPity5Star() : 90;
        int max4 = bannerInfo != null ? bannerInfo.getMaxPity4Star() : 10;

        Integer current5 = viewModel.getPity5StarLiveData().getValue();
        int val5 = current5 != null ? current5 : 0;
        NumberCountAnimator.animateFraction(tvPity5StarCounter, val5, max5);
        progressPity5Star.setProgressCompat(Math.min(val5, max5), true);

        Integer current4 = viewModel.getPity4StarLiveData().getValue();
        int val4 = current4 != null ? current4 : 0;
        NumberCountAnimator.animateFraction(tvPity4StarCounter, val4, max4);
        progressPity4Star.setProgressCompat(Math.min(val4, max4), true);

        boolean has5050 = bannerInfo != null && bannerInfo.hasFiftyFifty();
        if (has5050) {
            badgePityStatus.setVisibility(View.VISIBLE);
            Boolean isGuaranteed = viewModel.getIsGuaranteedLiveData().getValue();
            if (Boolean.TRUE.equals(isGuaranteed)) {
                tvPityStatusBadge.setText(R.string.pity_status_guaranteed_badge);
                badgePityStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.badge_green_bg));
                tvPityStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.checked));
            } else {
                tvPityStatusBadge.setText(R.string.pity_status_fifty_fifty);
                badgePityStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.badge_gold_bg));
                tvPityStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.rare_legendary));
            }
        } else {
            badgePityStatus.setVisibility(View.GONE);
        }
    }

    private void observeViewModel() {
        if (mainActivityView != null) {
            mainActivityView.getMainActivityViewModel().getEnabledGamesLiveData().observe(getViewLifecycleOwner(), games ->
                    updatePillsVisibility());
            mainActivityView.getMainActivityViewModel().getSubTypeLiveData().observe(getViewLifecycleOwner(), game -> {
                if (pillsAdapter != null) {
                    pillsAdapter.setSelectedGame(game);
                    int pos = pillsAdapter.getPositionForGame(game);
                    if (pos != -1) {
                        if (rvGameTypes != null && rvGameTypes.isLaidOut()) {
                            smoothScrollToCenter(pos);
                        } else {
                            centerPositionInstantly(pos);
                        }
                    }
                }
                if (viewModel != null && viewModel.getCurrentGameType() != game) {
                    viewModel.setGameType(game);
                    updateBannerSelector(game);
                }
            });
        }

        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                progressBar.animate().cancel();
                progressBar.setAlpha(0f);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.animate().alpha(1f).setDuration(120).start();
            } else {
                progressBar.animate().cancel();
                progressBar.animate().alpha(0f).setDuration(120).withEndAction(() -> progressBar.setVisibility(View.GONE)).start();
            }
        });

        viewModel.getWishesLiveData().observe(getViewLifecycleOwner(), pulls -> {
            if (pulls == null) return;
            boolean isEmpty = pulls.isEmpty();
            String currentBannerKey = viewModel.getCurrentBannerType();
            boolean bannerChanged = !Objects.equals(lastLoadedBannerKey, currentBannerKey);
            lastLoadedBannerKey = currentBannerKey;
            int previousSize = pullsAdapter.getItemCount();

            pullsAdapter.setCurrentGameType(viewModel.getCurrentGameType());

            List<Pull> newList = new ArrayList<>(pulls);
            pullsAdapter.submitList(newList, () -> {
                progressBar.setVisibility(View.GONE);

                if (isEmpty) {
                    if (emptyView.getVisibility() != View.VISIBLE) {
                        crossFadeViews(recyclerView, emptyView);
                    }
                    if (appbarLayout != null) {
                        appbarLayout.setExpanded(true, true);
                        int scrollRange = appbarLayout.getTotalScrollRange();
                        if (emptyView != null && emptyView.getPaddingBottom() != scrollRange) {
                            emptyView.setPadding(0, 0, 0, scrollRange);
                        }
                    }
                    if (btnScrollTop != null && btnScrollTop.getVisibility() == View.VISIBLE) {
                        btnScrollTop.hide();
                    }
                } else {
                    if (recyclerView.getVisibility() != View.VISIBLE) {
                        crossFadeViews(emptyView, recyclerView);
                        recyclerView.scheduleLayoutAnimation();
                    } else if (bannerChanged) {
                        recyclerView.scheduleLayoutAnimation();
                    }
                }

                if (appbarLayout != null && appbarLayout.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
                    CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) appbarLayout.getLayoutParams();
                    AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) lp.getBehavior();
                    if (behavior == null) {
                        behavior = new AppBarLayout.Behavior();
                        lp.setBehavior(behavior);
                    }
                    behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                        @Override
                        public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                            return !isEmpty;
                        }
                    });
                }

                if (!isEmpty && (newList.size() > previousSize || bannerChanged)) {
                    recyclerView.post(() -> {
                        LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (lm != null) {
                            lm.scrollToPositionWithOffset(0, 0);
                        }
                    });
                }
            });
        });

        viewModel.getPity5StarLiveData().observe(getViewLifecycleOwner(), pity5 -> updatePityTexts());
        viewModel.getPity4StarLiveData().observe(getViewLifecycleOwner(), pity4 -> updatePityTexts());
        viewModel.getIsGuaranteedLiveData().observe(getViewLifecycleOwner(), guaranteed -> updatePityTexts());

        viewModel.getCurrentBannerTypeLiveData().observe(getViewLifecycleOwner(), bannerKey -> {
            String label = viewModel.getCurrentGameType().getBannerLabel(requireContext(), bannerKey);
            bannerSelector.setText(label, false);
            if (bannerSelector.getAdapter() instanceof NoFilterArrayAdapter) {
                ((NoFilterArrayAdapter<?>) bannerSelector.getAdapter()).setSelectedItem(label);
            }
            updateDashboardLimits();
        });

        viewModel.getOpenDialogEvent().observe(getViewLifecycleOwner(), unused ->
                showWishDialog(null, getString(R.string.four_star), false)
        );
    }

    private void crossFadeViews(View viewToHide, View viewToShow) {
        if (viewToShow.getVisibility() == View.VISIBLE && viewToHide.getVisibility() == View.GONE) {
            return;
        }

        viewToShow.animate().cancel();
        viewToHide.animate().cancel();

        viewToShow.setAlpha(0f);
        viewToShow.setVisibility(View.VISIBLE);

        if (viewToShow == emptyView && emptyContentView != null) {
            emptyContentView.setScaleX(0.92f);
            emptyContentView.setScaleY(0.92f);
            emptyContentView.setAlpha(0f);
            emptyContentView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(240)
                    .setInterpolator(new DecelerateInterpolator())
                    .setListener(null);
        }

        viewToShow.animate()
                .alpha(1f)
                .setDuration(240)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(null);

        if (viewToHide == emptyView && emptyContentView != null) {
            emptyContentView.animate()
                    .alpha(0f)
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(220)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(null);
        }

        viewToHide.animate()
                .alpha(0f)
                .setDuration(240)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewToHide.setVisibility(View.GONE);
                        viewToHide.setAlpha(1f);
                        if (emptyContentView != null) {
                            emptyContentView.setScaleX(1f);
                            emptyContentView.setScaleY(1f);
                            emptyContentView.setAlpha(1f);
                        }
                    }
                });
    }

    private void showMoreMenu(View view) {
        if (mainActivityView == null) return;

        mainActivityView.showWishCounterMenu(new MainActivityView.OnWishCounterMenuClickListener() {
            @Override
            public void onAddOneAttempt() {
                viewModel.saveOneWish();
            }

            @Override
            public void onAddTenAttempts() {
                showWishDialog(null, getString(R.string.five_star), true);
            }

            @Override
            public void onAddFiveStarDrop() {
                showWishDialog(null, getString(R.string.five_star), false);
            }

            @Override
            public void onAddFourStarDrop() {
                showWishDialog(null, getString(R.string.four_star), false);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void showPullDetailsDialog(@NonNull Pull pull) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        @SuppressLint("InflateParams") View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pull_details, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            android.widget.FrameLayout bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<android.widget.FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        GameType currentGameType = viewModel.getCurrentGameType();
        Context context = requireContext();

        MaterialCardView cardAvatar = view.findViewById(R.id.card_details_avatar);
        ImageView ivAvatar = view.findViewById(R.id.iv_details_avatar);
        TextView tvLetter = view.findViewById(R.id.tv_details_avatar_letter);
        TextView tvName = view.findViewById(R.id.tv_details_item_name);
        MaterialCardView cardRarity = view.findViewById(R.id.card_details_rarity_badge);
        TextView tvRarity = view.findViewById(R.id.tv_details_rarity);
        MaterialCardView cardPity = view.findViewById(R.id.card_details_pity_badge);
        TextView tvPity = view.findViewById(R.id.tv_details_pity);
        MaterialCardView cardOutcome = view.findViewById(R.id.card_details_outcome);
        TextView tvOutcome = view.findViewById(R.id.tv_details_outcome);
        TextView tvBanner = view.findViewById(R.id.tv_details_banner_title);
        TextView tvDate = view.findViewById(R.id.tv_details_date);

        MaterialButton btnEdit = view.findViewById(R.id.btn_details_edit);
        MaterialButton btnDuplicate = view.findViewById(R.id.btn_details_duplicate);
        ImageButton btnDelete = view.findViewById(R.id.btn_details_delete);
        ImageButton btnClose = view.findViewById(R.id.btn_details_close);

        String name = pull.getDropType() != null ? pull.getDropType().trim() : "";
        tvName.setText(name);

        RareType rareType = RareType.from(pull.getDropRare(), currentGameType, context);
        if (rareType == RareType.LEGENDARY) {
            tvRarity.setText(Rarity.LEGENDARY.getDisplayRank(currentGameType, context));
            tvRarity.setTextColor(ContextCompat.getColor(context, R.color.rare_legendary));
            cardRarity.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_gold_bg));
            cardAvatar.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_gold_bg));
            cardAvatar.setStrokeColor(ContextCompat.getColor(context, R.color.rare_legendary));
            tvLetter.setTextColor(ContextCompat.getColor(context, R.color.rare_legendary));
            tvPity.setText(String.format(getString(R.string.pity_pull_at), pull.getPityNumber()));
        } else if (rareType == RareType.EPIC) {
            tvRarity.setText(Rarity.EPIC.getDisplayRank(currentGameType, context));
            tvRarity.setTextColor(ContextCompat.getColor(context, R.color.rare_epic));
            cardRarity.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_purple_bg));
            cardAvatar.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_purple_bg));
            cardAvatar.setStrokeColor(ContextCompat.getColor(context, R.color.rare_epic));
            tvLetter.setTextColor(ContextCompat.getColor(context, R.color.rare_epic));
            tvPity.setText(String.format(getString(R.string.pity_pull_at), pull.getPity4StarNumber()));
        } else {
            tvRarity.setText(Rarity.COMMON.getDisplayRank(currentGameType, context));
            tvRarity.setTextColor(ContextCompat.getColor(context, R.color.check));
            cardRarity.setCardBackgroundColor(ContextCompat.getColor(context, R.color.pull_pity_badge_bg));
            cardAvatar.setCardBackgroundColor(ContextCompat.getColor(context, R.color.pull_pity_badge_bg));
            cardAvatar.setStrokeColor(ContextCompat.getColor(context, R.color.pull_row_stroke));
            tvLetter.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            tvPity.setText(String.format(getString(R.string.pity_pull_at), pull.getPityNumber()));
        }

        String letter = !name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "•";
        tvLetter.setText(letter);
        CatalogItem catalogItem = CatalogRepository.getInstance(context).findItemByName(currentGameType, name);
        String iconUrl = (catalogItem != null) ? catalogItem.getIconUrl() : null;
        AvatarImageLoader.loadAvatar(context, iconUrl, name, ivAvatar, tvLetter);

        PityStatus pityStatus = pull.getPityStatus();
        if (rareType == RareType.LEGENDARY && pityStatus != null && pityStatus != PityStatus.NONE) {
            cardOutcome.setVisibility(View.VISIBLE);
            tvOutcome.setText(pityStatus.getLabelResId());
            if (pityStatus == PityStatus.WIN_50_50) {
                cardOutcome.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_green_bg));
                cardOutcome.setStrokeColor(ContextCompat.getColor(context, R.color.checked));
                tvOutcome.setTextColor(ContextCompat.getColor(context, R.color.checked));
            } else if (pityStatus == PityStatus.LOSS_50_50) {
                cardOutcome.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_purple_bg));
                cardOutcome.setStrokeColor(ContextCompat.getColor(context, R.color.missed));
                tvOutcome.setTextColor(ContextCompat.getColor(context, R.color.missed));
            } else {
                cardOutcome.setCardBackgroundColor(ContextCompat.getColor(context, R.color.pull_row_selected_bg));
                cardOutcome.setStrokeColor(ContextCompat.getColor(context, R.color.check));
                tvOutcome.setTextColor(ContextCompat.getColor(context, R.color.check));
            }
        } else {
            cardOutcome.setVisibility(View.GONE);
        }

        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault());
        DateTimeFormatter dbFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        if (pull.getDateTime() != null) {
            try {
                LocalDate date = LocalDate.parse(pull.getDateTime(), dbFormatter);
                tvDate.setText(date.format(displayFormatter));
            } catch (Exception e) {
                tvDate.setText(pull.getDateTime());
            }
        } else {
            tvDate.setText("");
        }

        String bannerLabel = currentGameType.getBannerLabel(context, pull.getBannerType());
        String patch = BannerScheduleRepository.getInstance(context).getPatchForDate(currentGameType, pull.getDateTime());
        if (patch != null && !patch.isEmpty()) {
            tvBanner.setText(bannerLabel + " • " + patch);
        } else {
            tvBanner.setText(bannerLabel);
        }

        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showWishDialog(pull, null, false);
        });

        btnDuplicate.setOnClickListener(v -> {
            viewModel.duplicatePull(pull);
            dialog.dismiss();
            Toast.makeText(context, R.string.journal_duplicate_success, Toast.LENGTH_SHORT).show();
            HapticHelper.performClickHaptic(context, btnDuplicate);
        });

        btnDelete.setOnClickListener(v -> {
            Context contextThemeWrapper = new ContextThemeWrapper(context, R.style.Dialog_GachaPoint_AlertDialog);
            androidx.appcompat.app.AlertDialog confirmDialog = new MaterialAlertDialogBuilder(contextThemeWrapper, R.style.Dialog_GachaPoint_AlertDialog)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.delete_confirm_message)
                    .setPositiveButton(R.string.delete, (d, w) -> {
                        dialog.dismiss();
                        viewModel.deleteWish(pull.getId());
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            confirmDialog.setOnShowListener(cd -> {
                Button btnPositive = confirmDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                if (btnPositive != null) {
                    btnPositive.setTextColor(ContextCompat.getColor(context, R.color.missed));
                }
            });
            confirmDialog.show();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @SuppressLint("SetTextI18n")
    private void showWishDialog(@Nullable Pull wishToEdit, @Nullable String defaultRarity, boolean isBatch) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        @SuppressLint("InflateParams") View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_pull_record, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            android.widget.FrameLayout bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<android.widget.FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        TextView dialogTitle = dialogView.findViewById(R.id.tv_dialog_header);
        ImageButton btnDelete = dialogView.findViewById(R.id.btn_dialog_delete);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        GameType currentGameType = viewModel.getCurrentGameType();
        MaterialButtonToggleGroup toggleRarity = dialogView.findViewById(R.id.toggle_rarity);
        MaterialButton btnRare5 = dialogView.findViewById(R.id.btn_rare_5);
        MaterialButton btnRare4 = dialogView.findViewById(R.id.btn_rare_4);
        MaterialButton btnRare3 = dialogView.findViewById(R.id.btn_rare_3);
        if (btnRare5 != null) btnRare5.setText(ru.menshovanton.gachapoint.domain.enums.Rarity.LEGENDARY.getDisplayRank(currentGameType, requireContext()));
        if (btnRare4 != null) btnRare4.setText(ru.menshovanton.gachapoint.domain.enums.Rarity.EPIC.getDisplayRank(currentGameType, requireContext()));
        if (btnRare3 != null) btnRare3.setText(ru.menshovanton.gachapoint.domain.enums.Rarity.COMMON.getDisplayRank(currentGameType, requireContext()));

        View llPityStatusSection = dialogView.findViewById(R.id.ll_pity_status_section);
        MaterialButtonToggleGroup togglePityStatus = dialogView.findViewById(R.id.toggle_pity_status);
        MaterialButton btnPityWin = dialogView.findViewById(R.id.btn_pity_win);
        MaterialButton btnPityLoss = dialogView.findViewById(R.id.btn_pity_loss);
        MaterialButton btnPityGuaranteed = dialogView.findViewById(R.id.btn_pity_guaranteed);

        View llCustomInputContainer = dialogView.findViewById(R.id.ll_custom_input_container);
        View llCatalogMatchInfo = dialogView.findViewById(R.id.ll_catalog_match_info);
        ImageView ivMatchAvatar = dialogView.findViewById(R.id.iv_match_avatar);
        TextView tvMatchLetter = dialogView.findViewById(R.id.tv_match_letter);
        TextView tvMatchInfo = dialogView.findViewById(R.id.tv_match_info);

        MaterialAutoCompleteTextView editDropType = dialogView.findViewById(R.id.tiet_drop_name_edit);
        TextInputEditText editDate = dialogView.findViewById(R.id.tiet_edit_date);
        MaterialButton btnCopyPrevDate = dialogView.findViewById(R.id.btn_copy_prev_date);
        MaterialButton btnPickBannerDate = dialogView.findViewById(R.id.btn_pick_banner_date);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_pull);
        LinearLayout llDropChips = dialogView.findViewById(R.id.ll_drop_chips);
        View btnToggleCustomName = dialogView.findViewById(R.id.btn_toggle_custom_name);
        ImageView ivCustomNameChevron = dialogView.findViewById(R.id.iv_custom_name_chevron);

        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault());
        DateTimeFormatter dbFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        final LocalDate[] selectedDate = new LocalDate[1];
        final BannerScheduleItem[] selectedBannerSchedule = new BannerScheduleItem[1];
        final boolean[] isSelectingFromChip = new boolean[]{false};
        boolean isEditMode = (wishToEdit != null);
        if (isEditMode) {
            try {
                selectedDate[0] = LocalDate.parse(wishToEdit.getDateTime(), dbFormatter);
            } catch (Exception e) {
                selectedDate[0] = LocalDate.now();
            }
        } else {
            selectedDate[0] = LocalDate.now();
        }
        final boolean[] suppressAutoOutcomeChange = new boolean[]{isEditMode};

        BannerInfo bannerInfo = viewModel.getCurrentBannerInfo();
        boolean bannerHas5050 = bannerInfo != null && bannerInfo.hasFiftyFifty();
        String currentBannerType = (wishToEdit != null && wishToEdit.getBannerType() != null)
                ? wishToEdit.getBannerType()
                : viewModel.getCurrentBannerType();
        boolean isCharacterBanner = BannerType.EVENT.getDbKey().equals(currentBannerType);
        boolean isWeaponBanner = BannerType.WEAPON.getDbKey().equals(currentBannerType);
        boolean isStandardBanner = BannerType.STANDARD.getDbKey().equals(currentBannerType)
                || BannerType.BEGINNER.getDbKey().equals(currentBannerType);

        CatalogRepository catalogRepo = CatalogRepository.getInstance(requireContext());
        List<CatalogItem> catalogItems = catalogRepo.getItemsForGame(currentGameType);

        java.util.function.Function<LocalDate, BannerScheduleItem> resolveBannerForDate = date -> {
            if (selectedBannerSchedule[0] != null) {
                try {
                    LocalDate start = LocalDate.parse(selectedBannerSchedule[0].getStartDate(), dbFormatter);
                    LocalDate end = LocalDate.parse(selectedBannerSchedule[0].getEndDate(), dbFormatter);
                    if (!date.isBefore(start) && !date.isAfter(end)) {
                        return selectedBannerSchedule[0];
                    }
                } catch (Exception ignored) {}
            }
            BannerScheduleRepository bannerRepo = BannerScheduleRepository.getInstance(requireContext());
            BannerScheduleItem b = bannerRepo.getBannerForDate(currentGameType, date);
            return b != null ? b : bannerRepo.getCurrentBanner(currentGameType);
        };

        java.lang.Runnable updateOutcomeVisuals = () -> {
            updateOutcomeButtonsVisuals(togglePityStatus, btnPityWin, btnPityLoss, btnPityGuaranteed);
        };

        java.lang.Runnable updateOutcomeSection = () -> {
            if (!bannerHas5050 || toggleRarity.getCheckedButtonId() != R.id.btn_rare_5) {
                if (llPityStatusSection != null) llPityStatusSection.setVisibility(View.GONE);
                return;
            }
            if (llPityStatusSection != null) llPityStatusSection.setVisibility(View.VISIBLE);

            BannerScheduleItem activeBanner = resolveBannerForDate.apply(selectedDate[0]);
            List<String> featuredFives = (activeBanner != null && !isStandardBanner) ? activeBanner.getFeaturedFiveStars(requireContext()) : Collections.emptyList();

            String currentName = editDropType.getText() != null ? editDropType.getText().toString().trim() : "";
            boolean isFeatured = false;
            for (String feat : featuredFives) {
                if (feat != null && (feat.trim().equalsIgnoreCase(currentName)
                        || CatalogRepository.cleanKey(feat).equals(CatalogRepository.cleanKey(currentName)))) {
                    isFeatured = true;
                    break;
                }
            }
            boolean isStd = catalogRepo.isStandardFiveStar(currentGameType, currentName);

            if (suppressAutoOutcomeChange[0]) {
                updateOutcomeVisuals.run();
                return;
            }

            if (isStd || (!featuredFives.isEmpty() && !isFeatured)) {
                togglePityStatus.check(R.id.btn_pity_loss);
            } else {
                Boolean guaranteed = viewModel.getIsGuaranteedLiveData().getValue();
                if (Boolean.TRUE.equals(guaranteed)) {
                    togglePityStatus.check(R.id.btn_pity_guaranteed);
                } else {
                    togglePityStatus.check(R.id.btn_pity_win);
                }
            }
            updateOutcomeVisuals.run();
        };

        if (togglePityStatus != null) {
            togglePityStatus.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    suppressAutoOutcomeChange[0] = true;
                    updateOutcomeVisuals.run();
                }
            });
        }

        java.lang.Runnable updateCatalogMatch = () -> {
            String currentText = editDropType.getText() != null ? editDropType.getText().toString().trim() : "";
            if (currentText.isEmpty() || llCatalogMatchInfo == null) {
                if (llCatalogMatchInfo != null) llCatalogMatchInfo.setVisibility(View.GONE);
                return;
            }
            CatalogItem matched = catalogRepo.findItemByName(currentGameType, currentText);
            if (matched != null) {
                llCatalogMatchInfo.setVisibility(View.VISIBLE);
                String rRank = matched.isFiveStar() ? getString(R.string.five_star) : (matched.isFourStar() ? getString(R.string.four_star) : getString(R.string.three_star));
                tvMatchInfo.setText(String.format(getString(R.string.journal_catalog_matched), matched.getName(requireContext()) + " (" + rRank + ")"));
                tvMatchInfo.setTextColor(ContextCompat.getColor(requireContext(), R.color.check));
                String letter = !matched.getName(requireContext()).isEmpty() ? String.valueOf(matched.getName(requireContext()).charAt(0)).toUpperCase() : "•";
                tvMatchLetter.setText(letter);
                AvatarImageLoader.loadAvatar(requireContext(), matched.getIconUrl(), matched.getName(requireContext()), ivMatchAvatar, tvMatchLetter);
            } else {
                llCatalogMatchInfo.setVisibility(View.VISIBLE);
                tvMatchInfo.setText(R.string.journal_custom_name_hint);
                tvMatchInfo.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                if (ivMatchAvatar != null) ivMatchAvatar.setVisibility(View.GONE);
                if (tvMatchLetter != null) {
                    tvMatchLetter.setVisibility(View.VISIBLE);
                    tvMatchLetter.setText("?");
                }
            }
        };

        java.util.function.Consumer<Integer> populateChips = rarityId -> {
            if (llDropChips == null) return;
            llDropChips.removeAllViews();

            String query = editDropType.getText() != null ? editDropType.getText().toString().trim() : "";
            String queryClean = CatalogRepository.cleanKey(query);
            boolean isQuerySearch = !queryClean.isEmpty() && editDropType.hasFocus() && !isSelectingFromChip[0];

            if (rarityId == R.id.btn_rare_3) {
                List<String> trashPresets = getTrashDropPresets(currentGameType);
                final MaterialCardView[] selectedChipCard = new MaterialCardView[1];

                for (String trashName : trashPresets) {
                    if (isQuerySearch) {
                        String cleanName = CatalogRepository.cleanKey(trashName);
                        if (!cleanName.contains(queryClean)) {
                            continue;
                        }
                    }

                    View chipView = LayoutInflater.from(requireContext()).inflate(R.layout.item_drop_chip, llDropChips, false);
                    MaterialCardView cardChip = chipView.findViewById(R.id.card_chip);
                    TextView tvName = chipView.findViewById(R.id.tv_chip_name);
                    TextView tvLetter = chipView.findViewById(R.id.tv_chip_letter);
                    ImageView ivAvatar = chipView.findViewById(R.id.iv_chip_avatar);
                    if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);

                    tvName.setText(trashName);
                    tvLetter.setText(ru.menshovanton.gachapoint.domain.enums.Rarity.COMMON.getDisplayRank(currentGameType, requireContext()));

                    boolean isSelected = (!query.isEmpty() && query.equalsIgnoreCase(trashName));
                    if (isSelected || (selectedChipCard[0] == null && query.isEmpty())) {
                        selectedChipCard[0] = cardChip;
                        cardChip.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.badge_blue_bg)));
                        cardChip.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.check)));
                        if (query.isEmpty()) {
                            isSelectingFromChip[0] = true;
                            editDropType.setText(trashName, false);
                            isSelectingFromChip[0] = false;
                        }
                    } else {
                        cardChip.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pull_row_bg)));
                        cardChip.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pull_row_stroke)));
                    }

                    cardChip.setOnClickListener(v -> {
                        if (selectedChipCard[0] != null) {
                            selectedChipCard[0].setCardBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pull_row_bg)));
                            selectedChipCard[0].setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pull_row_stroke)));
                        }
                        selectedChipCard[0] = cardChip;
                        cardChip.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.badge_blue_bg)));
                        cardChip.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.check)));
                        isSelectingFromChip[0] = true;
                        editDropType.setText(trashName, false);
                        isSelectingFromChip[0] = false;
                        HapticHelper.performClickHaptic(requireContext(), cardChip);
                        updateOutcomeSection.run();
                        updateCatalogMatch.run();
                    });

                    llDropChips.addView(chipView);
                }
                updateOutcomeSection.run();
                updateCatalogMatch.run();
                return;
            }

            BannerScheduleItem activeBanner = resolveBannerForDate.apply(selectedDate[0]);
            List<String> featuredFives = (activeBanner != null && !isStandardBanner) ? activeBanner.getFeaturedFiveStars(requireContext()) : Collections.emptyList();
            List<String> featuredFours = (activeBanner != null && !isStandardBanner) ? activeBanner.getFeaturedFourStars(requireContext()) : Collections.emptyList();

            boolean isFive = (rarityId == R.id.btn_rare_5);
            List<CatalogItem> filtered = new ArrayList<>();
            List<CatalogItem> priorityList = new ArrayList<>();
            List<CatalogItem> others = new ArrayList<>();
            Set<String> addedNames = new HashSet<>();

            if (isFive) {
                if (isQuerySearch) {
                    for (CatalogItem item : catalogItems) {
                        if (item.isFiveStar()) {
                            if (isCharacterBanner && !item.getType().equalsIgnoreCase("CHARACTER")) continue;
                            if (isWeaponBanner && !item.getType().equalsIgnoreCase("WEAPON")) continue;
                            if (matchesCatalogQuery(item, queryClean)) {
                                if (isItemFeatured(item, featuredFives)) {
                                    priorityList.add(item);
                                } else {
                                    others.add(item);
                                }
                            }
                        }
                    }
                } else {
                    for (String featName : featuredFives) {
                        CatalogItem featItem = catalogRepo.findItemByName(currentGameType, featName);
                        if (featItem != null && featItem.isFiveStar() && addedNames.add(featItem.getName())) {
                            priorityList.add(featItem);
                        }
                    }
                    for (CatalogItem item : catalogItems) {
                        if (item.isFiveStar() && item.isStandard()) {
                            if (isCharacterBanner && !item.getType().equalsIgnoreCase("CHARACTER")) continue;
                            if (isWeaponBanner && !item.getType().equalsIgnoreCase("WEAPON")) continue;
                            if (addedNames.add(item.getName())) {
                                others.add(item);
                            }
                        }
                    }
                    if (isStandardBanner) {
                        for (CatalogItem item : catalogItems) {
                            if (item.isFiveStar() && item.isStandard() && item.getType().equalsIgnoreCase("WEAPON")) {
                                if (addedNames.add(item.getName())) {
                                    others.add(item);
                                }
                            }
                        }
                    }
                }
            } else {
                if (isQuerySearch) {
                    for (CatalogItem item : catalogItems) {
                        if (item.isFourStar()) {
                            if (matchesCatalogQuery(item, queryClean)) {
                                if (isItemFeatured(item, featuredFours)) {
                                    priorityList.add(item);
                                } else {
                                    others.add(item);
                                }
                            }
                        }
                    }
                } else {
                    for (String fourName : featuredFours) {
                        CatalogItem fourItem = catalogRepo.findItemByName(currentGameType, fourName);
                        if (fourItem != null && fourItem.isFourStar() && addedNames.add(fourItem.getName())) {
                            priorityList.add(fourItem);
                        }
                    }
                    if (priorityList.isEmpty() || isStandardBanner) {
                        for (CatalogItem item : catalogItems) {
                            if (item.isFourStar() && item.isStandard() && addedNames.add(item.getName())) {
                                others.add(item);
                            }
                        }
                    }
                }
            }

            filtered.addAll(priorityList);
            filtered.addAll(others);

            final MaterialCardView[] selectedChipCard = new MaterialCardView[1];
            int selectedBgColor = ContextCompat.getColor(requireContext(), isFive ? R.color.badge_gold_bg : R.color.badge_purple_bg);
            int selectedStrokeColor = ContextCompat.getColor(requireContext(), isFive ? R.color.rare_legendary : R.color.rare_epic);

            for (CatalogItem item : filtered) {
                View chipView = LayoutInflater.from(requireContext()).inflate(R.layout.item_drop_chip, llDropChips, false);
                MaterialCardView cardChip = chipView.findViewById(R.id.card_chip);
                TextView tvName = chipView.findViewById(R.id.tv_chip_name);
                TextView tvLetter = chipView.findViewById(R.id.tv_chip_letter);
                ImageView ivAvatar = chipView.findViewById(R.id.iv_chip_avatar);

                String itemName = item.getName(requireContext());
                tvName.setText(itemName);

                AvatarImageLoader.loadAvatar(
                        requireContext(),
                        item.getIconUrl(),
                        itemName,
                        ivAvatar,
                        tvLetter
                );

                boolean isSelected = (!query.isEmpty() && (query.equalsIgnoreCase(itemName)
                        || query.equalsIgnoreCase(item.getName())
                        || query.equalsIgnoreCase(item.getNameRu())
                        || query.equalsIgnoreCase(item.getNameEn())));
                if (isSelected || (selectedChipCard[0] == null && query.isEmpty())) {
                    selectedChipCard[0] = cardChip;
                    cardChip.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(selectedBgColor));
                    cardChip.setStrokeColor(android.content.res.ColorStateList.valueOf(selectedStrokeColor));
                    if (query.isEmpty()) {
                        isSelectingFromChip[0] = true;
                        editDropType.setText(itemName, false);
                        isSelectingFromChip[0] = false;
                    }
                } else {
                    cardChip.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pull_row_bg)));
                    cardChip.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pull_row_stroke)));
                }

                cardChip.setOnClickListener(v -> {
                    if (selectedChipCard[0] != null) {
                        selectedChipCard[0].setCardBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pull_row_bg)));
                        selectedChipCard[0].setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pull_row_stroke)));
                    }
                    selectedChipCard[0] = cardChip;
                    cardChip.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(selectedBgColor));
                    cardChip.setStrokeColor(android.content.res.ColorStateList.valueOf(selectedStrokeColor));
                    isSelectingFromChip[0] = true;
                    editDropType.setText(itemName, false);
                    isSelectingFromChip[0] = false;
                    suppressAutoOutcomeChange[0] = false;
                    HapticHelper.performClickHaptic(requireContext(), cardChip);
                    updateOutcomeSection.run();
                    updateCatalogMatch.run();
                });

                llDropChips.addView(chipView);
            }
            updateOutcomeSection.run();
            updateCatalogMatch.run();
        };

        if (btnToggleCustomName != null) {
            btnToggleCustomName.setOnClickListener(v -> {
                if (llCustomInputContainer != null) {
                    boolean isVis = llCustomInputContainer.getVisibility() == View.VISIBLE;
                    llCustomInputContainer.setVisibility(isVis ? View.GONE : View.VISIBLE);
                    if (ivCustomNameChevron != null) {
                        ivCustomNameChevron.animate().rotation(isVis ? 0f : 180f).setDuration(200).start();
                    }
                    if (!isVis) {
                        editDropType.requestFocus();
                    }
                }
            });
        }

        editDropType.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateCatalogMatch.run();
                if (!isSelectingFromChip[0]) {
                    populateChips.accept(toggleRarity.getCheckedButtonId());
                }
            }
        });

        if (isEditMode) {
            dialogTitle.setText(getString(R.string.edit_pull));
            btnDelete.setVisibility(View.VISIBLE);
            btnSave.setText(getString(R.string.save));
            isSelectingFromChip[0] = true;
            editDropType.setText(wishToEdit.getDropType());
            isSelectingFromChip[0] = false;
            if (llCustomInputContainer != null) {
                llCustomInputContainer.setVisibility(View.VISIBLE);
            }
            if (ivCustomNameChevron != null) {
                ivCustomNameChevron.setRotation(180f);
            }

            try {
                selectedDate[0] = LocalDate.parse(wishToEdit.getDateTime(), dbFormatter);
            } catch (Exception e) {
                selectedDate[0] = LocalDate.now();
            }

            String rarity = wishToEdit.getDropRare();
            if (getString(R.string.four_star).equalsIgnoreCase(rarity)) {
                toggleRarity.check(R.id.btn_rare_4);
            } else if (getString(R.string.five_star).equalsIgnoreCase(rarity)) {
                toggleRarity.check(R.id.btn_rare_5);
            } else {
                toggleRarity.check(R.id.btn_rare_3);
            }

            PityStatus currentStatus = wishToEdit.getPityStatus();
            if (currentStatus == PityStatus.LOSS_50_50) {
                togglePityStatus.check(R.id.btn_pity_loss);
            } else if (currentStatus == PityStatus.GUARANTEED) {
                togglePityStatus.check(R.id.btn_pity_guaranteed);
            } else {
                togglePityStatus.check(R.id.btn_pity_win);
            }
        } else {
            dialogTitle.setText(getString(R.string.add_pull));
            btnDelete.setVisibility(View.GONE);
            btnSave.setText(getString(R.string.add_pull));
            selectedDate[0] = LocalDate.now();

            if (getString(R.string.five_star).equalsIgnoreCase(defaultRarity)) {
                toggleRarity.check(R.id.btn_rare_5);
            } else if (getString(R.string.four_star).equalsIgnoreCase(defaultRarity)) {
                toggleRarity.check(R.id.btn_rare_4);
            } else {
                toggleRarity.check(R.id.btn_rare_3);
            }

            Boolean isCurrentGuaranteed = viewModel.getIsGuaranteedLiveData().getValue();
            if (Boolean.TRUE.equals(isCurrentGuaranteed)) {
                togglePityStatus.check(R.id.btn_pity_guaranteed);
            } else {
                togglePityStatus.check(R.id.btn_pity_win);
            }
            if (ivCustomNameChevron != null) {
                ivCustomNameChevron.setRotation(0f);
            }
        }

        editDate.setText(selectedDate[0].format(displayFormatter));

        CatalogAutoCompleteAdapter autoAdapter = new CatalogAutoCompleteAdapter(requireContext(), catalogItems);
        editDropType.setAdapter(autoAdapter);
        editDropType.setOnItemClickListener((parent, view1, position, id) -> {
            CatalogItem selectedItem = autoAdapter.getItem(position);
            if (selectedItem != null) {
                editDropType.setText(selectedItem.getName(requireContext()), false);
                if (selectedItem.isFiveStar()) {
                    toggleRarity.check(R.id.btn_rare_5);
                } else if (selectedItem.isFourStar()) {
                    toggleRarity.check(R.id.btn_rare_4);
                }
                suppressAutoOutcomeChange[0] = false;
                updateOutcomeSection.run();
                updateCatalogMatch.run();
                HapticHelper.performClickHaptic(requireContext(), editDropType);
            }
        });

        populateChips.accept(toggleRarity.getCheckedButtonId());

        toggleRarity.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                populateChips.accept(checkedId);
            }
        });

        editDate.setOnClickListener(v -> {
            long selectionEpochMilli = selectedDate[0]
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli();

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_date))
                    .setSelection(selectionEpochMilli)
                    .setTheme(R.style.DatePicker_GachaPoint_MaterialCalendar)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDate[0] = Instant.ofEpochMilli(selection)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate();
                editDate.setText(selectedDate[0].format(displayFormatter));
                populateChips.accept(toggleRarity.getCheckedButtonId());
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        if (btnCopyPrevDate != null) {
            btnCopyPrevDate.setOnClickListener(v -> {
                LocalDate prevDate = viewModel.getLatestPullDate();
                if (prevDate != null) {
                    selectedDate[0] = prevDate;
                    editDate.setText(selectedDate[0].format(displayFormatter));
                    populateChips.accept(toggleRarity.getCheckedButtonId());
                    HapticHelper.performClickHaptic(requireContext(), btnCopyPrevDate);
                } else {
                    Toast.makeText(requireContext(), R.string.empty_pulls_list, Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnPickBannerDate != null) {
            btnPickBannerDate.setOnClickListener(v -> {
                showBannerDatePickerDialog(currentGameType, selectedDate, selectedBannerSchedule, editDate, displayFormatter, populateChips, toggleRarity);
            });
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        if (isEditMode) {
            btnDelete.setOnClickListener(v -> {
                Context contextThemeWrapper = new ContextThemeWrapper(requireContext(), R.style.Dialog_GachaPoint_AlertDialog);
                androidx.appcompat.app.AlertDialog confirmDialog = new MaterialAlertDialogBuilder(contextThemeWrapper, R.style.Dialog_GachaPoint_AlertDialog)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.delete_confirm_message)
                        .setPositiveButton(R.string.delete, (d, w) -> {
                            dialog.dismiss();
                            viewModel.deleteWish(wishToEdit.getId());
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create();

                confirmDialog.setOnShowListener(d -> {
                    Button btnPositive = confirmDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                    if (btnPositive != null) {
                        btnPositive.setTextColor(ContextCompat.getColor(requireContext(), R.color.missed));
                    }
                });
                confirmDialog.show();
            });
        }

        btnSave.setOnClickListener(v -> {
            String dropType = editDropType.getText() != null ? editDropType.getText().toString().trim() : "";
            int checkedToggleId = toggleRarity.getCheckedButtonId();

            String dropRare;
            if (checkedToggleId == R.id.btn_rare_5) {
                dropRare = ru.menshovanton.gachapoint.domain.enums.Rarity.LEGENDARY.getDisplayRank(currentGameType, requireContext());
            } else if (checkedToggleId == R.id.btn_rare_4) {
                dropRare = ru.menshovanton.gachapoint.domain.enums.Rarity.EPIC.getDisplayRank(currentGameType, requireContext());
            } else {
                dropRare = ru.menshovanton.gachapoint.domain.enums.Rarity.COMMON.getDisplayRank(currentGameType, requireContext());
            }

            PityStatus selectedPityStatus = getSelectedPityStatus(checkedToggleId, bannerHas5050, togglePityStatus);

            viewModel.saveWishFromUi(
                    wishToEdit,
                    selectedDate[0],
                    dropType,
                    dropRare,
                    selectedPityStatus,
                    isBatch
            );
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateOutcomeButtonsVisuals(MaterialButtonToggleGroup togglePityStatus,
                                             MaterialButton btnWin,
                                             MaterialButton btnLoss,
                                             MaterialButton btnGuaranteed) {
        if (togglePityStatus == null || getContext() == null) return;
        int checkedId = togglePityStatus.getCheckedButtonId();
        Context ctx = requireContext();

        if (btnWin != null) {
            if (checkedId == R.id.btn_pity_win) {
                btnWin.setBackgroundColor(ContextCompat.getColor(ctx, R.color.checked));
                btnWin.setTextColor(Color.WHITE);
            } else {
                btnWin.setBackgroundColor(Color.TRANSPARENT);
                btnWin.setTextColor(ContextCompat.getColor(ctx, R.color.text));
            }
        }

        if (btnLoss != null) {
            if (checkedId == R.id.btn_pity_loss) {
                btnLoss.setBackgroundColor(ContextCompat.getColor(ctx, R.color.missed));
                btnLoss.setTextColor(Color.WHITE);
            } else {
                btnLoss.setBackgroundColor(Color.TRANSPARENT);
                btnLoss.setTextColor(ContextCompat.getColor(ctx, R.color.text));
            }
        }

        if (btnGuaranteed != null) {
            if (checkedId == R.id.btn_pity_guaranteed) {
                btnGuaranteed.setBackgroundColor(ContextCompat.getColor(ctx, R.color.check));
                btnGuaranteed.setTextColor(Color.WHITE);
            } else {
                btnGuaranteed.setBackgroundColor(Color.TRANSPARENT);
                btnGuaranteed.setTextColor(ContextCompat.getColor(ctx, R.color.text));
            }
        }
    }

    private void showBannerDatePickerDialog(GameType gameType,
                                            LocalDate[] selectedDate,
                                            BannerScheduleItem[] selectedBannerSchedule,
                                            TextInputEditText editDate,
                                            DateTimeFormatter displayFormatter,
                                            java.util.function.Consumer<Integer> populateChips,
                                            MaterialButtonToggleGroup toggleRarity) {
        BannerScheduleRepository bannerRepo = BannerScheduleRepository.getInstance(requireContext());
        List<BannerScheduleItem> banners = bannerRepo.getBanners(gameType);
        if (banners == null || banners.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_pulls_list, Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_banner_picker, null);
        dialog.setContentView(sheetView);

        RecyclerView rvBanners = sheetView.findViewById(R.id.rv_banner_picker);
        ImageButton btnClose = sheetView.findViewById(R.id.btn_picker_close);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        DateTimeFormatter dbFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        BannerPickerAdapter adapter = new BannerPickerAdapter(gameType, chosen -> {
            selectedBannerSchedule[0] = chosen;
            try {
                LocalDate start = LocalDate.parse(chosen.getStartDate(), dbFormatter);
                LocalDate end = LocalDate.parse(chosen.getEndDate(), dbFormatter);
                LocalDate now = LocalDate.now();
                if (!now.isBefore(start) && !now.isAfter(end)) {
                    selectedDate[0] = now;
                } else {
                    selectedDate[0] = start;
                }
            } catch (Exception e) {
                selectedDate[0] = LocalDate.now();
            }
            editDate.setText(selectedDate[0].format(displayFormatter));
            populateChips.accept(toggleRarity.getCheckedButtonId());
            dialog.dismiss();
        });
        adapter.setItems(banners);
        rvBanners.setAdapter(adapter);

        dialog.show();
    }

    private boolean isItemFeatured(CatalogItem item, List<String> featuredNames) {
        if (item == null || featuredNames == null || featuredNames.isEmpty()) return false;
        for (String feat : featuredNames) {
            if (feat == null) continue;
            String cleanFeat = CatalogRepository.cleanKey(feat);
            if (cleanFeat.isEmpty()) continue;
            if (cleanFeat.equals(CatalogRepository.cleanKey(item.getName()))
                    || cleanFeat.equals(CatalogRepository.cleanKey(item.getNameRu()))
                    || cleanFeat.equals(CatalogRepository.cleanKey(item.getNameEn()))) {
                return true;
            }
            for (String alias : item.getAliases()) {
                if (cleanFeat.equals(CatalogRepository.cleanKey(alias))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesCatalogQuery(CatalogItem item, String queryClean) {
        if (item == null || queryClean == null || queryClean.isEmpty()) return true;
        if (CatalogRepository.cleanKey(item.getName()).contains(queryClean)
                || CatalogRepository.cleanKey(item.getNameRu()).contains(queryClean)
                || CatalogRepository.cleanKey(item.getNameEn()).contains(queryClean)) {
            return true;
        }
        for (String alias : item.getAliases()) {
            if (CatalogRepository.cleanKey(alias).contains(queryClean)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getTrashDropPresets(GameType gameType) {
        List<String> list = new ArrayList<>();
        list.add(getString(R.string.default_pull_content));
        boolean isRu = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());
        if (gameType == GameType.GENSHIN) {
            if (isRu) {
                list.add("Дубина переговоров");
                list.add("Черная кисть");
                list.add("Предвестник зари");
                list.add("Меч небесного всадника");
                list.add("Эпос о драконоборцах");
                list.add("Белая кисть");
            } else {
                list.add("Debate Club");
                list.add("Black Tassel");
                list.add("Harbinger of Dawn");
                list.add("Skyrider Sword");
                list.add("Thrilling Tales");
                list.add("White Tassel");
            }
        } else if (gameType == GameType.HSR) {
            if (isRu) {
                list.add("Стрела");
                list.add("Рог изобилия");
                list.add("Хор");
                list.add("Скрытая тень");
                list.add("Кукурузный початок");
            } else {
                list.add("Arrows");
                list.add("Cornucopia");
                list.add("Chorus");
                list.add("Hidden Shadow");
                list.add("Passkey");
            }
        } else if (gameType == GameType.ZZZ) {
            if (isRu) {
                list.add("Ревербератор");
                list.add("Магнитная буря");
                list.add("Лунная фаза");
                list.add("Орбита");
            } else {
                list.add("Reverberator");
                list.add("Magnetic Storm");
                list.add("Lunar Phase");
                list.add("Orbit");
            }
        }
        return list;
    }

    @NonNull
    private static PityStatus getSelectedPityStatus(int checkedToggleId, boolean bannerHas5050, MaterialButtonToggleGroup togglePityStatus) {
        PityStatus selectedPityStatus = PityStatus.NONE;
        if (checkedToggleId == R.id.btn_rare_5 && bannerHas5050 && togglePityStatus != null) {
            int statusCheckedId = togglePityStatus.getCheckedButtonId();
            if (statusCheckedId == R.id.btn_pity_loss) {
                selectedPityStatus = PityStatus.LOSS_50_50;
            } else if (statusCheckedId == R.id.btn_pity_guaranteed) {
                selectedPityStatus = PityStatus.GUARANTEED;
            } else {
                selectedPityStatus = PityStatus.WIN_50_50;
            }
        }
        return selectedPityStatus;
    }

    private void centerPositionInstantly(int position) {
        if (gameTypesLayoutManager == null) return;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int estimatedPillWidth = (int) (130 * getResources().getDisplayMetrics().density);
        int centerOffsetPx = Math.max(0, (screenWidth - estimatedPillWidth) / 2);
        gameTypesLayoutManager.scrollToPositionWithOffset(position, centerOffsetPx);
    }

    private void smoothScrollToCenter(int targetPosition) {
        if (pillsAdapter == null || gameTypesLayoutManager == null) return;

        if (rvGameTypes == null || getContext() == null) return;

        if (!rvGameTypes.isLaidOut() || rvGameTypes.getWidth() == 0) {
            centerPositionInstantly(targetPosition);
            return;
        }

        View targetChild = gameTypesLayoutManager.findViewByPosition(targetPosition);
        if (targetChild != null) {
            int childCenter = (targetChild.getLeft() + targetChild.getRight()) / 2;
            int rvCenter = (rvGameTypes.getPaddingStart() + (rvGameTypes.getWidth() - rvGameTypes.getPaddingEnd())) / 2;
            int dx = childCenter - rvCenter;

            if (Math.abs(dx) <= 3) {
                return;
            }
            if (dx < 0 && !rvGameTypes.canScrollHorizontally(-1)) {
                return;
            }
            if (dx > 0 && !rvGameTypes.canScrollHorizontally(1)) {
                return;
            }

            rvGameTypes.smoothScrollBy(dx, 0);
            return;
        }

        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(requireContext()) {
            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            }
        };
        smoothScroller.setTargetPosition(targetPosition);
        gameTypesLayoutManager.startSmoothScroll(smoothScroller);
    }
}