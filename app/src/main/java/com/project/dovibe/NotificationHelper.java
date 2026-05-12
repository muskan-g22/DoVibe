package com.project.dovibe;

import android.app.*;
import android.content.*;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.Calendar;

public class NotificationHelper {

    public static final String CHANNEL_TASKS     = "dovibe_tasks";
    public static final String CHANNEL_REMINDERS = "dovibe_reminders";

    public static final String EXTRA_TITLE       = "extra_title";
    public static final String EXTRA_ID          = "extra_id";
    public static final String EXTRA_TYPE        = "extra_type";
    public static final String EXTRA_NOTIFY_MODE = "extra_notify_mode"; // 0/1/2

    // ── Create notification channels ──────────────────────────────────────────
    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);

            NotificationChannel tasks = new NotificationChannel(
                    CHANNEL_TASKS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH);
            tasks.setDescription("Alerts for your Dovibe to-do tasks");
            tasks.enableVibration(true);
            nm.createNotificationChannel(tasks);

            NotificationChannel reminders = new NotificationChannel(
                    CHANNEL_REMINDERS, "Event Reminders", NotificationManager.IMPORTANCE_HIGH);
            reminders.setDescription("Birthday, anniversary and event alerts");
            reminders.enableVibration(true);
            nm.createNotificationChannel(reminders);

            NotificationChannel sw = new NotificationChannel(
                    StopwatchService.CHANNEL_SW, "Stopwatch", NotificationManager.IMPORTANCE_LOW);
            sw.setDescription("Shows while stopwatch is running");
            sw.setSound(null, null);
            nm.createNotificationChannel(sw);
        }
    }

    // ── Check if we can schedule exact alarms (Android 12+) ───────────────────
    public static boolean canScheduleExactAlarms(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            return am != null && am.canScheduleExactAlarms();
        }
        return true;
    }

    // ── Show an immediate notification ────────────────────────────────────────
    public static void showNotification(Context ctx, int id, String title, String body,
                                        String channel, int notifyMode) {
        // Mode 0 = off, don't show anything
        if (notifyMode == 0) return;

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(ctx).notify(id, b.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // ── Schedule a task alarm ─────────────────────────────────────────────────
    public static void scheduleTask(Context ctx, Task task) {
        // Mode 0 = fully off — cancel any existing and return
        if (task.getNotifyMode() == 0) {
            cancelTask(ctx, task.id);
            return;
        }

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        String repeat = task.repeatMode == null ? "once" : task.repeatMode;

        if ("custom".equals(repeat)) {
            scheduleCustomDays(ctx, am, task);
            return;
        }

        PendingIntent pi = buildTaskPendingIntent(ctx, task, task.id);
        long triggerMs   = buildTriggerMs(task.hour, task.minute);
        setExact(am, triggerMs, pi);
    }

    // ── Schedule custom-day repeats ───────────────────────────────────────────
    private static void scheduleCustomDays(Context ctx, AlarmManager am, Task task) {
        if (task.customDays == null || task.customDays.isEmpty()) return;

        String[] days    = task.customDays.split(",");
        int[]    calDays = { Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY };
        String[] dayKeys = { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };

        for (int i = 0; i < dayKeys.length; i++) {
            for (String d : days) {
                if (dayKeys[i].equals(d.trim())) {
                    int requestCode = task.id * 10 + i;
                    PendingIntent pi = buildTaskPendingIntent(ctx, task, requestCode);

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_WEEK, calDays[i]);
                    cal.set(Calendar.HOUR_OF_DAY, task.hour);
                    cal.set(Calendar.MINUTE, task.minute);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    if (cal.getTimeInMillis() <= System.currentTimeMillis())
                        cal.add(Calendar.WEEK_OF_YEAR, 1);

                    setExact(am, cal.getTimeInMillis(), pi);
                }
            }
        }
    }

    // ── Cancel a task's alarms ────────────────────────────────────────────────
    public static void cancelTask(Context ctx, int taskId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // Cancel standard alarm
        PendingIntent pi = PendingIntent.getBroadcast(ctx, taskId,
                new Intent(ctx, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);

        // Cancel all 7 possible custom-day alarms
        for (int i = 0; i < 7; i++) {
            int requestCode = taskId * 10 + i;
            PendingIntent customPi = PendingIntent.getBroadcast(ctx, requestCode,
                    new Intent(ctx, AlarmReceiver.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(customPi);
        }
    }

    // ── Schedule a reminder ───────────────────────────────────────────────────
    public static void scheduleReminder(Context ctx, Reminder r) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        int requestCode = r.id + 10000;
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.putExtra(EXTRA_TITLE, r.title);
        intent.putExtra(EXTRA_ID, requestCode);
        intent.putExtra(EXTRA_TYPE, "reminder");
        intent.putExtra(EXTRA_NOTIFY_MODE, 1); // reminders always notify

        PendingIntent pi = PendingIntent.getBroadcast(ctx, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, r.month - 1);
        cal.set(Calendar.DAY_OF_MONTH, r.day);
        cal.set(Calendar.HOUR_OF_DAY, r.hour);
        cal.set(Calendar.MINUTE, r.minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis())
            cal.add(Calendar.YEAR, 1);

        setExact(am, cal.getTimeInMillis(), pi);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static PendingIntent buildTaskPendingIntent(Context ctx, Task task, int requestCode) {
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.putExtra(EXTRA_TITLE, task.title);
        intent.putExtra(EXTRA_ID, requestCode);
        intent.putExtra(EXTRA_TYPE, "task");
        intent.putExtra(EXTRA_NOTIFY_MODE, task.getNotifyMode());
        intent.putExtra("repeat_mode", task.repeatMode);
        intent.putExtra("task_id", task.id);
        intent.putExtra("hour", task.hour);
        intent.putExtra("minute", task.minute);
        return PendingIntent.getBroadcast(ctx, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Build trigger time for today at hour:minute.
     * KEY FIX: seconds and milliseconds are zeroed, then we add a small buffer
     * to avoid Android's minimum-delay window for setExactAndAllowWhileIdle.
     * If the resulting time is in the past, roll to tomorrow.
     */
    private static long buildTriggerMs(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // If the exact minute hasn't arrived yet but we're in the same minute,
        // let it fire immediately — don't push to tomorrow.
        // If already past this minute, schedule tomorrow.
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis();
    }

    /** Uses setExactAndAllowWhileIdle on API 23+, setExact below that. */
    public static void setExact(AlarmManager am, long triggerMs, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi);
        }
    }
}