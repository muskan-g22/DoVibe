package com.project.dovibe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.*;

public class CalenderActivity extends AppCompatActivity {

    private CalendarView         calendarView;
    private RecyclerView         recyclerView;
    private TaskAdapter          adapter;
    private TextView             tvSelectedDate, tvNoTasks;
    private BottomNavigationView bottomNav;
    private TaskDao              taskDao;
    private List<Task>           dayTasks = new ArrayList<>();

    // Currently selected date in "YYYY-MM-DD" format
    private String selectedDateKey = todayKey();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calender);

        taskDao        = AppDatabase.getInstance(this).taskDao();
        calendarView   = findViewById(R.id.calendarView);
        recyclerView   = findViewById(R.id.rvCalendarTasks);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoTasks      = findViewById(R.id.tvNoTasks);
        bottomNav      = findViewById(R.id.bottomNav);

        setupRecyclerView();
        setupBottomNav();
        loadForDate(selectedDateKey, "Today, " + formatDisplay(Calendar.getInstance()));

        calendarView.setOnDateChangeListener((v, year, month, day) -> {
            String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
            selectedDateKey = String.format("%04d-%02d-%02d", year, month + 1, day);
            String label = day + " " + months[month] + " " + year;
            loadForDate(selectedDateKey, label);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadForDate(selectedDateKey,
                selectedDateKey.equals(todayKey())
                        ? "Today, " + formatDisplay(Calendar.getInstance())
                        : selectedDateKey);
    }

    private void loadForDate(String dateKey, String displayLabel) {
        tvSelectedDate.setText(displayLabel);
        AppDatabase.runAsync(
                () -> taskDao.getTasksForDate(dateKey),
                tasks -> {
                    dayTasks.clear();
                    dayTasks.addAll(tasks);
                    adapter.updateList(new ArrayList<>(dayTasks));
                    tvNoTasks.setVisibility(dayTasks.isEmpty() ? View.VISIBLE : View.GONE);
                }
        );
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(dayTasks, new TaskAdapter.OnTaskActionListenerLegacy() {

            @Override
            public void onToggleDone(Task t, int pos) {
                t.isDone = !t.isDone;
                AppDatabase.runAsync(() -> taskDao.update(t));
                loadForDate(selectedDateKey, tvSelectedDate.getText().toString());
            }

            @Override
            public void onToggleAlarm(Task t, int pos, boolean on) {
                t.alarmEnabled = on;
                AppDatabase.runAsync(() -> {
                    taskDao.update(t);
                    NotificationHelper.cancelTask(CalenderActivity.this, t.id);
                    NotificationHelper.scheduleTask(CalenderActivity.this, t);
                });
            }

            @Override
            public void onEditTask(Task t, int pos) {
                Toast.makeText(CalenderActivity.this,
                        "Edit from the To-Do screen",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteTask(Task t, int pos) {
                AppDatabase.runAsync(() -> {
                    NotificationHelper.cancelTask(CalenderActivity.this, t.id);
                    taskDao.delete(t);
                });

                loadForDate(selectedDateKey,
                        tvSelectedDate.getText().toString());
            }

            @Override
            public void onCycleNotifyMode(Task t, int pos) {
                // leave empty for now
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_calendar);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_todo)      { startActivity(new Intent(this, MainActivity.class));      finish(); return true; }
            else if (id == R.id.nav_calendar)  return true;
            else if (id == R.id.nav_stopwatch) { startActivity(new Intent(this, StopwatchActivity.class)); finish(); return true; }
            else if (id == R.id.nav_reminders) { startActivity(new Intent(this, ReminderActivity.class)); finish(); return true; }
        //    else if (id == R.id.nav_profile)   { startActivity(new Intent(this, ProfileActivity.class));   finish(); return true; }
            return false;
        });
    }

    private static String todayKey() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static String formatDisplay(Calendar c) {
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        return c.get(Calendar.DAY_OF_MONTH) + " " + months[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
    }
}