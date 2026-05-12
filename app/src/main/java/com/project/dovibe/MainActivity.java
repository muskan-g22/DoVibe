package com.project.dovibe;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private RecyclerView         recyclerView;
    private TaskAdapter          adapter;
    private ProgressBar          progressBar;
    private TextView             tvProgressLabel, tvProgressPercent;
    private EditText             etSearch;
    private TabLayout            tabLayout;
    private BottomNavigationView bottomNav;
    private Button               btnAddTask;

    private AppDatabase db;
    private TaskDao     taskDao;
    private List<Task>  allTasks      = new ArrayList<>();
    private List<Task>  filteredTasks = new ArrayList<>();
    private String      activeCategory = "All";
    private String      searchQuery    = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationHelper.createChannels(this);
        requestExactAlarmPermission();

        db      = AppDatabase.getInstance(this);
        taskDao = db.taskDao();

        bindViews();
        setupTabs();
        setupRecyclerView();
        setupSearch();
        setupAddButton();
        setupBottomNav();
        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_todo);
        loadTasks();
    }

    // ── Request exact alarm permission on Android 12+ ────────────────────────
    private void requestExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!NotificationHelper.canScheduleExactAlarms(this)) {
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private void bindViews() {
        recyclerView      = findViewById(R.id.recyclerView);
        progressBar       = findViewById(R.id.progressBar);
        tvProgressLabel   = findViewById(R.id.tvProgressLabel);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        etSearch          = findViewById(R.id.etSearch);
        tabLayout         = findViewById(R.id.tabLayout);
        bottomNav         = findViewById(R.id.bottomNav);
        btnAddTask        = findViewById(R.id.btnAddTask);
    }

    // ── Load (async) ──────────────────────────────────────────────────────────
    private void loadTasks() {
        AppDatabase.runAsync(
                () -> taskDao.getAllTasks(),
                tasks -> {
                    allTasks.clear();
                    allTasks.addAll(tasks);
                    applyFilter();
                }
        );
    }

    private void setupTabs() {
        for (String c : new String[]{"All", "Work", "Personal", "Urgent"})
            tabLayout.addTab(tabLayout.newTab().setText(c));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                activeCategory = tab.getText() != null ? tab.getText().toString() : "All";
                applyFilter();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(filteredTasks, new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onToggleDone(Task t, int pos) {
                t.isDone = !t.isDone;
                AppDatabase.runAsync(() -> {
                    taskDao.update(t);
                    if (t.isDone) NotificationHelper.cancelTask(MainActivity.this, t.id);
                });
                loadTasks();
            }

            @Override
            public void onCycleNotifyMode(Task t, int pos) {
                // Cycle: 0 (Off) → 1 (Notify) → 2 (Alarm+Notify) → 0
                int next = (t.getNotifyMode() + 1) % 3;
                t.notifyMode   = next;
                t.alarmEnabled = (next == 2);
                AppDatabase.runAsync(() -> {
                    taskDao.update(t);
                    NotificationHelper.cancelTask(MainActivity.this, t.id);
                    NotificationHelper.scheduleTask(MainActivity.this, t);
                });
                String[] labels = {"Notifications OFF", "Notify only", "Alarm + Notify ON"};
                Toast.makeText(MainActivity.this, labels[next], Toast.LENGTH_SHORT).show();
                loadTasks();
            }

            @Override
            public void onEditTask(Task t, int pos) {
                showAddEditDialog(t);
            }

            @Override
            public void onDeleteTask(Task t, int pos) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete task?")
                        .setMessage("\"" + t.title + "\" will be removed.")
                        .setPositiveButton("Delete", (d, w) -> {
                            AppDatabase.runAsync(() -> {
                                NotificationHelper.cancelTask(MainActivity.this, t.id);
                                taskDao.delete(t);
                            });
                            loadTasks();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }
        });
    }

    private void setupAddButton() {
        btnAddTask.setOnClickListener(v -> showAddEditDialog(null));
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_todo)      return true;
            else if (id == R.id.nav_calendar)  { startActivity(new Intent(this, CalenderActivity.class));  return true; }
            else if (id == R.id.nav_stopwatch) { startActivity(new Intent(this, StopwatchActivity.class)); return true; }
            else if (id == R.id.nav_reminders) { startActivity(new Intent(this, ReminderActivity.class));  return true; }
            //   else if (id == R.id.nav_profile)   { startActivity(new Intent(this, ProfileActivity.class));   return true; }
            return false;
        });
    }

    private void applyFilter() {
        filteredTasks.clear();
        for (Task t : allTasks) {
            boolean cat  = activeCategory.equals("All") || t.category.equals(activeCategory);
            boolean srch = searchQuery.isEmpty() || t.title.toLowerCase().contains(searchQuery);
            if (cat && srch) filteredTasks.add(t);
        }
        adapter.updateList(new ArrayList<>(filteredTasks));
        updateProgress();
    }

    private void updateProgress() {
        int total = filteredTasks.size(), done = 0;
        for (Task t : filteredTasks) if (t.isDone) done++;
        int pct = total == 0 ? 0 : done * 100 / total;
        progressBar.setProgress(pct);
        tvProgressLabel.setText(done + " of " + total + " done");
        tvProgressPercent.setText(pct + "%");
    }

    // ── Add / Edit Dialog ─────────────────────────────────────────────────────
    private void showAddEditDialog(Task existing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);

        EditText     etTitle      = v.findViewById(R.id.etTaskTitle);
        TextView     tvTime       = v.findViewById(R.id.tvSelectedTime);
        Button       btnPickTime  = v.findViewById(R.id.btnPickTime);
        Button       btnPickDate  = v.findViewById(R.id.btnPickDate);
        TextView     tvDate       = v.findViewById(R.id.tvSelectedDate);
        Spinner      spCategory   = v.findViewById(R.id.spCategory);
        CheckBox     cbUrgent     = v.findViewById(R.id.cbUrgent);
        // Switch       swAlarm      = v.findViewById(R.id.swAlarm);

        RadioGroup   rgRepeat     = v.findViewById(R.id.rgRepeat);
        RadioButton  rbOnce       = v.findViewById(R.id.rbOnce);
        RadioButton  rbDaily      = v.findViewById(R.id.rbDaily);
        RadioButton  rbWeekly     = v.findViewById(R.id.rbWeekly);
        RadioButton  rbCustom     = v.findViewById(R.id.rbCustom);
        LinearLayout layoutCustom = v.findViewById(R.id.layoutCustomDays);
        CheckBox cbMon = v.findViewById(R.id.cbMon), cbTue = v.findViewById(R.id.cbTue);
        CheckBox cbWed = v.findViewById(R.id.cbWed), cbThu = v.findViewById(R.id.cbThu);
        CheckBox cbFri = v.findViewById(R.id.cbFri), cbSat = v.findViewById(R.id.cbSat);
        CheckBox cbSun = v.findViewById(R.id.cbSun);

        final int[]    selH    = {9}, selM = {0};
        final int[]    selYear = new int[1], selMonth = new int[1], selDay = new int[1];
        Calendar today = Calendar.getInstance();
        selYear[0]  = today.get(Calendar.YEAR);
        selMonth[0] = today.get(Calendar.MONTH);
        selDay[0]   = today.get(Calendar.DAY_OF_MONTH);

        // Update date display helper
        Runnable updateDateLabel = () -> {
            String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
            tvDate.setText(selDay[0] + " " + months[selMonth[0]] + " " + selYear[0]);
        };
        updateDateLabel.run();

        rgRepeat.setOnCheckedChangeListener((g, id) ->
                layoutCustom.setVisibility(id == R.id.rbCustom ? View.VISIBLE : View.GONE));

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Work", "Personal", "Urgent"});
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        btnPickTime.setOnClickListener(btn -> new TimePickerDialog(this, (tp, h, m) -> {
            selH[0] = h; selM[0] = m;
            String ap = h >= 12 ? "PM" : "AM";
            int dh = h % 12 == 0 ? 12 : h % 12;
            tvTime.setText(String.format("%d:%02d %s", dh, m, ap));
        }, selH[0], selM[0], false).show());

        btnPickDate.setOnClickListener(btn -> new DatePickerDialog(this, (dp, y, mon, d) -> {
            selYear[0] = y; selMonth[0] = mon; selDay[0] = d;
            updateDateLabel.run();
        }, selYear[0], selMonth[0], selDay[0]).show());

        boolean isEdit = (existing != null);
        if (isEdit) {
            etTitle.setText(existing.title);
            tvTime.setText(existing.dueTime);
            selH[0] = existing.hour; selM[0] = existing.minute;
            //  swAlarm.setChecked(existing.alarmEnabled);
            cbUrgent.setChecked(existing.isUrgent);
            spCategory.setSelection(existing.category.equals("Personal") ? 1
                    : existing.category.equals("Urgent") ? 2 : 0);

            // Parse stored dueDate
            if (existing.dueDate != null && existing.dueDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] parts = existing.dueDate.split("-");
                selYear[0] = Integer.parseInt(parts[0]);
                selMonth[0] = Integer.parseInt(parts[1]) - 1;
                selDay[0] = Integer.parseInt(parts[2]);
                updateDateLabel.run();
            }

            switch (existing.repeatMode == null ? "once" : existing.repeatMode) {
                case "daily":  rbDaily.setChecked(true);  break;
                case "weekly": rbWeekly.setChecked(true); break;
                case "custom":
                    rbCustom.setChecked(true);
                    layoutCustom.setVisibility(View.VISIBLE);
                    String cd = existing.customDays == null ? "" : existing.customDays;
                    cbMon.setChecked(cd.contains("MON")); cbTue.setChecked(cd.contains("TUE"));
                    cbWed.setChecked(cd.contains("WED")); cbThu.setChecked(cd.contains("THU"));
                    cbFri.setChecked(cd.contains("FRI")); cbSat.setChecked(cd.contains("SAT"));
                    cbSun.setChecked(cd.contains("SUN")); break;
                default: rbOnce.setChecked(true); break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Edit Task" : "New Task")
                .setView(v)
                .setPositiveButton(isEdit ? "Save" : "Add", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String category = spCategory.getSelectedItem().toString();
                    // notifyMode is set via the label tap on the task card; default to 1 (notify) for new tasks
                    boolean urgent  = cbUrgent.isChecked();
                    int checkedId   = rgRepeat.getCheckedRadioButtonId();
                    String repeat   = checkedId == R.id.rbDaily  ? "daily"
                            : checkedId == R.id.rbWeekly ? "weekly"
                            : checkedId == R.id.rbCustom ? "custom" : "once";

                    List<String> days = new ArrayList<>();
                    if ("custom".equals(repeat)) {
                        if (cbMon.isChecked()) days.add("MON"); if (cbTue.isChecked()) days.add("TUE");
                        if (cbWed.isChecked()) days.add("WED"); if (cbThu.isChecked()) days.add("THU");
                        if (cbFri.isChecked()) days.add("FRI"); if (cbSat.isChecked()) days.add("SAT");
                        if (cbSun.isChecked()) days.add("SUN");
                    }

                    int h = selH[0], m = selM[0];
                    String ap = h >= 12 ? "PM" : "AM";
                    String dueTime = String.format("%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, ap);
                    String dueDate = String.format("%04d-%02d-%02d", selYear[0], selMonth[0] + 1, selDay[0]);

                    if (isEdit) {
                        existing.title = title; existing.category = category;
                        existing.dueTime = dueTime; existing.dueDate = dueDate;
                        existing.hour = h; existing.minute = m;
                        existing.isUrgent = urgent;
                        existing.repeatMode = repeat; existing.customDays = String.join(",", days);
                        AppDatabase.runAsync(() -> {
                            taskDao.update(existing);
                            NotificationHelper.cancelTask(this, existing.id);
                            NotificationHelper.scheduleTask(this, existing);
                        });
                    } else {
                        Task nt = new Task();
                        nt.title = title; nt.category = category; nt.dueTime = dueTime;
                        nt.dueDate = dueDate; nt.hour = h; nt.minute = m;
                        nt.notifyMode = 1; nt.alarmEnabled = false; nt.isUrgent = urgent;
                        nt.repeatMode = repeat; nt.customDays = String.join(",", days);
                        nt.isDone = false; nt.createdAt = System.currentTimeMillis();
                        AppDatabase.runAsync(() -> {
                            long newId = taskDao.insert(nt);
                            nt.id = (int) newId;
                            NotificationHelper.scheduleTask(this, nt);
                        });
                    }
                    loadTasks();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}