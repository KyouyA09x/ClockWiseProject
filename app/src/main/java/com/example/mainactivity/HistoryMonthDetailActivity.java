package com.example.mainactivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryMonthDetailActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextView monthTitle;
    private Button deleteAllButton;
    private RecyclerView tasksRecyclerView;
    private LinearLayout emptyState;

    private TaskRepository taskRepository;
    private HistoryTaskAdapter adapter;
    private List<Task> completedTasks;
    private int monthIndex;
    private String monthName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_month_detail);

        monthIndex = getIntent().getIntExtra("month_index", 0);
        monthName = getIntent().getStringExtra("month_name");
        if (monthName == null) monthName = "History";

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        initViews();
        loadCompletedTasks();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        monthTitle = findViewById(R.id.monthTitle);
        deleteAllButton = findViewById(R.id.deleteAllButton);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        emptyState = findViewById(R.id.emptyState);

        monthTitle.setText(monthName);

        backButton.setOnClickListener(v -> finish());

        deleteAllButton.setOnClickListener(v -> showDeleteAllConfirmation());

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        completedTasks = new ArrayList<>();
        adapter = new HistoryTaskAdapter(completedTasks, this::onDeleteTask);
        tasksRecyclerView.setAdapter(adapter);
    }

    private void loadCompletedTasks() {
        List<Task> allTasks = taskRepository.getAllTasks();
        completedTasks.clear();

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        for (Task task : allTasks) {
            if (task.isComplete && task.date != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date taskDate = sdf.parse(task.date);
                    if (taskDate != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(taskDate);
                        int taskMonth = cal.get(Calendar.MONTH);
                        int taskYear = cal.get(Calendar.YEAR);

                        // Match month (and optionally year - using current year)
                        if (taskMonth == monthIndex && taskYear == currentYear) {
                            completedTasks.add(task);
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        // Sort by date (most recent first)
        Collections.sort(completedTasks, (t1, t2) -> {
            if (t1.date == null || t2.date == null) return 0;
            return t2.date.compareTo(t1.date);
        });

        adapter.updateTasks(completedTasks);
        updateUI();
    }

    private void updateUI() {
        if (completedTasks.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            tasksRecyclerView.setVisibility(View.GONE);
            deleteAllButton.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            tasksRecyclerView.setVisibility(View.VISIBLE);
            deleteAllButton.setVisibility(View.VISIBLE);
        }
    }

    private void onDeleteTask(Task task) {
        ModernDialogHelper.showDestructiveDialog(
                this,
                "Delete Task?",
                "This action cannot be undone. {item} will be permanently removed from history.",
                task.name,
                R.drawable.ic_delete,
                () -> {
                    taskRepository.deleteTask(task);
                    loadCompletedTasks();
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                },
                null
        );
    }

    private void showDeleteAllConfirmation() {
        String message = "This action cannot be undone. All {count} completed tasks for " + monthName + " will be permanently removed.";
        
        ModernDialogHelper.showBulkDestructiveDialog(
                this,
                "Delete All Tasks?",
                message,
                completedTasks.size(),
                "Delete All",
                R.drawable.ic_delete,
                () -> {
                    for (Task task : new ArrayList<>(completedTasks)) {
                        taskRepository.deleteTask(task);
                    }
                    loadCompletedTasks();
                    Toast.makeText(this, "All tasks deleted", Toast.LENGTH_SHORT).show();
                },
                null
        );
    }

    public interface OnTaskDeleteListener {
        void onDelete(Task task);
    }
}

