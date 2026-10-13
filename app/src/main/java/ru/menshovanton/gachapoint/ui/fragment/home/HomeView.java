package ru.menshovanton.gachapoint.ui.fragment.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.BannerType;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.BannerInfo;
import ru.menshovanton.gachapoint.domain.models.Statistic;
import ru.menshovanton.gachapoint.ui.fragment.home.model.HomeWidgetItem;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class HomeView extends Fragment {

    private MainActivityView mainActivityView;
    private HomeViewModel viewModel;

    private MaterialCardView mcvCustomizeWidgets;
    private PillsAdapter pillsAdapter;
    private LinearLayoutManager layoutManager;

    private LinearLayout llWidgetsContainer;
    private MaterialCardView mcvQuickActions;
    private MaterialCardView mcvSavings;
    private MaterialCardView mcvStatistics;
    private MaterialCardView mcvPity;

    private MaterialCardView btnQuickDaily;
    private ImageView ivQuickDailyIcon;
    private TextView tvQuickDailyStatus;

    private MaterialCardView btnQuickSub;
    private ImageView ivQuickSubIcon;
    private TextView tvQuickSubStatus;

    private MaterialCardView btnQuickSavings;
    private MaterialCardView btnQuickJournal;

    private TextView tvSavedCounter;
    private ImageView ivSavedPulls;
    private TextView tvSavedPercent;
    private LinearProgressIndicator pbSaveProgress;
    private TextView tvSavingsCurrency;

    private TextView tvStatSubsInfo;
    private TextView tvStatDailiesInfo;
    private ImageView ivGem;
    private ImageView ivPull;
    private TextView tvReceivedGems, tvOmittedGems, tvUpcomingGems;
    private TextView tvReceivedPulls, tvOmittedPulls, tvUpcomingPulls;

    private TextView tvPityBannerName;
    private TextView tvPity5StarCounter;
    private TextView tvPityGuaranteeBadge;
    private LinearProgressIndicator pbPityProgress;
    private TextView tvPityUntil5Star;

    private int lastProgress = 0;
    private int lastTarget = 100;

    public HomeView() {}

    public static HomeView newInstance() {
        return new HomeView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityView = (MainActivityView) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        initGameTypePills(view);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        if (mainActivityView != null && mainActivityView.getSubType() != null) {
            viewModel.setGameType(mainActivityView.getSubType());
        }

        observeViewModel();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        mcvCustomizeWidgets = view.findViewById(R.id.mcv_customize_widgets);
        llWidgetsContainer = view.findViewById(R.id.ll_widgets_container);

        mcvQuickActions = view.findViewById(R.id.mcv_widget_quick_actions);
        mcvSavings = view.findViewById(R.id.mcv_widget_savings);
        mcvStatistics = view.findViewById(R.id.mcv_widget_statistics);
        mcvPity = view.findViewById(R.id.mcv_widget_pity);

        btnQuickDaily = view.findViewById(R.id.btn_quick_daily);
        ivQuickDailyIcon = view.findViewById(R.id.iv_quick_daily_icon);
        tvQuickDailyStatus = view.findViewById(R.id.tv_quick_daily_status);

        btnQuickSub = view.findViewById(R.id.btn_quick_sub);
        ivQuickSubIcon = view.findViewById(R.id.iv_quick_sub_icon);
        tvQuickSubStatus = view.findViewById(R.id.tv_quick_sub_status);

        btnQuickSavings = view.findViewById(R.id.btn_quick_savings);
        btnQuickJournal = view.findViewById(R.id.btn_quick_journal);

        tvSavedCounter = view.findViewById(R.id.tv_saved_counter);
        ivSavedPulls = view.findViewById(R.id.iv_saved_pulls);
        tvSavedPercent = view.findViewById(R.id.tv_saved_percent);
        pbSaveProgress = view.findViewById(R.id.pb_save_progress);
        tvSavingsCurrency = view.findViewById(R.id.tv_savings_currency);

        tvStatSubsInfo = view.findViewById(R.id.tv_stat_subs_info);
        tvStatDailiesInfo = view.findViewById(R.id.tv_stat_dailies_info);
        ivGem = view.findViewById(R.id.iv_gem);
        ivPull = view.findViewById(R.id.iv_pull);
        tvReceivedGems = view.findViewById(R.id.tv_received_gems);
        tvOmittedGems = view.findViewById(R.id.tv_omitted_gems);
        tvUpcomingGems = view.findViewById(R.id.tv_upcoming_gems);
        tvReceivedPulls = view.findViewById(R.id.tv_received_pulls);
        tvOmittedPulls = view.findViewById(R.id.tv_omitted_pulls);
        tvUpcomingPulls = view.findViewById(R.id.tv_upcoming_pulls);

        tvPityBannerName = view.findViewById(R.id.tv_pity_banner_name);
        tvPity5StarCounter = view.findViewById(R.id.tv_pity_5star_counter);
        tvPityGuaranteeBadge = view.findViewById(R.id.tv_pity_guarantee_badge);
        pbPityProgress = view.findViewById(R.id.pb_pity_progress);
        tvPityUntil5Star = view.findViewById(R.id.tv_pity_until_5star);
    }

    private void setupClickListeners() {
        if (mcvCustomizeWidgets != null) {
            mcvCustomizeWidgets.setOnClickListener(v -> {
                triggerVibration();
                showWidgetsBottomSheet();
            });
        }

        if (btnQuickDaily != null) {
            btnQuickDaily.setOnClickListener(v -> {
                triggerVibration();
                viewModel.markDailyToday(() -> {
                    Boolean isDone = viewModel.getIsDailyDoneTodayLiveData().getValue();
                    if (isDone != null && isDone) {
                        Toast.makeText(getContext(), R.string.check_today, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.cancel_check_today, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (btnQuickSub != null) {
            btnQuickSub.setOnClickListener(v -> {
                triggerVibration();
                Integer remaining = viewModel.getSubDaysRemainingTodayLiveData().getValue();
                if (remaining == null || remaining <= 0) {
                    Toast.makeText(getContext(), R.string.active_subs_null, Toast.LENGTH_SHORT).show();
                    navigateToTracker();
                } else {
                    viewModel.markSubToday(() -> {
                        Boolean claimed = viewModel.getIsSubClaimedTodayLiveData().getValue();
                        if (claimed != null && claimed) {
                            Toast.makeText(getContext(), R.string.check_today, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), R.string.cancel_check_today, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }

        if (btnQuickSavings != null) {
            btnQuickSavings.setOnClickListener(v -> {
                triggerVibration();
                navigateToPiggyBank();
            });
        }

        if (btnQuickJournal != null) {
            btnQuickJournal.setOnClickListener(v -> {
                triggerVibration();
                navigateToJournal();
            });
        }

        if (mcvSavings != null) {
            mcvSavings.setOnClickListener(v -> {
                triggerVibration();
                navigateToPiggyBank();
            });
        }

        if (mcvStatistics != null) {
            mcvStatistics.setOnClickListener(v -> {
                triggerVibration();
                navigateToTracker();
            });
        }

        if (mcvPity != null) {
            mcvPity.setOnClickListener(v -> {
                triggerVibration();
                navigateToJournal();
            });
        }
    }

    private void navigateToPiggyBank() {
        try {
            NavHostFragment.findNavController(this).navigate(R.id.action_home_to_piggy_bank);
        } catch (Exception ignored) {
        }
    }

    private void navigateToTracker() {
        if (getActivity() != null) {
            BottomNavigationView bnv = getActivity().findViewById(R.id.bnv_main);
            if (bnv != null) {
                bnv.setSelectedItemId(R.id.nav_tracker);
            }
        }
    }

    private void navigateToJournal() {
        if (getActivity() != null) {
            BottomNavigationView bnv = getActivity().findViewById(R.id.bnv_main);
            if (bnv != null) {
                bnv.setSelectedItemId(R.id.nav_journal);
            }
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
            viewModel.setGameType(current);
        } else if (viewModel != null) {
            viewModel.loadData();
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
                : GameType.GENSHIN;

        pillsAdapter = new PillsAdapter(categories, currentType.getCode(), (item, position) -> {
            GameType selected = GameType.fromCode(position);
            if (mainActivityView != null) mainActivityView.setSubType(selected);
            viewModel.setGameType(selected);
            smoothScrollToCenter(position);
        });

        gameTypeChanger.setAdapter(pillsAdapter);
        centerPositionInstantly(currentType.getCode());
    }

    private void observeViewModel() {
        viewModel.getGameTypeLiveData().observe(getViewLifecycleOwner(), this::updateGameTheme);

        viewModel.getPiggyProgressLiveData().observe(getViewLifecycleOwner(), progress -> updatePiggyProgress(progress, null));
        viewModel.getPiggyTargetLiveData().observe(getViewLifecycleOwner(), target -> updatePiggyProgress(null, target));

        viewModel.getStatisticLiveData().observe(getViewLifecycleOwner(), this::updateStatisticsUi);
        viewModel.getDailiesMonthProgressLiveData().observe(getViewLifecycleOwner(), this::updateDailiesMonthUi);
        viewModel.getSubDaysRemainingTodayLiveData().observe(getViewLifecycleOwner(), this::updateSubRemainingUi);

        viewModel.getIsDailyDoneTodayLiveData().observe(getViewLifecycleOwner(), this::updateDailyStatusUi);
        viewModel.getIsSubClaimedTodayLiveData().observe(getViewLifecycleOwner(), isClaimed -> updateSubClaimStatusUi());

        viewModel.getPity5StarLiveData().observe(getViewLifecycleOwner(), pity -> updatePityUi());
        viewModel.getMaxPityLiveData().observe(getViewLifecycleOwner(), max -> updatePityUi());
        viewModel.getIsGuaranteedLiveData().observe(getViewLifecycleOwner(), guaranteed -> updatePityUi());

        viewModel.getWidgetsConfigLiveData().observe(getViewLifecycleOwner(), this::applyWidgetsConfig);
    }

    private void applyWidgetsConfig(List<HomeWidgetItem> config) {
        if (config == null || llWidgetsContainer == null) return;

        Map<HomeWidgetItem.Type, View> widgetMap = new EnumMap<>(HomeWidgetItem.Type.class);
        widgetMap.put(HomeWidgetItem.Type.QUICK_ACTIONS, mcvQuickActions);
        widgetMap.put(HomeWidgetItem.Type.SAVINGS, mcvSavings);
        widgetMap.put(HomeWidgetItem.Type.STATISTICS, mcvStatistics);
        widgetMap.put(HomeWidgetItem.Type.PITY, mcvPity);

        llWidgetsContainer.removeAllViews();

        for (HomeWidgetItem item : config) {
            View widgetView = widgetMap.get(item.type);
            if (widgetView != null) {
                if (item.isVisible) {
                    widgetView.setVisibility(View.VISIBLE);
                    llWidgetsContainer.addView(widgetView);
                } else {
                    widgetView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void showWidgetsBottomSheet() {
        if (getContext() == null) return;
        HomeWidgetsSheetDialog.show(requireContext(), () -> viewModel.loadWidgetsConfig());
    }

    private void updateDailyStatusUi(Boolean isDone) {
        if (getContext() == null || tvQuickDailyStatus == null || ivQuickDailyIcon == null) return;

        int checkedColor = ContextCompat.getColor(requireContext(), R.color.checked);
        int checkColor = ContextCompat.getColor(requireContext(), R.color.check);

        if (isDone != null && isDone) {
            tvQuickDailyStatus.setText(R.string.home_action_daily_done);
            tvQuickDailyStatus.setTextColor(checkedColor);
            ivQuickDailyIcon.setImageResource(R.drawable.icon_check);
            ivQuickDailyIcon.setImageTintList(ColorStateList.valueOf(checkedColor));
        } else {
            tvQuickDailyStatus.setText(R.string.home_action_daily_todo);
            tvQuickDailyStatus.setTextColor(checkColor);
            ivQuickDailyIcon.setImageResource(R.drawable.icon_daily_task);
            ivQuickDailyIcon.setImageTintList(ColorStateList.valueOf(checkColor));
        }
    }

    private void updateSubClaimStatusUi() {
        if (getContext() == null || tvQuickSubStatus == null || ivQuickSubIcon == null) return;

        Integer remaining = viewModel.getSubDaysRemainingTodayLiveData().getValue();
        Boolean isClaimed = viewModel.getIsSubClaimedTodayLiveData().getValue();

        int checkedColor = ContextCompat.getColor(requireContext(), R.color.checked);
        int checkColor = ContextCompat.getColor(requireContext(), R.color.check);
        int secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        if (remaining == null || remaining <= 0) {
            tvQuickSubStatus.setText(R.string.home_action_sub_none);
            tvQuickSubStatus.setTextColor(secondaryColor);
            ivQuickSubIcon.setImageResource(R.drawable.icon_subscription);
            ivQuickSubIcon.setImageTintList(ColorStateList.valueOf(secondaryColor));
        } else if (isClaimed != null && isClaimed) {
            tvQuickSubStatus.setText(R.string.home_action_sub_claimed);
            tvQuickSubStatus.setTextColor(checkedColor);
            ivQuickSubIcon.setImageResource(R.drawable.icon_check);
            ivQuickSubIcon.setImageTintList(ColorStateList.valueOf(checkedColor));
        } else {
            tvQuickSubStatus.setText(R.string.home_action_sub_claim);
            tvQuickSubStatus.setTextColor(checkColor);
            ivQuickSubIcon.setImageResource(R.drawable.icon_subscription);
            ivQuickSubIcon.setImageTintList(ColorStateList.valueOf(checkColor));
        }
    }

    private void updateSubRemainingUi(Integer remaining) {
        updateSubClaimStatusUi();
        if (tvStatSubsInfo == null) return;
        if (remaining != null && remaining > 0) {
            tvStatSubsInfo.setText(getString(R.string.home_stat_subs_remaining, remaining));
        } else {
            tvStatSubsInfo.setText(R.string.home_stat_subs_inactive);
        }
    }

    private void updateDailiesMonthUi(int[] progress) {
        if (tvStatDailiesInfo == null || progress == null || progress.length < 2) return;
        tvStatDailiesInfo.setText(getString(R.string.home_stat_dailies_month, progress[0], progress[1]));
    }

    @SuppressLint("SetTextI18n")
    private void updatePiggyProgress(@Nullable Integer progress, @Nullable Integer target) {
        if (target != null) lastTarget = target;
        if (progress != null) lastProgress = progress;

        if (pbSaveProgress != null && tvSavedCounter != null) {
            pbSaveProgress.setMax(lastTarget > 0 ? lastTarget : 1);
            pbSaveProgress.setProgress(lastProgress, false);
            tvSavedCounter.setText(lastProgress + " / " + lastTarget);
        }

        if (tvSavedPercent != null) {
            int percent = lastTarget > 0 ? (int) Math.min(100, Math.round((lastProgress * 100.0) / lastTarget)) : 0;
            tvSavedPercent.setText(percent + "%");
        }

        if (tvSavingsCurrency != null) {
            GameType currentGame = viewModel.getGameTypeLiveData().getValue();
            int pullCost = (currentGame != null) ? currentGame.getPullCost() : 160;
            long totalCurrency = (long) lastProgress * pullCost;
            String formatted = NumberFormat.getIntegerInstance().format(totalCurrency);
            tvSavingsCurrency.setText("≈ " + formatted + " (" + getString(R.string.home_savings_in_currency) + ")");
        }
    }

    private void updateStatisticsUi(Statistic statistic) {
        if (statistic == null) return;

        if (tvReceivedGems != null) tvReceivedGems.setText(String.valueOf(statistic.claimGems));
        if (tvOmittedGems != null) tvOmittedGems.setText(String.valueOf(statistic.missedGems));
        if (tvReceivedPulls != null) tvReceivedPulls.setText(String.valueOf(statistic.claimWishes));
        if (tvOmittedPulls != null) tvOmittedPulls.setText(String.valueOf(statistic.missedWishes));

        if (tvUpcomingGems != null) {
            tvUpcomingGems.setText(statistic.laterGems > 0 ? String.valueOf(statistic.laterGems) : "0");
        }
        if (tvUpcomingPulls != null) {
            tvUpcomingPulls.setText(statistic.laterWishes > 0 ? String.valueOf(statistic.laterWishes) : "0");
        }
    }

    @SuppressLint("SetTextI18n")
    private void updatePityUi() {
        if (getContext() == null || tvPity5StarCounter == null || pbPityProgress == null) return;

        Integer pity = viewModel.getPity5StarLiveData().getValue();
        Integer max = viewModel.getMaxPityLiveData().getValue();
        Boolean guaranteed = viewModel.getIsGuaranteedLiveData().getValue();

        int p = (pity != null) ? pity : 0;
        int m = (max != null && max > 0) ? max : 90;
        boolean isGuar = (guaranteed != null && guaranteed);

        tvPity5StarCounter.setText(p + " / " + m);
        pbPityProgress.setMax(m);
        pbPityProgress.setProgress(p, false);

        int until = Math.max(0, m - p);
        if (tvPityUntil5Star != null) {
            tvPityUntil5Star.setText(getString(R.string.home_pity_until_5star, until));
        }

        if (tvPityGuaranteeBadge != null) {
            if (isGuar) {
                tvPityGuaranteeBadge.setText(R.string.home_pity_guaranteed_badge);
                tvPityGuaranteeBadge.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.badge_gold_bg)));
                tvPityGuaranteeBadge.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.rare_legendary));
            } else {
                tvPityGuaranteeBadge.setText(R.string.home_pity_fifty_fifty_badge);
                tvPityGuaranteeBadge.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.badge_blue_bg)));
                tvPityGuaranteeBadge.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.check));
            }
        }
    }

    private void updateGameTheme(GameType gameType) {
        if (gameType == null) return;

        if (ivGem != null) ivGem.setImageResource(gameType.getCurrencyIconResId());
        if (ivPull != null) ivPull.setImageResource(gameType.getPullIconResId());
        if (ivSavedPulls != null) ivSavedPulls.setImageResource(gameType.getPullIconResId());

        if (tvPityBannerName != null && getContext() != null) {
            BannerInfo banner = gameType.getBannerInfo(BannerType.EVENT.getDbKey());
            if (banner != null) {
                tvPityBannerName.setText(banner.getLabelResId());
            } else {
                tvPityBannerName.setText(R.string.home_pity_event);
            }
        }

        updatePiggyProgress(null, null);
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
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void centerPositionInstantly(int position) {
        if (layoutManager == null) {
            return;
        } else {
            getResources();
        }
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

            if (Math.abs(dx) <= 3) return;
            if (dx < 0 && !recyclerView.canScrollHorizontally(-1)) return;
            if (dx > 0 && !recyclerView.canScrollHorizontally(1)) return;

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
}