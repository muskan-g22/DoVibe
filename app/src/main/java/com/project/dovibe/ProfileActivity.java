package com.project.dovibe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS       = "dovibe_prefs";
    private static final String KEY_DARK    = "dark_mode";

    private TextView             tvTotal, tvDone, tvPending, tvUrgent, tvWork, tvPersonal;
    private TextView             tvAppVersion;
    private Switch               switchDarkMode;
    private BottomNavigationView bottomNav;
    private SharedPreferences    prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        tvTotal       = findViewById(R.id.tvTotalTasks);
        tvDone        = findViewById(R.id.tvDoneTasks);
        tvPending     = findViewById(R.id.tvPendingTasks);
        tvUrgent      = findViewById(R.id.tvUrgentTasks);
        tvWork        = findViewById(R.id.tvWorkTasks);
        tvPersonal    = findViewById(R.id.tvPersonalTasks);
        tvAppVersion  = findViewById(R.id.tvAppVersion);
        switchDarkMode= findViewById(R.id.switchDarkMode);
        bottomNav     = findViewById(R.id.bottomNav);

        // Set app version
        try {
            String vName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion.setText("v" + vName);
        } catch (Exception ignored) {}

        // Init dark mode switch from saved pref
        boolean isDark = prefs.getBoolean(KEY_DARK, isSystemDark());
        switchDarkMode.setChecked(isDark);

        // Dark mode toggle
        findViewById(R.id.rowDarkMode).setOnClickListener(v -> {
            boolean newVal = !switchDarkMode.isChecked();
            switchDarkMode.setChecked(newVal);
            prefs.edit().putBoolean(KEY_DARK, newVal).apply();
            AppCompatDelegate.setDefaultNightMode(
                    newVal ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Notification settings row → open system notification settings
        findViewById(R.id.rowNotifications).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        });

        // About row → simple dialog
        findViewById(R.id.rowAbout).setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("About DoVibe")
                    .setMessage("DoVibe helps you stay on top of tasks, events, and reminders.\n\nBuilt with ❤️ using Android & Room DB.")
                    .setPositiveButton("OK", null)
                    .show();
        });

        setupBottomNav();
        loadStats();
    }

    @Override protected void onResume() {
        super.onResume();
        loadStats();
    }

    private boolean isSystemDark() {
        int nightMode = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void loadStats() {
        AppDatabase.runAsync(
                () -> AppDatabase.getInstance(this).taskDao().getAllTasks(),
                tasks -> {
                    int total = tasks.size(), done = 0, pending = 0, urgent = 0, work = 0, personal = 0;
                    for (Task t : tasks) {
                        if (t.isDone) done++; else pending++;
                        if (t.isUrgent && !t.isDone)       urgent++;
                        if ("Work".equals(t.category))     work++;
                        if ("Personal".equals(t.category)) personal++;
                    }
                    tvTotal.setText(String.valueOf(total));
                    tvDone.setText(String.valueOf(done));
                    tvPending.setText(String.valueOf(pending));
                    tvUrgent.setText(String.valueOf(urgent));
                    tvWork.setText(String.valueOf(work));
                    tvPersonal.setText(String.valueOf(personal));
                }
        );
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_todo)      { startActivity(new Intent(this, MainActivity.class));      finish(); return true; }
            else if (id == R.id.nav_calendar)  { startActivity(new Intent(this, CalenderActivity.class));  finish(); return true; }
            else if (id == R.id.nav_stopwatch) { startActivity(new Intent(this, StopwatchActivity.class)); finish(); return true; }
            else if (id == R.id.nav_reminders) { startActivity(new Intent(this, ReminderActivity.class)); finish(); return true; }
            return false;
        });
    }
}