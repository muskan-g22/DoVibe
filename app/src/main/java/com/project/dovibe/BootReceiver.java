package com.project.dovibe;

import android.content.*;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        // Use goAsync so we can run DB queries off the main thread inside a BroadcastReceiver
        PendingResult result = goAsync();
        AppDatabase.runAsync(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            List<Task> tasks = db.taskDao().getPendingTasks();
            for (Task t : tasks) {
                NotificationHelper.scheduleTask(context, t);
            }

            List<Reminder> reminders = db.reminderDao().getAllReminders();
            for (Reminder r : reminders) {
                if (r.enabled) NotificationHelper.scheduleReminder(context, r);
            }

            result.finish();
        });
    }
}