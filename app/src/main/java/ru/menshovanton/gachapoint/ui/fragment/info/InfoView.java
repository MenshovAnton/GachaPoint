package ru.menshovanton.gachapoint.ui.fragment.info;

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

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.ui.common.ChromeCustomTabsHelper;

public class InfoView extends Fragment {

    private InfoViewModel viewModel;
    private ImageButton back;

    private View rowGithub;
    private View rowDataRepo;
    private View rowTelegram;
    private View rowDonation;
    private View rowDeveloper;

    public InfoView() {}

    public static InfoView newInstance() {
        return new InfoView();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        back = view.findViewById(R.id.btn_back);
        rowGithub = view.findViewById(R.id.row_github);
        rowDataRepo = view.findViewById(R.id.row_data_repo);
        rowTelegram = view.findViewById(R.id.row_telegram);
        rowDonation = view.findViewById(R.id.row_donation);
        rowDeveloper = view.findViewById(R.id.row_developer);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(InfoViewModel.class);

        back.setOnClickListener(v -> viewModel.onBackToMenuClicked());
        if (rowGithub != null) {
            rowGithub.setOnClickListener(v -> openUrl("https://github.com/MenshovAnton/GachaPoint/"));
        }
        if (rowDataRepo != null) {
            rowDataRepo.setOnClickListener(v -> openUrl("https://github.com/menshovanton/GachaPoint-Data"));
        }
        if (rowTelegram != null) {
            rowTelegram.setOnClickListener(v -> openUrl("https://t.me/GachaPoint_official"));
        }
        if (rowDonation != null) {
            rowDonation.setOnClickListener(v -> viewModel.onDonationClicked());
        }
        if (rowDeveloper != null) {
            rowDeveloper.setOnClickListener(v -> openUrl("https://github.com/MenshovAnton"));
        }

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getNavigateToMenuEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).popBackStack());

        viewModel.getNavigateToDonationEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_info_to_donation));
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;
        ChromeCustomTabsHelper.openUrl(requireContext(), url.trim());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        back = null;
        rowGithub = null;
        rowDataRepo = null;
        rowTelegram = null;
        rowDonation = null;
        rowDeveloper = null;
    }
}
