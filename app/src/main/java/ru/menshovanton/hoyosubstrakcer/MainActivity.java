package ru.menshovanton.hoyosubstrakcer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    public static Context context;
    @SuppressLint("StaticFieldLeak")
    public static MainActivity mainActivity;
    public static int subType;

    private static final int REQUEST_CODE = 123;

    public static DatabaseHelper dbHelper;

    private static final String PREF_FILE = "Settings";
    public static final String PREF_ALARM_HOURS = "Alarm Hours";
    public static final String PREF_ALARM_MINUTES = "Alarm Minutes";
    public static final String PREF_NOTIFICATIONS = "Enable notifications";
    public static final String PREF_CALENDAR_SIZE = "Calendar size";
    SharedPreferences settings;

    private final NavigationBarView.OnItemSelectedListener onItemSelectedListener
            = new NavigationBarView.OnItemSelectedListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.nav_home) {
                loadFragment(HomeFragment.newInstance());
                return true;
            }
            else if (item.getItemId() == R.id.nav_settings) {
                loadFragment(SettingsFragment.newInstance());
                return true;
            }
            else if (item.getItemId() == R.id.nav_info) {
                loadFragment(InfoFragment.newInstance());
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

        dbHelper = new DatabaseHelper(getApplicationContext());

        if (!isDatabaseExists(this)) {
            dbHelper.getWritableDatabase();
        }

        loadFragment(HomeFragment.newInstance());

        context = this;
        mainActivity = MainActivity.this;

        settings = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

        startService(new Intent(this, AlarmHelper.class));
    }

    public void updateLayout(Fragment fragment) {
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

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: При выходе на передний план спустя день не обновляется календарь

        HomeFragment.update();
    }

    public static void showMessage(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    public void saveIntPreference(String key, int value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public int getIntPreference(String key) {
        int defValue;
        if (key.equals(PREF_ALARM_HOURS)) {
            defValue = 12;
        } else {
            defValue = 0;
        }

        try {
            return settings.getInt(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    public void saveBooleanPreference(String key, boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getBooleanPreference(String key) {
        try {
            return settings.getBoolean(key, true);
        } catch (Exception e) {
            return true;
        }
    }
}