package com.example.mainactivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final int ADD_TASK_REQUEST = 1;
    private static final int EDIT_TASK_REQUEST = 2;
    public static final String ACTION_TASK_COMPLETED = "com.example.mainactivity.TASK_COMPLETED";

    private DrawerLayout drawerLayout;
    private MaterialToolbar topBar;
    private NavigationView navigationView;
    private ExtendedFloatingActionButton fabAddTask;
    private View progressTracker;
    private View emptyStateCard;
    private View tasksContainerCard;
    private LinearLayout morningTasksContainer;
    private LinearLayout afternoonTasksContainer;
    private LinearLayout nightTasksContainer;
    private TextView emptyTasksText;
    private TextView morningTasksHeader;
    private TextView afternoonTasksHeader;
    private TextView nightTasksHeader;
    private TextView taskCountText;
    private LinearProgressIndicator progressBar;
    private TextView completionText;

    private TaskRepository taskRepository;
    private ArrayList<Task> completedTasksToday = new ArrayList<>();
    private boolean isReceiverRegistered = false;

    private BroadcastReceiver taskCompletionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh task list immediately when a task is completed
            taskRepository.refreshTasks();
            updateTaskLists();
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        setTheme(ThemeHelper.getThemeResource(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        drawerLayout = findViewById(R.id.drawerLayout);
        topBar = findViewById(R.id.topBar);
        navigationView = findViewById(R.id.navigationView);
        fabAddTask = findViewById(R.id.fabAddTask);
        progressTracker = findViewById(R.id.progressTracker);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        tasksContainerCard = findViewById(R.id.tasksContainerCard);
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

        // Disable edge swiping - only open via hamburger button
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // FAB - shows task type chooser
        fabAddTask.setOnClickListener(v -> showTaskTypeChooser());

        // Setup toolbar navigation
        if (topBar != null) {
            topBar.setNavigationOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });

            topBar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_calendar) {
                    startActivity(new Intent(MainActivity.this, CalendarActivity.class));
                    return true;
                }
                return false;
            });
        }

        // Lock drawer when closed to prevent edge swipe
        if (drawerLayout != null) {
            drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {}

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                }

                @Override
                public void onDrawerStateChanged(int newState) {}
            });
        }

        // Set up navigation drawer
        setupNavigationDrawer();

        // Progress tracker - shows completed tasks for today
        if (progressTracker != null) {
            progressTracker.setOnClickListener(v -> showCompletedTasksDialog());
        }

        updateTaskLists();
    }

    private void showTaskTypeChooser() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_task_type_chooser, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Reminder option
        View reminderOption = dialogView.findViewById(R.id.reminderOption);
        reminderOption.setOnClickListener(v -> {
            dialog.dismiss();
            showReminderBottomSheet(null);
        });

        // Focus Task option
        View focusTaskOption = dialogView.findViewById(R.id.focusTaskOption);
        focusTaskOption.setOnClickListener(v -> {
            dialog.dismiss();
            showFocusTaskBottomSheet(null);
        });

        dialog.show();
    }

    private void showReminderBottomSheet(Task taskToEdit) {
        AddReminderBottomSheet bottomSheet;
        if (taskToEdit != null) {
            bottomSheet = AddReminderBottomSheet.newInstance(taskToEdit);
        } else {
            bottomSheet = AddReminderBottomSheet.newInstance();
        }
        bottomSheet.setOnTaskSavedListener(() -> {
            taskRepository.refreshTasks();
            updateTaskLists();
        });
        bottomSheet.show(getSupportFragmentManager(), "AddReminderBottomSheet");
    }

    private void showFocusTaskBottomSheet(Task taskToEdit) {
        AddFocusTaskBottomSheet bottomSheet;
        if (taskToEdit != null) {
            bottomSheet = AddFocusTaskBottomSheet.newInstance(taskToEdit);
        } else {
            bottomSheet = AddFocusTaskBottomSheet.newInstance();
        }
        bottomSheet.setOnTaskSavedListener(() -> {
            taskRepository.refreshTasks();
            updateTaskLists();
        });
        bottomSheet.show(getSupportFragmentManager(), "AddFocusTaskBottomSheet");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks from database
        taskRepository.refreshTasks();
        updateTaskLists();

        // Register receiver for task completion (only if not already registered)
        if (!isReceiverRegistered) {
            try {
                IntentFilter filter = new IntentFilter(ACTION_TASK_COMPLETED);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(taskCompletionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver(taskCompletionReceiver, filter);
                }
                isReceiverRegistered = true;
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error registering receiver", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister receiver to prevent leaks (only if registered)
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(taskCompletionReceiver);
                isReceiverRegistered = false;
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error unregistering receiver", e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == ADD_TASK_REQUEST || requestCode == EDIT_TASK_REQUEST) && resultCode == RESULT_OK) {
            // Refresh tasks from database
            taskRepository.refreshTasks();
            updateTaskLists();
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

        // Get Today's Date to filter
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String todayDate = sdf.format(new java.util.Date());

        int totalTasks = 0;
        int completedTasks = 0;
        boolean hasTasksForToday = false;

        // Check Morning - ONLY show INCOMPLETE tasks (completed tasks go to History)
        for (Task task : morningTasks) {
            // FILTER: ONLY SHOW IF DATE MATCHES TODAY AND TASK IS NOT COMPLETE
            if (task.date != null && task.date.equals(todayDate) && !task.isComplete) {
                morningTasksContainer.addView(createTaskView(task));
                totalTasks++;
                hasTasksForToday = true;
            } else if (task.date != null && task.date.equals(todayDate) && task.isComplete) {
                // Count completed tasks for progress but don't display them
                completedTasks++;
                totalTasks++;
            }
        }
        // Check Afternoon - ONLY show INCOMPLETE tasks
        for (Task task : afternoonTasks) {
            if (task.date != null && task.date.equals(todayDate) && !task.isComplete) {
                afternoonTasksContainer.addView(createTaskView(task));
                totalTasks++;
                hasTasksForToday = true;
            } else if (task.date != null && task.date.equals(todayDate) && task.isComplete) {
                completedTasks++;
                totalTasks++;
            }
        }
        // Check Night - ONLY show INCOMPLETE tasks
        for (Task task : nightTasks) {
            if (task.date != null && task.date.equals(todayDate) && !task.isComplete) {
                nightTasksContainer.addView(createTaskView(task));
                totalTasks++;
                hasTasksForToday = true;
            } else if (task.date != null && task.date.equals(todayDate) && task.isComplete) {
                completedTasks++;
                totalTasks++;
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
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (tasksContainerCard != null) tasksContainerCard.setVisibility(View.GONE);
            morningTasksHeader.setVisibility(View.GONE);
            afternoonTasksHeader.setVisibility(View.GONE);
            nightTasksHeader.setVisibility(View.GONE);
        } else {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.GONE);
            if (tasksContainerCard != null) tasksContainerCard.setVisibility(View.VISIBLE);
            morningTasksHeader.setVisibility(morningTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
            afternoonTasksHeader.setVisibility(afternoonTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
            nightTasksHeader.setVisibility(nightTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private View createTaskView(final Task task) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View taskView = inflater.inflate(R.layout.task_item, null, false);

        // Set layout params with margins for spacing between tasks
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginHorizontal = (int) (16 * getResources().getDisplayMetrics().density);
        int marginVertical = (int) (12 * getResources().getDisplayMetrics().density);
        layoutParams.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
        taskView.setLayoutParams(layoutParams);

        TextView taskNameTextView = taskView.findViewById(R.id.taskName);
        TextView taskTimeTextView = taskView.findViewById(R.id.taskTime);
        android.widget.ImageView taskTypeIcon = taskView.findViewById(R.id.taskTypeIcon);
        TextView repeatDaysTextView = taskView.findViewById(R.id.repeatDays);
        final View taskContent = taskView.findViewById(R.id.taskContent);
        SwitchCompat taskSwitch = taskView.findViewById(R.id.taskSwitch);

        // Set task background with colored border based on urgency
        if (taskContent != null && task.urgency != null) {
            switch (task.urgency.toLowerCase()) {
                case "high":
                    taskContent.setBackgroundResource(R.drawable.task_background_high);
                    break;
                case "medium":
                    taskContent.setBackgroundResource(R.drawable.task_background_medium);
                    break;
                case "low":
                    taskContent.setBackgroundResource(R.drawable.task_background_low);
                    break;
                default:
                    taskContent.setBackgroundResource(R.drawable.task_background_none);
                    break;
            }
        } else if (taskContent != null) {
            taskContent.setBackgroundResource(R.drawable.task_background_none);
        }

        // Hide edit mode layout completely (we removed edit mode)
        View normalModeLayout = taskView.findViewById(R.id.normalModeLayout);
        View editModeLayout = taskView.findViewById(R.id.editModeLayout);
        if (normalModeLayout != null) normalModeLayout.setVisibility(View.VISIBLE);
        if (editModeLayout != null) editModeLayout.setVisibility(View.GONE);

        taskNameTextView.setText(task.name);

        // Show time range for Focus Tasks, single time for Reminders
        if (task.isFocusTask()) {
            String timeRange = String.format("%d:%02d %s → %d:%02d %s",
                    task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                    task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
            taskTimeTextView.setText(timeRange);
        } else {
            taskTimeTextView.setText(String.format("%d:%02d %s", task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
        }
        taskSwitch.setChecked(task.isAlarmOn);

        if (task.isComplete) {
            taskNameTextView.setPaintFlags(taskNameTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            taskView.setAlpha(0.6f);
        } else {
            taskNameTextView.setPaintFlags(taskNameTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            taskView.setAlpha(1.0f);
        }

        // Make task clickable for editing
        taskContent.setOnClickListener(v -> openTaskForEditing(task));

        taskSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.isAlarmOn = isChecked;
            taskRepository.updateTask(task);
            if (isChecked) {
                if (task.isFocusTask()) {
                    AlarmHelper.scheduleFocusTaskAlarms(this, task);
                } else {
                    AlarmHelper.scheduleTaskAlarm(this, task);
                }
            } else {
                if (task.isFocusTask()) {
                    AlarmHelper.cancelFocusTaskAlarms(this, task);
                } else {
                    AlarmHelper.cancelTaskAlarm(this, task);
                }
            }
            Toast.makeText(MainActivity.this, "Alarm for " + task.name + " is " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        // Set task type icon (Reminder or Focus Task)
        if (task.isFocusTask()) {
            taskTypeIcon.setImageResource(R.drawable.ic_focus_task);
        } else {
            taskTypeIcon.setImageResource(R.drawable.ic_reminder);
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

    private void openTaskForEditing(Task task) {
        if (task.isFocusTask()) {
            showFocusTaskBottomSheet(task);
        } else {
            showReminderBottomSheet(task);
        }
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

    private void setupNavigationDrawer() {
        if (navigationView == null) return;

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Already on home
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(MainActivity.this, CalendarActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_about) {
                Toast.makeText(this, "ClockWise v1.0 - Your smart task manager", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void showCompletedTasksDialog() {
        // Create and show a dialog to display completed tasks for today
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_completed_tasks, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Make dialog appear as small centered overlay
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            params.width = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }

        LinearLayout completedTasksContainer = dialogView.findViewById(R.id.completedTasksContainer);
        View emptyCompletedTasksText = dialogView.findViewById(R.id.emptyCompletedTasksText);

        // Clear previous views
        completedTasksContainer.removeAllViews();

        // Get today's date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String todayDate = sdf.format(new java.util.Date());

        // Collect completed tasks for today
        completedTasksToday.clear();
        ArrayList<Task> allTasks = new ArrayList<>();
        allTasks.addAll(taskRepository.morningTasks);
        allTasks.addAll(taskRepository.afternoonTasks);
        allTasks.addAll(taskRepository.nightTasks);

        for (Task task : allTasks) {
            if (task.date != null && task.date.equals(todayDate) && task.isComplete) {
                completedTasksToday.add(task);
            }
        }

        if (completedTasksToday.isEmpty()) {
            emptyCompletedTasksText.setVisibility(View.VISIBLE);
            dialogView.findViewById(R.id.completedTasksScrollView).setVisibility(View.GONE);
        } else {
            emptyCompletedTasksText.setVisibility(View.GONE);
            dialogView.findViewById(R.id.completedTasksScrollView).setVisibility(View.VISIBLE);
            for (Task task : completedTasksToday) {
                completedTasksContainer.addView(createCompletedTaskView(task));
            }
        }

        dialogView.findViewById(R.id.closeButton).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private View createCompletedTaskView(Task task) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View taskView = inflater.inflate(R.layout.item_completed_task, null, false);

        // Set layout params with proper margins for spacing between items
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginVertical = (int) (16 * getResources().getDisplayMetrics().density);
        int marginHorizontal = (int) (12 * getResources().getDisplayMetrics().density);
        layoutParams.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
        taskView.setLayoutParams(layoutParams);

        TextView taskNameTextView = taskView.findViewById(R.id.completedTaskName);
        TextView taskTimeTextView = taskView.findViewById(R.id.completedTaskTime);

        taskNameTextView.setText(task.name);

        // Show time range for Focus Tasks, single time for Reminders
        if (task.isFocusTask()) {
            String timeRange = String.format("%d:%02d %s → %d:%02d %s",
                    task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                    task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
            taskTimeTextView.setText(timeRange);
        } else {
            taskTimeTextView.setText(String.format("%d:%02d %s", task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
        }


        return taskView;
    }
}
