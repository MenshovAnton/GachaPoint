package ru.menshovanton.gachapoint.ui.fragment.journal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.enums.PityStatus;
import ru.menshovanton.gachapoint.domain.models.BannerInfo;
import ru.menshovanton.gachapoint.domain.models.Pull;
import ru.menshovanton.gachapoint.ui.animation.NumberCountAnimator;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class JournalView extends Fragment {

    private JournalViewModel viewModel;
    private MainActivityView mainActivityView;

    private PillsAdapter pillsAdapter;
    private LinearLayoutManager gameTypesLayoutManager;
    private RecyclerView rvGameTypes;

    private FloatingActionButton btnAdd;
    private View tilBannerSelector;
    private View llSelectionBar;
    private TextView tvSelectionCount;
    private ImageButton btnCancelSelection;
    private ImageButton btnSelectAll;
    private ImageButton btnDeleteSelected;
    private AutoCompleteTextView bannerSelector;
    private RecyclerView recyclerView;
    private View emptyView;
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
        View view = inflater.inflate(R.layout.fragment_journal, container, false);

        rvGameTypes = view.findViewById(R.id.rv_game_types);
        btnAdd = view.findViewById(R.id.btn_add);
        tilBannerSelector = view.findViewById(R.id.til_banner_selector);
        llSelectionBar = view.findViewById(R.id.ll_selection_bar);
        tvSelectionCount = view.findViewById(R.id.tv_selection_count);
        btnCancelSelection = view.findViewById(R.id.btn_cancel_selection);
        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnDeleteSelected = view.findViewById(R.id.btn_delete_selected);
        bannerSelector = view.findViewById(R.id.mac_banner_selector);
        recyclerView = view.findViewById(R.id.rv_pulls_list);
        emptyView = view.findViewById(R.id.ll_empty_state);
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
    }

    private void setupGameTypeSelector() {
        List<String> categories = new ArrayList<>();
        for (GameType type : GameType.values()) {
            categories.add(getString(type.getNameResId()));
        }

        gameTypesLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvGameTypes.setLayoutManager(gameTypesLayoutManager);

        GameType currentType = (mainActivityView != null && mainActivityView.getSubType() != null)
                ? mainActivityView.getSubType()
                : GameType.GENSHIN;

        viewModel.setGameType(currentType);

        pillsAdapter = new PillsAdapter(categories, currentType.getCode(), (item, position) -> {
            GameType selectedGame = GameType.fromCode(position);
            if (mainActivityView != null) {
                mainActivityView.setSubType(selectedGame);
            }
            viewModel.setGameType(selectedGame);
            updateBannerSelector(selectedGame);
            smoothScrollToCenter(position);
        });

        rvGameTypes.setAdapter(pillsAdapter);
        centerPositionInstantly(currentType.getCode());
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

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(300);
        animator.setRemoveDuration(250);
        animator.setMoveDuration(250);
        animator.setChangeDuration(250);
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);
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
    }

    private void triggerHapticFeedback() {
        if (getView() != null) {
            getView().performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivityView != null && mainActivityView.getSubType() != null) {
            GameType current = mainActivityView.getSubType();
            if (pillsAdapter != null && pillsAdapter.getSelectedPosition() != current.getCode()) {
                pillsAdapter.setSelectedPosition(current.getCode());
                centerPositionInstantly(current.getCode());
            }
            if (viewModel != null && viewModel.getCurrentGameType() != current) {
                viewModel.setGameType(current);
                updateBannerSelector(current);
            }
        }
        if (!isFirstLaunch && viewModel != null) {
            viewModel.refreshData();
        }
        isFirstLaunch = false;
    }

    private void setupBannerSelector() {
        updateBannerSelector(viewModel.getCurrentGameType());

        bannerSelector.setOnItemClickListener((parent, itemView, position, id) -> {
            String selectedLabel = (String) parent.getItemAtPosition(position);
            String bannerKey = viewModel.getCurrentGameType().getBannerKeyByLabel(requireContext(), selectedLabel);
            viewModel.setBannerType(bannerKey);
            updateDashboardLimits();
        });
    }

    private void updateBannerSelector(GameType gameType) {
        if (gameType == null) return;
        List<BannerInfo> banners = gameType.getBanners();
        List<String> bannerLabels = new ArrayList<>(banners.size());
        for (BannerInfo banner : banners) {
            bannerLabels.add(getString(banner.getLabelResId()));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown_banner, bannerLabels);
        bannerSelector.setAdapter(adapter);

        String currentKey = viewModel.getCurrentBannerType();
        String currentLabel = gameType.getBannerLabel(requireContext(), currentKey);
        bannerSelector.setText(currentLabel, false);
        updateDashboardLimits();
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
                } else {
                    if (recyclerView.getVisibility() != View.VISIBLE) {
                        crossFadeViews(emptyView, recyclerView);
                        recyclerView.scheduleLayoutAnimation();
                    } else if (bannerChanged) {
                        recyclerView.scheduleLayoutAnimation();
                    }
                }

                if (!isEmpty && (newList.size() > previousSize || bannerChanged)) {
                    recyclerView.post(() -> {
                        if (pullsAdapter.getItemCount() > 0) {
                            recyclerView.scrollToPosition(0);
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
        viewToShow.animate()
                .alpha(1f)
                .setDuration(150)
                .setListener(null);

        viewToHide.animate()
                .alpha(0f)
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewToHide.setVisibility(View.GONE);
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
        Context contextThemeWrapper = new ContextThemeWrapper(requireContext(), R.style.Dialog_GachaPoint_AlertDialog);
        View dialogView = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.dialog_edit_pull_record, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(contextThemeWrapper, R.style.Dialog_GachaPoint_AlertDialog);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.tv_dialog_header);
        TextInputEditText editDate = dialogView.findViewById(R.id.tiet_edit_date);
        TextInputEditText editDropType = dialogView.findViewById(R.id.tiet_drop_name_edit);
        RadioGroup radioGroupRarity = dialogView.findViewById(R.id.rg_rarity);
        MaterialRadioButton radioThreeStar = dialogView.findViewById(R.id.rb_three_star);
        MaterialRadioButton radioFourStar = dialogView.findViewById(R.id.rb_four_star);
        MaterialRadioButton radioFiveStar = dialogView.findViewById(R.id.rb_five_star);

        View pityStatusContainer = dialogView.findViewById(R.id.ll_pity_status_container);
        RadioGroup rgPityStatus = dialogView.findViewById(R.id.rg_pity_status);
        MaterialRadioButton rbPityWin = dialogView.findViewById(R.id.rb_pity_win);
        MaterialRadioButton rbPityLoss = dialogView.findViewById(R.id.rb_pity_loss);
        MaterialRadioButton rbPityGuaranteed = dialogView.findViewById(R.id.rb_pity_guaranteed);

        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault());
        DateTimeFormatter dbFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        final LocalDate[] selectedDate = new LocalDate[1];
        boolean isEditMode = (wishToEdit != null);

        BannerInfo bannerInfo = viewModel.getCurrentBannerInfo();
        boolean bannerHas5050 = bannerInfo != null && bannerInfo.hasFiftyFifty();

        Runnable updateOutcomeVisibility = () -> {
            if (bannerHas5050 && radioFiveStar.isChecked()) {
                pityStatusContainer.setVisibility(View.VISIBLE);
            } else {
                pityStatusContainer.setVisibility(View.GONE);
            }
        };

        if (isEditMode) {
            dialogTitle.setText(getString(R.string.edit_pull));
            editDropType.setText(wishToEdit.getDropType());

            try {
                selectedDate[0] = LocalDate.parse(wishToEdit.getDateTime(), dbFormatter);
            } catch (Exception e) {
                selectedDate[0] = LocalDate.now();
            }

            String rarity = wishToEdit.getDropRare();
            if (getString(R.string.four_star).equalsIgnoreCase(rarity)) {
                radioFourStar.setChecked(true);
            } else if (getString(R.string.five_star).equalsIgnoreCase(rarity)) {
                radioFiveStar.setChecked(true);
            } else {
                radioThreeStar.setChecked(true);
            }

            PityStatus currentStatus = wishToEdit.getPityStatus();
            if (currentStatus == PityStatus.LOSS_50_50) {
                rbPityLoss.setChecked(true);
            } else if (currentStatus == PityStatus.GUARANTEED) {
                rbPityGuaranteed.setChecked(true);
            } else {
                rbPityWin.setChecked(true);
            }
        } else {
            dialogTitle.setText(getString(R.string.add_pull));
            selectedDate[0] = LocalDate.now();

            if (getString(R.string.five_star).equalsIgnoreCase(defaultRarity)) {
                radioFiveStar.setChecked(true);
            } else if (getString(R.string.four_star).equalsIgnoreCase(defaultRarity)) {
                radioFourStar.setChecked(true);
            } else {
                radioThreeStar.setChecked(true);
            }

            Boolean isCurrentGuaranteed = viewModel.getIsGuaranteedLiveData().getValue();
            if (Boolean.TRUE.equals(isCurrentGuaranteed)) {
                rbPityGuaranteed.setChecked(true);
            } else {
                rbPityWin.setChecked(true);
            }
        }

        updateOutcomeVisibility.run();
        editDate.setText(selectedDate[0].format(displayFormatter));

        radioGroupRarity.setOnCheckedChangeListener((group, checkedId) -> {
            updateOutcomeVisibility.run();
            String currentText = editDropType.getText() != null ? editDropType.getText().toString().trim() : "";
            if (TextUtils.isEmpty(currentText)) {
                if (checkedId == R.id.rb_three_star) {
                    editDropType.setText(getString(R.string.default_pull_content));
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

        builder.setPositiveButton(isEditMode ? getString(R.string.save) : getString(R.string.add_to_bank), (dialog, which) -> {
            String dropType = editDropType.getText() != null ? editDropType.getText().toString().trim() : "";
            int checkedRadioId = radioGroupRarity.getCheckedRadioButtonId();

            String dropRare;
            if (checkedRadioId == R.id.rb_five_star) {
                dropRare = getString(R.string.five_star);
            } else if (checkedRadioId == R.id.rb_four_star) {
                dropRare = getString(R.string.four_star);
            } else {
                dropRare = getString(R.string.three_star);
            }

            PityStatus selectedPityStatus = getSelectedPityStatus(checkedRadioId, bannerHas5050, rgPityStatus);

            viewModel.saveWishFromUi(
                    wishToEdit,
                    selectedDate[0],
                    dropType,
                    dropRare,
                    selectedPityStatus,
                    isBatch
            );
        });

        if (isEditMode) {
            builder.setNeutralButton(R.string.delete, (dialog, which) -> {
                androidx.appcompat.app.AlertDialog confirmDialog = new MaterialAlertDialogBuilder(contextThemeWrapper, R.style.Dialog_GachaPoint_AlertDialog)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.delete_confirm_message)
                        .setPositiveButton(R.string.delete, (d, w) -> viewModel.deleteWish(wishToEdit.getId()))
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

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        androidx.appcompat.app.AlertDialog editDialog = builder.create();
        if (isEditMode) {
            editDialog.setOnShowListener(d -> {
                Button btnNeutral = editDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);
                if (btnNeutral != null) {
                    btnNeutral.setTextColor(ContextCompat.getColor(requireContext(), R.color.missed));
                }
            });
        }
        editDialog.show();
    }

    @NonNull
    private static PityStatus getSelectedPityStatus(int checkedRadioId, boolean bannerHas5050, RadioGroup rgPityStatus) {
        PityStatus selectedPityStatus = PityStatus.NONE;
        if (checkedRadioId == R.id.rb_five_star && bannerHas5050) {
            int statusCheckedId = rgPityStatus.getCheckedRadioButtonId();
            if (statusCheckedId == R.id.rb_pity_loss) {
                selectedPityStatus = PityStatus.LOSS_50_50;
            } else if (statusCheckedId == R.id.rb_pity_guaranteed) {
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
        pillsAdapter.setSelectedPosition(targetPosition);

        if (rvGameTypes == null || getContext() == null) return;

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