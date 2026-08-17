package ru.menshovanton.gachapoint.ui.fragment.tracker;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.data.repository.DatabaseRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Statistic;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;
import ru.menshovanton.gachapoint.ui.fragment.tracker.model.CalendarCellUiModel;

public class TrackerView extends Fragment {

    private TrackerViewModel viewModel;

    private TextView subsCounterView;
    private Button checkButton;
    private ImageButton moreButton;
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

    private final List<TextView> cellViewsPool = new ArrayList<>();

    private SoundPool soundPool;
    private int successSound;

    private final ActivityResultLauncher<String> exportDbLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
                if (uri != null) {
                    writeDatabaseToUri(uri);
                }
            });

    public TrackerView() {}

    public static TrackerView newInstance() {
        return new TrackerView();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
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

        subsCounterView = view.findViewById(R.id.subsCount);
        calendarGrid = view.findViewById(R.id.calendarGrid);
        checkButton = view.findViewById(R.id.checkButton);
        moreButton = view.findViewById(R.id.moreButton);

        gemIcon = view.findViewById(R.id.gemIcon);
        subsCountTitle = view.findViewById(R.id.subsCountHeader);
        wishIcon = view.findViewById(R.id.wishIcon);

        monthHeader = view.findViewById(R.id.monthHeader);
        yearHeader = view.findViewById(R.id.yearHeader);

        claimPrimogems = view.findViewById(R.id.cliamsGemsCounter);
        missedPrimogems = view.findViewById(R.id.missGemsCounter);
        claimWishes = view.findViewById(R.id.claimWishesCounter);
        missedWishes = view.findViewById(R.id.missWishesCounter);
        laterPrimogems = view.findViewById(R.id.laterGemsCounter);
        laterWishes = view.findViewById(R.id.laterWishesCounter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
    }

    private void observeViewModel() {
        viewModel.getGameTypeLiveData().observe(getViewLifecycleOwner(), this::updateGameTypeUi);
        viewModel.getSelectedMonthLiveData().observe(getViewLifecycleOwner(), this::updateHeaderMonth);
        viewModel.getSelectedYearLiveData().observe(getViewLifecycleOwner(), year -> yearHeader.setText(String.valueOf(year)));
        viewModel.getSubsCountLiveData().observe(getViewLifecycleOwner(), count -> subsCounterView.setText(String.valueOf(count)));
        viewModel.getStatisticLiveData().observe(getViewLifecycleOwner(), this::updateStatisticsUi);
        viewModel.getCalendarCellsLiveData().observe(getViewLifecycleOwner(), this::renderCalendarGrid);

        viewModel.getToastMessageEvent().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getOpenQuestionDialogEvent().observe(getViewLifecycleOwner(), unused ->
                showQuestionDialog(requireContext())
        );

        viewModel.getVibrateEvent().observe(getViewLifecycleOwner(), unused -> triggerVibration());

        viewModel.getPlaySoundEvent().observe(requireActivity(), this::playSound);
    }

    private void triggerVibration() {
        Context context = getContext();
        if (context == null) return;

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
        RecyclerView gameTypeChanger = view.findViewById(R.id.gameTypeChangerTracker);

        List<String> categories = Arrays.asList(
                getString(R.string.genshin),
                getString(R.string.hsr),
                getString(R.string.zzz)
        );

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        gameTypeChanger.setLayoutManager(layoutManager);

        pillsAdapter = new PillsAdapter(categories, (item, position) -> {
            GameType selected = GameType.fromCode(position);
            viewModel.setGameType(selected);
        });

        gameTypeChanger.setAdapter(pillsAdapter);

        GameType currentType = viewModel.getCurrentGameType();
        selectAndScrollIfNeeded(currentType.getCode());
    }

    private void updateGameTypeUi(GameType gameType) {
        selectAndScrollIfNeeded(gameType.getCode());

        switch (gameType) {
            case GENSHIN:
                gemIcon.setImageResource(R.drawable.icon_primogem);
                wishIcon.setImageResource(R.drawable.icon_intertwined_fate);
                subsCountTitle.setText(R.string.blessing_of_the_welkin_moon_count_header);
                break;
            case HSR:
                gemIcon.setImageResource(R.drawable.icon_stellar_jade);
                wishIcon.setImageResource(R.drawable.icon_star_rail_special_pass);
                subsCountTitle.setText(R.string.star_rail_special_pass_count_header);
                break;
            case ZZZ:
                gemIcon.setImageResource(R.drawable.icon_polychrome);
                wishIcon.setImageResource(R.drawable.icon_encrypted_master_tape);
                subsCountTitle.setText(R.string.inter_knot_member_count_header);
                break;
        }
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
        if (cells == null || cells.size() < 42) return;

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
                cellView.setTextColor(ContextCompat.getColor(requireContext(), model.textColorRes));
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
            dayView.setTextSize(18f);

            if (customTypeface != null) {
                dayView.setTypeface(customTypeface);
            }

            calendarGrid.addView(dayView);
            cellViewsPool.add(dayView);
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
        exportDbLauncher.launch(DatabaseRepository.DATABASE_NAME);
    }

    private void writeDatabaseToUri(Uri targetUri) {
        if (!isAdded()) return;
        Context context = requireContext();

        try {
            AppDatabase.getInstance(context)
                    .getOpenHelper()
                    .getWritableDatabase()
                    .query("PRAGMA wal_checkpoint(TRUNCATE)")
                    .close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File dbFile = context.getDatabasePath(DatabaseRepository.DATABASE_NAME);

        if (!dbFile.exists()) {
            Toast.makeText(context, R.string.db_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = context.getContentResolver().openOutputStream(targetUri)) {

            if (out == null) {
                Toast.makeText(context, R.string.db_export_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();

            Toast.makeText(context, R.string.db_export_successful, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.db_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void showQuestionDialog(Context context) {
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

    private void playSound(int resId) {
        soundPool.play(successSound, 1.0f, 1.0f, 0, 0, 1.0f);
    }
}