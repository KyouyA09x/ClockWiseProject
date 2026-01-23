package com.example.mainactivity;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks WHERE timeCategory = :category AND (isDeleted = 0 OR isDeleted IS NULL)")
    List<Task> getTasksByCategory(String category);

    @Query("SELECT * FROM tasks WHERE (isDeleted = 0 OR isDeleted IS NULL)")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getTaskById(int id);

    @Query("SELECT * FROM tasks WHERE date = :date AND (isDeleted = 0 OR isDeleted IS NULL)")
    List<Task> getTasksByDate(String date);

    @Query("DELETE FROM tasks WHERE id = :id")
    void deleteById(int id);
    
    // Trash bin queries
    @Query("SELECT * FROM tasks WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    List<Task> getDeletedTasks();
    
    @Query("SELECT * FROM tasks WHERE isDeleted = 1 AND taskType = 'reminder' ORDER BY deletedAt DESC")
    List<Task> getDeletedReminders();
    
    @Query("SELECT * FROM tasks WHERE isDeleted = 1 AND taskType = 'focus' ORDER BY deletedAt DESC")
    List<Task> getDeletedFocusTasks();
    
    // Soft delete - move to trash
    @Query("UPDATE tasks SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    void softDelete(int id, long deletedAt);
    
    // Restore from trash
    @Query("UPDATE tasks SET isDeleted = 0, deletedAt = 0 WHERE id = :id")
    void restore(int id);
    
    // Permanently delete all items in trash
    @Query("DELETE FROM tasks WHERE isDeleted = 1")
    void emptyTrash();
    
    // Delete all completed tasks
    @Query("DELETE FROM tasks WHERE isComplete = 1")
    void deleteCompletedTasks();
    
    // Delete all tasks
    @Query("DELETE FROM tasks")
    void deleteAllTasks();
}

