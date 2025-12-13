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
    private com.google.android.material.navigation.NavigationView navigationView;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabAddTask;
    private View progressTracker;
    private View emptyStateCard;
    private View tasksContainerCard;
    private View upcomingFocusCard;
    private LinearLayout upcomingFocusTasksContainer;
    private TextView upcomingCount;
    private com.google.android.material.button.MaterialButton viewAllUpcomingButton;
    private LinearLayout focusTasksSection;
    private LinearLayout focusTasksContainer;
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
    private android.view.MenuItem calendarMenuItem;

    private TaskRepository taskRepository;
    private ArrayList<Task> completedTasksToday = new ArrayList<>();
    private boolean isReceiverRegistered = false;

    private final BroadcastReceiver taskCompletionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh task list immediately when a task is completed
            taskRepository.refreshTasks();
            refreshAllFragments();
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
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Setup bottom navigation
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_tasks) {
                    loadFragment(new TasksContainerFragment());
                    return true;
                } else if (itemId == R.id.navigation_notepad) {
                    loadFragment(new NotepadFragment());
                    return true;
                }
                return false;
            });
        }

        // Load default fragment (Tasks)
        if (savedInstanceState == null) {
            loadFragment(new TasksContainerFragment());
        }

        // Disable edge swiping - only open via hamburger button
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // FAB - shows task type chooser
        if (fabAddTask != null) {
            fabAddTask.setOnClickListener(v -> showTaskTypeChooser());
        }

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

        // Setup navigation drawer
        setupNavigationDrawer();
    }

    private void refreshAllFragments() {
        // Refresh fragments when activity resumes
        for (androidx.fragment.app.Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof TasksContainerFragment) {
                ((TasksContainerFragment) fragment).refreshTasks();
            } else if (fragment instanceof CurrentTasksFragment) {
                ((CurrentTasksFragment) fragment).refreshTasks();
            } else if (fragment instanceof UpcomingTasksFragment) {
                ((UpcomingTasksFragment) fragment).refreshTasks();
            }
        }
    }

    private void loadFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void showCompletedDialog() {
        showCompletedTasksDialog();
    }
    
    public void triggerShowCompletedTasksDialog() {
        showCompletedTasksDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        taskRepository.refreshTasks();
        refreshAllFragments();
    }

    public void showTaskTypeChooser() {
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
            refreshAllFragments();
        });
        bottomSheet.show(getSupportFragmentManager(), "AddReminderBottomSheet");
    }

    private void showFocusTaskBottomSheet(Task taskToEdit) {
        AddFocusTaskBottomSheet bottomSheet;
        if (taskToEdit != null) {
            bottomSheet = AddFocusTaskBottomSheet.newInstance(taskToEdit);
            // Check if this is a quick task (has today's date pre-set)
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            String today = sdf.format(new java.util.Date());
            if (taskToEdit.date != null && taskToEdit.date.equals(today) && taskToEdit.id == 0) {
                // It's a new task with today's date - mark as quick task
                Bundle args = new Bundle();
                args.putParcelable("TASK", taskToEdit);
                args.putBoolean("QUICK_TASK", true);
                bottomSheet = new AddFocusTaskBottomSheet();
                bottomSheet.setArguments(args);
            }
        } else {
            bottomSheet = AddFocusTaskBottomSheet.newInstance();
        }
        bottomSheet.setOnTaskSavedListener(() -> {
            taskRepository.refreshTasks();
            refreshAllFragments();
        });
        bottomSheet.show(getSupportFragmentManager(), "AddFocusTaskBottomSheet");
    }

    public void showQuickTaskBottomSheet() {
        // Show Material dialog for task type selection with better styling
        String[] taskTypes = {"⏰  Reminder", "🎯  Focus Task"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Quick Task")
                .setItems(taskTypes, (dialog, which) -> {
                    if (which == 0) {
                        // Create quick reminder with today's date
                        Task quickTask = new Task();
                        quickTask.taskType = "reminder";
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                        quickTask.date = sdf.format(new java.util.Date());
                        quickTask.hour = 9;
                        quickTask.minute = 0;
                        quickTask.amPm = "AM";
                        quickTask.isAlarmOn = true;
                        quickTask.urgency = "None";
                        quickTask.selectedDays = new boolean[7];
                        
                        showReminderBottomSheet(quickTask);
                    } else if (which == 1) {
                        // Create quick focus task with today's date
                        Task quickTask = new Task();
                        quickTask.taskType = "focus";
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                        quickTask.date = sdf.format(new java.util.Date());
                        quickTask.hour = 9;
                        quickTask.minute = 0;
                        quickTask.amPm = "AM";
                        quickTask.endHour = 10;
                        quickTask.endMinute = 0;
                        quickTask.endAmPm = "AM";
                        quickTask.isAlarmOn = true;
                        quickTask.urgency = "None";
                        quickTask.selectedDays = new boolean[7];
                        
                        showFocusTaskBottomSheet(quickTask);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void openQuickTaskInNotepad() {
        // Switch to notepad tab
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.navigation_notepad);
        }

        // Create a new note with "Quick Task" template
        Note quickNote = new Note();
        quickNote.title = "Quick Task";
        quickNote.description = "";

        // Show add note dialog after fragment transition
        new android.os.Handler().postDelayed(() -> {
            for (androidx.fragment.app.Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment instanceof NotepadFragment && fragment.isVisible()) {
                    AddNoteDialog dialog = new AddNoteDialog(this, quickNote, () -> {
                        ((NotepadFragment) fragment).refreshNotes();
                    });
                    dialog.show();
                    break;
                }
            }
        }, 300); // Small delay to allow fragment transition
    }

    public void showNotepadToConvertToTask() {
        // Switch to notepad tab
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.navigation_notepad);
        }
        
        // Enable selection mode in notepad after fragment transition
        new android.os.Handler().postDelayed(() -> {
            for (androidx.fragment.app.Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment instanceof NotepadFragment && fragment.isVisible()) {
                    ((NotepadFragment) fragment).enableSelectionMode();
                    break;
                }
            }
        }, 300);
    }

    private View createFocusTaskView(final Task task) {
        if (task == null) return new View(this);
        
        LayoutInflater inflater = LayoutInflater.from(this);
        View taskView = inflater.inflate(R.layout.focus_task_item, null, false);

        // Set layout params
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginHorizontal = (int) (16 * getResources().getDisplayMetrics().density);
        int marginVertical = (int) (12 * getResources().getDisplayMetrics().density);
        layoutParams.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
        taskView.setLayoutParams(layoutParams);

        // Get views
        TextView taskNameTextView = taskView.findViewById(R.id.taskName);
        TextView taskTimeTextView = taskView.findViewById(R.id.taskTime);
        TextView fullDateText = taskView.findViewById(R.id.fullDateText);
        TextView timeRangeText = taskView.findViewById(R.id.timeRangeText);
        TextView repeatDaysText = taskView.findViewById(R.id.repeatDaysText);
        TextView priorityText = taskView.findViewById(R.id.priorityText);
        TextView alarmStatusText = taskView.findViewById(R.id.alarmStatusText);
        android.widget.ImageView expandIcon = taskView.findViewById(R.id.expandIcon);
        
        if (taskNameTextView == null || taskTimeTextView == null) return taskView;
        android.widget.ImageView priorityIcon = taskView.findViewById(R.id.priorityIcon);
        View expandableDetails = taskView.findViewById(R.id.expandableDetails);
        View repeatSection = taskView.findViewById(R.id.repeatSection);
        View prioritySection = taskView.findViewById(R.id.prioritySection);
        SwitchCompat taskSwitch = taskView.findViewById(R.id.taskSwitch);
        final View taskContent = taskView.findViewById(R.id.taskContent);

        // Set basic info
        taskNameTextView.setText(task.name);
        String timeRange = String.format(java.util.Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
        taskTimeTextView.setText(timeRange);

        // Set verbose date information
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(task.date);
            if (date != null && fullDateText != null) {
                fullDateText.setText(outputFormat.format(date));
            }
        } catch (Exception e) {
            if (fullDateText != null) fullDateText.setText(task.date);
        }

        // Calculate duration
        int startMinutes = convertTo24Hour(task.hour, task.amPm) * 60 + task.minute;
        int endMinutes = convertTo24Hour(task.endHour, task.endAmPm) * 60 + task.endMinute;
        int durationMinutes = endMinutes - startMinutes;
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        String duration = hours > 0 ? String.format(java.util.Locale.getDefault(), "%d hour%s", hours, hours > 1 ? "s" : "") : "";
        if (minutes > 0) {
            duration += (hours > 0 ? " " : "") + String.format(java.util.Locale.getDefault(), "%d min", minutes);
        }
        if (timeRangeText != null) {
            timeRangeText.setText(timeRange + " (" + duration + ")");
        }

        // Set repeat days
        if (task.selectedDays != null && repeatDaysText != null && repeatSection != null) {
            ArrayList<String> selectedDayNames = new ArrayList<>();
            String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (int i = 0; i < task.selectedDays.length; i++) {
                if (task.selectedDays[i]) {
                    selectedDayNames.add(daysOfWeek[i]);
                }
            }
            if (!selectedDayNames.isEmpty()) {
                repeatSection.setVisibility(View.VISIBLE);
                repeatDaysText.setText("Repeats: " + String.join(", ", selectedDayNames));
            }
        }

        // Set priority
        if (task.urgency != null && !task.urgency.equals("None") && prioritySection != null && priorityText != null && priorityIcon != null) {
            prioritySection.setVisibility(View.VISIBLE);
            priorityText.setText("Priority: " + task.urgency);
            switch (task.urgency.toLowerCase()) {
                case "high":
                    priorityIcon.setImageResource(R.drawable.ic_priority_high);
                    priorityIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.error));
                    break;
                case "medium":
                    priorityIcon.setImageResource(R.drawable.ic_priority_medium);
                    priorityIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.warning));
                    break;
                case "low":
                    priorityIcon.setImageResource(R.drawable.ic_priority_low);
                    priorityIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.success));
                    break;
            }
        }

        // Set alarm status
        if (alarmStatusText != null) {
            alarmStatusText.setText(task.isAlarmOn ? "Notifications enabled" : "Notifications disabled");
        }

        // Handle expand/collapse with smooth animation
        final boolean[] isExpanded = {false};
        if (taskContent != null && expandableDetails != null && expandIcon != null) {
            taskContent.setOnClickListener(v -> {
                if (isExpanded[0]) {
                    // Collapse
                    android.view.ViewGroup.LayoutParams params = expandableDetails.getLayoutParams();
                    android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(expandableDetails.getHeight(), 0);
                    animator.addUpdateListener(animation -> {
                        params.height = (int) animation.getAnimatedValue();
                        expandableDetails.setLayoutParams(params);
                    });
                    animator.addListener(new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            expandableDetails.setVisibility(View.GONE);
                        }
                    });
                    animator.setDuration(300);
                    animator.start();
                    expandIcon.animate().rotation(90).setDuration(300).start();
                    isExpanded[0] = false;
                } else {
                    // Expand
                    expandableDetails.setVisibility(View.VISIBLE);
                    expandableDetails.measure(
                            View.MeasureSpec.makeMeasureSpec(((View)expandableDetails.getParent()).getWidth(), View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    );
                    int targetHeight = expandableDetails.getMeasuredHeight();
                    android.view.ViewGroup.LayoutParams params = expandableDetails.getLayoutParams();
                    params.height = 0;
                    expandableDetails.setLayoutParams(params);
                    android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(0, targetHeight);
                    animator.addUpdateListener(animation -> {
                        params.height = (int) animation.getAnimatedValue();
                        expandableDetails.setLayoutParams(params);
                    });
                    animator.setDuration(300);
                    animator.start();
                    expandIcon.animate().rotation(270).setDuration(300).start();
                    isExpanded[0] = true;
                }
            });
        }

        // Delete button
        com.google.android.material.button.MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Focus Session")
                    .setMessage("Are you sure you want to delete \"" + task.name + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        AlarmHelper.cancelFocusTaskAlarms(this, task);
                        taskRepository.deleteTask(task);
                        taskRepository.refreshTasks();
                        refreshAllFragments();
                        Toast.makeText(this, "Focus session deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }

        // Handle task switch
        if (taskSwitch != null) {
            taskSwitch.setChecked(task.isAlarmOn);
            taskSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.isAlarmOn = isChecked;
                taskRepository.updateTask(task);
                if (alarmStatusText != null) {
                    alarmStatusText.setText(isChecked ? "Notifications enabled" : "Notifications disabled");
                }
                if (isChecked) {
                    AlarmHelper.scheduleFocusTaskAlarms(this, task);
                    Toast.makeText(this, "🔔 Alerts enabled for " + task.name, Toast.LENGTH_SHORT).show();
                } else {
                    AlarmHelper.cancelFocusTaskAlarms(this, task);
                    Toast.makeText(this, "🔕 Alerts disabled for " + task.name, Toast.LENGTH_SHORT).show();
                }
            });
        }

        return taskView;
    }

    private int convertTo24Hour(int hour, String amPm) {
        if (amPm != null && amPm.equals("AM")) {
            return hour == 12 ? 0 : hour;
        } else {
            return hour == 12 ? 12 : hour + 12;
        }
    }

    private View createUpcomingFocusTaskView(final Task task) {
        if (task == null) return new View(this);
        
        LayoutInflater inflater = LayoutInflater.from(this);
        View taskView = inflater.inflate(R.layout.task_item, null, false);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        layoutParams.setMargins(margin, margin, margin, margin);
        taskView.setLayoutParams(layoutParams);

        TextView taskNameTextView = taskView.findViewById(R.id.taskName);
        TextView taskTimeTextView = taskView.findViewById(R.id.taskTime);
        android.widget.ImageView taskTypeIcon = taskView.findViewById(R.id.taskTypeIcon);
        View taskContent = taskView.findViewById(R.id.taskContent);
        View normalModeLayout = taskView.findViewById(R.id.normalModeLayout);
        View editModeLayout = taskView.findViewById(R.id.editModeLayout);

        if (normalModeLayout != null) normalModeLayout.setVisibility(View.GONE);
        if (editModeLayout != null) editModeLayout.setVisibility(View.GONE);

        if (taskNameTextView != null) taskNameTextView.setText(task.name);

        // Show date and time for upcoming tasks
        if (taskTimeTextView != null) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(task.date);
                String dateStr = date != null ? outputFormat.format(date) : task.date;
                String timeRange = String.format(java.util.Locale.getDefault(), "%s • %d:%02d %s",
                        dateStr, task.hour, task.minute, task.amPm != null ? task.amPm : "AM");
                taskTimeTextView.setText(timeRange);
            } catch (Exception e) {
                taskTimeTextView.setText(String.format(java.util.Locale.getDefault(), "%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
            }
        }

        if (taskTypeIcon != null) taskTypeIcon.setImageResource(R.drawable.ic_focus_task);

        if (taskContent != null) {
            taskContent.setBackgroundResource(R.drawable.task_background_none);
            taskContent.setOnClickListener(v -> {
                // Open edit activity
                Intent intent = new Intent(this, EditFocusTaskActivity.class);
                intent.putExtra("task_id", task.id);
                startActivity(intent);
            });
        }

        return taskView;
    }

    private void updateCalendarBadge(int count) {
        if (topBar != null && topBar.getMenu() != null) {
            android.view.MenuItem calendarItem = topBar.getMenu().findItem(R.id.action_calendar);
            if (calendarItem != null) {
                if (count > 0) {
                    // Create badge drawable
                    android.graphics.drawable.LayerDrawable badge = (android.graphics.drawable.LayerDrawable) 
                            ContextCompat.getDrawable(this, R.drawable.notification_badge);
                    
                    // Combine calendar icon with badge
                    android.graphics.drawable.Drawable calendarIcon = ContextCompat.getDrawable(this, R.drawable.ic_calendar);
                    android.graphics.drawable.Drawable[] layers = {calendarIcon, badge};
                    android.graphics.drawable.LayerDrawable layerDrawable = new android.graphics.drawable.LayerDrawable(layers);
                    
                    // Position badge at top-right
                    layerDrawable.setLayerGravity(1, android.view.Gravity.TOP | android.view.Gravity.END);
                    layerDrawable.setLayerInsetTop(1, 0);
                    layerDrawable.setLayerInsetEnd(1, 0);
                    
                    calendarItem.setIcon(layerDrawable);
                } else {
                    // No badge - just show calendar icon
                    calendarItem.setIcon(R.drawable.ic_calendar);
                }
            }
        }
    }

    private View createTaskView(final Task task) {
        if (task == null) return new View(this);
        
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

        // Delete button
        com.google.android.material.button.MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete \"" + task.name + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        taskRepository.deleteTask(task);
                        taskRepository.refreshTasks();
                        refreshAllFragments();
                        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }

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
