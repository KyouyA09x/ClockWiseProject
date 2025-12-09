package com.example.mainactivity;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private static TaskRepository instance;
    private final TaskDao taskDao;
    private final ExecutorService executorService;
    private final LiveData<List<Task>> allTasks;

    private TaskRepository(Application application) {
        TaskDatabase database = TaskDatabase.getDatabase(application);
        taskDao = database.taskDao();
        allTasks = taskDao.getAllTasks();
        executorService = Executors.newSingleThreadExecutor();
    }

    public static synchronized TaskRepository getInstance(Application application) {
        if (instance == null) {
            instance = new TaskRepository(application);
        }
        return instance;
    }

    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    public void addTask(Task task, final OnTaskAddedListener listener) {
        executorService.execute(() -> {
            long newId = taskDao.insertTask(task);
            task.id = (int) newId;
            if (listener != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> listener.onTaskAdded(task));
            }
        });
    }

    public void updateTask(Task task) {
        executorService.execute(() -> taskDao.updateTask(task));
    }

    public void deleteTask(Task task) {
        executorService.execute(() -> taskDao.deleteTask(task));
    }

    public interface OnTaskAddedListener {
        void onTaskAdded(Task task);
    }
}
