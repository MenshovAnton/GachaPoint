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

import com.google.android.material.card.MaterialCardView;

import ru.menshovanton.gachapoint.R;

public class MoreView extends Fragment {

    private MoreViewModel viewModel;

    private MaterialCardView infoButton;
    private MaterialCardView settingsButton;
    private MaterialCardView feedbackButton;


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
        viewModel.getNavigateToInfoEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_info));

        viewModel.getNavigateToSettingsEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_settings));

        viewModel.getNavigateToFeedbackEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_to_feedback));
    }

    private void setupListeners() {
        infoButton.setOnClickListener(v -> viewModel.onInfoButtonClicked());
        settingsButton.setOnClickListener(v -> viewModel.onSettingsButtonClicked());
        feedbackButton.setOnClickListener(v -> viewModel.onFeedbackButtonClicked());
    }
}