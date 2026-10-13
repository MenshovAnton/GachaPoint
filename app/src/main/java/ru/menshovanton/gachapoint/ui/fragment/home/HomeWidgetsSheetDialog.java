package ru.menshovanton.gachapoint.ui.fragment.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.ui.fragment.home.model.HomeWidgetItem;

public class HomeWidgetsSheetDialog {

    public static void show(@NonNull Context context, @Nullable Runnable onSaveOrDismiss) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        @SuppressLint("InflateParams") View sheetView = LayoutInflater.from(context).inflate(R.layout.home_widgets_sheet, null);
        dialog.setContentView(sheetView);

        Preferences preferences = new Preferences(context);
        RecyclerView rv = sheetView.findViewById(R.id.rv_widgets_reorder);
        rv.setLayoutManager(new LinearLayoutManager(context));

        String configStr = preferences.getHomeWidgetsConfig();
        List<HomeWidgetItem> currentItems = HomeWidgetItem.parseConfig(configStr);

        final List<HomeWidgetItem> workingList = new ArrayList<>(currentItems.size());
        for (HomeWidgetItem item : currentItems) {
            workingList.add(new HomeWidgetItem(item.type, item.isVisible));
        }

        final ItemTouchHelper[] touchHelperRef = new ItemTouchHelper[1];

        HomeWidgetsReorderAdapter adapter = new HomeWidgetsReorderAdapter(
                workingList,
                viewHolder -> {
                    if (touchHelperRef[0] != null) {
                        touchHelperRef[0].startDrag(viewHolder);
                    }
                },
                updatedList -> {
                    workingList.clear();
                    workingList.addAll(updatedList);
                }
        );

        ItemTouchHelper touchHelper = getTouchHelper(adapter);
        touchHelper.attachToRecyclerView(rv);
        touchHelperRef[0] = touchHelper;

        rv.setAdapter(adapter);

        sheetView.findViewById(R.id.btn_reset_widgets).setOnClickListener(v -> {
            triggerVibration(context, preferences);
            preferences.saveHomeWidgetsConfig(Preferences.DEFAULT_HOME_WIDGETS);
            List<HomeWidgetItem> resetList = HomeWidgetItem.parseConfig(Preferences.DEFAULT_HOME_WIDGETS);
            workingList.clear();
            for (HomeWidgetItem item : resetList) {
                workingList.add(new HomeWidgetItem(item.type, item.isVisible));
            }
            adapter.setItems(workingList);
            if (onSaveOrDismiss != null) {
                onSaveOrDismiss.run();
            }
        });

        sheetView.findViewById(R.id.btn_done_widgets).setOnClickListener(v -> {
            triggerVibration(context, preferences);
            String savedConfig = HomeWidgetItem.toConfigString(adapter.getItems());
            preferences.saveHomeWidgetsConfig(savedConfig);
            if (onSaveOrDismiss != null) {
                onSaveOrDismiss.run();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @NonNull
    private static ItemTouchHelper getTouchHelper(HomeWidgetsReorderAdapter adapter) {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                 @NonNull RecyclerView.ViewHolder viewHolder,
                                 @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                adapter.onItemMove(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        };

        return new ItemTouchHelper(callback);
    }

    private static void triggerVibration(Context context, Preferences preferences) {
        if (!preferences.getBooleanPreference(Preferences.VIBRATION_MODE)) return;

        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
