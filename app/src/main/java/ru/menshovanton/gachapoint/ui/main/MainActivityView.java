package ru.menshovanton.gachapoint.ui.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.worker.NotificationScheduler;

public class MainActivityView extends AppCompatActivity {

    public interface OnBottomNavReselectedListener {
        void onBottomNavReselected(int itemId);
    }

    private MainActivityViewModel viewModel;
    private OnBottomNavReselectedListener bottomNavReselectedListener;

    public void setOnBottomNavReselectedListener(OnBottomNavReselectedListener listener) {
        this.bottomNavReselectedListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        Preferences preferences = new Preferences(this);
        int themeMode = preferences.getIntPreference(Preferences.APP_THEME);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        String langCode = preferences.getStringPreference(Preferences.APP_LANGUAGE, "sys");

        if (langCode.equals("sys")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode));
        }

        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cl_main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView navigation = findViewById(R.id.bnv_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            navigation.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        v.getPaddingBottom()
                );
                return insets;
            });
        }

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(navigation, navController);
            navigation.setOnItemReselectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_menu) {
                    navController.popBackStack(R.id.moreView, false);
                } else if (itemId == R.id.nav_home) {
                    navController.popBackStack(R.id.homeView, false);
                } else {
                    navController.popBackStack(itemId, false);
                }
                if (bottomNavReselectedListener != null) {
                    bottomNavReselectedListener.onBottomNavReselected(itemId);
                }
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestPermissions();
        }

        NotificationScheduler.scheduleDailyNotification(this);
    }

    @Override
    public void recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        } else {
            overridePendingTransition(0, 0);
        }
        super.recreate();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                    Manifest.permission.POST_NOTIFICATIONS
            };

            List<String> permissionsToRequest = new ArrayList<>();

            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                int REQUEST_CODE = 123;
                ActivityCompat.requestPermissions(
                        this,
                        permissionsToRequest.toArray(new String[0]),
                        REQUEST_CODE
                );
            }
        }
    }

    public interface OnCalendarMenuClickListener {
        void onAdd();
        void onDel();
        void onRecovery();
        void onCancel();
        void onMultiSelect();
    }

    public void showCalendarMenu(OnCalendarMenuClickListener listener) {
        final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        dialog.setContentView(R.layout.calendar_more_sheet);

        setDialogClickListener(dialog, R.id.ll_multi_select, listener::onMultiSelect);
        setDialogClickListener(dialog, R.id.ll_add, listener::onAdd);
        setDialogClickListener(dialog, R.id.ll_delete, listener::onDel);
        setDialogClickListener(dialog, R.id.ll_mark_up_missed_day, listener::onRecovery);
        setDialogClickListener(dialog, R.id.ll_cancel_mark, listener::onCancel);

        dialog.show();
    }

    public interface OnPiggyBankMenuClickListener {
        void onAddOnePity();
        void onAddTwoPity();
        void onAddSixPity();
        void onReset();
    }

    public void showPiggyBankMenu(OnPiggyBankMenuClickListener listener) {
        final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        dialog.setContentView(R.layout.piggy_bank_more_sheet);

        setDialogClickListener(dialog, R.id.layoutAddOneGarant, listener::onAddOnePity);
        setDialogClickListener(dialog, R.id.layoutAddTwoGarant, listener::onAddTwoPity);
        setDialogClickListener(dialog, R.id.layoutAddSixGarant, listener::onAddSixPity);
        setDialogClickListener(dialog, R.id.layoutResetGoal, listener::onReset);

        dialog.show();
    }

    public interface OnWishCounterMenuClickListener {
        void onAddOneAttempt();
        void onAddTenAttempts();
        void onAddFiveStarDrop();
        void onAddFourStarDrop();
    }

    public void showWishCounterMenu(OnWishCounterMenuClickListener listener) {
        final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        dialog.setContentView(R.layout.wishes_counter_more_sheet);

        setDialogClickListener(dialog, R.id.ll_add_one_attempt, listener::onAddOneAttempt);
        setDialogClickListener(dialog, R.id.ll_add_ten_attempts, listener::onAddTenAttempts);
        setDialogClickListener(dialog, R.id.ll_add_five_star_drop, listener::onAddFiveStarDrop);
        setDialogClickListener(dialog, R.id.ll_add_four_start_drop, listener::onAddFourStarDrop);

        dialog.show();
    }

    private void setDialogClickListener(Dialog dialog, int viewId, Runnable action) {
        View view = dialog.findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> {
                action.run();
                dialog.dismiss();
            });
        }
    }

    public MainActivityViewModel getMainActivityViewModel() {
        return viewModel;
    }

    public List<GameType> getEnabledGames() {
        return viewModel != null ? viewModel.getEnabledGames() : Collections.singletonList(GameType.GENSHIN);
    }

    public void refreshGamesConfig() {
        if (viewModel != null) {
            viewModel.refreshEnabledGames();
        }
    }

    public GameType getSubType() {
        return viewModel.getSubType();
    }

    public void setSubType(GameType value) {
        viewModel.setSubType(value);
    }

    public void setSubType(int code) {
        viewModel.setSubType(code);
    }

    public void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            this.finish();
            Runtime.getRuntime().exit(0);
        }
    }
}
