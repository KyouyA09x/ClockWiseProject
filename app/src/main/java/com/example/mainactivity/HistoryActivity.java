package com.example.mainactivity;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView tvSelectedDate;
    private RecyclerView recyclerViewHistory;
    private ImageButton btnBack;

    // Use the HistoryAdapter we created in Step 2
    private HistoryAdapter adapter;

    // To store the tasks we find for the selected date
    private ArrayList<Task> tasksForSelectedDate;

    // Access to your global task data
    private TaskRepository taskRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize the Repository
        taskRepository = TaskRepository.getInstance();
        tasksForSelectedDate = new ArrayList<>();

        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        btnBack = findViewById(R.id.btnBack);

        // Setup RecyclerView
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(tasksForSelectedDate);
        recyclerViewHistory.setAdapter(adapter);

        // Back Button Logic
        btnBack.setOnClickListener(v -> finish());

        // Initialize with Today's date
        long todayInMillis = System.currentTimeMillis();

        // Set calendar view to today
        calendarView.setDate(todayInMillis);

        // Load data for today immediately
        updateHeaderAndLoadTasks(todayInMillis);

        // Calendar Click Logic
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);

            updateHeaderAndLoadTasks(calendar.getTimeInMillis());
        });
    }

    private void updateHeaderAndLoadTasks(long dateInMillis) {
        // 1. Update the Header Text
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);

        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        tvSelectedDate.setText("Tasks For " + displayFormat.format(calendar.getTime()));

        // 2. Load the tasks
        loadTasksForDate(dateInMillis);
    }

    private void loadTasksForDate(long dateInMillis) {
        // Format the selected date to match how we saved it in AddTaskActivity ("yyyy-MM-dd")
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateString = sdf.format(calendar.getTime());

        // Clear the old list
        tasksForSelectedDate.clear();

        // Search ALL lists (Morning, Afternoon, Night) for matches
        searchList(taskRepository.morningTasks, selectedDateString);
        searchList(taskRepository.afternoonTasks, selectedDateString);
        searchList(taskRepository.nightTasks, selectedDateString);

        // Update the UI
        adapter.notifyDataSetChanged();

        // Optional: Show a message if empty
        if (tasksForSelectedDate.isEmpty()) {
            // You can comment this out if the Toast is annoying
            Toast.makeText(this, "No tasks found for " + selectedDateString, Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to check a list for tasks matching the date
    private void searchList(ArrayList<Task> list, String dateString) {
        for (Task task : list) {
            // Check if task has a date AND if it matches the selected date
            if (task.date != null && task.date.equals(dateString)) {
                tasksForSelectedDate.add(task);
            }
        }
    }
}
