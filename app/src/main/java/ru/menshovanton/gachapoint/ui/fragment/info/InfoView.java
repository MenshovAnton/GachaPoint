package ru.menshovanton.gachapoint.ui.fragment.info;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.ui.fragment.settings.SettingsView;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;

public class InfoView extends Fragment {

    private MainActivityView mainActivityView;
    private InfoViewModel viewModel;
    private ImageView backToSettings;

    public InfoView() {}

    public static InfoView newInstance() {
        return new InfoView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityView = (MainActivityView) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        backToSettings = view.findViewById(R.id.backToSettings);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(InfoViewModel.class);

        backToSettings.setOnClickListener(v -> viewModel.onBackToSettingsClicked());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getNavigateToSettingsEvent().observe(getViewLifecycleOwner(), unused -> {
            if (mainActivityView != null) {
                mainActivityView.replaceFragment(SettingsView.newInstance(), MainActivityView.SETTINGS_TAG);
            }
        });
    }
}