package com.example.mainactivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class UpcomingTasksFragment extends Fragment {

    private View emptyStateCard;
    private LinearLayout upcomingTasksContainer;
    private TextView upcomingCount;
    private TaskRepository taskRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upcoming_tasks, container, false);

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
        emptyStateCard = view.findViewById(R.id.emptyStateCard);
        upcomingTasksContainer = view.findViewById(R.id.upcomingTasksContainer);
        upcomingCount = view.findViewById(R.id.upcomingCount);
    }

    public void refreshTasks() {
        if (taskRepository == null || getContext() == null) return;

        taskRepository.refreshTasks();

        ArrayList<Task> morningTasks = taskRepository.morningTasks;
        ArrayList<Task> afternoonTasks = taskRepository.afternoonTasks;
        ArrayList<Task> nightTasks = taskRepository.nightTasks;

        if (upcomingTasksContainer != null) upcomingTasksContainer.removeAllViews();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new java.util.Date());

        ArrayList<Task> upcomingTasks = new ArrayList<>();

        // Collect all upcoming focus tasks
        collectUpcomingTasks(morningTasks, todayDate, upcomingTasks);
        collectUpcomingTasks(afternoonTasks, todayDate, upcomingTasks);
        collectUpcomingTasks(nightTasks, todayDate, upcomingTasks);

        // Sort by date and time
        upcomingTasks.sort((t1, t2) -> {
            if (t1.date != null && t2.date != null) {
                int dateCompare = t1.date.compareTo(t2.date);
                if (dateCompare != 0) return dateCompare;
                int t1Minutes = convertTo24Hour(t1.hour, t1.amPm) * 60 + t1.minute;
                int t2Minutes = convertTo24Hour(t2.hour, t2.amPm) * 60 + t2.minute;
                return Integer.compare(t1Minutes, t2Minutes);
            }
            return 0;
        });

        // Display upcoming tasks
        if (!upcomingTasks.isEmpty() && upcomingTasksContainer != null) {
            for (Task task : upcomingTasks) {
                upcomingTasksContainer.addView(createUpcomingTaskView(task));
            }
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.GONE);
        } else {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
        }

        if (upcomingCount != null) {
            upcomingCount.setText(String.valueOf(upcomingTasks.size()));
        }
    }

    private void collectUpcomingTasks(ArrayList<Task> tasks, String todayDate, ArrayList<Task> upcomingList) {
        if (tasks == null) return;

        for (Task task : tasks) {
            if (task == null || task.date == null) continue;
            if (task.date.compareTo(todayDate) > 0 && task.isFocusTask() && !task.isComplete) {
                upcomingList.add(task);
            }
        }
    }

    private View createUpcomingTaskView(Task task) {
        if (task == null || getContext() == null) return new View(getContext());

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View taskView = inflater.inflate(R.layout.focus_task_item, null, false);

        TextView taskNameTextView = taskView.findViewById(R.id.taskName);
        TextView taskTimeTextView = taskView.findViewById(R.id.taskTime);
        TextView fullDateText = taskView.findViewById(R.id.fullDateText);
        SwitchCompat taskSwitch = taskView.findViewById(R.id.taskSwitch);
        View taskContent = taskView.findViewById(R.id.taskContent);

        if (taskNameTextView != null) {
            taskNameTextView.setText(task.name);
        }

        if (taskTimeTextView != null) {
            String timeRange = String.format(Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                    task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                    task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
            taskTimeTextView.setText(timeRange);
        }

        if (fullDateText != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
                java.util.Date date = inputFormat.parse(task.date);
                if (date != null) {
                    fullDateText.setText(outputFormat.format(date));
                }
            } catch (Exception e) {
                fullDateText.setText(task.date);
            }
        }

        if (taskSwitch != null) {
            taskSwitch.setChecked(task.isAlarmOn);
            taskSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.isAlarmOn = isChecked;
                taskRepository.updateTask(task);
                if (isChecked) {
                    AlarmHelper.scheduleFocusTaskAlarms(getContext(), task);
                    Toast.makeText(getContext(), "🔔 Alerts enabled", Toast.LENGTH_SHORT).show();
                } else {
                    AlarmHelper.cancelFocusTaskAlarms(getContext(), task);
                    Toast.makeText(getContext(), "🔕 Alerts disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Make entire task clickable to edit
        if (taskContent != null) {
            taskContent.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), EditFocusTaskActivity.class);
                intent.putExtra("task_id", task.id);
                startActivity(intent);
            });
        }

        // Delete button
        com.google.android.material.button.MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Delete Focus Session")
                    .setMessage("Are you sure you want to delete \"" + task.name + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        AlarmHelper.cancelFocusTaskAlarms(getContext(), task);
                        taskRepository.deleteTask(task);
                        taskRepository.refreshTasks();
                        refreshTasks();
                        Toast.makeText(getContext(), "Focus session deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
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
}
