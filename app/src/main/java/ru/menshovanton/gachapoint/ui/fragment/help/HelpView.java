package ru.menshovanton.gachapoint.ui.fragment.help;

import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import ru.menshovanton.gachapoint.R;

public class HelpView extends Fragment {

    private HelpViewModel viewModel;
    private ImageButton back;
    private ViewGroup containerLayout;

    public HelpView() {}

    public static HelpView newInstance() {
        return new HelpView();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);

        back = view.findViewById(R.id.btn_back);
        containerLayout = view.findViewById(R.id.layout_help_container);

        setupCollapsibleCard(
                view.findViewById(R.id.header_help_pulls),
                view.findViewById(R.id.content_help_pulls),
                view.findViewById(R.id.iv_chevron_pulls)
        );
        setupCollapsibleCard(
                view.findViewById(R.id.header_help_widgets),
                view.findViewById(R.id.content_help_widgets),
                view.findViewById(R.id.iv_chevron_widgets)
        );
        setupCollapsibleCard(
                view.findViewById(R.id.header_help_calendar),
                view.findViewById(R.id.content_help_calendar),
                view.findViewById(R.id.iv_chevron_calendar)
        );
        setupCollapsibleCard(
                view.findViewById(R.id.header_help_backup),
                view.findViewById(R.id.content_help_backup),
                view.findViewById(R.id.iv_chevron_backup)
        );

        setupCollapsibleCard(
                view.findViewById(R.id.header_help_pity),
                view.findViewById(R.id.content_help_pity),
                view.findViewById(R.id.iv_chevron_pity)
        );
        setupCollapsibleCard(
                view.findViewById(R.id.header_help_5050),
                view.findViewById(R.id.content_help_5050),
                view.findViewById(R.id.iv_chevron_5050)
        );
        setupCollapsibleCard(
                view.findViewById(R.id.header_help_weapon),
                view.findViewById(R.id.content_help_weapon),
                view.findViewById(R.id.iv_chevron_weapon)
        );
        setupCollapsibleCard(
                view.findViewById(R.id.header_help_radiance),
                view.findViewById(R.id.content_help_radiance),
                view.findViewById(R.id.iv_chevron_radiance)
        );
        setupCollapsibleCard(
                view.findViewById(R.id.header_help_hi3),
                view.findViewById(R.id.content_help_hi3),
                view.findViewById(R.id.iv_chevron_hi3)
        );

        setupCollapsibleCard(
                view.findViewById(R.id.header_help_savings),
                view.findViewById(R.id.content_help_savings),
                view.findViewById(R.id.iv_chevron_savings)
        );
        setupCollapsibleCard(
                view.findViewById(R.id.header_help_stats),
                view.findViewById(R.id.content_help_stats),
                view.findViewById(R.id.iv_chevron_stats)
        );
        setupCollapsibleCard(
                view.findViewById(R.id.header_help_privacy),
                view.findViewById(R.id.content_help_privacy),
                view.findViewById(R.id.iv_chevron_privacy)
        );

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HelpViewModel.class);

        if (back != null) {
            back.setOnClickListener(v -> viewModel.onBackToMenuClicked());
        }

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getNavigateToMenuEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    private void setupCollapsibleCard(View header, View content, ImageView chevron) {
        if (header == null || content == null) return;

        header.setOnClickListener(v -> {
            boolean isExpanded = content.getVisibility() == View.VISIBLE;

            if (containerLayout != null) {
                AutoTransition transition = new AutoTransition();
                transition.setDuration(200);
                TransitionManager.beginDelayedTransition(containerLayout, transition);
            }

            if (isExpanded) {
                content.setVisibility(View.GONE);
                if (chevron != null) {
                    chevron.animate().rotation(0f).setDuration(200).start();
                }
            } else {
                content.setVisibility(View.VISIBLE);
                if (chevron != null) {
                    chevron.animate().rotation(180f).setDuration(200).start();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        back = null;
        containerLayout = null;
    }
}
