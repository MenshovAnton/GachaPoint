package ru.menshovanton.gachapoint.ui.fragment.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.AppDatabase;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;

public class SettingsView extends Fragment {

    private TextView hourTextView;
    private TextView minuteTextView;

    private Button dbImportButton;
    private Button dbExportButton;
    private Button infoButton;

    private SwitchMaterial notificationsSwitch;
    private ImageView edit;

    private MainActivityView mainActivityView;
    private SettingsViewModel viewModel;

    private AutoCompleteTextView themeSelector;
    private AutoCompleteTextView languageSelector;

    private SwitchMaterial vibrationSwitch;

    private final ActivityResultLauncher<String> exportDbLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("*/*"), uri -> {
                if (uri != null && viewModel != null) {
                    viewModel.writeDatabaseToUri(uri);
                }
            });

    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    showQuestionDialog(requireContext(), uri);
                }
            }
    );

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
        dbImportButton = view.findViewById(R.id.btn_import_database);
        dbExportButton = view.findViewById(R.id.btn_export_database);
        infoButton = view.findViewById(R.id.btn_about_app);
        themeSelector = view.findViewById(R.id.mac_theme_selector);
        languageSelector = view.findViewById(R.id.mac_lang_selector);
        vibrationSwitch = view.findViewById(R.id.sm_vibro_switch);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupListeners();
        setupThemesSelector();
        setupLanguageSelector();
        observeViewModel();
    }

    private void setupListeners() {
        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.onVibrationModeChanged(isChecked);
            }
        });

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.onNotificationsChanged(isChecked);
            }
        });

        edit.setOnClickListener(v -> showTimePicker());
        dbExportButton.setOnClickListener(v -> viewModel.onExportDatabaseClicked());
        dbImportButton.setOnClickListener(v -> viewModel.onImportDatabaseClicked());
        infoButton.setOnClickListener(v -> viewModel.onInfoButtonClicked());
    }

    private void setupThemesSelector() {
        String[] themes = new String[]{
                getString(R.string.theme_default),
                getString(R.string.theme_day),
                getString(R.string.theme_night)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, themes);
        themeSelector.setAdapter(adapter);

        themeSelector.setOnItemClickListener((parent, view, position, id) -> viewModel.onThemeSelected(position));
    }

    private void setupLanguageSelector() {
        String[] languages = new String[]{
                getString(R.string.lang_default),
                getString(R.string.lang_eng),
                getString(R.string.lang_ru)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, languages);
        languageSelector.setAdapter(adapter);

        languageSelector.setOnItemClickListener((parent, view, position, id) -> {
            languageSelector.clearFocus();
            languageSelector.postDelayed(() -> viewModel.onLanguageSelected(position), 150);
        });
    }

    private void observeViewModel() {
        viewModel.getSelectedTheme().observe(getViewLifecycleOwner(), mode -> {
            if (mode == null) return;

            int index;
            if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
                index = 1;
            } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
                index = 2;
            } else {
                index = 0;
            }

            themeSelector.setText(themeSelector.getAdapter().getItem(index).toString(), false);
        });

        viewModel.getSelectedLanguage().observe(getViewLifecycleOwner(), langCode -> {
            if (langCode == null) return;

            int index;
            switch (langCode) {
                case "en":
                    index = 1;
                    break;
                case "ru":
                    index = 2;
                    break;
                case "sys":
                default:
                    index = 0;
                    break;
            }

            if (languageSelector.getAdapter() != null) {
                languageSelector.setText(languageSelector.getAdapter().getItem(index).toString(), false);
            }
        });

        viewModel.getVibrationMode().observe(getViewLifecycleOwner(), mode -> {
            if (mode != null) {
                vibrationSwitch.setChecked(mode);
            }
        });

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

        viewModel.getNavigateToInfoEvent().observe(getViewLifecycleOwner(), unused ->
                NavHostFragment.findNavController(this).navigate(R.id.action_settings_to_info));

        viewModel.getExportDbEvent().observe(getViewLifecycleOwner(), unused ->
                exportDbLauncher.launch(AppDatabase.DATABASE_NAME));

        viewModel.getImportDbEvent().observe(getViewLifecycleOwner(), unused ->
                filePickerLauncher.launch(new String[]{"*/*"}));

        viewModel.getRestartAppEvent().observe(getViewLifecycleOwner(), unused -> {
            if (mainActivityView != null) {
                mainActivityView.restartApp();
            }
        });

        viewModel.getImportErrorEvent().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && getContext() != null) {
                new MaterialAlertDialogBuilder(requireContext(), R.style.Dialog_GachaPoint_AlertDialog)
                        .setTitle(getString(R.string.db_import_failed))
                        .setMessage(errorMessage)
                        .setPositiveButton(getString(R.string.ok_button), (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });

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

    public void showQuestionDialog(Context context, Uri fileUri) {
        new MaterialAlertDialogBuilder(context, R.style.Dialog_GachaPoint_AlertDialog)
                .setTitle(getString(R.string.db_import))
                .setMessage(R.string.db_import_message)
                .setPositiveButton(getString(R.string.ok_button), (dialog, which) -> {
                    viewModel.importDatabaseFromUri(fileUri);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }
}