package ru.menshovanton.gachapoint.ui.common;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ru.menshovanton.gachapoint.data.local.Preferences;

public final class HapticHelper {

    private HapticHelper() {}

    public static void performClickHaptic(@NonNull Context context, @Nullable View view) {
        Preferences prefs = new Preferences(context);
        boolean vibrationEnabled = prefs.getBooleanPreference(Preferences.VIBRATION_MODE, true);
        if (!vibrationEnabled) {
            return;
        }

        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            return;
        }

        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = manager != null ? manager.getDefaultVibrator() : null;
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception ignored) {}
    }
}
