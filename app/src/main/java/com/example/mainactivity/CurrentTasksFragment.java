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
