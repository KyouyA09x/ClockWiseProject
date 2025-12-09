package com.example.mainactivity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final int ADD_TASK_REQUEST = 1;

    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private Button editButton;
    private ImageButton addButton;
    private LinearLayout morningTasksContainer;
    private LinearLayout afternoonTasksContainer;
    private LinearLayout nightTasksContainer;
    private TextView emptyTasksText;
    private TextView morningTasksHeader;
    private TextView afternoonTasksHeader;
    private TextView nightTasksHeader;
    private TextView taskCountText;
    private ProgressBar progressBar;
    private TextView completionText;

    private TaskRepository taskRepository;
    private boolean isEditMode = false;
    private View currentlyOpenTaskView = null;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        menuButton = findViewById(R.id.menuButton);
        editButton = findViewById(R.id.editButton);
        addButton = findViewById(R.id.addButton);
        morningTasksContainer = findViewById(R.id.morningTasksContainer);
        afternoonTasksContainer = findViewById(R.id.afternoonTasksContainer);
        nightTasksContainer = findViewById(R.id.nightTasksContainer);
        emptyTasksText = findViewById(R.id.emptyTasksText);
        morningTasksHeader = findViewById(R.id.morningTasksHeader);
        afternoonTasksHeader = findViewById(R.id.afternoonTasksHeader);
        nightTasksHeader = findViewById(R.id.nightTasksHeader);
        taskCountText = findViewById(R.id.taskCountText);
        progressBar = findViewById(R.id.progressBar);
        completionText = findViewById(R.id.completionText);

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        // Request notification permission for Android 13+
        requestNotificationPermission();

        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        Button calendarHistoryButton = headerView.findViewById(R.id.nav_calendar_history);
        calendarHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        editButton.setOnClickListener(v -> toggleEditMode());
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivityForResult(intent, ADD_TASK_REQUEST);
        });

        updateTaskLists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks from database
        taskRepository.refreshTasks();
        updateTaskLists();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_TASK_REQUEST && resultCode == RESULT_OK) {
            updateTaskLists();
        }
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        editButton.setText(isEditMode ? "Done" : "Edit");

        if (!isEditMode && currentlyOpenTaskView != null) {
            closeDeleteButton(currentlyOpenTaskView, false);
        }

        animateAllTasks(true);
    }

    private void animateAllTasks(boolean animate) {
        long duration = animate ? 300 : 0;

        for (int i = 0; i < morningTasksContainer.getChildCount(); i++) {
            animateTaskView(morningTasksContainer.getChildAt(i), duration);
        }
        for (int i = 0; i < afternoonTasksContainer.getChildCount(); i++) {
            animateTaskView(afternoonTasksContainer.getChildAt(i), duration);
        }
        for (int i = 0; i < nightTasksContainer.getChildCount(); i++) {
            animateTaskView(nightTasksContainer.getChildAt(i), duration);
        }
    }

    private void animateTaskView(View taskView, long duration) {
        ImageButton removeTaskButton = taskView.findViewById(R.id.removeTaskButton);
        ImageButton editTaskButton = taskView.findViewById(R.id.editTaskButton);
        View timeLayout = taskView.findViewById(R.id.timeLayout);

        if (isEditMode) {
            timeLayout.animate().alpha(0f).setDuration(duration).withEndAction(() -> timeLayout.setVisibility(View.GONE));

            removeTaskButton.setVisibility(View.VISIBLE);
            removeTaskButton.setAlpha(0f);
            removeTaskButton.animate().alpha(1f).setDuration(duration).start();

            editTaskButton.setVisibility(View.VISIBLE);
            editTaskButton.setAlpha(0f);
            editTaskButton.animate().alpha(1f).setDuration(duration).start();

        } else {
            timeLayout.setVisibility(View.VISIBLE);
            timeLayout.setAlpha(0f);
            timeLayout.animate().alpha(1f).setDuration(duration).start();

            removeTaskButton.animate().alpha(0f).setDuration(duration).withEndAction(() -> removeTaskButton.setVisibility(View.GONE));
            editTaskButton.animate().alpha(0f).setDuration(duration).withEndAction(() -> editTaskButton.setVisibility(View.GONE));
        }
    }

    private void updateTaskLists() {
        ArrayList<Task> morningTasks = taskRepository.morningTasks;
        ArrayList<Task> afternoonTasks = taskRepository.afternoonTasks;
        ArrayList<Task> nightTasks = taskRepository.nightTasks;

        sortTasks(morningTasks);
        sortTasks(afternoonTasks);
        sortTasks(nightTasks);

        morningTasksContainer.removeAllViews();
        afternoonTasksContainer.removeAllViews();
        nightTasksContainer.removeAllViews();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String todayDate = sdf.format(new java.util.Date());

        int totalTasks = 0;
        int completedTasks = 0;
        boolean hasTasksForToday = false;

        for (Task task : morningTasks) {
            if (task.date != null && task.date.equals(todayDate)) {
                morningTasksContainer.addView(createTaskView(task));
                totalTasks++;
                if (task.isComplete) completedTasks++;
                hasTasksForToday = true;
            }
        }
        for (Task task : afternoonTasks) {
            if (task.date != null && task.date.equals(todayDate)) {
                afternoonTasksContainer.addView(createTaskView(task));
                totalTasks++;
                if (task.isComplete) completedTasks++;
                hasTasksForToday = true;
            }
        }
        for (Task task : nightTasks) {
            if (task.date != null && task.date.equals(todayDate)) {
                nightTasksContainer.addView(createTaskView(task));
                totalTasks++;
                if (task.isComplete) completedTasks++;
                hasTasksForToday = true;
            }
        }

        taskCountText.setText("Task " + completedTasks + "/" + totalTasks);
        if(totalTasks > 0) {
            int progress = (completedTasks * 100) / totalTasks;
            progressBar.setProgress(progress);
            completionText.setText(progress + "% completed");
        } else {
            progressBar.setProgress(0);
            completionText.setText("0% completed");
        }

        if (!hasTasksForToday) {
            emptyTasksText.setVisibility(View.VISIBLE);
            morningTasksHeader.setVisibility(View.GONE);
            afternoonTasksHeader.setVisibility(View.GONE);
            nightTasksHeader.setVisibility(View.GONE);
            editButton.setVisibility(View.GONE);
            if (isEditMode) {
                isEditMode = false;
                editButton.setText("Edit");
            }
        } else {
            emptyTasksText.setVisibility(View.GONE);
            morningTasksHeader.setVisibility(morningTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
            afternoonTasksHeader.setVisibility(afternoonTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
            nightTasksHeader.setVisibility(nightTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
            editButton.setVisibility(View.VISIBLE);
        }
    }

    private View createTaskView(final Task task) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View taskView = inflater.inflate(R.layout.task_item, null, false);

        TextView taskNameTextView = taskView.findViewById(R.id.taskName);
        TextView taskTimeTextView = taskView.findViewById(R.id.taskTime);
        View taskCircle = taskView.findViewById(R.id.taskCircle);
        TextView repeatDaysTextView = taskView.findViewById(R.id.repeatDays);
        ImageButton removeTaskButton = taskView.findViewById(R.id.removeTaskButton);
        ImageButton editTaskButton = taskView.findViewById(R.id.editTaskButton);
        Button deleteButton = taskView.findViewById(R.id.deleteButton);
        final View taskContent = taskView.findViewById(R.id.taskContent);
        View timeLayout = taskView.findViewById(R.id.timeLayout);
        SwitchCompat taskSwitch = taskView.findViewById(R.id.taskSwitch);

        taskNameTextView.setText(task.name);
        taskTimeTextView.setText(String.format("%d:%02d %s", task.hour, task.minute, task.amPm));
        taskSwitch.setChecked(task.isAlarmOn);

        if (task.isComplete) {
            taskNameTextView.setPaintFlags(taskNameTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            taskView.setAlpha(0.6f);
        } else {
            taskNameTextView.setPaintFlags(taskNameTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            taskView.setAlpha(1.0f);
        }

        taskSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.isAlarmOn = isChecked;
            taskRepository.updateTask(task);
            if (isChecked) {
                AlarmHelper.scheduleTaskAlarm(this, task);
            } else {
                AlarmHelper.cancelTaskAlarm(this, task);
            }
            Toast.makeText(MainActivity.this, "Alarm for " + task.name + " is " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        deleteButton.setVisibility(View.GONE);
        animateTaskView(taskView, 0);

        removeTaskButton.setOnClickListener(v -> {
            if (currentlyOpenTaskView != null && currentlyOpenTaskView != taskView) {
                closeDeleteButton(currentlyOpenTaskView, true);
            }

            if (currentlyOpenTaskView == taskView) {
                closeDeleteButton(taskView, true);
            } else {
                openDeleteButton(taskView, true);
            }
        });

        deleteButton.setOnClickListener(v -> {
            AlarmHelper.cancelTaskAlarm(this, task);
            taskRepository.deleteTask(task);
            updateTaskLists();
        });

        taskContent.setOnClickListener(v -> {
            if (isEditMode) {
                if (currentlyOpenTaskView == taskView) {
                    closeDeleteButton(taskView, true);
                }
            } else {
                task.isComplete = !task.isComplete;
                taskRepository.updateTask(task);
                updateTaskLists();
            }
        });

        switch (task.urgency) {
            case "Low":
                taskCircle.setBackgroundResource(R.drawable.green_circle);
                break;
            case "Medium":
                taskCircle.setBackgroundResource(R.drawable.yellow_circle);
                break;
            case "High":
                taskCircle.setBackgroundResource(R.drawable.red_circle);
                break;
            default:
                taskCircle.setVisibility(View.INVISIBLE);
                break;
        }

        ArrayList<String> selectedDayNames = new ArrayList<>();
        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < task.selectedDays.length; i++) {
            if (task.selectedDays[i]) {
                selectedDayNames.add(daysOfWeek[i]);
            }
        }

        if (selectedDayNames.size() == 7) {
            repeatDaysTextView.setText("Every day");
            repeatDaysTextView.setVisibility(View.VISIBLE);
        } else if (!selectedDayNames.isEmpty()) {
            repeatDaysTextView.setText(String.join(", ", selectedDayNames));
            repeatDaysTextView.setVisibility(View.VISIBLE);
        } else {
            repeatDaysTextView.setVisibility(View.GONE);
        }

        return taskView;
    }

    private void openDeleteButton(View taskView, boolean animate) {
        final View taskContent = taskView.findViewById(R.id.taskContent);
        final Button deleteButton = taskView.findViewById(R.id.deleteButton);
        deleteButton.setVisibility(View.VISIBLE);

        taskContent.post(() -> {
            if (animate) {
                ObjectAnimator animation = ObjectAnimator.ofFloat(taskContent, "translationX", -deleteButton.getWidth());
                animation.setDuration(300);
                animation.start();
            } else {
                taskContent.setTranslationX(-deleteButton.getWidth());
            }
        });

        currentlyOpenTaskView = taskView;
    }

    private void closeDeleteButton(View taskView, boolean animate) {
        final View taskContent = taskView.findViewById(R.id.taskContent);
        final Button deleteButton = taskView.findViewById(R.id.deleteButton);

        if (animate) {
            ObjectAnimator animation = ObjectAnimator.ofFloat(taskContent, "translationX", 0f);
            animation.setDuration(300);
            animation.start();

            taskContent.postDelayed(() -> deleteButton.setVisibility(View.GONE), 300);
        } else {
            taskContent.setTranslationX(0f);
            deleteButton.setVisibility(View.GONE);
        }

        currentlyOpenTaskView = null;
    }

    private void sortTasks(ArrayList<Task> tasks) {
        Collections.sort(tasks, (t1, t2) -> {
            if (t1.hour != t2.hour) {
                return Integer.compare(t1.hour, t2.hour);
            } else {
                return Integer.compare(t1.minute, t2.minute);
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }
}
