package ru.menshovanton.gachapoint.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import ru.menshovanton.gachapoint.db.AppDatabase;
import ru.menshovanton.gachapoint.helpers.AlarmHelper;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.utils.Notification;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;
import ru.menshovanton.gachapoint.R;

public class SettingsFragment extends Fragment {

    private TextView hourTextView;
    private TextView minuteTextView;

    private Button dbBackupButton;
    private Button infoButton;

    private SwitchMaterial notificationsSwitch;

    private ImageView edit;

    private MainActivity mainActivity;
    private PreferencesHelper preferencesHelper;

    private final ActivityResultLauncher<String> exportDbLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
                if (uri != null) {
                    writeDatabaseToUri(uri);
                }
            });

    public SettingsFragment() {}

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        preferencesHelper = new PreferencesHelper(Objects.requireNonNull(mainActivity));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        hourTextView = view.findViewById(R.id.hourTextView);
        minuteTextView = view.findViewById(R.id.minutesTextView);
        edit = view.findViewById(R.id.editTime);
        dbBackupButton = view.findViewById(R.id.dbBackupButton);
        infoButton = view.findViewById(R.id.infoButton);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationsSwitch.setOnCheckedChangeListener(this::onCheckedChange);
        notificationsSwitch.setChecked(preferencesHelper.getBooleanPreference(PreferencesHelper.ALLOW_NOTIFICATIONS));

        edit.setOnClickListener(this::showTimePicker);
        dbBackupButton.setOnClickListener(this::exportDatabase);
        infoButton.setOnClickListener(this::onInfoButton);

        updateTimeDisplay();
    }

    private void onCheckedChange(CompoundButton compoundButton, boolean b) {
        preferencesHelper.saveBooleanPreference(PreferencesHelper.ALLOW_NOTIFICATIONS, b);
        Notification.allowNotifications = b;
    }

    public void showTimePicker(View view) {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(AlarmHelper.alarmHour)
                .setMinute(AlarmHelper.alarmMinute)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            int hourOfDay = picker.getHour();
            int minute = picker.getMinute();

            AlarmHelper.alarmHour = hourOfDay;
            AlarmHelper.alarmMinute = minute;

            preferencesHelper.saveIntPreference(PreferencesHelper.ALARM_HOURS, hourOfDay);
            preferencesHelper.saveIntPreference(PreferencesHelper.ALARM_MINUTES, minute);

            updateTimeDisplay();

            Context appContext = requireContext().getApplicationContext();
            AlarmHelper.cancelAlarm(appContext);
            AlarmHelper.setDailyAlarm(appContext);
        });

        picker.show(getParentFragmentManager(), "MATERIAL_TIME_PICKER");
    }

    @SuppressLint("SetTextI18n")
    private void updateTimeDisplay() {
        hourTextView.setText(String.valueOf(AlarmHelper.alarmHour));
        if (AlarmHelper.alarmMinute < 9) {
            minuteTextView.setText("0" + AlarmHelper.alarmMinute);
        } else {
            minuteTextView.setText(String.valueOf(AlarmHelper.alarmMinute));
        }
    }

    private void onInfoButton(View view) {
        mainActivity.replaceFragment(InfoFragment.newInstance(), mainActivity.INFO_TAG);
    }

    public void exportDatabase(View view) {
        exportDbLauncher.launch(DatabaseHelper.DATABASE_NAME);
    }

    private void writeDatabaseToUri(Uri targetUri) {
        if (!isAdded()) return;

        AppDatabase.getInstance(requireContext()).checkpoint();

        File dbFile = requireContext().getDatabasePath(DatabaseHelper.DATABASE_NAME);

        if (!dbFile.exists()) {
            Toast.makeText(requireContext(), R.string.db_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = requireContext().getContentResolver().openOutputStream(targetUri)) {

            if (out == null) {
                Toast.makeText(requireContext(), R.string.db_export_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();

            Toast.makeText(requireContext(), R.string.db_export_successful, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), R.string.db_export_failed, Toast.LENGTH_SHORT).show();
        }
    }
}