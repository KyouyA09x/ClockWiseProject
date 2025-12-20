package com.example.mainactivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends BaseThemedActivity {

    public static final String ACTION_TASK_COMPLETED = "com.example.mainactivity.TASK_COMPLETED";

    private DrawerLayout drawerLayout;
    private MaterialToolbar topBar;
    private com.google.android.material.navigation.NavigationView navigationView;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabCenterAction;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabQuickTask;
    private View toggleBar;
    private boolean isToggleBarVisible = false;
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
    private final ArrayList<Task> completedTasksToday = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        drawerLayout = findViewById(R.id.drawerLayout);
        topBar = findViewById(R.id.topBar);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabCenterAction = findViewById(R.id.fabCenterAction);
        fabQuickTask = findViewById(R.id.fabQuickTask);
        toggleBar = findViewById(R.id.toggleBar);
        
        // Initialize toggle bar buttons
        com.google.android.material.button.MaterialButton btnAddTask = findViewById(R.id.btnAddTask);
        com.google.android.material.button.MaterialButton btnQuickTask = findViewById(R.id.btnQuickTask);
        
        // Setup Add Task button in toggle bar
        if (btnAddTask != null) {
            btnAddTask.setOnClickListener(v -> showTaskTypeChooser());
        }
        
        // Setup Quick Task button in toggle bar
        if (btnQuickTask != null) {
            btnQuickTask.setOnClickListener(v -> showQuickTaskOptionsDialog());
        }
        
        // Setup center FAB click listener with animation (keeping for backward compatibility)
        if (fabCenterAction != null) {
            fabCenterAction.setOnClickListener(v -> {
                // Animate FAB rotation
                fabCenterAction.animate()
                    .rotation(fabCenterAction.getRotation() + 45)
                    .setDuration(150)
                    .withEndAction(() -> {
                        showTaskTypeChooser();
                        // Reset rotation after dialog closes
                        fabCenterAction.animate()
                            .rotation(0)
                            .setDuration(150)
                            .start();
                    })
                    .start();
            });
        }
        
        // Setup Quick Task FAB - shows options dialog
        if (fabQuickTask != null) {
            fabQuickTask.setOnClickListener(v -> showQuickTaskOptionsDialog());
        }
        
        // Handle window insets for bottom navigation bar (works with 3-button navigation)
        if (bottomNavigation != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                try {
                    androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                    v.setPadding(0, 0, 0, systemBars.bottom);
                } catch (Exception e) {
                    // Fallback: no padding if insets fail
                    v.setPadding(0, 0, 0, 0);
                }
                return insets;
            });
        }
        
        // Handle window insets for fragment container
        final android.view.View fragmentContainer = findViewById(R.id.fragmentContainer);
        if (fragmentContainer != null && bottomNavigation != null) {
            // Post the layout to ensure bottom nav height is available
            fragmentContainer.post(() -> {
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, insets) -> {
                    try {
                        androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                        int bottomNavHeight = bottomNavigation.getHeight();
                        if (bottomNavHeight == 0) {
                            // Use default Material bottom nav height
                            bottomNavHeight = (int) (56 * getResources().getDisplayMetrics().density);
                        }
                        v.setPadding(0, 0, 0, bottomNavHeight + systemBars.bottom);
                    } catch (Exception e) {
                        // Fallback: just use bottom nav height
                        int bottomNavHeight = (int) (56 * getResources().getDisplayMetrics().density);
                        v.setPadding(0, 0, 0, bottomNavHeight);
                    }
                    return insets;
                });
            });
        }

        // Setup bottom navigation
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_tasks) {
                    loadFragment(new TasksContainerFragment());
                    return true;
                } else if (itemId == R.id.navigation_toggle) {
                    toggleBarVisibility();
                    return false; // Don't select this item
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

        // Handle back button press
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    public void refreshAllFragments() {
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
        
        // Hide MainActivity FABs when in Notepad to avoid overlap with Add Note FAB
        if (fabQuickTask != null && fabCenterAction != null) {
            if (fragment instanceof TasksContainerFragment) {
                fabQuickTask.show();
                fabCenterAction.show();
            } else if (fragment instanceof NotepadFragment) {
                fabQuickTask.hide();
                fabCenterAction.hide();
            }
        }
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
        // Show custom dialog with icons for Task and Focus Session
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_task_type_chooser, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Setup Reminder/Task option
        View reminderOption = dialogView.findViewById(R.id.reminderOption);
        if (reminderOption != null) {
            reminderOption.setOnClickListener(v -> {
                dialog.dismiss();
                showReminderBottomSheet(null);
            });
        }

        // Setup Focus Task option
        View focusTaskOption = dialogView.findViewById(R.id.focusTaskOption);
        if (focusTaskOption != null) {
            focusTaskOption.setOnClickListener(v -> {
                dialog.dismiss();
                showFocusTaskBottomSheet(null);
            });
        }

        // Setup Quick Note option
        View quickNoteOption = dialogView.findViewById(R.id.quickNoteOption);
        if (quickNoteOption != null) {
            quickNoteOption.setOnClickListener(v -> {
                dialog.dismiss();
                openQuickTaskInNotepad();
            });
        }

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

    public void showQuickTaskOptionsDialog() {
        // Show Material dialog with two options: Convert note to task and New quick task
        String[] options = {"📝  Convert note to task", "⚡  New quick task"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Quick Task")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Convert note to task
                        showNotepadToConvertToTask();
                    } else if (which == 1) {
                        // New quick task - show Reminder/Focus Session options
                        showNewQuickTaskDialog();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNewQuickTaskDialog() {
        // Show dialog with Reminder and Focus Session options (date set to today)
        String[] taskTypes = {"⏰  Reminder", "🎯  Focus Session"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("New Quick Task")
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

                        // Pass as quick task to set date to today
                        AddReminderBottomSheet bottomSheet = new AddReminderBottomSheet();
                        Bundle args = new Bundle();
                        args.putParcelable("TASK", quickTask);
                        args.putBoolean("QUICK_TASK", true);
                        bottomSheet.setArguments(args);
                        bottomSheet.setOnTaskSavedListener(() -> {
                            taskRepository.refreshTasks();
                            refreshAllFragments();
                        });
                        bottomSheet.show(getSupportFragmentManager(), "AddReminderBottomSheet");
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

                        // Pass as quick task to set date to today
                        AddFocusTaskBottomSheet bottomSheet = new AddFocusTaskBottomSheet();
                        Bundle args = new Bundle();
                        args.putParcelable("TASK", quickTask);
                        args.putBoolean("QUICK_TASK", true);
                        bottomSheet.setArguments(args);
                        bottomSheet.setOnTaskSavedListener(() -> {
                            taskRepository.refreshTasks();
                            refreshAllFragments();
                        });
                        bottomSheet.show(getSupportFragmentManager(), "AddFocusTaskBottomSheet");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showQuickTaskBottomSheet() {
        // Show Material dialog for task type selection with better styling
        String[] taskTypes = {"⏰  Task", "🎯  Focus Session"};
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

    private void toggleBarVisibility() {
        if (toggleBar == null) return;
        
        isToggleBarVisible = !isToggleBarVisible;
        
        if (isToggleBarVisible) {
            // Show the toggle bar with slide up animation
            toggleBar.setVisibility(View.VISIBLE);
            toggleBar.setTranslationY(toggleBar.getHeight());
            toggleBar.animate()
                .translationY(0)
                .setDuration(300)
                .start();
        } else {
            // Hide the toggle bar with slide down animation
            toggleBar.animate()
                .translationY(toggleBar.getHeight())
                .setDuration(300)
                .withEndAction(() -> toggleBar.setVisibility(View.GONE))
                .start();
        }
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
        if (taskContent != null && expandableDetails != null) {
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
                    isExpanded[0] = true;
                }
            });
        }

        // Delete button
        com.google.android.material.button.MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Focus Session?")
                    .setMessage("This action cannot be undone. \"" + task.name + "\" will be permanently removed.")
                    .setIcon(R.drawable.ic_delete)
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
            String timeRange = String.format(java.util.Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                    task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                    task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
            taskTimeTextView.setText(timeRange);
        } else {
            taskTimeTextView.setText(String.format(java.util.Locale.getDefault(), "%d:%02d %s", task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
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
        if (taskContent != null) {
            taskContent.setOnClickListener(v -> openTaskForEditing(task));
        }

        // Delete button
        com.google.android.material.button.MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                ModernDialogHelper.showDestructiveDialog(
                    this,
                    "Delete Task?",
                    "This action cannot be undone. {item} will be permanently removed.",
                    task.name,
                    R.drawable.ic_delete,
                    () -> {
                        // Animate slide-to-right deletion
                        taskView.animate()
                            .translationX(taskView.getWidth())
                            .alpha(0f)
                            .setDuration(300)
                            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                            .withEndAction(() -> {
                                taskRepository.deleteTask(task);
                                taskRepository.refreshTasks();
                                refreshAllFragments();
                                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                            })
                            .start();
                    },
                    null
                );
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
            } else if (id == R.id.nav_tutorial) {
                startActivity(new Intent(MainActivity.this, TutorialActivityNew.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_about) {
                // Close drawer first, then show dialog
                drawerLayout.closeDrawer(GravityCompat.START);
                // Delay dialog to allow drawer to close
                drawerLayout.postDelayed(() -> showAboutDialog(), 250);
                return true;
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
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

        // No placeholder data - app starts empty until user populates via Settings

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
            String timeRange = String.format(java.util.Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                    task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                    task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
            taskTimeTextView.setText(timeRange);
        } else {
            taskTimeTextView.setText(String.format(java.util.Locale.getDefault(), "%d:%02d %s", task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
        }


        return taskView;
    }

    private ArrayList<Task> generatePlaceholderCompletedTasks(String todayDate) {
        ArrayList<Task> placeholderTasks = new ArrayList<>();
        
        String[] taskNames = {
            "Morning workout ✓",
            "Review emails ✓",
            "Team standup ✓",
            "Code review ✓"
        };
        
        String[] focusNames = {
            "Deep work session ✓"
        };

        // Add 3 completed regular tasks
        for (int i = 0; i < 3; i++) {
            Task task = new Task();
            task.id = -(i + 200); // Negative ID for placeholder
            task.name = taskNames[i % taskNames.length];
            task.taskType = "reminder";
            task.date = todayDate;
            task.hour = 7 + (i * 2);
            task.minute = 0;
            task.amPm = task.hour >= 12 ? "PM" : "AM";
            if (task.hour > 12) task.hour -= 12;
            task.isComplete = true;
            task.isAlarmOn = false;
            placeholderTasks.add(task);
        }

        // Add 1 completed focus session
        Task focusTask = new Task();
        focusTask.id = -300;
        focusTask.name = focusNames[0];
        focusTask.taskType = "focus";
        focusTask.date = todayDate;
        focusTask.hour = 9;
        focusTask.minute = 0;
        focusTask.amPm = "AM";
        focusTask.endHour = 11;
        focusTask.endMinute = 0;
        focusTask.endAmPm = "AM";
        focusTask.isComplete = true;
        focusTask.isAlarmOn = false;
        placeholderTasks.add(focusTask);

        return placeholderTasks;
    }

    private void showAboutDialog() {
        // Create and show a dialog to display app information
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_about, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Make dialog appear as centered overlay
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            params.width = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }

        // Setup close button
        dialogView.findViewById(R.id.closeButton).setOnClickListener(v -> dialog.dismiss());

        // Apply HTML formatting to feature TextViews
        applyHtmlToTextView(dialogView, R.id.featureTaskManagement, R.string.feature_task_management);
        applyHtmlToTextView(dialogView, R.id.featureFocusSessions, R.string.feature_focus_sessions);
        applyHtmlToTextView(dialogView, R.id.featureCalendarIntegration, R.string.feature_calendar_integration);
        applyHtmlToTextView(dialogView, R.id.featureNotepad, R.string.feature_notepad);
        applyHtmlToTextView(dialogView, R.id.featureHistoryTracking, R.string.feature_history_tracking);
        applyHtmlToTextView(dialogView, R.id.featureReminders, R.string.feature_reminders);

        // Get references to the sections
        View aboutClockwiseSection = dialogView.findViewById(R.id.aboutClockwiseSection);
        View aboutUsSection = dialogView.findViewById(R.id.aboutUsSection);
        View featuresSection = dialogView.findViewById(R.id.featuresSection);
        com.google.android.material.button.MaterialButton toggleButton = 
            dialogView.findViewById(R.id.aboutUsToggleButton);
        com.google.android.material.button.MaterialButton featuresToggleButton = 
            dialogView.findViewById(R.id.featuresToggleButton);

        // Setup features toggle button click listener
        featuresToggleButton.setOnClickListener(v -> {
            if (featuresSection.getVisibility() == View.GONE) {
                // Show features section
                featuresSection.setVisibility(View.VISIBLE);
                featuresToggleButton.setIcon(getDrawable(R.drawable.ic_arrow_up));
            } else {
                // Hide features section
                featuresSection.setVisibility(View.GONE);
                featuresToggleButton.setIcon(getDrawable(R.drawable.ic_arrow_down));
            }
        });

        // Setup toggle button click listener
        toggleButton.setOnClickListener(v -> {
            if (aboutUsSection.getVisibility() == View.GONE) {
                // Show About Us, hide About ClockWise
                aboutUsSection.setVisibility(View.VISIBLE);
                aboutClockwiseSection.setVisibility(View.GONE);
                toggleButton.setText("About ClockWise");
                toggleButton.setIcon(getDrawable(R.drawable.app_icon));
            } else {
                // Show About ClockWise, hide About Us
                aboutUsSection.setVisibility(View.GONE);
                aboutClockwiseSection.setVisibility(View.VISIBLE);
                toggleButton.setText("About the development team");
                toggleButton.setIcon(getDrawable(R.drawable.ic_person));
            }
        });

        dialog.show();
    }

    private void applyHtmlToTextView(View rootView, int textViewId, int stringResId) {
        TextView textView = rootView.findViewById(textViewId);
        if (textView != null) {
            String htmlText = getString(stringResId);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                textView.setText(android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_COMPACT));
            } else {
                textView.setText(android.text.Html.fromHtml(htmlText));
            }
        }
    }
}
