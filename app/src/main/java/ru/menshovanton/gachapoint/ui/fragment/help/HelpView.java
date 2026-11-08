package ru.menshovanton.gachapoint.ui.fragment.help;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;

public class HelpView extends Fragment {

    private HelpViewModel viewModel;
    private ImageButton back;
    private RecyclerView rvHelpSections;

    public HelpView() {}

    public static HelpView newInstance() {
        return new HelpView();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        back = view.findViewById(R.id.btn_back);
        rvHelpSections = view.findViewById(R.id.rv_help_sections);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HelpViewModel.class);

        if (back != null) {
            back.setOnClickListener(v -> viewModel.onBackToMenuClicked());
        }

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        if (rvHelpSections == null) return;

        List<HelpSection> sections = new ArrayList<>();
        sections.add(new HelpSection(R.string.help_feature_pulls_title, R.string.help_feature_pulls_detail, R.drawable.icon_journal));
        sections.add(new HelpSection(R.string.help_mech_pity_title, R.string.help_mech_pity_detail, R.drawable.icon_calculator));
        sections.add(new HelpSection(R.string.help_mech_5050_title, R.string.help_mech_5050_detail, R.drawable.icon_star_rounded));
        sections.add(new HelpSection(R.string.help_mech_weapon_title, R.string.help_mech_weapon_detail, R.drawable.icon_star));
        sections.add(new HelpSection(R.string.help_mech_radiance_title, R.string.help_mech_radiance_detail, R.drawable.icon_star_rounded));
        sections.add(new HelpSection(R.string.help_mech_hi3_title, R.string.help_mech_hi3_detail, R.drawable.icon_star));
        sections.add(new HelpSection(R.string.help_feature_calendar_title, R.string.help_feature_calendar_detail, R.drawable.icon_calendar));
        sections.add(new HelpSection(R.string.help_faq_savings_title, R.string.help_faq_savings_detail, R.drawable.icon_piggy_bank));
        sections.add(new HelpSection(R.string.help_feature_promocodes_title, R.string.help_feature_promocodes_detail, R.drawable.icon_promocode));
        sections.add(new HelpSection(R.string.help_feature_banners_title, R.string.help_feature_banners_detail, R.drawable.icon_notification));
        sections.add(new HelpSection(R.string.help_faq_stats_title, R.string.help_faq_stats_detail, R.drawable.icon_statistics));
        sections.add(new HelpSection(R.string.help_feature_widgets_title, R.string.help_feature_widgets_detail, R.drawable.icon_home));
        sections.add(new HelpSection(R.string.help_feature_backup_title, R.string.help_feature_backup_detail, R.drawable.icon_backup));
        sections.add(new HelpSection(R.string.help_faq_privacy_title, R.string.help_faq_privacy_detail, R.drawable.icon_about));
        sections.add(new HelpSection(R.string.help_faq_sources_title, R.string.help_faq_sources_detail, R.drawable.icon_help));

        rvHelpSections.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHelpSections.setAdapter(new HelpAdapter(sections));
    }

    private void observeViewModel() {
        viewModel.getNavigateToMenuEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rvHelpSections != null) {
            rvHelpSections.setAdapter(null);
            rvHelpSections = null;
        }
        back = null;
    }
}
