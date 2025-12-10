package com.example.mainactivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryTaskAdapter extends RecyclerView.Adapter<HistoryTaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private final HistoryMonthDetailActivity.OnTaskDeleteListener deleteListener;

    public HistoryTaskAdapter(List<Task> tasks, HistoryMonthDetailActivity.OnTaskDeleteListener deleteListener) {
        this.tasks = tasks;
        this.deleteListener = deleteListener;
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task, deleteListener);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskName;
        private final TextView taskTime;
        private final TextView taskDate;
        private final View urgencyIndicator;
        private final ImageButton deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.taskName);
            taskTime = itemView.findViewById(R.id.taskTime);
            taskDate = itemView.findViewById(R.id.taskDate);
            urgencyIndicator = itemView.findViewById(R.id.urgencyIndicator);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Task task, HistoryMonthDetailActivity.OnTaskDeleteListener deleteListener) {
            taskName.setText(task.name);

            // Format time
            if (task.isFocusTask()) {
                String timeRange = String.format("%d:%02d %s → %d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                        task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                taskTime.setText(timeRange);
            } else {
                taskTime.setText(String.format("%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
            }

            // Format date
            if (task.date != null) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    Date date = inputFormat.parse(task.date);
                    if (date != null) {
                        taskDate.setText(outputFormat.format(date));
                    }
                } catch (ParseException e) {
                    taskDate.setText(task.date);
                }
            }

            // Set urgency color
            switch (task.urgency != null ? task.urgency : "None") {
                case "High":
                    urgencyIndicator.setBackgroundResource(R.drawable.red_circle);
                    break;
                case "Medium":
                    urgencyIndicator.setBackgroundResource(R.drawable.yellow_circle);
                    break;
                case "Low":
                    urgencyIndicator.setBackgroundResource(R.drawable.green_circle);
                    break;
                default:
                    urgencyIndicator.setBackgroundResource(R.drawable.green_circle);
                    break;
            }

            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(task);
                }
            });
        }
    }
}

