package ru.menshovanton.gachapoint.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;

import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationBarView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.helpers.AlarmHelper;
import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.fragments.HomeFragment;
import ru.menshovanton.gachapoint.fragments.JournalFragment;
import ru.menshovanton.gachapoint.fragments.SettingsFragment;
import ru.menshovanton.gachapoint.fragments.TrackerFragment;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;

public class MainActivity extends AppCompatActivity {
    public static DatabaseHelper dbHelper;

    public static int subType;

    private static final int REQUEST_CODE = 123;

    public final String HOME_TAG = "HOME";
    public final String INFO_TAG = "INFO";
    public final String JOURNAL_TAG = "JOURNAL";
    public final String SETTINGS_TAG = "SETTINGS";
    public final String TRACKER_TAG = "TRACKER";

    public static int subTypeScrollX;

    private static final String KEY_SELECTED_NAV_ID = "selected_nav_id";

    private int currentNavId = R.id.nav_home;

    private final NavigationBarView.OnItemSelectedListener onItemSelectedListener
            = item -> {
                if (item.getItemId() == R.id.nav_home) {
                    replaceFragment(HomeFragment.newInstance(), HOME_TAG);
                    return true;
                }
                else if (item.getItemId() == R.id.nav_tracker) {
                    replaceFragment(TrackerFragment.newInstance(), TRACKER_TAG);
                    return true;
                }
                else if (item.getItemId() == R.id.nav_journal) {
                    replaceFragment(JournalFragment.newInstance(), JOURNAL_TAG);
                    return true;
                }
                else if (item.getItemId() == R.id.nav_settings) {
                    replaceFragment(SettingsFragment.newInstance(), SETTINGS_TAG);
                    return true;
                } else {
                    return false;
                }
            };

    private void replaceFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.frameLayout, fragment, tag)
                .commit();
    }

    private void replaceFragmentWithoutAnimation(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, fragment, tag)
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NavigationBarView navigation = findViewById(R.id.bottomNavigationView);

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

        if (savedInstanceState != null) {
            currentNavId = savedInstanceState.getInt(KEY_SELECTED_NAV_ID, R.id.nav_home);
            navigation.setSelectedItemId(currentNavId);
        } else {
            replaceFragmentWithoutAnimation(TrackerFragment.newInstance(), TRACKER_TAG);
            replaceFragmentWithoutAnimation(HomeFragment.newInstance(), HOME_TAG);
        }

        navigation.setOnItemSelectedListener(onItemSelectedListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestPermissions();
        }

        dbHelper = new DatabaseHelper(getApplicationContext(), null);

        if (!isDatabaseExists(this)) {
            dbHelper.getWritableDatabase();
        }

        startService(new Intent(this, AlarmHelper.class));
    }

    private boolean isDatabaseExists(Context context) {
        File dbFile = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);
        return dbFile.exists();
    }

    public void updateFragment(Fragment fragment, String tag) {
        replaceFragment(fragment, tag);
    }

    public void updateFragmentWithoutAnimation(Fragment fragment, String tag) {
        replaceFragmentWithoutAnimation(fragment, tag);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void checkAndRequestPermissions() {
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
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CODE
            );
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_NAV_ID, currentNavId);
    }

    public static void showMessage(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
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

        dialog.findViewById(R.id.layoutAddSub).setOnClickListener(v -> {
            listener.onAdd();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.layoutDelSub).setOnClickListener(v -> {
            listener.onDel();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.layoutCreateBackup).setOnClickListener(v -> {
            listener.onExport();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.layoutEditStatus).setOnClickListener(v -> {
            listener.onRecovery();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.layoutCancelCheck).setOnClickListener(v -> {
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

    public interface OnPiggyMenuClickListener {
        void onAddOne();
        void onAddTwo();
        void onAddSix();
        void onReset();
    }

    public void showPiggyBankMenu(OnPiggyMenuClickListener listener) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.piggy_bank_more_sheet);

        dialog.findViewById(R.id.layoutAddOneGarant).setOnClickListener(v -> {
            listener.onAddOne();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.layoutAddTwoGarant).setOnClickListener(v -> {
            listener.onAddTwo();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.layoutAddSixGarant).setOnClickListener(v -> {
            listener.onAddSix();
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
}