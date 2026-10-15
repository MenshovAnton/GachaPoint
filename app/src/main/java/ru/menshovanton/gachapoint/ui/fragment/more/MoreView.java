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

    private View infoButton;
    private View settingsButton;
    private View feedbackButton;

    private View rowStatistics;
    private View rowCalculator;
    private View rowPromocodes;

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

        infoButton = view.findViewById(R.id.mcv_item_about);
        settingsButton = view.findViewById(R.id.mcv_item_settings);
        feedbackButton = view.findViewById(R.id.mcv_item_feedback);

        View rowSavings = view.findViewById(R.id.row_savings);
        rowStatistics = view.findViewById(R.id.row_statistics);
        rowCalculator = view.findViewById(R.id.row_calculator);
        rowPromocodes = view.findViewById(R.id.row_promocodes);

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
    }

    private void setupListeners() {
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

        View.OnClickListener comingSoonListener = v ->
                Toast.makeText(requireContext(), R.string.menu_feature_in_dev, Toast.LENGTH_SHORT).show();

        if (rowCalculator != null) rowCalculator.setOnClickListener(comingSoonListener);
        if (rowPromocodes != null) rowPromocodes.setOnClickListener(comingSoonListener);
    }
}