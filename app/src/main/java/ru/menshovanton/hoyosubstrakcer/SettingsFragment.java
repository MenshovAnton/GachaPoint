package ru.menshovanton.hoyosubstrakcer;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsFragment extends Fragment {

    TextView hourTextView;
    TextView minuteTextView;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch notificationsSwitch;

    ImageView edit;

    MainActivity mainActivity = MainActivity.mainActivity;

    public SettingsFragment() {}

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        hourTextView = view.findViewById(R.id.hourTextView);
        minuteTextView = view.findViewById(R.id.minutesTextView);
        edit = view.findViewById(R.id.editTime);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationsSwitch.setOnCheckedChangeListener(this::onCheckedChange);
        notificationsSwitch.setChecked(mainActivity.getBooleanPreference(MainActivity.PREF_NOTIFICATIONS));

        edit.setOnClickListener(this::showTimePicker);

        updateTimeDisplay();
    }

    private void onCheckedChange(CompoundButton compoundButton, boolean b) {
        mainActivity.saveBooleanPreference(MainActivity.PREF_NOTIFICATIONS, b);
        Notification.allowNotifications = b;
    }

    public void showTimePicker(View view) {
        TimePickerDialog dialog = new TimePickerDialog(
                MainActivity.context,
                (view1, hourOfDay, minute) -> {
                    AlarmHelper.alarmHour = hourOfDay;
                    AlarmHelper.alarmMinute = minute;

                    mainActivity.saveIntPreference(MainActivity.PREF_ALARM_HOURS, hourOfDay);
                    mainActivity.saveIntPreference(MainActivity.PREF_ALARM_MINUTES, minute);

                    updateTimeDisplay();

                    AlarmHelper.cancelAlarm(MainActivity.context);
                    AlarmHelper.setDailyAlarm(MainActivity.context);
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
}