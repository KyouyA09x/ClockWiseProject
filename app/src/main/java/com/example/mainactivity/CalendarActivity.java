package com.example.mainactivity;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarActivity extends BaseThemedActivity {

    private CalendarView calendarView;
    private RecyclerView tasksRecyclerView;
    private TextView selectedDateHeader;
    private LinearLayout emptyState;
    private ImageButton backButton;
    private TextView taskDatesInfo;
    private TextView totalTasksCount;
    private MaterialCardView taskDatesCard;

    private TaskRepository taskRepository;
    private CalendarTaskAdapter adapter;
    private String selectedDate;
    private Set<String> datesWithTasks = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        initViews();
        setupCalendar();
        loadDatesWithTasks();

        // Set today's date as default
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(Calendar.getInstance().getTime());
        loadTasksForDate(selectedDate);
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        selectedDateHeader = findViewById(R.id.selectedDateHeader);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);
        taskDatesInfo = findViewById(R.id.taskDatesInfo);
        totalTasksCount = findViewById(R.id.totalTasksCount);
        taskDatesCard = findViewById(R.id.taskDatesCard);

        backButton.setOnClickListener(v -> finish());

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CalendarTaskAdapter(new ArrayList<>());
        adapter.setOnTaskDeletedListener(() -> {
            loadTasksForDate(selectedDate);
            loadDatesWithTasks(); // Refresh the summary
        });
        tasksRecyclerView.setAdapter(adapter);
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDate = sdf.format(calendar.getTime());

            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            selectedDateHeader.setText("Tasks for " + displayFormat.format(calendar.getTime()));

            loadTasksForDate(selectedDate);
        });
    }
    
    /**
     * Load all dates that have tasks and update the summary card.
     */
    private void loadDatesWithTasks() {
        List<Task> allTasks = taskRepository.getAllTasks();
        datesWithTasks.clear();
        
        int upcomingTaskCount = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(Calendar.getInstance().getTime());
        
        for (Task task : allTasks) {
            if (task.date != null && !task.isComplete) {
                datesWithTasks.add(task.date);
                
                // Count future and today's tasks
                if (task.date.compareTo(todayDate) >= 0) {
                    upcomingTaskCount++;
                }
            }
        }
        
        // Update the summary card
        if (taskDatesInfo != null) {
            int daysWithTasks = datesWithTasks.size();
            if (daysWithTasks == 0) {
                taskDatesInfo.setText("No upcoming tasks scheduled");
            } else if (daysWithTasks == 1) {
                taskDatesInfo.setText("1 day has scheduled tasks");
            } else {
                taskDatesInfo.setText(daysWithTasks + " days have scheduled tasks");
            }
        }
        
        if (totalTasksCount != null) {
            totalTasksCount.setText(upcomingTaskCount + " upcoming");
        }
    }

    private void loadTasksForDate(String date) {
        List<Task> allTasks = taskRepository.getAllTasks();
        List<Task> tasksForDate = new ArrayList<>();

        for (Task task : allTasks) {
            // Only show ongoing (not completed) tasks
            if (task.date != null && task.date.equals(date) && !task.isComplete) {
                tasksForDate.add(task);
            }
        }

        adapter.updateTasks(tasksForDate);
        
        // Update header to show task count
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String formattedDate = displayFormat.format(sdf.parse(date));
            if (tasksForDate.isEmpty()) {
                selectedDateHeader.setText("Tasks for " + formattedDate);
            } else {
                selectedDateHeader.setText("Tasks for " + formattedDate + " (" + tasksForDate.size() + ")");
            }
        } catch (Exception e) {
            selectedDateHeader.setText("Tasks for " + date);
        }

        if (tasksForDate.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            tasksRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            tasksRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}

