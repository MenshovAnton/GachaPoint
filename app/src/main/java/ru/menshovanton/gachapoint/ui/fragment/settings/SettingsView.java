package ru.menshovanton.gachapoint.ui.fragment.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.repository.DatabaseRepository;
import ru.menshovanton.gachapoint.ui.fragment.info.InfoView;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;

public class SettingsView extends Fragment {

    private TextView hourTextView;
    private TextView minuteTextView;

    private Button dbBackupButton;
    private Button infoButton;

    private SwitchMaterial notificationsSwitch;
    private ImageView edit;

    private MainActivityView mainActivityView;
    private SettingsViewModel viewModel;

    private final ActivityResultLauncher<String> exportDbLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
                if (uri != null && viewModel != null) {
                    viewModel.writeDatabaseToUri(uri);
                }
            });

    public SettingsView() {}

    public static SettingsView newInstance() {
        return new SettingsView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityView = (MainActivityView) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        notificationsSwitch = view.findViewById(R.id.sm_notification_switch);
        hourTextView = view.findViewById(R.id.tv_hour);
        minuteTextView = view.findViewById(R.id.tv_minutes);
        edit = view.findViewById(R.id.btn_select_time);
        dbBackupButton = view.findViewById(R.id.btn_export_database);
        infoButton = view.findViewById(R.id.btn_about_app);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.onNotificationsChanged(isChecked);
            }
        });

        edit.setOnClickListener(v -> showTimePicker());
        dbBackupButton.setOnClickListener(v -> viewModel.onExportDatabaseClicked());
        infoButton.setOnClickListener(v -> viewModel.onInfoButtonClicked());
    }

    private void observeViewModel() {
        viewModel.getNotificationsEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (enabled != null) {
                notificationsSwitch.setChecked(enabled);
            }
        });

        viewModel.getAlarmHour().observe(getViewLifecycleOwner(), hour -> {
            if (hour != null) {
                hourTextView.setText(String.valueOf(hour));
            }
        });

        viewModel.getAlarmMinute().observe(getViewLifecycleOwner(), minute -> {
            if (minute != null) {
                minuteTextView.setText(minute < 10 ? "0" + minute : String.valueOf(minute));
            }
        });

        viewModel.getNavigateToInfoEvent().observe(getViewLifecycleOwner(), unused -> {
            if (mainActivityView != null) {
                mainActivityView.replaceFragment(InfoView.newInstance(), MainActivityView.INFO_TAG);
            }
        });

        viewModel.getExportDbEvent().observe(getViewLifecycleOwner(), unused ->
                exportDbLauncher.launch(DatabaseRepository.DATABASE_NAME));

        viewModel.getToastMessageEvent().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null && getContext() != null) {
                Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void showTimePicker() {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }

        Integer currentHour = viewModel.getAlarmHour().getValue();
        Integer currentMinute = viewModel.getAlarmMinute().getValue();

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(currentHour != null ? currentHour : 12)
                .setMinute(currentMinute != null ? currentMinute : 0)
                .build();

        picker.addOnPositiveButtonClickListener(v ->
                viewModel.onTimeSelected(picker.getHour(), picker.getMinute()));

        picker.show(getParentFragmentManager(), "MATERIAL_TIME_PICKER");
    }
}