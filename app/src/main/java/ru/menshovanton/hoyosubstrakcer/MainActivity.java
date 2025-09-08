package ru.menshovanton.hoyosubstrakcer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

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

    public void updateDbHelper() {
        dbHelper = new DatabaseHelper(getApplicationContext());

        if (!isDatabaseExists(this)) {
            dbHelper.getWritableDatabase();
        }
    }
}