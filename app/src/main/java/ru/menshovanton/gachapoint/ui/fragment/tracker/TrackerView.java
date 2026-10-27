package ru.menshovanton.gachapoint.ui.fragment.tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.DayState;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.enums.TrackerMode;
import ru.menshovanton.gachapoint.domain.models.Statistic;
import ru.menshovanton.gachapoint.ui.animation.CarouselAnimator;
import ru.menshovanton.gachapoint.ui.animation.NumberCountAnimator;
import ru.menshovanton.gachapoint.ui.animation.TextAnimator;
import ru.menshovanton.gachapoint.ui.fragment.tracker.model.CalendarCellUiModel;
import ru.menshovanton.gachapoint.ui.main.GamesConfigBottomSheetDialog;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class TrackerView extends Fragment {

    private TrackerViewModel viewModel;

    private TextView subsCounterView;
    private CalendarGrid calendarGrid;

    private RecyclerView rvGameTypes;
    private PillsAdapter pillsAdapter;
    private LinearLayoutManager layoutManager;

    private ImageView gemIcon;
    private TextView subsCountTitle;
    private ImageView wishIcon;

    private TextView monthHeader;
    private TextView yearHeader;

    private MaterialCardView cardModeSub;
    private MaterialCardView cardModeDailies;
    private TextView tvModeSubTitle;
    private TextView tvModeSubSubtitle;
    private ImageView ivModeSubIcon;
    private TextView tvModeDailiesTitle;
    private TextView tvModeDailiesSubtitle;
    private ImageView ivModeDailiesIcon;

    private MaterialButton checkButton;
    private TextView statsHeader;

    private FrameLayout flActionsContainer;
    private LinearLayout llActions;
    private LinearLayout llSelectionActions;
    private MaterialButton btnSelectionMark;
    private MaterialButton btnSelectionUnmark;
    private ImageButton btnSelectAll;
    private ImageButton btnCloseSelection;
    private OnBackPressedCallback backPressedCallback;

    private TextView claimPrimogems;
    private TextView missedPrimogems;
    private TextView claimWishes;
    private TextView missedWishes;
    private TextView laterPrimogems;
    private TextView laterWishes;

    private boolean isAnimatingMonth = false;
    private int pendingMonthDirection = 0;
    private boolean isSlideOutFinished = false;
    private List<CalendarCellUiModel> pendingCells = null;
    private int lastActiveRows = -1;
    private boolean isFirstRender = true;
    private boolean isFirstLaunch = true;

    private final List<View> cellViewsPool = new ArrayList<>(42);

    private SoundPool soundPool;
    private int successSound;

    public TrackerView() {}

    public static TrackerView newInstance() {
        return new TrackerView();
    }

    private MainActivityView mainActivityView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityView = (MainActivityView) getActivity();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        successSound = soundPool.load(requireContext(), R.raw.success, 1);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivityView = (MainActivityView) getActivity();
        View view = inflater.inflate(R.layout.fragment_tracker, container, false);

        subsCounterView = view.findViewById(R.id.tv_sub_counter);
        calendarGrid = view.findViewById(R.id.cg_calendar_grid);
        checkButton = view.findViewById(R.id.btn_mark_today);
        ImageButton moreButton = view.findViewById(R.id.btn_more);

        ImageButton btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        ImageButton btnNextMonth = view.findViewById(R.id.btn_next_month);

        cardModeSub = view.findViewById(R.id.card_mode_sub);
        cardModeDailies = view.findViewById(R.id.card_mode_dailies);
        tvModeSubTitle = view.findViewById(R.id.tv_mode_sub_title);
        tvModeSubSubtitle = view.findViewById(R.id.tv_mode_sub_subtitle);
        ivModeSubIcon = view.findViewById(R.id.iv_mode_sub_icon);
        tvModeDailiesTitle = view.findViewById(R.id.tv_mode_dailies_title);
        tvModeDailiesSubtitle = view.findViewById(R.id.tv_mode_dailies_subtitle);
        ivModeDailiesIcon = view.findViewById(R.id.iv_mode_dailies_icon);

        gemIcon = view.findViewById(R.id.iv_gem);
        subsCountTitle = view.findViewById(R.id.tv_sub_counter_label);
        wishIcon = view.findViewById(R.id.iv_pull);

        monthHeader = view.findViewById(R.id.tv_month);
        yearHeader = view.findViewById(R.id.tv_year);
        statsHeader = view.findViewById(R.id.tv_stats_header);

        claimPrimogems = view.findViewById(R.id.tv_received_gems);
        missedPrimogems = view.findViewById(R.id.tv_omitted_gems);
        claimWishes = view.findViewById(R.id.tv_received_pulls);
        missedWishes = view.findViewById(R.id.tv_omitted_pulls);
        laterPrimogems = view.findViewById(R.id.tv_upcoming_gems);
        laterWishes = view.findViewById(R.id.tv_upcoming_pulls);

        viewModel = new ViewModelProvider(this).get(TrackerViewModel.class);

        initCalendarGrid();
        initGameTypePills(view);

        checkButton.setOnClickListener(v -> {
            checkButton.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(70)
                    .withEndAction(() -> checkButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start())
                    .start();
            viewModel.onCheckClick();
        });

        moreButton.setOnClickListener(v -> {
            moreButton.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(70)
                    .withEndAction(() -> moreButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start())
                    .start();
            showMoreMenu(v);
        });

        btnPrevMonth.setOnClickListener(v -> {
            btnPrevMonth.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(60)
                    .withEndAction(() -> btnPrevMonth.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start())
                    .start();
            animateMonthChange(false);
        });

        btnNextMonth.setOnClickListener(v -> {
            btnNextMonth.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(60)
                    .withEndAction(() -> btnNextMonth.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start())
                    .start();
            animateMonthChange(true);
        });

        cardModeSub.setOnClickListener(v -> viewModel.setTrackerMode(TrackerMode.SUBSCRIPTION));
        cardModeDailies.setOnClickListener(v -> viewModel.setTrackerMode(TrackerMode.DAILIES));

        flActionsContainer = view.findViewById(R.id.fl_actions_container);
        llActions = view.findViewById(R.id.ll_actions);
        llSelectionActions = view.findViewById(R.id.ll_selection_actions);
        btnSelectionMark = view.findViewById(R.id.btn_selection_mark);
        btnSelectionUnmark = view.findViewById(R.id.btn_selection_unmark);
        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnCloseSelection = view.findViewById(R.id.btn_close_selection);

        btnSelectionMark.setOnClickListener(v -> {
            btnSelectionMark.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(70)
                    .withEndAction(() -> btnSelectionMark.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start())
                    .start();
            viewModel.markSelectedDays(true);
        });

        btnSelectionUnmark.setOnClickListener(v -> {
            btnSelectionUnmark.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(70)
                    .withEndAction(() -> btnSelectionUnmark.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start())
                    .start();
            viewModel.markSelectedDays(false);
        });

        btnSelectAll.setOnClickListener(v -> {
            btnSelectAll.animate()
                    .scaleX(0.88f)
                    .scaleY(0.88f)
                    .setDuration(60)
                    .withEndAction(() -> btnSelectAll.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start())
                    .start();
            viewModel.selectAllCurrentMonth(viewModel.getCalendarCellsLiveData().getValue());
        });

        btnCloseSelection.setOnClickListener(v -> {
            btnCloseSelection.animate()
                    .scaleX(0.88f)
                    .scaleY(0.88f)
                    .setDuration(60)
                    .withEndAction(() -> btnCloseSelection.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start())
                    .start();
            viewModel.exitSelectionMode();
        });

        backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                viewModel.exitSelectionMode();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);

        calendarGrid.setOnSwipeListener(new CalendarGrid.OnSwipeListener() {
            @Override public void onSwipeLeft() { animateMonthChange(true); }
            @Override public void onSwipeRight() { animateMonthChange(false); }
        });

        observeViewModel();
        viewModel.refreshData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePillsVisibility();
        if (mainActivityView != null && mainActivityView.getSubType() != null) {
            GameType current = mainActivityView.getSubType();
            if (pillsAdapter != null) {
                pillsAdapter.setSelectedGame(current);
                int pos = pillsAdapter.getPositionForGame(current);
                if (pos != -1) {
                    centerPositionInstantly(pos);
                }
            }
            if (viewModel.getCurrentGameType() != current) {
                viewModel.setGameType(current);
            }
        }
        if (!isFirstLaunch && viewModel != null) {
            viewModel.refreshData();
        }
        isFirstLaunch = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (backPressedCallback != null) {
            backPressedCallback.remove();
            backPressedCallback = null;
        }
        isAnimatingMonth = false;
        pendingMonthDirection = 0;
        isSlideOutFinished = false;
        pendingCells = null;
        isFirstRender = true;
        cellViewsPool.clear();
        if (calendarGrid != null) {
            calendarGrid.removeCallbacks(null);
            calendarGrid = null;
        }
        mainActivityView = null;
        subsCounterView = null;
        rvGameTypes = null;
        pillsAdapter = null;
        layoutManager = null;
        gemIcon = null;
        subsCountTitle = null;
        wishIcon = null;
        monthHeader = null;
        yearHeader = null;
        cardModeSub = null;
        cardModeDailies = null;
        tvModeSubTitle = null;
        tvModeSubSubtitle = null;
        ivModeSubIcon = null;
        tvModeDailiesTitle = null;
        tvModeDailiesSubtitle = null;
        ivModeDailiesIcon = null;
        checkButton = null;
        statsHeader = null;
        flActionsContainer = null;
        llActions = null;
        llSelectionActions = null;
        btnSelectionMark = null;
        btnSelectionUnmark = null;
        btnSelectAll = null;
        btnCloseSelection = null;
        claimPrimogems = null;
        missedPrimogems = null;
        claimWishes = null;
        missedWishes = null;
        laterPrimogems = null;
        laterWishes = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    private void observeViewModel() {
        viewModel.getGameTypeLiveData().observe(getViewLifecycleOwner(), this::updateGameTypeUi);
        viewModel.getTrackerModeLiveData().observe(getViewLifecycleOwner(), this::updateTrackerModeUi);
        viewModel.getSelectedMonthLiveData().observe(getViewLifecycleOwner(), this::updateHeaderMonth);
        viewModel.getSelectedYearLiveData().observe(getViewLifecycleOwner(), year -> {
            String yearStr = String.valueOf(year);
            if (!isFirstRender) {
                TextAnimator.crossfade(yearHeader, yearStr);
            } else {
                yearHeader.setText(yearStr);
            }
        });
        viewModel.getSubsCountLiveData().observe(getViewLifecycleOwner(), this::updateSubsCountUi);
        viewModel.getDailiesProgressLiveData().observe(getViewLifecycleOwner(), this::updateDailiesProgressUi);
        viewModel.getStatisticLiveData().observe(getViewLifecycleOwner(), this::updateStatisticsUi);
        viewModel.getCalendarCellsLiveData().observe(getViewLifecycleOwner(), this::renderCalendarGrid);
        viewModel.getOpenDayDetailsEvent().observe(getViewLifecycleOwner(), this::showDayDetailsSheet);

        viewModel.getToastMessageEvent().observe(getViewLifecycleOwner(), resId -> {
            Context context = getContext();
            if (resId != null && context != null) {
                Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getOpenAddSubscriptionDialogEvent().observe(getViewLifecycleOwner(), this::showAddSubscriptionDialog);

        viewModel.getOpenSubAddingDialogEvent().observe(getViewLifecycleOwner(), unused -> {
            Context context = getContext();
            if (context != null) {
                showAddingSubDialog(context);
            }
        });

        viewModel.getOpenSubDeletingDialogEvent().observe(getViewLifecycleOwner(), unused -> {
            Context context = getContext();
            if (context != null) {
                showDeletingSubDialog(context);
            }
        });

        viewModel.getVibrateEvent().observe(getViewLifecycleOwner(), unused -> triggerVibration());
        viewModel.getPlaySoundEvent().observe(getViewLifecycleOwner(), this::playSound);
        viewModel.getIsSelectionModeLiveData().observe(getViewLifecycleOwner(), this::updateSelectionModeUi);
        viewModel.getSelectedDatesLiveData().observe(getViewLifecycleOwner(), this::updateSelectedDatesUi);

        if (mainActivityView != null) {
            mainActivityView.getMainActivityViewModel().getEnabledGamesLiveData().observe(getViewLifecycleOwner(), games ->
                    updatePillsVisibility());
            mainActivityView.getMainActivityViewModel().getSubTypeLiveData().observe(getViewLifecycleOwner(), game -> {
                if (pillsAdapter != null) {
                    pillsAdapter.setSelectedGame(game);
                    int pos = pillsAdapter.getPositionForGame(game);
                    if (pos != -1) {
                        smoothScrollToCenter(pos);
                    }
                }
                if (viewModel != null && viewModel.getCurrentGameType() != game) {
                    viewModel.setGameType(game);
                }
            });
        }
    }

    private void updateSelectionModeUi(boolean isSelectionMode) {
        if (backPressedCallback != null) {
            backPressedCallback.setEnabled(isSelectionMode);
        }
        if (flActionsContainer != null) {
            TransitionManager.beginDelayedTransition(flActionsContainer, new Fade().setDuration(160));
            llActions.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
            llSelectionActions.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        }
        updateSelectedDatesUi(viewModel.getSelectedDatesLiveData().getValue());
    }

    private void updateSelectedDatesUi(Set<LocalDate> selectedDates) {
        if (!isAdded()) return;
        int count = (selectedDates != null) ? selectedDates.size() : 0;
        if (btnSelectionMark != null) {
            btnSelectionMark.setText(count > 0 ? getString(R.string.btn_selection_mark_count, count) : getString(R.string.btn_selection_mark));
        }
        if (btnSelectionUnmark != null) {
            btnSelectionUnmark.setText(count > 0 ? getString(R.string.btn_selection_unmark_count, count) : getString(R.string.btn_selection_unmark));
        }

        List<CalendarCellUiModel> cells = viewModel.getCalendarCellsLiveData().getValue();
        if (cells == null || cellViewsPool.isEmpty()) return;

        boolean isSelectionMode = viewModel.isSelectionMode();
        for (int i = 0; i < Math.min(cells.size(), cellViewsPool.size()); i++) {
            View cellView = cellViewsPool.get(i);
            View overlay = cellView.findViewById(R.id.view_selection_overlay);
            if (overlay == null) continue;

            CalendarCellUiModel model = cells.get(i);
            if (!model.isVisible || !isSelectionMode) {
                overlay.setVisibility(View.GONE);
            } else {
                LocalDate cellDate = LocalDate.of(model.year, model.month, model.dayOfMonth);
                boolean isSelected = selectedDates != null && selectedDates.contains(cellDate);
                if (isSelected) {
                    if (overlay.getVisibility() != View.VISIBLE) {
                        overlay.setVisibility(View.VISIBLE);
                        overlay.setScaleX(0.7f);
                        overlay.setScaleY(0.7f);
                        overlay.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    }
                } else {
                    overlay.setVisibility(View.GONE);
                }
            }
        }
    }

    private void updateTrackerModeUi(TrackerMode mode) {
        if (getContext() == null) return;
        Context context = requireContext();
        float density = getResources().getDisplayMetrics().density;

        if (mode == TrackerMode.SUBSCRIPTION) {
            cardModeSub.setStrokeColor(ContextCompat.getColor(context, R.color.check));
            cardModeSub.setStrokeWidth((int) (2 * density));
            tvModeSubTitle.setTextColor(ContextCompat.getColor(context, R.color.text));
            ivModeSubIcon.setColorFilter(ContextCompat.getColor(context, R.color.check));

            cardModeDailies.setStrokeColor(ContextCompat.getColor(context, R.color.divider_color));
            cardModeDailies.setStrokeWidth((int) (1 * density));
            tvModeDailiesTitle.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            ivModeDailiesIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));

            if (!isFirstRender) {
                TextAnimator.crossfade(checkButton, R.string.btn_mark_sub);
                TextAnimator.crossfade(statsHeader, R.string.stat_header_sub);
                GameType game = viewModel.getCurrentGameType();
                TextAnimator.crossfade(subsCountTitle, game.getSubTitleResId());
            } else {
                checkButton.setText(R.string.btn_mark_sub);
                statsHeader.setText(R.string.stat_header_sub);
                GameType game = viewModel.getCurrentGameType();
                subsCountTitle.setText(game.getSubTitleResId());
            }
            Integer count = viewModel.getSubsCountLiveData().getValue();
            NumberCountAnimator.animate(subsCounterView, count != null ? count : 0);
        } else {
            cardModeDailies.setStrokeColor(ContextCompat.getColor(context, R.color.check));
            cardModeDailies.setStrokeWidth((int) (2 * density));
            tvModeDailiesTitle.setTextColor(ContextCompat.getColor(context, R.color.text));
            ivModeDailiesIcon.setColorFilter(ContextCompat.getColor(context, R.color.check));

            cardModeSub.setStrokeColor(ContextCompat.getColor(context, R.color.divider_color));
            cardModeSub.setStrokeWidth((int) (1 * density));
            tvModeSubTitle.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            ivModeSubIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));

            if (!isFirstRender) {
                TextAnimator.crossfade(checkButton, R.string.btn_mark_dailies);
                TextAnimator.crossfade(statsHeader, R.string.stat_header_dailies);
                TextAnimator.crossfade(subsCountTitle, R.string.sheet_status_completed);
            } else {
                checkButton.setText(R.string.btn_mark_dailies);
                statsHeader.setText(R.string.stat_header_dailies);
                subsCountTitle.setText(R.string.sheet_status_completed);
            }
            int[] progress = viewModel.getDailiesProgressLiveData().getValue();
            if (progress != null && progress.length >= 2) {
                NumberCountAnimator.animate(subsCounterView, progress[0], val -> getString(R.string.tracker_mode_dailies_subtitle, val, progress[1]));
            }
        }
    }

    private void updateSubsCountUi(Integer count) {
        int cnt = (count != null) ? count : 0;
        tvModeSubSubtitle.setText(getString(R.string.tracker_mode_sub_subtitle, cnt));
        if (viewModel.getCurrentMode() == TrackerMode.SUBSCRIPTION) {
            NumberCountAnimator.animate(subsCounterView, cnt);
        }
    }

    private void updateDailiesProgressUi(int[] progress) {
        if (progress == null || progress.length < 2) return;
        tvModeDailiesSubtitle.setText(getString(R.string.tracker_mode_dailies_subtitle, progress[0], progress[1]));
        if (viewModel.getCurrentMode() == TrackerMode.DAILIES) {
            NumberCountAnimator.animate(subsCounterView, progress[0], val -> getString(R.string.tracker_mode_dailies_subtitle, val, progress[1]));
        }
    }

    private void triggerVibration() {
        Context context = getContext();
        if (context == null) return;

        Preferences preferences = new Preferences(context);
        if (!preferences.getBooleanPreference(Preferences.VIBRATION_MODE)) return;

        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void initGameTypePills(View view) {
        rvGameTypes = view.findViewById(R.id.rv_game_types);

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        rvGameTypes.setLayoutManager(layoutManager);

        GameType currentType = (mainActivityView != null && mainActivityView.getSubType() != null)
                ? mainActivityView.getSubType()
                : viewModel.getCurrentGameType();

        List<GameType> enabledGames = mainActivityView != null ? mainActivityView.getEnabledGames() : Collections.singletonList(GameType.GENSHIN);

        pillsAdapter = new PillsAdapter(
                enabledGames,
                currentType,
                (selected, position) -> {
                    if (mainActivityView != null) mainActivityView.setSubType(selected);
                    viewModel.setGameType(selected);
                    smoothScrollToCenter(position);
                },
                () -> GamesConfigBottomSheetDialog.show(requireContext(), () -> {
                    if (mainActivityView != null) mainActivityView.refreshGamesConfig();
                })
        );

        CarouselAnimator.setupItemAnimator(rvGameTypes);
        rvGameTypes.setAdapter(pillsAdapter);
        viewModel.setGameTypeSilently(currentType);
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

    private void updateGameTypeUi(GameType gameType) {
        gemIcon.setImageResource(gameType.getCurrencyIconResId());
        wishIcon.setImageResource(gameType.getPullIconResId());
        if (viewModel.getCurrentMode() == TrackerMode.SUBSCRIPTION) {
            if (!isFirstRender) {
                TextAnimator.crossfade(subsCountTitle, gameType.getSubTitleResId());
            } else {
                subsCountTitle.setText(gameType.getSubTitleResId());
            }
        }
    }

    private void animateMonthChange(boolean isNext) {
        if (isAnimatingMonth) return;
        isAnimatingMonth = true;
        pendingMonthDirection = isNext ? 1 : -1;
        isSlideOutFinished = false;
        pendingCells = null;

        int width = calendarGrid.getWidth();
        if (width == 0) {
            notifyViewModelMonthChange(isNext);
            isAnimatingMonth = false;
            pendingMonthDirection = 0;
            return;
        }

        float targetOutX = isNext ? -width * 0.35f : width * 0.35f;

        calendarGrid.animate()
                .translationX(targetOutX)
                .alpha(0f)
                .setDuration(120)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    isSlideOutFinished = true;
                    if (pendingCells != null) {
                        applyCellsAndSlideIn(pendingCells, pendingMonthDirection);
                        pendingCells = null;
                    }
                })
                .start();

        notifyViewModelMonthChange(isNext);
    }

    private void notifyViewModelMonthChange(boolean isNext) {
        if (isNext) {
            viewModel.nextMonth();
        } else {
            viewModel.previousMonth();
        }
    }

    private void renderCalendarGrid(List<CalendarCellUiModel> cells) {
        if (!isAdded() || getContext() == null || cells == null || cells.size() < 42) return;

        if (isAnimatingMonth && !isSlideOutFinished) {
            pendingCells = cells;
            return;
        }

        if (isAnimatingMonth) {
            applyCellsAndSlideIn(cells, pendingMonthDirection);
            return;
        }

        bindCellData(cells);
    }

    private void applyCellsAndSlideIn(List<CalendarCellUiModel> cells, int direction) {
        int width = calendarGrid.getWidth();
        float startInX = direction > 0 ? width * 0.35f : -width * 0.35f;

        calendarGrid.setTranslationX(startInX);
        calendarGrid.setAlpha(0f);

        bindCellData(cells);

        calendarGrid.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    isAnimatingMonth = false;
                    pendingMonthDirection = 0;
                    isSlideOutFinished = false;
                })
                .start();
    }

    private void bindCellData(List<CalendarCellUiModel> cells) {
        Context context = requireContext();
        boolean hasSixthRow = false;
        for (int i = 35; i < 42; i++) {
            if (cells.get(i).isVisible) {
                hasSixthRow = true;
                break;
            }
        }
        int activeRows = hasSixthRow ? 6 : 5;

        if (!isFirstRender && lastActiveRows != -1 && lastActiveRows != activeRows) {
            ViewGroup parent = (ViewGroup) calendarGrid.getParent();
            if (parent != null && parent.getParent() instanceof ViewGroup) {
                ViewGroup rootContainer = (ViewGroup) parent.getParent();
                TransitionSet set = new TransitionSet();
                set.addTransition(new ChangeBounds());
                set.addTransition(new Fade());
                set.setDuration(220);
                set.setInterpolator(new AccelerateDecelerateInterpolator());
                TransitionManager.beginDelayedTransition(rootContainer, set);
            }
        }
        lastActiveRows = activeRows;
        isFirstRender = false;

        Set<LocalDate> selectedDates = viewModel.getSelectedDatesLiveData().getValue();
        boolean isSelectionMode = viewModel.isSelectionMode();

        for (int i = 0; i < 42; i++) {
            View cellView = cellViewsPool.get(i);
            TextView tvDay = cellView.findViewById(R.id.tv_day_number);
            View overlay = cellView.findViewById(R.id.view_selection_overlay);
            CalendarCellUiModel model = cells.get(i);

            if (i >= 35 && activeRows == 5) {
                cellView.setVisibility(View.GONE);
                tvDay.setText("");
                tvDay.setBackground(null);
                if (overlay != null) overlay.setVisibility(View.GONE);
                cellView.setOnClickListener(null);
                cellView.setOnLongClickListener(null);
                continue;
            }

            if (!model.isVisible) {
                cellView.setVisibility(View.INVISIBLE);
                tvDay.setText("");
                tvDay.setBackground(null);
                if (overlay != null) overlay.setVisibility(View.GONE);
                cellView.setOnClickListener(null);
                cellView.setOnLongClickListener(null);
            } else {
                cellView.setVisibility(View.VISIBLE);
                tvDay.setText(String.valueOf(model.dayOfMonth));
                if (model.backgroundRes != 0) {
                    tvDay.setBackgroundResource(model.backgroundRes);
                } else {
                    tvDay.setBackground(null);
                }
                tvDay.setTextColor(ContextCompat.getColor(context, model.textColorRes));

                LocalDate cellDate = LocalDate.of(model.year, model.month, model.dayOfMonth);
                String dateDesc = cellDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()));
                cellView.setContentDescription(dateDesc);

                boolean isSelected = isSelectionMode && selectedDates != null && selectedDates.contains(cellDate);
                if (overlay != null) {
                    overlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                }

                cellView.setOnClickListener(v -> {
                    if (viewModel.isSelectionMode()) {
                        viewModel.toggleDateSelection(cellDate);
                    } else {
                        viewModel.onCellClick(model);
                    }
                });

                cellView.setOnLongClickListener(v -> {
                    if (!viewModel.isSelectionMode()) {
                        viewModel.enterSelectionMode(cellDate);
                    } else {
                        viewModel.toggleDateSelection(cellDate);
                    }
                    return true;
                });
            }
        }
    }

    private void updateStatisticsUi(Statistic statistic) {
        if (statistic == null) return;

        NumberCountAnimator.animate(claimPrimogems, statistic.claimGems);
        NumberCountAnimator.animate(missedPrimogems, statistic.missedGems);
        NumberCountAnimator.animate(claimWishes, statistic.claimWishes);
        NumberCountAnimator.animate(missedWishes, statistic.missedWishes);

        NumberCountAnimator.animate(laterPrimogems, Math.max(0, statistic.laterGems));
        NumberCountAnimator.animate(laterWishes, Math.max(0, statistic.laterWishes));
    }

    @SuppressLint("SetTextI18n")
    private void updateHeaderMonth(int month) {
        Month monthObj = Month.of(month);
        Locale locale = LocaleList.getDefault().get(0);
        String printMonth = monthObj.getDisplayName(TextStyle.FULL_STANDALONE, locale);
        String formattedMonth = printMonth.substring(0, 1).toUpperCase(locale) + printMonth.substring(1);
        if (!isFirstRender) {
            TextAnimator.crossfade(monthHeader, formattedMonth);
        } else {
            monthHeader.setText(formattedMonth);
        }
    }

    private void initCalendarGrid() {
        if (!cellViewsPool.isEmpty() && calendarGrid.getChildCount() == 42) return;

        calendarGrid.removeAllViews();
        cellViewsPool.clear();

        Context context = requireContext();
        float density = getResources().getDisplayMetrics().density;
        int cellHeightInPx = getResources().getDimensionPixelSize(R.dimen.calendar_cell_min_height);
        int marginInPx = getResources().getDimensionPixelSize(R.dimen.calendar_cell_margin);
        int tvSizePx = getResources().getDimensionPixelSize(R.dimen.calendar_day_number_size);
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.genshin_font);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true);
        int rippleResId = typedValue.resourceId;

        for (int i = 0; i < 42; i++) {
            FrameLayout cellView = new FrameLayout(context);
            cellView.setClickable(true);
            cellView.setFocusable(true);
            if (rippleResId != 0) {
                cellView.setBackgroundResource(rippleResId);
            }

            TextView tvDay = new TextView(context);
            tvDay.setId(R.id.tv_day_number);
            FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(tvSizePx, tvSizePx, Gravity.CENTER);
            tvDay.setLayoutParams(tvParams);
            tvDay.setGravity(Gravity.CENTER);
            tvDay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvDay.setTypeface(customTypeface != null ? customTypeface : Typeface.DEFAULT, Typeface.BOLD);
            tvDay.setTextColor(ContextCompat.getColor(context, R.color.text));
            cellView.addView(tvDay);

            View selectionOverlay = new View(context);
            selectionOverlay.setId(R.id.view_selection_overlay);
            int overlaySizePx = (int) (40 * density);
            FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(overlaySizePx, overlaySizePx, Gravity.CENTER);
            selectionOverlay.setLayoutParams(overlayParams);
            selectionOverlay.setBackgroundResource(R.drawable.bg_calendar_cell_selected);
            selectionOverlay.setVisibility(View.GONE);
            cellView.addView(selectionOverlay);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = cellHeightInPx;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
            cellView.setLayoutParams(params);

            calendarGrid.addView(cellView);
            cellViewsPool.add(cellView);
        }
    }

    private void showDayDetailsSheet(CalendarCellUiModel cell) {
        if (cell == null || !isAdded()) return;

        Context context = requireContext();
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_day_details_sheet);

        TextView tvDate = dialog.findViewById(R.id.tv_sheet_date);
        TextView tvGame = dialog.findViewById(R.id.tv_sheet_game);
        MaterialSwitch switchDaily = dialog.findViewById(R.id.switch_daily);
        View rowSub = dialog.findViewById(R.id.row_sub);
        View rowAddMoreSub = dialog.findViewById(R.id.row_add_more_sub);
        View rowStartSub = dialog.findViewById(R.id.row_start_sub);
        MaterialSwitch switchSub = dialog.findViewById(R.id.switch_sub);
        TextView tvSubTitle = dialog.findViewById(R.id.tv_sub_row_title);
        TextView tvSubSubtitle = dialog.findViewById(R.id.tv_sub_row_subtitle);
        ImageView ivSubIcon = dialog.findViewById(R.id.iv_sub_icon);

        LocalDate dateObj = LocalDate.of(cell.year, cell.month, cell.dayOfMonth);
        Locale locale = LocaleList.getDefault().get(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale);
        String formattedDate = dateObj.format(formatter);
        formattedDate = formattedDate.substring(0, 1).toUpperCase(locale) + formattedDate.substring(1);

        if (tvDate != null) tvDate.setText(formattedDate);
        if (tvGame != null) tvGame.setText(getString(viewModel.getCurrentGameType().getNameResId()));

        if (switchDaily != null) {
            switchDaily.setChecked(cell.dailyStatus == 1);
            switchDaily.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    viewModel.toggleDailyStatus(cell.year, cell.dayOfYear, isChecked);
                }
            });
        }

        GameType currentGame = viewModel.getCurrentGameType();
        if (ivSubIcon != null) {
            ivSubIcon.setImageResource(currentGame.getCurrencyIconResId());
        }

        int currentSubs = viewModel.getSubsCountLiveData().getValue() != null ? viewModel.getSubsCountLiveData().getValue() : 0;

        if (cell.subDaysRemaining > 0) {
            if (rowSub != null) rowSub.setVisibility(View.VISIBLE);
            if (rowStartSub != null) rowStartSub.setVisibility(View.GONE);

            if (rowAddMoreSub != null) {
                if (currentSubs < 6) {
                    rowAddMoreSub.setVisibility(View.VISIBLE);
                    rowAddMoreSub.setOnClickListener(v -> {
                        dialog.dismiss();
                        showAddSubscriptionDialog(LocalDate.now());
                    });
                } else {
                    rowAddMoreSub.setVisibility(View.GONE);
                }
            }

            if (tvSubTitle != null) {
                tvSubTitle.setText(currentGame.getSubscriptionNameResId());
            }
            if (tvSubSubtitle != null) {
                String subInfo = getString(R.string.sheet_sub_reward) + " • " + getString(R.string.sheet_sub_remaining, cell.subDaysRemaining);
                tvSubSubtitle.setText(subInfo);
            }
            if (switchSub != null) {
                boolean isClaimed = (cell.status == DayState.STATUS_RECEIVED || cell.status == DayState.STATUS_OLD_RECEIVED);
                switchSub.setChecked(isClaimed);
                switchSub.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (buttonView.isPressed()) {
                        viewModel.toggleSubStatus(cell.year, cell.dayOfYear, isChecked);
                    }
                });
            }
        } else {
            if (rowSub != null) rowSub.setVisibility(View.GONE);
            if (rowAddMoreSub != null) rowAddMoreSub.setVisibility(View.GONE);
            if (rowStartSub != null) {
                rowStartSub.setVisibility(View.VISIBLE);
                rowStartSub.setOnClickListener(v -> {
                    dialog.dismiss();
                    showAddSubscriptionDialog(dateObj);
                });
            }
        }

        dialog.show();
    }

    private void centerPositionInstantly(int position) {
        if (layoutManager == null) return;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int estimatedPillWidth = (int) (130 * getResources().getDisplayMetrics().density);
        int centerOffsetPx = Math.max(0, (screenWidth - estimatedPillWidth) / 2);
        layoutManager.scrollToPositionWithOffset(position, centerOffsetPx);
    }

    private void smoothScrollToCenter(int targetPosition) {
        if (pillsAdapter == null || layoutManager == null) return;

        RecyclerView recyclerView = getView() != null ? getView().findViewById(R.id.rv_game_types) : null;
        if (recyclerView == null || getContext() == null) return;

        View targetChild = layoutManager.findViewByPosition(targetPosition);
        if (targetChild != null) {
            int childCenter = (targetChild.getLeft() + targetChild.getRight()) / 2;
            int rvCenter = (recyclerView.getPaddingStart() + (recyclerView.getWidth() - recyclerView.getPaddingEnd())) / 2;
            int dx = childCenter - rvCenter;

            if (Math.abs(dx) <= 3) {
                return;
            }
            if (dx < 0 && !recyclerView.canScrollHorizontally(-1)) {
                return;
            }
            if (dx > 0 && !recyclerView.canScrollHorizontally(1)) {
                return;
            }

            recyclerView.smoothScrollBy(dx, 0);
            return;
        }

        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(requireContext()) {
            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            }
        };
        smoothScroller.setTargetPosition(targetPosition);
        layoutManager.startSmoothScroll(smoothScroller);
    }

    private void showMoreMenu(View view) {
        if (getActivity() instanceof MainActivityView) {
            ((MainActivityView) getActivity()).showCalendarMenu(new MainActivityView.OnCalendarMenuClickListener() {
                @Override public void onAdd() { viewModel.onAddClick(); }
                @Override public void onDel() { viewModel.onDelClick(); }
                @Override public void onRecovery() { viewModel.recoveryMissDay(); }
                @Override public void onCancel() { viewModel.onCancelCheck(); }
                @Override public void onMultiSelect() { viewModel.enterSelectionMode(null); }
            });
        }
    }

    public void showAddingSubDialog(Context context) {
        new MaterialAlertDialogBuilder(context, R.style.Dialog_GachaPoint_AlertDialog)
                .setTitle(getString(R.string.check_button_text))
                .setMessage(R.string.active_subs_null_question)
                .setPositiveButton(getString(R.string.ok_button), (dialog, which) -> {
                    showAddSubscriptionDialog(LocalDate.now());
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void showAddSubscriptionDialog(@Nullable LocalDate startDate) {
        if (!isAdded()) return;
        Context context = requireContext();

        final LocalDate effectiveStartDate = (startDate != null) ? startDate : LocalDate.now();
        GameType currentGame = viewModel.getCurrentGameType();
        int currentSubs = viewModel.getSubsCountLiveData().getValue() != null ? viewModel.getSubsCountLiveData().getValue() : 0;
        int maxSubs = currentGame.getMaxSubs();
        int maxAllowed = Math.max(1, maxSubs - currentSubs);

        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        dialog.setContentView(R.layout.dialog_add_subscription);

        ImageView ivCurrency = dialog.findViewById(R.id.iv_dialog_currency);
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvGame = dialog.findViewById(R.id.tv_dialog_game);
        TextView tvChainStatus = dialog.findViewById(R.id.tv_dialog_chain_status);
        ChipGroup chipGroup = dialog.findViewById(R.id.chip_group_count);
        TextView tvDuration = dialog.findViewById(R.id.tv_preview_duration);
        TextView tvReward = dialog.findViewById(R.id.tv_preview_reward);
        TextView tvStartDate = dialog.findViewById(R.id.tv_preview_start_date);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel_add_sub);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm_add_sub);

        if (ivCurrency != null) ivCurrency.setImageResource(currentGame.getCurrencyIconResId());
        if (tvTitle != null) tvTitle.setText(currentGame.getSubscriptionNameResId());
        if (tvGame != null) tvGame.setText(currentGame.getNameResId());

        if (tvChainStatus != null) {
            if (currentSubs > 0) {
                tvChainStatus.setText(getString(R.string.dialog_add_sub_current_chain, currentSubs, maxSubs));
            } else {
                tvChainStatus.setText(R.string.dialog_add_sub_no_active);
            }
        }

        Chip[] chips = new Chip[6];
        if (chipGroup != null) {
            chips[0] = dialog.findViewById(R.id.chip_1);
            chips[1] = dialog.findViewById(R.id.chip_2);
            chips[2] = dialog.findViewById(R.id.chip_3);
            chips[3] = dialog.findViewById(R.id.chip_4);
            chips[4] = dialog.findViewById(R.id.chip_5);
            chips[5] = dialog.findViewById(R.id.chip_6);

            for (int i = 0; i < 6; i++) {
                if (chips[i] != null) {
                    int countVal = i + 1;
                    if (countVal > maxSubs) {
                        chips[i].setVisibility(View.GONE);
                    } else if (countVal > maxAllowed) {
                        chips[i].setVisibility(View.VISIBLE);
                        chips[i].setEnabled(false);
                        chips[i].setAlpha(0.35f);
                    } else {
                        chips[i].setVisibility(View.VISIBLE);
                        chips[i].setEnabled(true);
                        chips[i].setAlpha(1.0f);
                    }
                }
            }
        }

        final int[] selectedCount = {1};

        Runnable updatePreview = () -> {
            int count = selectedCount[0];
            int subDays = currentGame.getSubscriptionDurationDays();
            int addedDays = count * subDays;
            int totalChainDays = (currentSubs + count) * subDays;
            int totalGems = count * currentGame.getSubscriptionTotalCurrency();
            int totalPulls = totalGems / currentGame.getPullCost();

            if (tvDuration != null) {
                tvDuration.setText(getString(R.string.dialog_add_sub_preview_duration, addedDays, totalChainDays));
            }
            if (tvReward != null) {
                String formattedGems = String.format(Locale.getDefault(), "%,d", totalGems).replace(',', ' ');
                tvReward.setText(getString(R.string.dialog_add_sub_preview_reward, formattedGems, totalPulls));
            }
            if (tvStartDate != null) {
                Locale locale = LocaleList.getDefault().get(0);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
                tvStartDate.setText(getString(R.string.dialog_add_sub_start_date, effectiveStartDate.format(formatter)));
            }
        };

        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                for (int i = 0; i < 6; i++) {
                    if (chips[i] != null && chips[i].getId() == id) {
                        selectedCount[0] = i + 1;
                        break;
                    }
                }
                updatePreview.run();
            });
        }

        updatePreview.run();

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                viewModel.addSubscriptionChain(selectedCount[0], effectiveStartDate);
            });
        }

        dialog.show();
    }

    public void showDeletingSubDialog(Context context) {
        new MaterialAlertDialogBuilder(context, R.style.Dialog_GachaPoint_AlertDialog)
                .setTitle(getString(R.string.warning))
                .setMessage(R.string.delete_sub_confirm_message)
                .setPositiveButton(getString(R.string.ok_button), (dialog, which) -> {
                    viewModel.deletingSub();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void playSound(int resId) {
        Context context = getContext();
        if (context == null) return;

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            boolean isSilent = am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
            if (!isSilent && soundPool != null) {
                soundPool.play(successSound, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }
}