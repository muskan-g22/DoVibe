package com.project.dovibe;

import android.content.*;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.*;

public class StopwatchActivity extends AppCompatActivity {

    private TextView             tvTime, tvLapTime;
    private View                 btnStartPause, btnReset, btnLap;
    private TextView             tvStartPauseLabel;
    private RecyclerView         rvLaps;
    private BottomNavigationView bottomNav;
    private LapAdapter           lapAdapter;
    private List<String>         laps = new ArrayList<>();

    private StopwatchService service;
    private boolean          bound   = false;
    private long             lapStartMs = 0;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            service = ((StopwatchService.LocalBinder) b).getService();
            bound   = true;
            refreshUi(service.getElapsed(), service.isRunning());
        }
        @Override public void onServiceDisconnected(ComponentName n) { bound = false; }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) {
            long elapsed  = i.getLongExtra(StopwatchService.EXTRA_ELAPSED, 0);
            boolean running = i.getBooleanExtra(StopwatchService.EXTRA_RUNNING, false);
            refreshUi(elapsed, running);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch);

        tvTime          = findViewById(R.id.tvStopwatchTime);
        tvLapTime       = findViewById(R.id.tvLapTime);
        btnStartPause   = findViewById(R.id.btnStartPause);
        btnReset        = findViewById(R.id.btnReset);
        btnLap          = findViewById(R.id.btnLap);
        tvStartPauseLabel = findViewById(R.id.tvStartPauseLabel);
        rvLaps          = findViewById(R.id.rvLaps);
        bottomNav       = findViewById(R.id.bottomNav);

        lapAdapter = new LapAdapter(laps);
        rvLaps.setLayoutManager(new LinearLayoutManager(this));
        rvLaps.setAdapter(lapAdapter);

        btnStartPause.setOnClickListener(v -> {
            if (!bound) return;
            if (service.isRunning()) {
                service.pause();
            } else {
                if (lapStartMs == 0)
                    lapStartMs = service.getElapsed();
                service.start();
            }
        });

        btnReset.setOnClickListener(v -> {
            if (!bound) return;
            service.reset();
            laps.clear();
            lapAdapter.notifyDataSetChanged();
            lapStartMs = 0;
            tvLapTime.setText("Lap — 00:00.00");
        });

        btnLap.setOnClickListener(v -> {
            if (!bound || !service.isRunning()) return;
            long elapsed = service.getElapsed();
            long lapDuration = elapsed - lapStartMs;
            lapStartMs = elapsed;
            String lapLabel = String.format("Lap %d    %s",
                    laps.size() + 1, StopwatchService.formatTime(lapDuration));
            laps.add(0, lapLabel);
            lapAdapter.notifyItemInserted(0);
            rvLaps.scrollToPosition(0);
        });

        setupBottomNav();

        Intent serviceIntent = new Intent(this, StopwatchService.class);
        startService(serviceIntent);
        bindService(serviceIntent, conn, BIND_AUTO_CREATE);
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(StopwatchService.ACTION_TICK);
        ContextCompat.registerReceiver(this, receiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        bottomNav.setSelectedItemId(R.id.nav_stopwatch);
    }

    @Override protected void onPause() {
        super.onPause();
        try { unregisterReceiver(receiver); } catch (IllegalArgumentException ignored) {}
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (bound) { unbindService(conn); bound = false; }
    }

    private void refreshUi(long elapsed, boolean running) {
        tvTime.setText(StopwatchService.formatTime(elapsed));

        long lapDur = elapsed - lapStartMs;
        tvLapTime.setText("Current lap  " + StopwatchService.formatTime(lapDur));

        // Resolve theme colors so buttons respect dark mode
        int colorPrimary = resolveColor(com.google.android.material.R.attr.colorPrimary);
        int colorSurface = resolveColor(com.google.android.material.R.attr.colorSurface);

        if (running) {
            tvStartPauseLabel.setText("⏸");
            btnStartPause.setBackgroundTintList(
                    ColorStateList.valueOf(0xFFE24B4A));
        } else {
            tvStartPauseLabel.setText("▶");
            btnStartPause.setBackgroundTintList(
                    ColorStateList.valueOf(colorPrimary));
        }

        btnLap.setAlpha(running ? 1f : 0.4f);
        btnLap.setEnabled(running);
    }

    /** Resolve a theme color attribute to an ARGB int. */
    private int resolveColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_todo)      { startActivity(new Intent(this, MainActivity.class));      finish(); return true; }
            else if (id == R.id.nav_calendar)  { startActivity(new Intent(this, CalenderActivity.class));  finish(); return true; }
            else if (id == R.id.nav_stopwatch) return true;
            else if (id == R.id.nav_reminders) { startActivity(new Intent(this, ReminderActivity.class)); finish(); return true; }
            return false;
        });
    }

    // ── Lap list adapter ──────────────────────────────────────────────────────
    static class LapAdapter extends RecyclerView.Adapter<LapAdapter.VH> {
        private final List<String> laps;
        LapAdapter(List<String> laps) { this.laps = laps; }

        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            TextView tv = new TextView(p.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(0, 16, 0, 16);
            tv.setTextSize(14);
            // Use theme attribute for text color — works in both light and dark
            android.util.TypedValue tvAttr = new android.util.TypedValue();
            p.getContext().getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary, tvAttr, true);
            tv.setTextColor(tvAttr.data);
            tv.setTypeface(android.graphics.Typeface.MONOSPACE);
            return new VH(tv);
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            h.tv.setText(laps.get(pos));
            h.tv.setAlpha(pos == 0 ? 1f : 0.6f);
        }

        @Override public int getItemCount() { return laps.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(TextView v) { super(v); tv = v; }
        }
    }
}