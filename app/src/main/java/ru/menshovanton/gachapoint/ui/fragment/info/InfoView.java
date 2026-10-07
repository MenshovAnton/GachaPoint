package ru.menshovanton.gachapoint.ui.fragment.info;

import android.content.Intent;
import android.net.Uri;
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

public class InfoView extends Fragment {

    private InfoViewModel viewModel;
    private ImageButton back;

    private ImageButton githubLink;
    private ImageButton telegramLink;

    public InfoView() {}

    public static InfoView newInstance() {
        return new InfoView();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        back = view.findViewById(R.id.btn_back);
        githubLink = view.findViewById(R.id.btn_github);
        telegramLink = view.findViewById(R.id.btn_telegram);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(InfoViewModel.class);

        back.setOnClickListener(v -> viewModel.onBackToMenuClicked());
        githubLink.setOnClickListener(v -> openUrl("https://github.com/MenshovAnton/GachaPoint/"));
        telegramLink.setOnClickListener(v -> openUrl("https://t.me/GachaPoint_official"));

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getNavigateToMenuEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) return;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}