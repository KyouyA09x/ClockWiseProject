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
        TaskDatabase database = TaskDatabase.getInstance(context);
        taskDao = database.taskDao();
        loadTasksFromDatabase();
    }

    private void loadTasksFromDatabase() {
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
        long id = taskDao.insert(task);
        task.id = (int) id;
        loadTasksFromDatabase();
        return id;
    }

    public void updateTask(Task task) {
        taskDao.update(task);
        loadTasksFromDatabase();
    }

    public void deleteTask(Task task) {
        taskDao.delete(task);
        loadTasksFromDatabase();
    }

    public List<Task> getAllTasks() {
        return taskDao.getAllTasks();
    }

    public Task getTaskById(int id) {
        return taskDao.getTaskById(id);
    }
}