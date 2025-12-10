package com.example.mainactivity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final int ADD_TASK_REQUEST = 1;

    private View editButtonContainer;
    private TextView editButtonText;
    private ImageView editButtonIcon;
    private ImageView addButton;
    private View calendarButtonContainer;
    private ImageView calendarIcon;
    private TextView calendarText;
    private ImageButton historyMenuButton;
    private View progressTracker;
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
    private ArrayList<Task> completedTasksToday = new ArrayList<>();


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editButtonContainer = findViewById(R.id.editButtonContainer);
        editButtonText = findViewById(R.id.editButton);
        editButtonIcon = findViewById(R.id.editButtonIcon);
        addButton = findViewById(R.id.addButton);
        calendarButtonContainer = findViewById(R.id.calendarButtonContainer);
        calendarIcon = findViewById(R.id.calendarIcon);
        calendarText = findViewById(R.id.calendarButton);
        historyMenuButton = findViewById(R.id.historyMenuButton);
        progressTracker = findViewById(R.id.progressTracker);
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

        editButtonContainer.setOnClickListener(v -> toggleEditMode());
        addButton.setOnClickListener(v -> showTaskTypeChooser());

        // Calendar button - shows ongoing tasks by date
        if (calendarButtonContainer != null) {
            calendarButtonContainer.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
                startActivity(intent);
            });
        }

        // History hamburger menu - shows completed tasks by month
        if (historyMenuButton != null) {
            historyMenuButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            });
        }

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
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivityForResult(intent, ADD_TASK_REQUEST);
        });

        // Focus Task option
        View focusTaskOption = dialogView.findViewById(R.id.focusTaskOption);
        focusTaskOption.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, AddFocusTaskActivity.class);
            startActivityForResult(intent, ADD_TASK_REQUEST);
        });

        dialog.show();
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
        editButtonText.setText(isEditMode ? "Done" : "Edit");

        // Update all task views to show/hide edit mode buttons
        updateAllTasksEditMode();
    }

    private void updateAllTasksEditMode() {
        for (int i = 0; i < morningTasksContainer.getChildCount(); i++) {
            updateTaskViewEditMode(morningTasksContainer.getChildAt(i));
        }
        for (int i = 0; i < afternoonTasksContainer.getChildCount(); i++) {
            updateTaskViewEditMode(afternoonTasksContainer.getChildAt(i));
        }
        for (int i = 0; i < nightTasksContainer.getChildCount(); i++) {
            updateTaskViewEditMode(nightTasksContainer.getChildAt(i));
        }
    }

    private void updateTaskViewEditMode(View taskView) {
        View normalModeLayout = taskView.findViewById(R.id.normalModeLayout);
        View editModeLayout = taskView.findViewById(R.id.editModeLayout);

        if (isEditMode) {
            // Show edit mode buttons (pencil + X)
            normalModeLayout.setVisibility(View.GONE);
            editModeLayout.setVisibility(View.VISIBLE);
        } else {
            // Show normal mode (switch)
            normalModeLayout.setVisibility(View.VISIBLE);
            editModeLayout.setVisibility(View.GONE);
        }
    }

    // **** THIS IS THE UPDATED METHOD ****
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
            emptyTasksText.setVisibility(View.VISIBLE);
            morningTasksHeader.setVisibility(View.GONE);
            afternoonTasksHeader.setVisibility(View.GONE);
            nightTasksHeader.setVisibility(View.GONE);
            // Hide edit button when no tasks (but keep bottom bar visible)
            if (isEditMode) {
                isEditMode = false;
                editButtonText.setText("Edit");
            }
        } else {
            emptyTasksText.setVisibility(View.GONE);
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
        ImageView taskTypeIcon = taskView.findViewById(R.id.taskTypeIcon);
        TextView repeatDaysTextView = taskView.findViewById(R.id.repeatDays);
        final View taskContent = taskView.findViewById(R.id.taskContent);
        SwitchCompat taskSwitch = taskView.findViewById(R.id.taskSwitch);

        // Edit mode buttons
        View normalModeLayout = taskView.findViewById(R.id.normalModeLayout);
        View editModeLayout = taskView.findViewById(R.id.editModeLayout);
        ImageButton editTimeButton = taskView.findViewById(R.id.editTimeButton);
        ImageButton deleteTaskButton = taskView.findViewById(R.id.deleteTaskButton);

        // Set initial visibility based on edit mode
        normalModeLayout.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        editModeLayout.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

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

        // Pencil button click - show time edit dialog
        editTimeButton.setOnClickListener(v -> showEditTimeDialog(task, taskTimeTextView));

        // X button click - delete task completely (not move to history)
        deleteTaskButton.setOnClickListener(v -> {
            // Cancel alarms first
            if (task.isFocusTask()) {
                AlarmHelper.cancelFocusTaskAlarms(this, task);
            } else {
                AlarmHelper.cancelTaskAlarm(this, task);
            }

            // Delete the task completely from database (not moving to history)
            taskRepository.deleteTask(task);
            updateTaskLists();
            Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
        });

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

    private void showEditTimeDialog(Task task, TextView taskTimeTextView) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_time, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Get views
        android.widget.NumberPicker hourPicker = dialogView.findViewById(R.id.hourPicker);
        android.widget.NumberPicker minutePicker = dialogView.findViewById(R.id.minutePicker);
        android.widget.NumberPicker amPmPicker = dialogView.findViewById(R.id.amPmPicker);
        View endTimeSection = dialogView.findViewById(R.id.endTimeSection);
        android.widget.NumberPicker endHourPicker = dialogView.findViewById(R.id.endHourPicker);
        android.widget.NumberPicker endMinutePicker = dialogView.findViewById(R.id.endMinutePicker);
        android.widget.NumberPicker endAmPmPicker = dialogView.findViewById(R.id.endAmPmPicker);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        // Setup hour picker
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setValue(task.hour);

        // Setup minute picker
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(i -> String.format("%02d", i));
        minutePicker.setValue(task.minute);

        // Setup AM/PM picker
        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        amPmPicker.setValue(task.amPm != null && task.amPm.equals("PM") ? 1 : 0);

        // If Focus Task, show end time section
        if (task.isFocusTask()) {
            endTimeSection.setVisibility(View.VISIBLE);

            endHourPicker.setMinValue(1);
            endHourPicker.setMaxValue(12);
            endHourPicker.setValue(task.endHour);

            endMinutePicker.setMinValue(0);
            endMinutePicker.setMaxValue(59);
            endMinutePicker.setFormatter(i -> String.format("%02d", i));
            endMinutePicker.setValue(task.endMinute);

            endAmPmPicker.setMinValue(0);
            endAmPmPicker.setMaxValue(1);
            endAmPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
            endAmPmPicker.setValue(task.endAmPm != null && task.endAmPm.equals("PM") ? 1 : 0);
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            // Cancel old alarm
            if (task.isFocusTask()) {
                AlarmHelper.cancelFocusTaskAlarms(this, task);
            } else {
                AlarmHelper.cancelTaskAlarm(this, task);
            }

            // Update task time
            task.hour = hourPicker.getValue();
            task.minute = minutePicker.getValue();
            task.amPm = amPmPicker.getDisplayedValues()[amPmPicker.getValue()];

            if (task.isFocusTask()) {
                task.endHour = endHourPicker.getValue();
                task.endMinute = endMinutePicker.getValue();
                task.endAmPm = endAmPmPicker.getDisplayedValues()[endAmPmPicker.getValue()];
            }

            // Update time category
            if (task.amPm.equals("AM")) {
                task.timeCategory = "morning";
            } else if (task.hour == 12 || (task.hour >= 1 && task.hour < 6)) {
                task.timeCategory = "afternoon";
            } else {
                task.timeCategory = "night";
            }

            // Save to database
            taskRepository.updateTask(task);

            // Reschedule alarm if enabled
            if (task.isAlarmOn) {
                if (task.isFocusTask()) {
                    AlarmHelper.scheduleFocusTaskAlarms(this, task);
                } else {
                    AlarmHelper.scheduleTaskAlarm(this, task);
                }
            }

            // Update UI
            if (task.isFocusTask()) {
                String timeRange = String.format("%d:%02d %s → %d:%02d %s",
                        task.hour, task.minute, task.amPm,
                        task.endHour, task.endMinute, task.endAmPm);
                taskTimeTextView.setText(timeRange);
            } else {
                taskTimeTextView.setText(String.format("%d:%02d %s", task.hour, task.minute, task.amPm));
            }

            Toast.makeText(MainActivity.this, "Time updated!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            // Refresh to apply time category changes
            taskRepository.refreshTasks();
            updateTaskLists();
        });

        dialog.show();
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

    private void showCompletedTasksDialog() {
        // Create and show a dialog to display completed tasks for today
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_completed_tasks, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        LinearLayout completedTasksContainer = dialogView.findViewById(R.id.completedTasksContainer);
        TextView emptyCompletedTasksText = dialogView.findViewById(R.id.emptyCompletedTasksText);

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
