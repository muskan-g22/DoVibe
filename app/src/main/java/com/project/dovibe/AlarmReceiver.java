package com.project.dovibe;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String title      = intent.getStringExtra(NotificationHelper.EXTRA_TITLE);
        int    id         = intent.getIntExtra(NotificationHelper.EXTRA_ID, 0);
        String type       = intent.getStringExtra(NotificationHelper.EXTRA_TYPE);
        int    notifyMode = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFY_MODE, 1);
        String repeat     = intent.getStringExtra("repeat_mode");
        int    taskId     = intent.getIntExtra("task_id", 0);
        int    hour       = intent.getIntExtra("hour", 0);
        int    minute     = intent.getIntExtra("minute", 0);

        // Mode 0 = off, do nothing
        if (notifyMode == 0) return;

        boolean isReminder = "reminder".equals(type);
        String body    = isReminder ? "Don't forget: " + title : "Time for your task: " + title;
        String channel = isReminder ? NotificationHelper.CHANNEL_REMINDERS : NotificationHelper.CHANNEL_TASKS;

        // Fire notification
        NotificationHelper.showNotification(context, id, title, body, channel, notifyMode);

        // Mode 2 = Alarm+Notify → ring + vibrate
        if (notifyMode == 2) {
            try {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (uri == null)
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
                if (ringtone != null) ringtone.play();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                else
                    v.vibrate(2000);
            }
        }

        // Re-chain next occurrence for repeating tasks
        if (!isReminder && repeat != null) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            int daysAhead = 0;
            switch (repeat) {
                case "daily":  daysAhead = 1; break;
                case "weekly": daysAhead = 7; break;
            }

            if (daysAhead > 0) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.add(Calendar.DAY_OF_YEAR, daysAhead);

                PendingIntent pi = PendingIntent.getBroadcast(context, taskId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                NotificationHelper.setExact(am, cal.getTimeInMillis(), pi);
            }
        }

        // Re-chain yearly reminders
        if (isReminder) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.YEAR, 1);
                PendingIntent pi = PendingIntent.getBroadcast(context, id, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                NotificationHelper.setExact(am, cal.getTimeInMillis(), pi);
            }
        }
    }
}