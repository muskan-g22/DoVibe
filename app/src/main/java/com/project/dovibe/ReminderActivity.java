package com.project.dovibe;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.*;

public class ReminderActivity extends AppCompatActivity {

    private RecyclerView         recyclerView;
    private ReminderAdapter      adapter;
    private BottomNavigationView bottomNav;
    private ReminderDao          reminderDao;
    private List<Reminder>       list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        reminderDao  = AppDatabase.getInstance(this).reminderDao();
        recyclerView = findViewById(R.id.rvReminders);
        bottomNav    = findViewById(R.id.bottomNav);

        setupRecyclerView();
        setupFab();
        setupBottomNav();
        loadReminders();
    }

    @Override protected void onResume() { super.onResume(); loadReminders(); }

    private void loadReminders() {
        AppDatabase.runAsync(
                () -> reminderDao.getAllReminders(),
                reminders -> {
                    list.clear();
                    list.addAll(reminders);
                    adapter.updateList(new ArrayList<>(list));
                }
        );
    }

    private void setupRecyclerView() {
        adapter = new ReminderAdapter(list, new ReminderAdapter.OnReminderActionListener() {
            @Override public void onToggleEnabled(Reminder r, int pos, boolean on) {
                r.enabled = on;
                AppDatabase.runAsync(() -> {
                    reminderDao.update(r);
                    if (on) NotificationHelper.scheduleReminder(ReminderActivity.this, r);
                    else    NotificationHelper.cancelTask(ReminderActivity.this, r.id + 10000);
                });
            }
            @Override public void onDelete(Reminder r, int pos) {
                new AlertDialog.Builder(ReminderActivity.this)
                        .setTitle("Delete reminder?")
                        .setMessage("\"" + r.title + "\" will be removed.")
                        .setPositiveButton("Delete", (d, w) -> {
                            AppDatabase.runAsync(() -> {
                                NotificationHelper.cancelTask(ReminderActivity.this, r.id + 10000);
                                reminderDao.delete(r);
                            });
                            loadReminders();
                        })
                        .setNegativeButton("Cancel", null).show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabReminder);
        fab.setOnClickListener(v -> showAddReminderDialog());
    }

    private void showAddReminderDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null);

        EditText  etTitle  = v.findViewById(R.id.etReminderTitle);
        Spinner   spType   = v.findViewById(R.id.spReminderType);
        Spinner   spMonth  = v.findViewById(R.id.spMonth);
        Spinner   spDay    = v.findViewById(R.id.spDay);
        Button    btnTime  = v.findViewById(R.id.btnPickReminderTime);
        TextView  tvTime   = v.findViewById(R.id.tvReminderTime);
        RadioGroup rgRepeat = v.findViewById(R.id.rgReminderRepeat);

        final int[] selH = {9}, selM = {0};

        String[] types = {"Birthday","Anniversary","Meeting","Holiday","Other"};
        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types));

        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        spMonth.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months));

        String[] days = new String[31];
        for (int i = 0; i < 31; i++) days[i] = String.valueOf(i + 1);
        spDay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, days));

        btnTime.setOnClickListener(btn -> new TimePickerDialog(this, (tp, h, m) -> {
            selH[0] = h; selM[0] = m;
            String ap = h >= 12 ? "PM" : "AM";
            tvTime.setText(String.format("%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, ap));
        }, selH[0], selM[0], false).show());

        new AlertDialog.Builder(this)
                .setTitle("🔔  New Reminder")
                .setView(v)
                .setPositiveButton("Add", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) { Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show(); return; }
                    Reminder r   = new Reminder();
                    r.title      = title;
                    r.type       = spType.getSelectedItem().toString();
                    r.month      = spMonth.getSelectedItemPosition() + 1;
                    r.day        = spDay.getSelectedItemPosition() + 1;
                    r.date       = months[spMonth.getSelectedItemPosition()] + " " + r.day;
                    r.hour       = selH[0]; r.minute = selM[0];
                    r.repeatMode = rgRepeat.getCheckedRadioButtonId() == R.id.rbYearly ? "yearly" : "once";
                    r.enabled    = true;
                    r.createdAt  = System.currentTimeMillis();
                    AppDatabase.runAsync(() -> {
                        long newId = reminderDao.insert(r);
                        r.id = (int) newId;
                        NotificationHelper.scheduleReminder(this, r);
                    });
                    loadReminders();
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_reminders);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_todo)      { startActivity(new Intent(this, MainActivity.class));     finish(); return true; }
            else if (id == R.id.nav_calendar)  { startActivity(new Intent(this, CalenderActivity.class)); finish(); return true; }
            else if (id == R.id.nav_stopwatch) { startActivity(new Intent(this, StopwatchActivity.class)); finish(); return true; }
            else if (id == R.id.nav_reminders) return true;
      //      else if (id == R.id.nav_profile)   { startActivity(new Intent(this, ProfileActivity.class));  finish(); return true; }
            return false;
        });
    }
}