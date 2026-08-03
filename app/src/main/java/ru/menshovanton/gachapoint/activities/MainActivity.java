package ru.menshovanton.gachapoint.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;

import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationBarView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.menshovanton.gachapoint.fragments.PiggyBankFragment;
import ru.menshovanton.gachapoint.helpers.AlarmHelper;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.fragments.HomeFragment;
import ru.menshovanton.gachapoint.fragments.JournalFragment;
import ru.menshovanton.gachapoint.fragments.SettingsFragment;
import ru.menshovanton.gachapoint.fragments.TrackerFragment;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    public static Context context;
    @SuppressLint("StaticFieldLeak")
    public static MainActivity mainActivity;

    public static int subType;

    private static final int REQUEST_CODE = 123;

    public static int subTypeScrollX;

    DatabaseHelper dbHelper;

    private final NavigationBarView.OnItemSelectedListener onItemSelectedListener
            = new NavigationBarView.OnItemSelectedListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.nav_home) {
                loadFragment(HomeFragment.newInstance());
                return true;
            }
            else if (item.getItemId() == R.id.nav_tracker) {
                loadFragment(TrackerFragment.newInstance());
                return true;
            }
            else if (item.getItemId() == R.id.nav_journal) {
                loadFragment(JournalFragment.newInstance());
                return true;
            }
            else if (item.getItemId() == R.id.nav_settings) {
                loadFragment(SettingsFragment.newInstance());
                return true;
            } else {
                return false;
            }
        }
    };

    private void loadFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();
    }

    @SuppressLint({"ShortAlarm", "ScheduleExactAlarm"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NavigationBarView navigation = findViewById(R.id.bottomNavigationView);
        navigation.setOnItemSelectedListener(onItemSelectedListener);

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestPermissions();
        }

        loadFragment(TrackerFragment.newInstance());
        loadFragment(HomeFragment.newInstance());

        context = this;
        mainActivity = MainActivity.this;

        startService(new Intent(this, AlarmHelper.class));
    }

    public void updateFragment(Fragment fragment) {
        loadFragment(fragment);
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

    private boolean isDatabaseExists(Context context) {
        File dbFile = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);
        return dbFile.exists();
    }

    public static void showMessage(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    private WeakReference<Dialog> calendarMenuRef;

    @SuppressLint("SetTextI18n")
    public void showCalendarMenu() {
        final Dialog dialog = new Dialog(this);
        calendarMenuRef = new WeakReference<>(dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.calendar_more_sheet);

        TrackerFragment trackerFragment = TrackerFragment.instance;

        LinearLayout layoutAddSub = dialog.findViewById(R.id.layoutAddSub);
        layoutAddSub.setOnClickListener(trackerFragment::onAddClick);

        LinearLayout layoutDelSub = dialog.findViewById(R.id.layoutDelSub);
        layoutDelSub.setOnClickListener(trackerFragment::onDelClick);

        LinearLayout layoutCreateBackup = dialog.findViewById(R.id.layoutCreateBackup);
        layoutCreateBackup.setOnClickListener(trackerFragment::createDataBaseBackup);

        LinearLayout layoutEditStatus = dialog.findViewById(R.id.layoutEditStatus);
        layoutEditStatus.setOnClickListener(trackerFragment::recoveryMissDay);

        LinearLayout layoutCancelCheck = dialog.findViewById(R.id.layoutCancelCheck);
        layoutCancelCheck.setOnClickListener(trackerFragment::onCancelCheck);

        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    public void closeCalendarMenu() {
        Dialog dialog = calendarMenuRef != null ? calendarMenuRef.get() : null;
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            calendarMenuRef.clear();
        }
    }

    private WeakReference<Dialog> piggyBankMenuRef;

    @SuppressLint("SetTextI18n")
    public void showPiggyBankMenu() {
        final Dialog dialog = new Dialog(this);
        piggyBankMenuRef = new WeakReference<>(dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.piggy_bank_more_sheet);

        PiggyBankFragment piggyBankFragment = PiggyBankFragment.instance;

        LinearLayout layoutAddOne = dialog.findViewById(R.id.layoutAddOneGarant);
        layoutAddOne.setOnClickListener(piggyBankFragment::addTargetOne);

        LinearLayout layoutAddTwo = dialog.findViewById(R.id.layoutAddTwoGarant);
        layoutAddTwo.setOnClickListener(piggyBankFragment::addTargetTwo);

        LinearLayout layoutAddSix = dialog.findViewById(R.id.layoutAddSixGarant);
        layoutAddSix.setOnClickListener(piggyBankFragment::addTargetSix);

        LinearLayout layoutReset = dialog.findViewById(R.id.layoutResetGoal);
        layoutReset.setOnClickListener(piggyBankFragment::resetTarget);

        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    public void closePiggyBankMenu() {
        Dialog dialog = piggyBankMenuRef != null ? piggyBankMenuRef.get() : null;
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            piggyBankMenuRef.clear();
        }
    }
}