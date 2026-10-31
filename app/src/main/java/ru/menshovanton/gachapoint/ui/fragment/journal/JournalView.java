package ru.menshovanton.gachapoint.ui.fragment.journal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.android.material.textfield.TextInputEditText;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
    private LinearProgressIndicator progressPity5Star;
    private MaterialCardView badgePityStatus;
    private TextView tvPityStatusBadge;
    private TextView tvPity4StarCounter;
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
        progressPity5Star = view.findViewById(R.id.progress_pity_5star);
        badgePityStatus = view.findViewById(R.id.badge_pity_status);
        tvPityStatusBadge = view.findViewById(R.id.tv_pity_status_badge);
        tvPity4StarCounter = view.findViewById(R.id.tv_pity_4star_counter);
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
        pullsAdapter.setOnItemClickListener(pull -> showWishDialog(pull, null, false));
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
        progressPity5Star = null;
        badgePityStatus = null;
        tvPityStatusBadge = null;
        tvPity4StarCounter = null;
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
                        int firstVisible = lm != null ? lm.findFirstVisibleItemPosition() : 0;
                        if (firstVisible > 0) {
                            recyclerView.smoothScrollToPosition(0);
                        }
                        if (appbarLayout != null) {
                            appbarLayout.setExpanded(true, true);
                        }
                        if (btnScrollTop != null && btnScrollTop.getVisibility() == View.VISIBLE) {
                            btnScrollTop.hide();
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
        MaterialButtonToggleGroup toggleRarity = dialogView.findViewById(R.id.toggle_rarity);
        View pityStatusContainer = dialogView.findViewById(R.id.ll_pity_status_container);
        MaterialButtonToggleGroup togglePityStatus = dialogView.findViewById(R.id.toggle_pity_status);
        TextInputEditText editDropType = dialogView.findViewById(R.id.tiet_drop_name_edit);
        TextInputEditText editDate = dialogView.findViewById(R.id.tiet_edit_date);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_pull);

        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault());
        DateTimeFormatter dbFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        final LocalDate[] selectedDate = new LocalDate[1];
        boolean isEditMode = (wishToEdit != null);

        BannerInfo bannerInfo = viewModel.getCurrentBannerInfo();
        boolean bannerHas5050 = bannerInfo != null && bannerInfo.hasFiftyFifty();
        final ValueAnimator[] outcomeAnimator = new ValueAnimator[1];

        java.util.function.Consumer<Boolean> updateOutcomeVisibility = animate -> {
            boolean shouldShow = bannerHas5050 && toggleRarity.getCheckedButtonId() == R.id.btn_rare_5;
            if (outcomeAnimator[0] != null && outcomeAnimator[0].isRunning()) {
                outcomeAnimator[0].cancel();
            }

            if (shouldShow) {
                if (pityStatusContainer.getVisibility() != View.VISIBLE || pityStatusContainer.getHeight() == 0) {
                    if (Boolean.TRUE.equals(animate)) {
                        pityStatusContainer.setVisibility(View.VISIBLE);
                        pityStatusContainer.measure(
                                View.MeasureSpec.makeMeasureSpec(dialogView.getWidth() > 0 ? dialogView.getWidth() : dialogView.getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        );
                        int measuredH = pityStatusContainer.getMeasuredHeight();
                        int targetH = measuredH > 0 ? measuredH : (int) (68 * dialogView.getResources().getDisplayMetrics().density);
                        int startH = Math.max(0, pityStatusContainer.getHeight());

                        outcomeAnimator[0] = ValueAnimator.ofInt(startH, targetH);
                        outcomeAnimator[0].setDuration(240);
                        outcomeAnimator[0].setInterpolator(new DecelerateInterpolator());
                        outcomeAnimator[0].addUpdateListener(animation -> {
                            pityStatusContainer.getLayoutParams().height = (int) (Integer) animation.getAnimatedValue();
                            pityStatusContainer.setAlpha(animation.getAnimatedFraction());
                            pityStatusContainer.requestLayout();
                        });
                        outcomeAnimator[0].addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                pityStatusContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                pityStatusContainer.setAlpha(1f);
                                pityStatusContainer.requestLayout();
                            }
                        });
                        outcomeAnimator[0].start();
                    } else {
                        pityStatusContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        pityStatusContainer.setAlpha(1f);
                        pityStatusContainer.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                if (pityStatusContainer.getVisibility() == View.VISIBLE) {
                    if (Boolean.TRUE.equals(animate)) {
                        int startH = pityStatusContainer.getHeight();
                        if (startH <= 0) {
                            pityStatusContainer.setVisibility(View.GONE);
                            pityStatusContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            return;
                        }
                        outcomeAnimator[0] = ValueAnimator.ofInt(startH, 0);
                        outcomeAnimator[0].setDuration(200);
                        outcomeAnimator[0].setInterpolator(new DecelerateInterpolator());
                        outcomeAnimator[0].addUpdateListener(animation -> {
                            pityStatusContainer.getLayoutParams().height = (int) (Integer) animation.getAnimatedValue();
                            pityStatusContainer.setAlpha(1f - animation.getAnimatedFraction());
                            pityStatusContainer.requestLayout();
                        });
                        outcomeAnimator[0].addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                pityStatusContainer.setVisibility(View.GONE);
                                pityStatusContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                pityStatusContainer.setAlpha(1f);
                                pityStatusContainer.requestLayout();
                            }
                        });
                        outcomeAnimator[0].start();
                    } else {
                        pityStatusContainer.setVisibility(View.GONE);
                        pityStatusContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
            }
        };

        if (isEditMode) {
            dialogTitle.setText(getString(R.string.edit_pull));
            btnDelete.setVisibility(View.VISIBLE);
            btnSave.setText(getString(R.string.save));
            editDropType.setText(wishToEdit.getDropType());

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
        }

        updateOutcomeVisibility.accept(false);
        editDate.setText(selectedDate[0].format(displayFormatter));

        toggleRarity.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateOutcomeVisibility.accept(true);
                String currentText = editDropType.getText() != null ? editDropType.getText().toString().trim() : "";
                if (TextUtils.isEmpty(currentText)) {
                    if (checkedId == R.id.btn_rare_3) {
                        editDropType.setText(getString(R.string.default_pull_content));
                    }
                }
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
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

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
                dropRare = getString(R.string.five_star);
            } else if (checkedToggleId == R.id.btn_rare_4) {
                dropRare = getString(R.string.four_star);
            } else {
                dropRare = getString(R.string.three_star);
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