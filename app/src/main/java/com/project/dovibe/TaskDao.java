package com.project.dovibe;

import androidx.room.*;
import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY createdAt DESC")
    List<Task> getByCategory(String category);

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :q || '%' ORDER BY createdAt DESC")
    List<Task> search(String q);

    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY hour ASC, minute ASC")
    List<Task> getPendingTasks();

    // Returns tasks for a specific date OR tasks with no date set (repeatMode != "once")
    @Query("SELECT * FROM tasks WHERE dueDate = :date OR repeatMode != 'once' ORDER BY hour ASC, minute ASC")
    List<Task> getTasksForDate(String date);

    @Query("UPDATE tasks SET alarmEnabled = :enabled WHERE id = :id")
    void setAlarmEnabled(int id, boolean enabled);
}