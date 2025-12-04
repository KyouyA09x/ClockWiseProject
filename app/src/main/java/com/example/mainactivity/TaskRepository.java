package com.example.mainactivity;

import java.util.ArrayList;

public class TaskRepository {
    private static TaskRepository instance;
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
}