package ru.menshovanton.gachapoint.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

import ru.menshovanton.gachapoint.Calendar;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.R;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {
    public static DatabaseHelper dbHelper;
    Calendar calendar;
    SplashScreen splashScreen;

    public static int subType = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        splashScreen = this;

        Thread initialization = new Thread(this::appInitialization);
        initialization.start();
    }

    private void appInitialization() {
        dbHelper = new DatabaseHelper(getApplicationContext(), null);

        if (!isDatabaseExists(splashScreen)) {
            dbHelper.getWritableDatabase();
        }

        calendar = new Calendar(splashScreen);

        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isDatabaseExists(Context context) {
        File dbFile = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);
        return dbFile.exists();
    }
}