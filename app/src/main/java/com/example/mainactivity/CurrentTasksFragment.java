package com.example.mainactivity;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CurrentTasksFragment extends Fragment {

    private View progressTracker;
    private View emptyStateCard;
    private View tasksContainerCard;
    private LinearLayout focusTasksSection;
    private LinearLayout focusTasksContainer;
    private LinearLayout morningTasksContainer;
    private LinearLayout afternoonTasksContainer;
    private LinearLayout nightTasksContainer;
    private TextView morningTasksHeader;
    private TextView afternoonTasksHeader;
    private TextView nightTasksHeader;
    private TextView taskCountText;
    private LinearProgressIndicator progressBar;
    private TextView completionText;

    private TaskRepository taskRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_tasks, container, false);

        initViews(view);
        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(requireContext());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshTasks();
    }

    private void initViews(View view) {
        progressTracker = view.findViewById(R.id.progressTracker);
        emptyStateCard = view.findViewById(R.id.emptyStateCard);
        tasksContainerCard = view.findViewById(R.id.tasksContainerCard);
        focusTasksSection = view.findViewById(R.id.focusTasksSection);
        focusTasksContainer = view.findViewById(R.id.focusTasksContainer);
        morningTasksContainer = view.findViewById(R.id.morningTasksContainer);
        afternoonTasksContainer = view.findViewById(R.id.afternoonTasksContainer);
        nightTasksContainer = view.findViewById(R.id.nightTasksContainer);
        morningTasksHeader = view.findViewById(R.id.morningTasksHeader);
        afternoonTasksHeader = view.findViewById(R.id.afternoonTasksHeader);
        nightTasksHeader = view.findViewById(R.id.nightTasksHeader);
        taskCountText = view.findViewById(R.id.taskCountText);
        progressBar = view.findViewById(R.id.progressBar);
        completionText = view.findViewById(R.id.completionText);

        if (progressTracker != null) {
            progressTracker.setOnClickListener(v -> showCompletedTasksDialog());
        }

        // Setup dynamic padding for content to avoid being hidden by FABs
        setupDynamicPadding(view);
    }

    private void setupDynamicPadding(View view) {
        LinearLayout container = view.findViewById(R.id.currentTasksContainer);
        if (container == null) return;

        view.post(() -> {
            android.app.Activity activity = getActivity();
            if (!(activity instanceof MainActivity)) return;

            MainActivity mainActivity = (MainActivity) activity;

            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                );

                float density = getResources().getDisplayMetrics().density;

                // Calculate total reserved space at bottom
                int bottomNavHeight = 0;
                try {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        mainActivity.findViewById(R.id.bottomNavigation);
                    if (bottomNav != null) {
                        bottomNavHeight = bottomNav.getHeight();
                        if (bottomNavHeight == 0) {
                            bottomNavHeight = (int) (56 * density);
                        }
                    }
                } catch (Exception e) {
                    bottomNavHeight = (int) (56 * density);
                }

                // Reserve space for: bottom nav + system nav + FABs (2 stacked) + margins
                // Extended FAB height ~48dp, spacing 16dp between, margin 16dp
                int fabsReservedSpace = (int) ((48 + 16 + 48 + 16) * density); // Two FABs + spacing + margin
                int totalBottomPadding = bottomNavHeight + systemBars.bottom + fabsReservedSpace;

                // Set dynamic padding
                container.setPadding(
                    container.getPaddingLeft(),
                    container.getPaddingTop(),
                    container.getPaddingRight(),
                    totalBottomPadding
                );

                return insets;
            });

            androidx.core.view.ViewCompat.requestApplyInsets(view);
        });
    }

    public void refreshTasks() {
        if (taskRepository == null || getContext() == null) return;

        taskRepository.refreshTasks();

        ArrayList<Task> morningTasks = taskRepository.morningTasks;
        ArrayList<Task> afternoonTasks = taskRepository.afternoonTasks;
        ArrayList<Task> nightTasks = taskRepository.nightTasks;

        if (focusTasksContainer != null) focusTasksContainer.removeAllViews();
        if (morningTasksContainer != null) morningTasksContainer.removeAllViews();
        if (afternoonTasksContainer != null) afternoonTasksContainer.removeAllViews();
        if (nightTasksContainer != null) nightTasksContainer.removeAllViews();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new java.util.Date());

        int totalTasks = 0;
        int completedTasks = 0;
        boolean hasTasksForToday = false;
        boolean hasTodayFocusTasks = false;
        boolean hasAnyTodayTasks = false;

        // Check if there are any tasks for today
        for (Task task : morningTasks) {
            if (task != null && task.date != null && task.date.equals(todayDate)) {
                hasAnyTodayTasks = true;
                break;
            }
        }
        if (!hasAnyTodayTasks) {
            for (Task task : afternoonTasks) {
                if (task != null && task.date != null && task.date.equals(todayDate)) {
                    hasAnyTodayTasks = true;
                    break;
                }
            }
        }
        if (!hasAnyTodayTasks) {
            for (Task task : nightTasks) {
                if (task != null && task.date != null && task.date.equals(todayDate)) {
                    hasAnyTodayTasks = true;
                    break;
                }
            }
        }

        // No placeholder data - app starts empty until user populates via Settings

        // Process all task lists
        processTaskList(morningTasks, todayDate, morningTasksContainer, focusTasksContainer);
        processTaskList(afternoonTasks, todayDate, afternoonTasksContainer, focusTasksContainer);
        processTaskList(nightTasks, todayDate, nightTasksContainer, focusTasksContainer);

        // Count tasks
        for (Task task : morningTasks) {
            if (task != null && task.date != null && task.date.equals(todayDate)) {
                totalTasks++;
                if (task.isComplete) completedTasks++;
                else hasTasksForToday = true;
                if (task.isFocusTask() && !task.isComplete) hasTodayFocusTasks = true;
            }
        }
        for (Task task : afternoonTasks) {
            if (task != null && task.date != null && task.date.equals(todayDate)) {
                totalTasks++;
                if (task.isComplete) completedTasks++;
                else hasTasksForToday = true;
                if (task.isFocusTask() && !task.isComplete) hasTodayFocusTasks = true;
            }
        }
        for (Task task : nightTasks) {
            if (task != null && task.date != null && task.date.equals(todayDate)) {
                totalTasks++;
                if (task.isComplete) completedTasks++;
                else hasTasksForToday = true;
                if (task.isFocusTask() && !task.isComplete) hasTodayFocusTasks = true;
            }
        }

        // Update UI
        if (taskCountText != null) {
            taskCountText.setText(String.format(Locale.getDefault(), "Task %d/%d", completedTasks, totalTasks));
        }
        if (progressBar != null && totalTasks > 0) {
            int progress = (completedTasks * 100) / totalTasks;
            progressBar.setProgress(progress);
            if (completionText != null) {
                completionText.setText(String.format(Locale.getDefault(), "%d%% completed", progress));
            }
        } else if (completionText != null) {
            completionText.setText("0% completed");
        }

        if (focusTasksSection != null) {
            focusTasksSection.setVisibility(hasTodayFocusTasks ? View.VISIBLE : View.GONE);
        }

        if (!hasTasksForToday) {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (tasksContainerCard != null) tasksContainerCard.setVisibility(View.GONE);
        } else {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.GONE);
            if (tasksContainerCard != null) tasksContainerCard.setVisibility(View.VISIBLE);
        }

        if (morningTasksHeader != null && morningTasksContainer != null) {
            morningTasksHeader.setVisibility(morningTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
        if (afternoonTasksHeader != null && afternoonTasksContainer != null) {
            afternoonTasksHeader.setVisibility(afternoonTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
        if (nightTasksHeader != null && nightTasksContainer != null) {
            nightTasksHeader.setVisibility(nightTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private ArrayList<Task> generatePlaceholderCurrentTasks(String todayDate) {
        ArrayList<Task> placeholderTasks = new ArrayList<>();
        
        String[] morningTasks = {"Morning workout", "Review emails", "Team standup"};
        String[] afternoonTasks = {"Lunch meeting", "Code review", "Project planning"};
        String[] nightTasks = {"Wrap up tasks", "Prepare for tomorrow"};
        String[] focusNames = {"Deep work: Feature development"};
        String[] priorities = {"None", "Low", "Medium", "High"};
        
        java.util.Random random = new java.util.Random(System.currentTimeMillis());

        // Add morning tasks
        for (int i = 0; i < 2; i++) {
            Task task = new Task();
            task.id = -(i + 500);
            task.name = morningTasks[i % morningTasks.length];
            task.taskType = "reminder";
            task.date = todayDate;
            task.hour = 8 + i;
            task.minute = i == 0 ? 0 : 30;
            task.amPm = "AM";
            task.timeCategory = "morning";
            task.urgency = priorities[1 + random.nextInt(3)];
            task.isAlarmOn = true;
            task.isComplete = false;
            placeholderTasks.add(task);
        }

        // Add afternoon tasks
        for (int i = 0; i < 2; i++) {
            Task task = new Task();
            task.id = -(i + 510);
            task.name = afternoonTasks[i % afternoonTasks.length];
            task.taskType = "reminder";
            task.date = todayDate;
            task.hour = 1 + i;
            task.minute = 0;
            task.amPm = "PM";
            task.timeCategory = "afternoon";
            task.urgency = priorities[random.nextInt(4)];
            task.isAlarmOn = true;
            task.isComplete = false;
            placeholderTasks.add(task);
        }

        // Add a focus session
        Task focusTask = new Task();
        focusTask.id = -520;
        focusTask.name = focusNames[0];
        focusTask.taskType = "focus";
        focusTask.date = todayDate;
        focusTask.hour = 10;
        focusTask.minute = 0;
        focusTask.amPm = "AM";
        focusTask.endHour = 12;
        focusTask.endMinute = 0;
        focusTask.endAmPm = "PM";
        focusTask.timeCategory = "morning";
        focusTask.urgency = "High";
        focusTask.isAlarmOn = true;
        focusTask.isComplete = false;
        placeholderTasks.add(focusTask);

        return placeholderTasks;
    }

    private void processTaskList(ArrayList<Task> tasks, String todayDate, LinearLayout container, LinearLayout focusContainer) {
        if (tasks == null || container == null) return;

        for (Task task : tasks) {
            if (task == null || task.date == null) continue;
            if (!task.date.equals(todayDate) || task.isComplete) continue;

            if (task.isFocusTask() && focusContainer != null) {
                focusContainer.addView(createTaskView(task, true));
            } else {
                container.addView(createTaskView(task, false));
            }
        }
    }

    private View createTaskView(Task task, boolean isFocusTaskView) {
        if (task == null || getContext() == null) return new View(getContext());

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View taskView;

        if (isFocusTaskView) {
            taskView = inflater.inflate(R.layout.focus_task_item, null, false);
        } else {
            taskView = inflater.inflate(R.layout.task_item, null, false);
        }

        // Setup task view with click listeners
        setupTaskView(taskView, task);

        return taskView;
    }

    private void setupTaskView(View taskView, Task task) {
        TextView taskNameTextView = taskView.findViewById(R.id.taskName);
        TextView taskTimeTextView = taskView.findViewById(R.id.taskTime);
        SwitchCompat taskSwitch = taskView.findViewById(R.id.taskSwitch);
        View taskContent = taskView.findViewById(R.id.taskContent);
        
        // Get task type icon for notification preview
        View taskTypeIcon = taskView.findViewById(R.id.taskTypeIcon);
        // For focus_task_item, the icon might have a different parent
        View iconContainer = taskView.findViewWithTag("iconContainer");
        if (iconContainer == null) {
            // Try finding the first MaterialCardView child that contains the icon
            android.view.ViewGroup parent = (android.view.ViewGroup) taskView;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof com.google.android.material.card.MaterialCardView) {
                    iconContainer = child;
                    break;
                }
            }
        }

        if (taskNameTextView != null) {
            taskNameTextView.setText(task.name);
        }

        if (taskTimeTextView != null) {
            if (task.isFocusTask()) {
                String timeRange = String.format(Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                        task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                taskTimeTextView.setText(timeRange);
            } else {
                taskTimeTextView.setText(String.format(Locale.getDefault(), "%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
            }
        }
        
        // Add click listener on task icon to show notification preview
        View clickableIcon = taskTypeIcon != null ? taskTypeIcon : iconContainer;
        if (clickableIcon != null) {
            clickableIcon.setClickable(true);
            clickableIcon.setOnClickListener(v -> {
                // Show notification preview popup
                showNotificationPreviewPopup(v, task, task.isFocusTask());
            });
        }

        // Set priority border color
        if (taskView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView cardView = 
                (com.google.android.material.card.MaterialCardView) taskView;
            String priority = task.urgency != null ? task.urgency : "None";
            int borderColor;
            switch (priority) {
                case "High":
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_high);
                    break;
                case "Medium":
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_medium);
                    break;
                case "Low":
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_low);
                    break;
                default:
                    borderColor = task.isFocusTask() ? 
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary) :
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success);
                    break;
            }
            cardView.setStrokeColor(borderColor);
        }

        if (taskSwitch != null) {
            taskSwitch.setChecked(task.isAlarmOn);
            taskSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.isAlarmOn = isChecked;
                taskRepository.updateTask(task);
                if (isChecked) {
                    if (task.isFocusTask()) {
                        AlarmHelper.scheduleFocusTaskAlarms(getContext(), task);
                    } else {
                        AlarmHelper.scheduleTaskAlarm(getContext(), task);
                    }
                    Toast.makeText(getContext(), "🔔 Alerts enabled", Toast.LENGTH_SHORT).show();
                } else {
                    if (task.isFocusTask()) {
                        AlarmHelper.cancelFocusTaskAlarms(getContext(), task);
                    } else {
                        AlarmHelper.cancelTaskAlarm(getContext(), task);
                    }
                    Toast.makeText(getContext(), "🔕 Alerts disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Make entire task clickable to edit
        if (taskContent != null) {
            taskContent.setOnClickListener(v -> openTaskForEditing(task));
            
            // Long press with 3D touch effect
            taskContent.setOnLongClickListener(v -> {
                // Apply 3D touch scale effect
                taskView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        taskView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                        
                        // Haptic feedback
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        
                        // Show quick info popup - same as UpcomingTasksFragment
                        showQuickInfoPopup(v, task, task.isFocusTask());
                    })
                    .start();
                return true;
            });
        }

        // Delete button
        com.google.android.material.button.MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle(task.isFocusTask() ? "Delete Focus Session" : "Delete Task")
                    .setMessage("Are you sure you want to delete \"" + task.name + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (task.isFocusTask()) {
                            AlarmHelper.cancelFocusTaskAlarms(getContext(), task);
                        } else {
                            AlarmHelper.cancelTaskAlarm(getContext(), task);
                        }
                        taskRepository.deleteTask(task);
                        taskRepository.refreshTasks();
                        refreshTasks();
                        Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }

    private void showQuickInfoPopup(View anchorView, Task task, boolean isFocusSession) {
        if (getContext() == null) return;

        // Create popup window
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(getContext());
        
        // Inflate the popup layout
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View popupView = inflater.inflate(R.layout.popup_quick_info, null);
        popupWindow.setContentView(popupView);
        
        // Configure popup window
        popupWindow.setWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(24f);
        
        // Animate popup
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        // Populate popup content
        TextView titleText = popupView.findViewById(R.id.quickInfoTitle);
        TextView typeText = popupView.findViewById(R.id.quickInfoType);
        TextView dateText = popupView.findViewById(R.id.quickInfoDate);
        TextView timeText = popupView.findViewById(R.id.quickInfoTime);
        TextView durationText = popupView.findViewById(R.id.quickInfoDuration);
        TextView alarmText = popupView.findViewById(R.id.quickInfoAlarm);
        View durationRow = popupView.findViewById(R.id.durationRow);
        ImageView typeIcon = popupView.findViewById(R.id.typeIcon);
        ImageView alarmIcon = popupView.findViewById(R.id.alarmIcon);
        com.google.android.material.card.MaterialCardView iconContainer = popupView.findViewById(R.id.typeIconContainer);

        // Set title
        if (titleText != null) {
            titleText.setText(task.name);
        }

        // Set type
        if (typeText != null) {
            typeText.setText(isFocusSession ? "Focus Session" : "Task");
            typeText.setTextColor(isFocusSession ? 
                getResources().getColor(R.color.primary, null) : 
                getResources().getColor(R.color.success, null));
        }

        // Set icon and color
        if (typeIcon != null && iconContainer != null) {
            if (isFocusSession) {
                typeIcon.setImageResource(R.drawable.ic_focus);
                iconContainer.setCardBackgroundColor(getResources().getColor(R.color.primary, null));
            } else {
                typeIcon.setImageResource(R.drawable.ic_reminder);
                iconContainer.setCardBackgroundColor(getResources().getColor(R.color.success, null));
            }
        }

        // Set date
        if (dateText != null && task.date != null) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
                java.util.Date date = inputFormat.parse(task.date);
                if (date != null) {
                    dateText.setText(outputFormat.format(date));
                }
            } catch (java.text.ParseException e) {
                dateText.setText(task.date);
            }
        }

        // Set time
        if (timeText != null) {
            if (isFocusSession) {
                String timeRange = String.format(Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                        task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                timeText.setText(timeRange);
            } else {
                String time = String.format(Locale.getDefault(), "%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM");
                timeText.setText(time);
            }
        }

        // Set duration for focus sessions
        if (isFocusSession && durationRow != null && durationText != null) {
            durationRow.setVisibility(View.VISIBLE);
            int startMinutes = convertTo24Hour(task.hour, task.amPm) * 60 + task.minute;
            int endMinutes = convertTo24Hour(task.endHour, task.endAmPm) * 60 + task.endMinute;
            int durationMins = endMinutes - startMinutes;
            if (durationMins < 0) durationMins += 24 * 60;
            
            int hours = durationMins / 60;
            int mins = durationMins % 60;
            String durationStr;
            if (hours > 0 && mins > 0) {
                durationStr = hours + " hr " + mins + " min";
            } else if (hours > 0) {
                durationStr = hours + " hour" + (hours > 1 ? "s" : "");
            } else {
                durationStr = mins + " minutes";
            }
            durationText.setText(durationStr);
        } else if (durationRow != null) {
            durationRow.setVisibility(View.GONE);
        }

        // Set alarm status
        if (alarmText != null && alarmIcon != null) {
            if (task.isAlarmOn) {
                alarmText.setText("Alerts enabled");
                alarmIcon.setImageResource(R.drawable.ic_reminder);
            } else {
                alarmText.setText("Alerts disabled");
                alarmIcon.setImageResource(R.drawable.ic_reminder);
            }
        }

        // Show popup at center of screen
        popupWindow.showAtLocation(anchorView, android.view.Gravity.CENTER, 0, 0);
    }

    /**
     * Shows a notification preview popup - a squircle popup showing how the notification
     * will appear when the task is due.
     */
    private void showNotificationPreviewPopup(View anchorView, Task task, boolean isFocusSession) {
        if (getContext() == null) return;

        // Create dialog for notification preview
        android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_notification_preview);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            params.gravity = android.view.Gravity.CENTER;
            dialog.getWindow().setAttributes(params);
        }

        // Populate dialog with task info
        TextView titleText = dialog.findViewById(R.id.notificationTitle);
        TextView typeText = dialog.findViewById(R.id.notificationType);
        TextView timeText = dialog.findViewById(R.id.notificationTime);
        TextView messageText = dialog.findViewById(R.id.notificationMessage);
        ImageView iconView = dialog.findViewById(R.id.notificationIcon);
        com.google.android.material.card.MaterialCardView iconContainer = dialog.findViewById(R.id.notificationIconContainer);
        com.google.android.material.button.MaterialButton dismissButton = dialog.findViewById(R.id.dismissButton);
        com.google.android.material.button.MaterialButton actionButton = dialog.findViewById(R.id.actionButton);

        if (titleText != null) titleText.setText(task.name);
        
        if (typeText != null) {
            typeText.setText(isFocusSession ? "Focus Session" : "Task Reminder");
            typeText.setTextColor(isFocusSession ? 
                getResources().getColor(R.color.primary, null) : 
                getResources().getColor(R.color.success, null));
        }
        
        if (timeText != null) {
            if (isFocusSession) {
                timeText.setText(String.format(Locale.getDefault(), 
                    "%d:%02d %s → %d:%02d %s", 
                    task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                    task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM"));
            } else {
                timeText.setText(String.format(Locale.getDefault(), 
                    "%d:%02d %s", task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
            }
        }
        
        if (messageText != null) {
            String message = task.noteContent != null && !task.noteContent.isEmpty() 
                ? task.noteContent 
                : (isFocusSession ? "Your focus session is about to start. Time to eliminate distractions!" 
                    : "It's time for your scheduled task. Stay productive!");
            messageText.setText(message);
        }

        if (iconView != null && iconContainer != null) {
            if (isFocusSession) {
                iconView.setImageResource(R.drawable.ic_focus);
                iconContainer.setCardBackgroundColor(getResources().getColor(R.color.primary, null));
            } else {
                iconView.setImageResource(R.drawable.ic_reminder);
                iconContainer.setCardBackgroundColor(getResources().getColor(R.color.success, null));
            }
        }

        if (dismissButton != null) {
            dismissButton.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (actionButton != null) {
            actionButton.setText(isFocusSession ? "Start Session" : "Mark Complete");
            actionButton.setOnClickListener(v -> {
                if (!isFocusSession) {
                    // Mark task as complete
                    task.isComplete = true;
                    taskRepository.updateTask(task);
                    refreshTasks();
                    Toast.makeText(getContext(), "Task marked complete!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Focus session started!", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            });
        }

        // Haptic feedback
        anchorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        
        dialog.show();
    }
    
    private int convertTo24Hour(int hour, String amPm) {
        if (amPm == null) amPm = "AM";
        if (hour == 12) {
            return amPm.equals("AM") ? 0 : 12;
        } else {
            return amPm.equals("PM") ? hour + 12 : hour;
        }
    }

    private void openTaskForEditing(Task task) {
        if (task.isFocusTask()) {
            // Open focus task editor
            Intent intent = new Intent(getContext(), EditFocusTaskActivity.class);
            intent.putExtra("task_id", task.id);
            startActivity(intent);
        } else {
            // Open reminder editor using bottom sheet
            if (getActivity() != null) {
                AddReminderBottomSheet bottomSheet = AddReminderBottomSheet.newInstance(task);
                bottomSheet.setOnTaskSavedListener(() -> {
                    taskRepository.refreshTasks();
                    refreshTasks();
                });
                bottomSheet.show(getActivity().getSupportFragmentManager(), "AddReminderBottomSheet");
            }
        }
    }

    private void showCompletedTasksDialog() {
        // Show completed tasks dialog
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).triggerShowCompletedTasksDialog();
        }
    }
}
