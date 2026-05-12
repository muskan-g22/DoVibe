package com.project.dovibe;

import android.app.*;
import android.content.*;
import android.os.*;
import androidx.core.app.NotificationCompat;

public class StopwatchService extends Service {

    public static final String CHANNEL_SW      = "dovibe_stopwatch";
    public static final String ACTION_START    = "sw_start";
    public static final String ACTION_PAUSE    = "sw_pause";
    public static final String ACTION_RESET    = "sw_reset";
    public static final String ACTION_LAP      = "sw_lap";
    public static final String ACTION_TICK     = "sw_tick";

    public static final String EXTRA_ELAPSED   = "elapsed";
    public static final String EXTRA_RUNNING   = "running";

    private static final int NOTIF_ID = 7777;

    private final IBinder binder = new LocalBinder();
    private Handler  handler;
    private Runnable ticker;

    private long    elapsedMs   = 0;
    private long    startTime   = 0;
    private boolean running     = false;

    public class LocalBinder extends Binder {
        StopwatchService getService() { return StopwatchService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        handler = new Handler(Looper.getMainLooper());
        ticker  = new Runnable() {
            @Override public void run() {
                if (running) {
                    broadcast();
                    updateNotification();
                    handler.postDelayed(this, 10); // 10ms = centisecond precision
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        switch (intent.getAction() == null ? "" : intent.getAction()) {
            case ACTION_START: start(); break;
            case ACTION_PAUSE: pause(); break;
            case ACTION_RESET: reset(); break;
        }
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return binder; }

    // ── Controls ──────────────────────────────────────────────────────────────
    public void start() {
        if (running) return;
        running   = true;
        startTime = System.currentTimeMillis() - elapsedMs;
        startForeground(NOTIF_ID, buildNotification());
        handler.post(ticker);
        broadcast();
    }

    public void pause() {
        if (!running) return;
        running   = false;
        elapsedMs = System.currentTimeMillis() - startTime;
        handler.removeCallbacks(ticker);
        updateNotification();
        broadcast();
    }

    public void reset() {
        running   = false;
        elapsedMs = 0;
        startTime = 0;
        handler.removeCallbacks(ticker);
        stopForeground(true);
        broadcast();
    }

    public long  getElapsed() { return running ? System.currentTimeMillis() - startTime : elapsedMs; }
    public boolean isRunning() { return running; }

    // ── Broadcast tick to Activity ────────────────────────────────────────────
    private void broadcast() {
        Intent i = new Intent(ACTION_TICK);
        i.setPackage(getPackageName()); // required for implicit broadcasts on Android 8+
        i.putExtra(EXTRA_ELAPSED, getElapsed());
        i.putExtra(EXTRA_RUNNING, running);
        sendBroadcast(i);
    }

    // ── Notification ──────────────────────────────────────────────────────────
    private Notification buildNotification() {
        Intent open = new Intent(this, StopwatchActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, StopwatchService.class).setAction(ACTION_PAUSE);
        PendingIntent pausePi = PendingIntent.getService(this, 1, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent resetIntent = new Intent(this, StopwatchService.class).setAction(ACTION_RESET);
        PendingIntent resetPi = PendingIntent.getService(this, 2, resetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_SW)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⏱ Stopwatch Running")
                .setContentText(formatTime(getElapsed()))
                .setContentIntent(openPi)
                .addAction(android.R.drawable.ic_media_pause, "Pause", pausePi)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reset", resetPi)
                .setOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_SW, "Stopwatch", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Shows while stopwatch is running");
            ch.setSound(null, null);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    public static String formatTime(long ms) {
        long h  = ms / 3600000;
        long m  = (ms % 3600000) / 60000;
        long s  = (ms % 60000)   / 1000;
        long cs = (ms % 1000)    / 10;
        if (h > 0) return String.format("%d:%02d:%02d.%02d", h, m, s, cs);
        return String.format("%02d:%02d.%02d", m, s, cs);
    }
}