package ru.menshovanton.gachapoint.ui.fragment.more;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

import ru.menshovanton.gachapoint.R;

public class MoreView extends Fragment {

    private MoreViewModel viewModel;

    private View helpButton;
    private View infoButton;
    private View settingsButton;
    private View feedbackButton;

    private View rowStatistics;
    private View rowCalculator;
    private View rowPromocodes;
    private View rowBanners;
    private View badgeNewPromocodes;
    private View badgeNewBanners;

    public MoreView() {}

    public static MoreView newInstance() {
        return new MoreView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        helpButton = view.findViewById(R.id.mcv_item_help);
        infoButton = view.findViewById(R.id.mcv_item_about);
        settingsButton = view.findViewById(R.id.mcv_item_settings);
        feedbackButton = view.findViewById(R.id.mcv_item_feedback);

        View rowSavings = view.findViewById(R.id.row_savings);
        rowStatistics = view.findViewById(R.id.row_statistics);
        rowCalculator = view.findViewById(R.id.row_calculator);
        rowPromocodes = view.findViewById(R.id.row_promocodes);
        rowBanners = view.findViewById(R.id.row_banners);
        badgeNewPromocodes = view.findViewById(R.id.badge_new_promocodes);
        badgeNewBanners = view.findViewById(R.id.badge_new_banners);

        if (rowSavings != null) {
            rowSavings.setOnClickListener(v -> viewModel.onSavingsButtonClicked());
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MoreViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getNavigateToHelpEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_help));

        viewModel.getNavigateToPiggyBankEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_piggy_bank));

        viewModel.getNavigateToInfoEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_info));

        viewModel.getNavigateToSettingsEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_settings));

        viewModel.getNavigateToFeedbackEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_feedback));

        viewModel.getNavigateToStatisticsEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_statistics));

        viewModel.getNavigateToPromocodesEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_promocodes));

        viewModel.getNavigateToBannersEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_banners));
    }

    private void setupListeners() {
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> viewModel.onHelpButtonClicked());
        }
        if (infoButton != null) {
            infoButton.setOnClickListener(v -> viewModel.onInfoButtonClicked());
        }
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> viewModel.onSettingsButtonClicked());
        }
        if (feedbackButton != null) {
            feedbackButton.setOnClickListener(v -> viewModel.onFeedbackButtonClicked());
        }

        if (rowStatistics != null) {
            rowStatistics.setOnClickListener(v -> viewModel.onStatisticsButtonClicked());
        }

        if (rowPromocodes != null) {
            rowPromocodes.setOnClickListener(v -> viewModel.onPromocodesButtonClicked());
        }

        if (rowBanners != null) {
            rowBanners.setOnClickListener(v -> viewModel.onBannersButtonClicked());
        }

        View.OnClickListener comingSoonListener = v ->
                Toast.makeText(requireContext(), R.string.menu_feature_in_dev, Toast.LENGTH_SHORT).show();

        if (rowCalculator != null) rowCalculator.setOnClickListener(comingSoonListener);
        ru.menshovanton.gachapoint.data.local.JsonDataLoader.addDataUpdatedListener(dataUpdatedListener);
    }

    private final ru.menshovanton.gachapoint.data.local.JsonDataLoader.OnDataUpdatedListener dataUpdatedListener = cacheFileName -> {
        if (ru.menshovanton.gachapoint.data.local.DataSyncConfig.CACHE_PROMOCODES.equals(cacheFileName)) {
            updatePromocodesBadge();
        } else if (ru.menshovanton.gachapoint.data.local.DataSyncConfig.CACHE_BANNERS.equals(cacheFileName)) {
            updateBannersBadge();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        updatePromocodesBadge();
        updateBannersBadge();
    }

    private void updatePromocodesBadge() {
        if (getContext() == null || badgeNewPromocodes == null) return;
        ru.menshovanton.gachapoint.data.db.AppDatabase.getExecutor().execute(() -> {
            ru.menshovanton.gachapoint.data.repository.PromocodeRepository repo =
                    ru.menshovanton.gachapoint.data.repository.PromocodeRepository.getInstance(requireContext());
            int unviewed = repo.getUnviewedPromoCodesCount();
            ru.menshovanton.gachapoint.data.db.AppDatabase.postToMain(() -> {
                if (badgeNewPromocodes != null) {
                    badgeNewPromocodes.setVisibility(unviewed > 0 ? View.VISIBLE : View.GONE);
                }
            });
        });
    }

    private void updateBannersBadge() {
        if (getContext() == null || badgeNewBanners == null) return;
        ru.menshovanton.gachapoint.data.db.AppDatabase.getExecutor().execute(() -> {
            ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository repo =
                    ru.menshovanton.gachapoint.data.repository.BannerScheduleRepository.getInstance(requireContext());
            int unviewed = repo.getUnviewedBannersCount();
            ru.menshovanton.gachapoint.data.db.AppDatabase.postToMain(() -> {
                if (badgeNewBanners != null) {
                    badgeNewBanners.setVisibility(unviewed > 0 ? View.VISIBLE : View.GONE);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        ru.menshovanton.gachapoint.data.local.JsonDataLoader.removeDataUpdatedListener(dataUpdatedListener);
        super.onDestroyView();
        helpButton = null;
        infoButton = null;
        settingsButton = null;
        feedbackButton = null;
        rowStatistics = null;
        rowCalculator = null;
        rowPromocodes = null;
        rowBanners = null;
        badgeNewPromocodes = null;
        badgeNewBanners = null;
    }
}
