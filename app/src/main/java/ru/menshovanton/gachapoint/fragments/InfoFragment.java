package ru.menshovanton.gachapoint.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.R;

public class InfoFragment extends Fragment {

    ImageView backToSettings;

    public InfoFragment() {}
    public static InfoFragment newInstance() {
        return new InfoFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        backToSettings = view.findViewById(R.id.backToSettings);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        backToSettings.setOnClickListener(this::onBackToSettings);
    }

    private void onBackToSettings(View view) {
        MainActivity.mainActivity.updateFragment(SettingsFragment.newInstance());
    }
}