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
import java.util.List;
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
    
    // Cache for all tasks from LiveData
    private List<Task> allTasksCache = new ArrayList<>();
    private long currentSelectedDateInMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize the Repository correctly with Application context
        taskRepository = TaskRepository.getInstance(getApplication());
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
        currentSelectedDateInMillis = System.currentTimeMillis();
        calendarView.setDate(currentSelectedDateInMillis);
        updateHeader(currentSelectedDateInMillis);

        // Observe the LiveData to get updates whenever the database changes
        taskRepository.getAllTasks().observe(this, tasks -> {
            allTasksCache = tasks;
            // Reload for the currently selected date
            loadTasksForDate(currentSelectedDateInMillis);
        });

        // Calendar Click Logic
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            
            currentSelectedDateInMillis = calendar.getTimeInMillis();
            updateHeader(currentSelectedDateInMillis);
            loadTasksForDate(currentSelectedDateInMillis);
        });
    }

    private void updateHeader(long dateInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);

        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        tvSelectedDate.setText("Tasks For " + displayFormat.format(calendar.getTime()));
    }

    private void loadTasksForDate(long dateInMillis) {
        // Format the selected date to match how we saved it in AddTaskActivity ("yyyy-MM-dd")
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateString = sdf.format(calendar.getTime());

        // Clear the old list
        tasksForSelectedDate.clear();

        // Filter the cached list for tasks matching the date
        if (allTasksCache != null) {
            for (Task task : allTasksCache) {
                if (task.date != null && task.date.equals(selectedDateString)) {
                    tasksForSelectedDate.add(task);
                }
            }
        }

        // Update the UI
        adapter.notifyDataSetChanged();
    }
}
