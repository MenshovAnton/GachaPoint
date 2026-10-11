package ru.menshovanton.gachapoint.ui.fragment.tracker;

import android.animation.ValueAnimator;
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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Statistic;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;
import ru.menshovanton.gachapoint.ui.fragment.tracker.model.CalendarCellUiModel;

public class TrackerView extends Fragment {

    private TrackerViewModel viewModel;

    private TextView subsCounterView;
    private CalendarGrid calendarGrid;

    private PillsAdapter pillsAdapter;
    private LinearLayoutManager layoutManager;

    private ImageView gemIcon;
    private TextView subsCountTitle;
    private ImageView wishIcon;

    private TextView monthHeader;
    private TextView yearHeader;

    private TextView claimPrimogems;
    private TextView missedPrimogems;
    private TextView claimWishes;
    private TextView missedWishes;
    private TextView laterPrimogems;
    private TextView laterWishes;

    private boolean isAnimating = false;
    private boolean isFirstLaunch = true;

    private final List<TextView> cellViewsPool = new ArrayList<>();

    private SoundPool soundPool;
    private int successSound;

    private final ActivityResultLauncher<String> exportDbLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("*/*"), uri -> {
                if (uri != null) {
                    viewModel.writeDatabaseToUri(uri);
                }
            });

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
        View view = inflater.inflate(R.layout.fragment_tracker, container, false);

        subsCounterView = view.findViewById(R.id.tv_sub_counter);
        calendarGrid = view.findViewById(R.id.cg_calendar_grid);
        Button checkButton = view.findViewById(R.id.btn_mark_today);
        ImageButton moreButton = view.findViewById(R.id.btn_more);

        gemIcon = view.findViewById(R.id.iv_gem);
        subsCountTitle = view.findViewById(R.id.tv_sub_counter_label);
        wishIcon = view.findViewById(R.id.iv_pull);

        monthHeader = view.findViewById(R.id.tv_month);
        yearHeader = view.findViewById(R.id.tv_year);

        claimPrimogems = view.findViewById(R.id.tv_received_gems);
        missedPrimogems = view.findViewById(R.id.tv_omitted_gems);
        claimWishes = view.findViewById(R.id.tv_received_pulls);
        missedWishes = view.findViewById(R.id.tv_omitted_pulls);
        laterPrimogems = view.findViewById(R.id.tv_upcoming_gems);
        laterWishes = view.findViewById(R.id.tv_upcoming_pulls);

        viewModel = new ViewModelProvider(this).get(TrackerViewModel.class);

        initCalendarGrid();
        initGameTypePills(view);

        checkButton.setOnClickListener(v -> viewModel.onCheckClick());
        moreButton.setOnClickListener(this::showMoreMenu);

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
        if (mainActivityView != null && mainActivityView.getSubType() != null) {
            GameType current = mainActivityView.getSubType();
            if (pillsAdapter != null && pillsAdapter.getSelectedPosition() != current.getCode()) {
                pillsAdapter.setSelectedPosition(current.getCode());
                centerPositionInstantly(current.getCode());
            }
            viewModel.setGameType(current);
        }
        if (!isFirstLaunch && viewModel != null) {
            viewModel.refreshData();
        }
        isFirstLaunch = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isAnimating = false;
        calendarGrid.removeCallbacks(null);
    }

    private void observeViewModel() {
        viewModel.getGameTypeLiveData().observe(getViewLifecycleOwner(), this::updateGameTypeUi);
        viewModel.getSelectedMonthLiveData().observe(getViewLifecycleOwner(), this::updateHeaderMonth);
        viewModel.getSelectedYearLiveData().observe(getViewLifecycleOwner(), year -> yearHeader.setText(String.valueOf(year)));
        viewModel.getSubsCountLiveData().observe(getViewLifecycleOwner(), count -> subsCounterView.setText(String.valueOf(count)));
        viewModel.getStatisticLiveData().observe(getViewLifecycleOwner(), this::updateStatisticsUi);
        viewModel.getCalendarCellsLiveData().observe(getViewLifecycleOwner(), this::renderCalendarGrid);

        viewModel.getToastMessageEvent().observe(getViewLifecycleOwner(), resId -> {
            Context context = getContext();
            if (resId != null && context != null) {
                Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
            }
        });

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
        RecyclerView gameTypeChanger = view.findViewById(R.id.rv_game_types);

        List<String> categories = new ArrayList<>();
        for (GameType type : GameType.values()) {
            categories.add(getString(type.getNameResId()));
        }

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        gameTypeChanger.setLayoutManager(layoutManager);

        GameType currentType = (mainActivityView != null && mainActivityView.getSubType() != null)
                ? mainActivityView.getSubType()
                : viewModel.getCurrentGameType();

        pillsAdapter = new PillsAdapter(categories, currentType.getCode(), (item, position) -> {
            GameType selected = GameType.fromCode(position);
            if (mainActivityView != null) mainActivityView.setSubType(selected);
            viewModel.setGameType(selected);
            smoothScrollToCenter(position);
        });

        gameTypeChanger.setAdapter(pillsAdapter);

        viewModel.setGameType(currentType);
        centerPositionInstantly(currentType.getCode());
    }

    private void updateGameTypeUi(GameType gameType) {
        gemIcon.setImageResource(gameType.getCurrencyIconResId());
        wishIcon.setImageResource(gameType.getPullIconResId());
        subsCountTitle.setText(gameType.getSubTitleResId());
    }

    private void animateGridHeight(int targetHeight) {
        int initialHeight = calendarGrid.getHeight();
        if (initialHeight == targetHeight || initialHeight == 0) {
            ViewGroup.LayoutParams params = calendarGrid.getLayoutParams();
            params.height = targetHeight;
            calendarGrid.setLayoutParams(params);
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = calendarGrid.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            calendarGrid.setLayoutParams(params);
        });
        animator.start();
    }

    private void animateMonthChange(boolean isNext) {
        if (isAnimating) return;
        isAnimating = true;

        int width = calendarGrid.getWidth();
        if (width == 0) {
            notifyViewModelMonthChange(isNext);
            isAnimating = false;
            return;
        }

        float targetOutX = isNext ? -width * 0.4f : width * 0.4f;
        float startInX = isNext ? width * 0.4f : -width * 0.4f;

        calendarGrid.animate()
                .translationX(targetOutX)
                .alpha(0f)
                .setDuration(130)
                .withEndAction(() -> {
                    notifyViewModelMonthChange(isNext);

                    calendarGrid.setTranslationX(startInX);
                    calendarGrid.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(130)
                            .withEndAction(() -> isAnimating = false)
                            .start();
                })
                .start();
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

        Context context = requireContext();
        int activeRows = IntStream.range(35, 42).anyMatch(i -> cells.get(i).isVisible) ? 6 : 5;

        for (int i = 0; i < 42; i++) {
            TextView cellView = cellViewsPool.get(i);
            CalendarCellUiModel model = cells.get(i);

            if (i >= 35 && activeRows == 5) {
                cellView.setVisibility(View.GONE);
                cellView.setText("");
                cellView.setBackground(null);
                continue;
            }

            if (!model.isVisible) {
                cellView.setVisibility(View.INVISIBLE);
                cellView.setText("");
                cellView.setBackground(null);
            } else {
                cellView.setVisibility(View.VISIBLE);
                cellView.setText(String.valueOf(model.dayOfMonth));
                cellView.setBackgroundResource(model.backgroundRes);
                cellView.setTextColor(ContextCompat.getColor(context, model.textColorRes));
            }
        }

        calendarGrid.post(() -> {
            int gridWidth = calendarGrid.getWidth();
            if (gridWidth == 0 || !isAdded()) return;

            float density = getResources().getDisplayMetrics().density;
            int marginPx = (int) (4 * density);
            int cellSidePx = (gridWidth - (marginPx * 2 * 7)) / 7;

            for (int i = 0; i < 42; i++) {
                TextView cell = cellViewsPool.get(i);
                GridLayout.LayoutParams params = (GridLayout.LayoutParams) cell.getLayoutParams();
                if (params != null) {
                    params.width = cellSidePx;
                    params.height = cellSidePx;
                    params.setMargins(marginPx, marginPx, marginPx, marginPx);
                    cell.setLayoutParams(params);
                }
            }

            int targetHeight = (cellSidePx + (marginPx * 2)) * activeRows;

            animateGridHeight(targetHeight);
        });
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

    @SuppressLint("SetTextI18n")
    private void updateHeaderMonth(int month) {
        Month monthObj = Month.of(month);
        Locale locale = LocaleList.getDefault().get(0);
        String printMonth = monthObj.getDisplayName(TextStyle.FULL_STANDALONE, locale);
        monthHeader.setText(printMonth.substring(0, 1).toUpperCase() + printMonth.substring(1));
    }

    private void initCalendarGrid() {
        if (!cellViewsPool.isEmpty() && calendarGrid.getChildCount() == 42) return;

        calendarGrid.removeAllViews();
        cellViewsPool.clear();

        float density = getResources().getDisplayMetrics().density;
        int cellHeightInPx = (int) (48 * density);
        int marginInPx = (int) (4 * density);

        Typeface customTypeface = ResourcesCompat.getFont(requireContext(), R.font.genshin_font);

        for (int i = 0; i < 42; i++) {
            TextView dayView = new TextView(getContext());

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = cellHeightInPx;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
            dayView.setLayoutParams(params);

            dayView.setGravity(Gravity.CENTER);

            dayView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);

            if (customTypeface != null) {
                dayView.setTypeface(customTypeface);
            }

            calendarGrid.addView(dayView);
            cellViewsPool.add(dayView);
        }
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
        pillsAdapter.setSelectedPosition(targetPosition);

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
                @Override public void onExport() { exportDatabase(); }
                @Override public void onRecovery() { viewModel.recoveryMissDay(); }
                @Override public void onCancel() { viewModel.onCancelCheck(); }
            });
        }
    }

    public void exportDatabase() {
        exportDbLauncher.launch(AppDatabase.DATABASE_NAME);
    }

    public void showAddingSubDialog(Context context) {
        new MaterialAlertDialogBuilder(context, R.style.Dialog_GachaPoint_AlertDialog)
                .setTitle(getString(R.string.check_button_text))
                .setMessage(R.string.active_subs_null_question)
                .setPositiveButton(getString(R.string.ok_button), (dialog, which) -> {
                    viewModel.onAddClick();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> dialog.dismiss())
                .show();
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

    public void showMessageDialog(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context, R.style.Dialog_GachaPoint_AlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok_button), (dialog, which) -> dialog.dismiss())
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