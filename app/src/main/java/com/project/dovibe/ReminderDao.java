package com.project.dovibe;

import androidx.room.*;
import java.util.List;

@Dao
public interface ReminderDao {

    @Insert
    long insert(Reminder reminder);

    @Update
    void update(Reminder reminder);

    @Delete
    void delete(Reminder reminder);

    @Query("SELECT * FROM reminders ORDER BY month ASC, day ASC")
    List<Reminder> getAllReminders();

    @Query("UPDATE reminders SET enabled = :enabled WHERE id = :id")
    void setEnabled(int id, boolean enabled);
}