package ru.menshovanton.gachapoint.ui.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;

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

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.worker.NotificationScheduler;

public class MainActivityView extends AppCompatActivity {

    private MainActivityViewModel viewModel;

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
        void onExport();
        void onRecovery();
        void onCancel();
    }

    public void showCalendarMenu(OnCalendarMenuClickListener listener) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.calendar_more_sheet);

        dialog.findViewById(R.id.ll_add).setOnClickListener(v -> {
            listener.onAdd();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.ll_delete).setOnClickListener(v -> {
            listener.onDel();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.ll_export).setOnClickListener(v -> {
            listener.onExport();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.ll_mark_up_missed_day).setOnClickListener(v -> {
            listener.onRecovery();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.ll_cancel_mark).setOnClickListener(v -> {
            listener.onCancel();
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }

    public interface OnPiggyBankMenuClickListener {
        void onAddOnePity();
        void onAddTwoPity();
        void onAddSixPity();
        void onReset();
    }

    public void showPiggyBankMenu(OnPiggyBankMenuClickListener listener) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.piggy_bank_more_sheet);

        dialog.findViewById(R.id.layoutAddOneGarant).setOnClickListener(v -> {
            listener.onAddOnePity();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.layoutAddTwoGarant).setOnClickListener(v -> {
            listener.onAddTwoPity();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.layoutAddSixGarant).setOnClickListener(v -> {
            listener.onAddSixPity();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.layoutResetGoal).setOnClickListener(v -> {
            listener.onReset();
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }

    public interface OnWishCounterMenuClickListener {
        void onAddOneAttempt();
        void onAddTenAttempts();
        void onAddFiveStarDrop();
        void onAddFourStarDrop();
    }

    public void showWishCounterMenu(OnWishCounterMenuClickListener listener) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.wishes_counter_more_sheet);

        dialog.findViewById(R.id.ll_add_one_attempt).setOnClickListener(v -> {
            listener.onAddOneAttempt();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.ll_add_ten_attempts).setOnClickListener(v -> {
            listener.onAddTenAttempts();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.ll_add_five_star_drop).setOnClickListener(v -> {
            listener.onAddFiveStarDrop();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.ll_add_four_start_drop).setOnClickListener(v -> {
            listener.onAddFourStarDrop();
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM);
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
        }
    }
}