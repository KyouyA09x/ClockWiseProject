package com.example.mainactivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.TaskViewHolder> {

    private List<Task> tasks;

    public CalendarTaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskName;
        private final TextView taskTime;
        private final TextView taskType;
        private final View urgencyIndicator;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.taskName);
            taskTime = itemView.findViewById(R.id.taskTime);
            taskType = itemView.findViewById(R.id.taskType);
            urgencyIndicator = itemView.findViewById(R.id.urgencyIndicator);
        }

        public void bind(Task task) {
            taskName.setText(task.name);

            if (task.isFocusTask()) {
                String timeRange = String.format("%d:%02d %s → %d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                        task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                taskTime.setText(timeRange);
                taskType.setText("Focus Task");
                taskType.setVisibility(View.VISIBLE);
            } else {
                taskTime.setText(String.format("%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
                taskType.setText("Reminder");
                taskType.setVisibility(View.VISIBLE);
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
        }
    }
}

