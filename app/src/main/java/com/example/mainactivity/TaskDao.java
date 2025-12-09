package com.example.mainactivity;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY hour, minute")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks")
    List<Task> getAllTasksSync(); // Synchronous method for background work

    @Insert
    long insertTask(Task task);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);
}
