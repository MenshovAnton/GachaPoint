package ru.menshovanton.gachapoint.fragments;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;

import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Objects;

import ru.menshovanton.gachapoint.helpers.AlarmHelper;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.Notification;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;
import ru.menshovanton.gachapoint.R;

public class SettingsFragment extends Fragment {

    TextView hourTextView;
    TextView minuteTextView;

    Button dbBackupButton;
    Button infoButton;

    SwitchMaterial notificationsSwitch;

    ImageView edit;

    MainActivity mainActivity;
    PreferencesHelper preferencesHelper;
    DatabaseHelper dbHelper;

    public SettingsFragment() {}

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        preferencesHelper = new PreferencesHelper(Objects.requireNonNull(mainActivity));
        dbHelper = new DatabaseHelper(mainActivity, mainActivity);
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
        dbBackupButton.setOnClickListener(dbHelper::createExport);
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

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view1, hourOfDay, minute) -> {
                    AlarmHelper.alarmHour = hourOfDay;
                    AlarmHelper.alarmMinute = minute;

                    preferencesHelper.saveIntPreference(PreferencesHelper.ALARM_HOURS, hourOfDay);
                    preferencesHelper.saveIntPreference(PreferencesHelper.ALARM_MINUTES, minute);

                    updateTimeDisplay();

                    Context appContext = requireContext().getApplicationContext();
                    AlarmHelper.cancelAlarm(appContext);
                    AlarmHelper.setDailyAlarm(appContext);
                },
                AlarmHelper.alarmHour,
                AlarmHelper.alarmMinute,
                true
        );
        dialog.show();
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateTimeDisplay() {
        hourTextView.setText(String.valueOf(AlarmHelper.alarmHour));
        if (AlarmHelper.alarmMinute < 9) {
            minuteTextView.setText("0" + AlarmHelper.alarmMinute);
        } else {
            minuteTextView.setText(String.valueOf(AlarmHelper.alarmMinute));
        }
    }

    private void onInfoButton(View view) {
        mainActivity.updateFragment(InfoFragment.newInstance(), mainActivity.INFO_TAG);
    }
}