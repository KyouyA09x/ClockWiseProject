package com.example.mainactivity;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private static TaskRepository instance;
    private TaskDao taskDao;
    public ArrayList<Task> morningTasks = new ArrayList<>();
    public ArrayList<Task> afternoonTasks = new ArrayList<>();
    public ArrayList<Task> nightTasks = new ArrayList<>();

    private TaskRepository() {}

    public static synchronized TaskRepository getInstance() {
        if (instance == null) {
            instance = new TaskRepository();
        }
        return instance;
    }

    public void initialize(Context context) {
        if (taskDao != null) return; // Already initialized

        TaskDatabase database = TaskDatabase.getInstance(context);
        taskDao = database.taskDao();
        loadTasksFromDatabase();
    }

    private void loadTasksFromDatabase() {
        if (taskDao == null) return; // Safety check

        morningTasks.clear();
        afternoonTasks.clear();
        nightTasks.clear();

        List<Task> morning = taskDao.getTasksByCategory("morning");
        List<Task> afternoon = taskDao.getTasksByCategory("afternoon");
        List<Task> night = taskDao.getTasksByCategory("night");

        if (morning != null) morningTasks.addAll(morning);
        if (afternoon != null) afternoonTasks.addAll(afternoon);
        if (night != null) nightTasks.addAll(night);
    }

    public void refreshTasks() {
        loadTasksFromDatabase();
    }

    public long addTask(Task task) {
        if (taskDao == null) return -1; // Safety check

        // Add to in-memory list immediately for instant UI update
        addTaskToMemory(task);

        // Persist to database (allowMainThreadQueries is enabled in TaskDatabase)
        long id = taskDao.insert(task);

        // Update the task object with the real ID (task is already in the list by reference)
        task.id = (int) id;

        return id;
    }

    private void addTaskToMemory(Task task) {
        // Add task to appropriate time category list immediately
        switch (task.timeCategory) {
            case "morning":
                morningTasks.add(task);
                break;
            case "afternoon":
                afternoonTasks.add(task);
                break;
            case "night":
                nightTasks.add(task);
                break;
        }
    }


    public void updateTask(Task task) {
        if (taskDao == null) return; // Safety check

        // Update database
        taskDao.update(task);

        // Update in-memory lists immediately
        updateTaskInMemory(task);
    }

    private void updateTaskInMemory(Task task) {
        // Remove from all lists first
        morningTasks.removeIf(t -> t.id == task.id);
        afternoonTasks.removeIf(t -> t.id == task.id);
        nightTasks.removeIf(t -> t.id == task.id);

        // Add to appropriate list based on current timeCategory
        switch (task.timeCategory) {
            case "morning":
                morningTasks.add(task);
                break;
            case "afternoon":
                afternoonTasks.add(task);
                break;
            case "night":
                nightTasks.add(task);
                break;
        }
    }

    public void deleteTask(Task task) {
        if (taskDao == null) return; // Safety check

        // Soft delete - move to trash instead of permanent deletion
        taskDao.softDelete(task.id, System.currentTimeMillis());

        // Remove from in-memory lists immediately
        removeTaskFromMemory(task);
    }

    private void removeTaskFromMemory(Task task) {
        morningTasks.removeIf(t -> t.id == task.id);
        afternoonTasks.removeIf(t -> t.id == task.id);
        nightTasks.removeIf(t -> t.id == task.id);
    }
    
    public void restoreTask(Task task) {
        if (taskDao == null) return; // Safety check

        taskDao.restore(task.id);

        // Reload from DB to get fresh data and add back to memory
        Task restoredTask = taskDao.getTaskById(task.id);
        if (restoredTask != null) {
            addTaskToMemory(restoredTask);
        }
    }
    
    public void permanentlyDeleteTask(Task task) {
        if (taskDao == null) return; // Safety check

        taskDao.delete(task);

        // Remove from in-memory lists immediately
        removeTaskFromMemory(task);
    }

    public List<Task> getAllTasks() {
        if (taskDao == null) return new ArrayList<>(); // Safety check
        return taskDao.getAllTasks();
    }

    public Task getTaskById(int id) {
        if (taskDao == null) return null; // Safety check
        return taskDao.getTaskById(id);
    }
    
    /**
     * Search tasks by name
     * @param query Search query string
     * @return List of tasks matching the query
     */
    public List<Task> searchTasks(String query) {
        if (taskDao == null) return new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();
        return taskDao.searchTasks(query.trim());
    }

    /**
     * Clear all completed tasks from the database and memory
     */
    public void clearCompletedTasks(Context context) {
        if (taskDao == null) return;
        
        // Remove completed tasks from memory
        morningTasks.removeIf(t -> t.isComplete);
        afternoonTasks.removeIf(t -> t.isComplete);
        nightTasks.removeIf(t -> t.isComplete);
        
        // Delete completed tasks from database
        taskDao.deleteCompletedTasks();
    }
    
    /**
     * Clear all tasks from the database and memory
     */
    public void clearAllTasks(Context context) {
        if (taskDao == null) return;
        
        // Clear memory
        morningTasks.clear();
        afternoonTasks.clear();
        nightTasks.clear();
        
        // Clear database
        taskDao.deleteAllTasks();
    }
}