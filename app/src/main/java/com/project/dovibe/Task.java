package com.project.dovibe;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String  title;
    public String  category;       // "Work" | "Personal" | "Urgent"
    public String  dueDate;        // "YYYY-MM-DD"
    public String  dueTime;        // display string  e.g. "10:30 AM"
    public int     hour;
    public int     minute;
    public boolean isUrgent;
    public boolean isDone;

    /**
     * Notification mode — 3 states stored as int:
     *   0 = Off        (no notification, no alarm)
     *   1 = Notify     (silent notification only)
     *   2 = Alarm+Notify (notification + ringtone + vibrate)
     *
     * alarmEnabled kept for backwards compatibility (true = mode 2, false = mode 1 by default)
     */
    public int    notifyMode;   // 0 | 1 | 2
    public boolean alarmEnabled; // legacy — kept so old data still works

    public String  repeatMode;     // "once" | "daily" | "weekly" | "custom"
    public String  customDays;     // comma-separated e.g. "MON,WED,FRI"
    public long    createdAt;

    // Helper — converts old alarmEnabled to notifyMode on first access
    public int getNotifyMode() {
        if (notifyMode == 0 && alarmEnabled) return 2;
        return notifyMode;
    }
}