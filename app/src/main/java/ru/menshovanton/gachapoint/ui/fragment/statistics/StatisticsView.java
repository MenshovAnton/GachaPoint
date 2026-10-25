package ru.menshovanton.gachapoint.ui.fragment.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.BannerInfo;
import ru.menshovanton.gachapoint.ui.animation.NumberCountAnimator;
import ru.menshovanton.gachapoint.ui.fragment.statistics.adapter.SubscriptionHistoryAdapter;
import ru.menshovanton.gachapoint.ui.fragment.statistics.model.OverallStatisticsData;
import ru.menshovanton.gachapoint.ui.fragment.statistics.share.StatisticsShareSheetDialog;
import ru.menshovanton.gachapoint.ui.fragment.statistics.views.BannerDistributionBarView;
import ru.menshovanton.gachapoint.ui.fragment.statistics.views.SavingsSourceBarView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class StatisticsView extends Fragment {

    private StatisticsViewModel viewModel;

    private TextView tvTotalPullsCount;
    private TextView tvTotalCurrencySpent;
    private TextView tvLegendaryStat;
    private TextView tvLegendarySub;
    private TextView tvEpicStat;
    private TextView tvEpicSub;

    private TextView tvWinratePercent;
    private TextView tvWinrateCounts;
    private TextView tvAvgPityValue;
    private TextView tvPityRangeValue;

    private BannerDistributionBarView bannerDistributionBars;

    private TextView tvSubsPurchasedCount;
    private TextView tvSubsAttendanceRate;
    private View rowHistoryHeader;
    private TextView tvHistoryToggle;
    private TextView tvHistoryEmpty;
    private RecyclerView rvSubsHistory;
    private MaterialButton btnHistoryToggle;
    private SubscriptionHistoryAdapter historyAdapter;

    private static final int HISTORY_PREVIEW_LIMIT = 3;

    private TextView tvSavingsProgressSummary;
    private SavingsSourceBarView savingsSourceBar;
    private TextView tvSourceManualVal;
    private TextView tvSourceSubsVal;
    private TextView tvSourceDailiesVal;

    private TextView tvTotalTrackedWealth;

    private boolean isHistoryExpanded = false;
    private List<SubscriptionEntity> fullHistoryList = new ArrayList<>();

    public StatisticsView() {}

    public static StatisticsView newInstance() {
        return new StatisticsView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        ImageButton btnShare = view.findViewById(R.id.btn_share);

        tvTotalPullsCount = view.findViewById(R.id.tv_total_pulls_count);
        tvTotalCurrencySpent = view.findViewById(R.id.tv_total_currency_spent);
        tvLegendaryStat = view.findViewById(R.id.tv_legendary_stat);
        tvLegendarySub = view.findViewById(R.id.tv_legendary_sub);
        tvEpicStat = view.findViewById(R.id.tv_epic_stat);
        tvEpicSub = view.findViewById(R.id.tv_epic_sub);

        tvWinratePercent = view.findViewById(R.id.tv_winrate_percent);
        tvWinrateCounts = view.findViewById(R.id.tv_winrate_counts);
        tvAvgPityValue = view.findViewById(R.id.tv_avg_pity_value);
        tvPityRangeValue = view.findViewById(R.id.tv_pity_range_value);

        bannerDistributionBars = view.findViewById(R.id.banner_distribution_bars);

        tvSubsPurchasedCount = view.findViewById(R.id.tv_subs_purchased_count);
        tvSubsAttendanceRate = view.findViewById(R.id.tv_subs_attendance_rate);
        rowHistoryHeader = view.findViewById(R.id.row_history_header);
        tvHistoryToggle = view.findViewById(R.id.tv_history_toggle);
        tvHistoryEmpty = view.findViewById(R.id.tv_history_empty);
        rvSubsHistory = view.findViewById(R.id.rv_subs_history);
        btnHistoryToggle = view.findViewById(R.id.btn_history_toggle);

        tvSavingsProgressSummary = view.findViewById(R.id.tv_savings_progress_summary);
        savingsSourceBar = view.findViewById(R.id.savings_source_bar);
        tvSourceManualVal = view.findViewById(R.id.tv_source_manual_val);
        tvSourceSubsVal = view.findViewById(R.id.tv_source_subs_val);
        tvSourceDailiesVal = view.findViewById(R.id.tv_source_dailies_val);

        tvTotalTrackedWealth = view.findViewById(R.id.tv_total_tracked_wealth);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                StatisticsShareSheetDialog sheet = StatisticsShareSheetDialog.newInstance();
                sheet.setListener(new StatisticsShareSheetDialog.OnOptionSelectedListener() {
                    @Override
                    public void onShareSend() {
                        if (viewModel != null) {
                            viewModel.shareReport(requireContext());
                        }
                    }

                    @Override
                    public void onSaveToGallery() {
                        if (viewModel != null) {
                            viewModel.saveReportToGallery(requireContext());
                        }
                    }
                });
                sheet.show(getChildFragmentManager(), "StatisticsShareSheetDialog");
            });
        }

        setupHistoryList();
        setupPills(view);

        return view;
    }

    private void setupHistoryList() {
        historyAdapter = new SubscriptionHistoryAdapter();
        rvSubsHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSubsHistory.setAdapter(historyAdapter);

        if (rowHistoryHeader != null) {
            rowHistoryHeader.setOnClickListener(v -> toggleHistoryExpanded());
        }
        if (btnHistoryToggle != null) {
            btnHistoryToggle.setOnClickListener(v -> toggleHistoryExpanded());
        }
    }

    private void toggleHistoryExpanded() {
        if (fullHistoryList == null || fullHistoryList.size() <= HISTORY_PREVIEW_LIMIT) {
            return;
        }
        isHistoryExpanded = !isHistoryExpanded;
        updateHistoryListDisplay();
    }

    private void setupPills(View view) {
        RecyclerView rvGameTypes = view.findViewById(R.id.rv_game_types);
        List<String> gameNames = new ArrayList<>();
        for (GameType game : GameType.values()) {
            gameNames.add(getString(game.getNameResId()));
        }

        PillsAdapter pillsAdapter = new PillsAdapter(gameNames, 0, (item, position) -> {
            if (viewModel != null && position >= 0 && position < GameType.values().length) {
                isHistoryExpanded = false;
                viewModel.setGameType(GameType.values()[position]);
            }
        });

        rvGameTypes.setAdapter(pillsAdapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        observeViewModel();
        viewModel.loadStatistics();
    }

    private void observeViewModel() {
        viewModel.getStatisticsLiveData().observe(getViewLifecycleOwner(), this::bindStatistics);

        viewModel.getShareIntentEvent().observe(getViewLifecycleOwner(), shareIntent -> {
            if (shareIntent != null) {
                startActivity(shareIntent);
            }
        });

        viewModel.getSaveToGallerySuccessEvent().observe(getViewLifecycleOwner(), unused ->
                Toast.makeText(requireContext(), R.string.stat_report_saved_success, Toast.LENGTH_SHORT).show());

        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindStatistics(OverallStatisticsData data) {
        if (data == null) return;

        GameType gameType = data.getGameType();

        NumberCountAnimator.animate(tvTotalPullsCount, data.getTotalPulls(),
                val -> String.format(Locale.getDefault(), "%,d", val).replace(',', ' '));

        NumberCountAnimator.animate(tvTotalCurrencySpent, (int) Math.min(Integer.MAX_VALUE, data.getTotalCurrencySpent()),
                val -> String.format(Locale.getDefault(), "%,d", val).replace(',', ' '));

        NumberCountAnimator.animate(tvLegendaryStat, Math.round(data.getFiveStarPercent() * 10f),
                val -> String.format(Locale.getDefault(), "%.1f%%", val / 10f));
        if (data.getFiveStarCount() > 0) {
            tvLegendarySub.setText(String.format(Locale.getDefault(), getString(R.string.stat_badge_sub_count_freq),
                    data.getFiveStarCount(), getString(R.string.stat_subs_unit), data.getAveragePullsPerFiveStar()));
        } else {
            tvLegendarySub.setText(String.format(Locale.getDefault(), getString(R.string.stat_badge_sub_empty_count), getString(R.string.stat_subs_unit)));
        }

        NumberCountAnimator.animate(tvEpicStat, Math.round(data.getFourStarPercent() * 10f),
                val -> String.format(Locale.getDefault(), "%.1f%%", val / 10f));
        if (data.getFourStarCount() > 0) {
            tvEpicSub.setText(String.format(Locale.getDefault(), getString(R.string.stat_badge_sub_count_freq),
                    data.getFourStarCount(), getString(R.string.stat_subs_unit), data.getAveragePullsPerFourStar()));
        } else {
            tvEpicSub.setText(String.format(Locale.getDefault(), getString(R.string.stat_badge_sub_empty_count), getString(R.string.stat_subs_unit)));
        }

        int total5050 = data.getWin5050Count() + data.getLoss5050Count();
        if (total5050 > 0) {
            NumberCountAnimator.animate(tvWinratePercent, Math.round(data.getWin5050Rate() * 10f),
                    val -> String.format(Locale.getDefault(), "%.1f%%", val / 10f));
            tvWinrateCounts.setText(String.format(Locale.getDefault(), "%dW / %dL", data.getWin5050Count(), data.getLoss5050Count()));
        } else {
            tvWinratePercent.setText("—");
            tvWinrateCounts.setText(getString(R.string.stat_empty_pulls));
        }

        if (data.getAvg5StarPity() > 0) {
            NumberCountAnimator.animate(tvAvgPityValue, Math.round(data.getAvg5StarPity() * 10f),
                    val -> String.format(Locale.getDefault(), "%.1f", val / 10f));
            tvPityRangeValue.setText(String.format(getString(R.string.stat_pity_range), data.getMin5StarPity(), data.getMax5StarPity()));
        } else {
            tvAvgPityValue.setText("—");
            tvPityRangeValue.setText("—");
        }

        List<BannerDistributionBarView.BannerItem> bannerItems = new ArrayList<>();
        Map<String, Integer> bannerCounts = data.getBannerPullsCount();
        int totalP = data.getTotalPulls();

        for (BannerInfo banner : gameType.getBanners()) {
            Integer countObj = bannerCounts.get(banner.getDbKey());
            int count = countObj != null ? countObj : 0;
            float pct = (totalP > 0) ? (count * 100f / totalP) : 0f;
            bannerItems.add(new BannerDistributionBarView.BannerItem(
                    getString(banner.getLabelResId()),
                    count,
                    pct
            ));
        }
        bannerDistributionBars.setBannersData(bannerItems);

        // Subscriptions
        String subsSummary = String.format(Locale.getDefault(), "%d %s (%d %s)",
                data.getTotalSubsPurchased(), getString(R.string.stat_subs_unit),
                data.getTotalSubDays(), getString(R.string.stat_days_unit));
        tvSubsPurchasedCount.setText(subsSummary);

        NumberCountAnimator.animate(tvSubsAttendanceRate, Math.round(data.getSubAttendanceRate() * 10f),
                val -> String.format(Locale.getDefault(), "%.1f%%", val / 10f));

        fullHistoryList = data.getSubscriptionHistory();
        updateHistoryListDisplay();

        String savingsSummary = String.format(Locale.getDefault(), "%d / %d %s",
                data.getTotalProgressPulls(), data.getTargetPulls(), getString(R.string.pulls).toLowerCase(Locale.ROOT));
        tvSavingsProgressSummary.setText(savingsSummary);

        savingsSourceBar.setSourceData(data.getManualPulls(), data.getSubsPulls(), data.getDailiesPulls(), true);

        NumberCountAnimator.animate(tvSourceManualVal, data.getManualPulls(),
                val -> String.format(Locale.getDefault(), "%d %s", val, getString(R.string.stat_pity_pulls_unit)));
        NumberCountAnimator.animate(tvSourceSubsVal, data.getSubsPulls(),
                val -> String.format(Locale.getDefault(), "%d %s", val, getString(R.string.stat_pity_pulls_unit)));
        NumberCountAnimator.animate(tvSourceDailiesVal, data.getDailiesPulls(),
                val -> String.format(Locale.getDefault(), "%d %s", val, getString(R.string.stat_pity_pulls_unit)));

        NumberCountAnimator.animate(tvTotalTrackedWealth, (int) Math.min(Integer.MAX_VALUE, data.getTotalTrackedCurrency()),
                val -> String.format(Locale.getDefault(), "%,d", val).replace(',', ' '));
    }

    private void updateHistoryListDisplay() {
        if (fullHistoryList == null || fullHistoryList.isEmpty()) {
            tvHistoryEmpty.setVisibility(View.VISIBLE);
            rvSubsHistory.setVisibility(View.GONE);
            tvHistoryToggle.setVisibility(View.GONE);
            if (btnHistoryToggle != null) {
                btnHistoryToggle.setVisibility(View.GONE);
            }
            return;
        }

        tvHistoryEmpty.setVisibility(View.GONE);
        rvSubsHistory.setVisibility(View.VISIBLE);

        GameType curGame = (viewModel != null && viewModel.getCurrentGameTypeLiveData().getValue() != null)
                ? viewModel.getCurrentGameTypeLiveData().getValue()
                : GameType.GENSHIN;

        if (fullHistoryList.size() <= HISTORY_PREVIEW_LIMIT) {
            tvHistoryToggle.setVisibility(View.GONE);
            if (btnHistoryToggle != null) {
                btnHistoryToggle.setVisibility(View.GONE);
            }
            historyAdapter.setData(fullHistoryList, curGame);
        } else {
            tvHistoryToggle.setVisibility(View.VISIBLE);
            if (btnHistoryToggle != null) {
                btnHistoryToggle.setVisibility(View.VISIBLE);
            }

            if (isHistoryExpanded) {
                tvHistoryToggle.setText(R.string.stat_history_toggle_collapse);
                if (btnHistoryToggle != null) {
                    btnHistoryToggle.setText(R.string.stat_history_collapse);
                    btnHistoryToggle.setIconResource(R.drawable.icon_chevron_up);
                }
                historyAdapter.setData(fullHistoryList, curGame);
            } else {
                tvHistoryToggle.setText(String.format(getString(R.string.stat_history_toggle_expand), fullHistoryList.size()));
                if (btnHistoryToggle != null) {
                    btnHistoryToggle.setText(R.string.stat_history_show_all);
                    btnHistoryToggle.setIconResource(R.drawable.icon_chevron_down);
                }
                List<SubscriptionEntity> preview = fullHistoryList.subList(0, HISTORY_PREVIEW_LIMIT);
                historyAdapter.setData(preview, curGame);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rvSubsHistory != null) {
            rvSubsHistory.setAdapter(null);
            rvSubsHistory = null;
        }
        historyAdapter = null;
        btnHistoryToggle = null;
        tvTotalPullsCount = null;
        tvTotalCurrencySpent = null;
        tvLegendaryStat = null;
        tvLegendarySub = null;
        tvEpicStat = null;
        tvEpicSub = null;
        tvWinratePercent = null;
        tvWinrateCounts = null;
        tvAvgPityValue = null;
        tvPityRangeValue = null;
        bannerDistributionBars = null;
        tvSubsPurchasedCount = null;
        tvSubsAttendanceRate = null;
        rowHistoryHeader = null;
        tvHistoryToggle = null;
        tvHistoryEmpty = null;
        tvSavingsProgressSummary = null;
        savingsSourceBar = null;
        tvSourceManualVal = null;
        tvSourceSubsVal = null;
        tvSourceDailiesVal = null;
        tvTotalTrackedWealth = null;
    }
}
