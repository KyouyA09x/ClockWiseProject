package com.example.mainactivity;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private RecyclerView tasksRecyclerView;
    private TextView selectedDateHeader;
    private LinearLayout emptyState;
    private ImageButton backButton;

    private TaskRepository taskRepository;
    private CalendarTaskAdapter adapter;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        setTheme(ThemeHelper.getThemeResource(this));
        setContentView(R.layout.activity_calendar);

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        initViews();
        setupCalendar();

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

        backButton.setOnClickListener(v -> finish());

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CalendarTaskAdapter(new ArrayList<>());
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

        if (tasksForDate.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            tasksRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            tasksRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}

