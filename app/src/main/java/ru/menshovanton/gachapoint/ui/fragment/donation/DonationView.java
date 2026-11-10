package ru.menshovanton.gachapoint.ui.fragment.donation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.models.SupporterItem;
import ru.menshovanton.gachapoint.domain.models.SupportersData;
import ru.menshovanton.gachapoint.ui.common.ChromeCustomTabsHelper;

public class DonationView extends Fragment {

    private DonationViewModel viewModel;
    private ImageButton btnBack;
    private MaterialButton btnOpenBoosty;
    private View clTier3;
    private View clTier4;
    private View clTier5;

    private View llHallOfFameEmpty;
    private View llTierLegendary;
    private View divTierLegendary;
    private View llTierEpic;
    private View divTierEpic;
    private View llTierSupporter;

    private TextView tvTierLegendaryList;
    private TextView tvTierEpicList;
    private TextView tvTierSupporterList;

    public DonationView() {}

    public static DonationView newInstance() {
        return new DonationView();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_donation, container, false);
        btnBack = view.findViewById(R.id.btn_back);
        btnOpenBoosty = view.findViewById(R.id.btn_open_boosty);
        clTier3 = view.findViewById(R.id.cl_tier_3);
        clTier4 = view.findViewById(R.id.cl_tier_4);
        clTier5 = view.findViewById(R.id.cl_tier_5);

        llHallOfFameEmpty = view.findViewById(R.id.ll_hall_of_fame_empty);
        llTierLegendary = view.findViewById(R.id.ll_tier_legendary);
        divTierLegendary = view.findViewById(R.id.div_tier_legendary);
        llTierEpic = view.findViewById(R.id.ll_tier_epic);
        divTierEpic = view.findViewById(R.id.div_tier_epic);
        llTierSupporter = view.findViewById(R.id.ll_tier_supporter);

        tvTierLegendaryList = view.findViewById(R.id.tv_tier_legendary_list);
        tvTierEpicList = view.findViewById(R.id.tv_tier_epic_list);
        tvTierSupporterList = view.findViewById(R.id.tv_tier_supporter_list);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DonationViewModel.class);

        btnBack.setOnClickListener(v -> viewModel.onBackClicked());

        View.OnClickListener openBoostyListener = v ->
                ChromeCustomTabsHelper.openUrl(requireContext(), getString(R.string.boosty_url));

        if (btnOpenBoosty != null) {
            btnOpenBoosty.setOnClickListener(openBoostyListener);
        }
        if (clTier3 != null) {
            clTier3.setOnClickListener(openBoostyListener);
        }
        if (clTier4 != null) {
            clTier4.setOnClickListener(openBoostyListener);
        }
        if (clTier5 != null) {
            clTier5.setOnClickListener(openBoostyListener);
        }

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getNavigateBackEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).popBackStack());

        viewModel.getSupportersData().observe(getViewLifecycleOwner(), this::renderSupporters);
    }

    private void renderSupporters(@Nullable SupportersData data) {
        if (data == null || data.isEmpty()) {
            if (llHallOfFameEmpty != null) llHallOfFameEmpty.setVisibility(View.VISIBLE);
            if (llTierLegendary != null) llTierLegendary.setVisibility(View.GONE);
            if (divTierLegendary != null) divTierLegendary.setVisibility(View.GONE);
            if (llTierEpic != null) llTierEpic.setVisibility(View.GONE);
            if (divTierEpic != null) divTierEpic.setVisibility(View.GONE);
            if (llTierSupporter != null) llTierSupporter.setVisibility(View.GONE);
            return;
        }

        if (llHallOfFameEmpty != null) llHallOfFameEmpty.setVisibility(View.GONE);

        boolean hasLegendary = false;
        if (llTierLegendary != null && tvTierLegendaryList != null) {
            List<SupporterItem> legendary = data.getLegendary();
            if (legendary.isEmpty()) {
                llTierLegendary.setVisibility(View.GONE);
            } else {
                hasLegendary = true;
                llTierLegendary.setVisibility(View.VISIBLE);
                tvTierLegendaryList.setText(formatSupporters(legendary, true));
            }
        }

        boolean hasEpic = false;
        if (llTierEpic != null && tvTierEpicList != null) {
            List<SupporterItem> epic = data.getEpic();
            if (epic.isEmpty()) {
                llTierEpic.setVisibility(View.GONE);
            } else {
                hasEpic = true;
                llTierEpic.setVisibility(View.VISIBLE);
                tvTierEpicList.setText(formatSupporters(epic, false));
            }
        }

        if (divTierLegendary != null) {
            divTierLegendary.setVisibility(hasLegendary && hasEpic ? View.VISIBLE : View.GONE);
        }

        boolean hasSupporter = false;
        if (llTierSupporter != null && tvTierSupporterList != null) {
            List<SupporterItem> supporter = data.getSupporter();
            if (supporter.isEmpty()) {
                llTierSupporter.setVisibility(View.GONE);
            } else {
                hasSupporter = true;
                llTierSupporter.setVisibility(View.VISIBLE);
                tvTierSupporterList.setText(formatSupporters(supporter, false));
            }
        }

        if (divTierEpic != null) {
            divTierEpic.setVisibility((hasLegendary || hasEpic) && hasSupporter ? View.VISIBLE : View.GONE);
        }
    }

    private String formatSupporters(List<SupporterItem> items, boolean includeComments) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            SupporterItem item = items.get(i);
            sb.append("•  ").append(item.getName());
            if (includeComments && item.getComment() != null && !item.getComment().trim().isEmpty()) {
                sb.append(" — «").append(item.getComment().trim()).append("»");
            }
            if (i < items.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        btnBack = null;
        btnOpenBoosty = null;
        clTier3 = null;
        clTier4 = null;
        clTier5 = null;
        llHallOfFameEmpty = null;
        llTierLegendary = null;
        divTierLegendary = null;
        llTierEpic = null;
        divTierEpic = null;
        llTierSupporter = null;
        tvTierLegendaryList = null;
        tvTierEpicList = null;
        tvTierSupporterList = null;
    }
}
