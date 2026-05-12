package com.project.dovibe;

import android.content.Context;
import androidx.room.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Task.class, Reminder.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;
    private static final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);

    public abstract TaskDao     taskDao();
    public abstract ReminderDao reminderDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "dovibe_db"
                    )
                    // .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    static final androidx.room.migration.Migration MIGRATION_1_2 =
            new androidx.room.migration.Migration(1, 2) {
                @Override
                public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
                    database.execSQL(
                            "ALTER TABLE tasks ADD COLUMN dueDate TEXT NOT NULL DEFAULT ''");
                }
            };

    static final androidx.room.migration.Migration MIGRATION_2_3 =
            new androidx.room.migration.Migration(2, 3) {
                @Override
                public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
                    database.execSQL(
                            "ALTER TABLE tasks ADD COLUMN notifyMode INTEGER NOT NULL DEFAULT 1");
                }
            };

    /** Run a DB write on the background executor. */
    public static void runAsync(Runnable r) {
        dbExecutor.execute(r);
    }

    /** Run a DB read on background, deliver result on main thread. */
    public static <T> void runAsync(java.util.concurrent.Callable<T> query,
                                    java.util.function.Consumer<T> onResult) {
        dbExecutor.execute(() -> {
            try {
                T result = query.call();
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> onResult.accept(result));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}