package com.example.mainactivity;

import android.content.Context;
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
    private Context context;

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
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_history_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task, deleteListener, context);
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
            taskName = itemView.findViewById(R.id.historyTaskName);
            taskTime = itemView.findViewById(R.id.historyTaskTime);
            taskDate = itemView.findViewById(R.id.historyTaskDate);
            urgencyIndicator = itemView.findViewById(R.id.priorityIndicator);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Task task, HistoryMonthDetailActivity.OnTaskDeleteListener deleteListener, Context context) {
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
                    // Animate slide-to-right deletion
                    itemView.animate()
                        .translationX(itemView.getWidth())
                        .alpha(0f)
                        .setDuration(300)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .withEndAction(() -> deleteListener.onDelete(task))
                        .start();
                }
            });

            // Both long press and single tap show quick info popup (view only, no edit)
            itemView.setOnLongClickListener(v -> {
                showTaskQuickInfo(v, task, context);
                return true;
            });

            itemView.setOnClickListener(v -> {
                showTaskQuickInfo(v, task, context);
            });
        }

        /**
         * Show task details popup - view only, no edit functionality for completed tasks
         */
        private void showTaskQuickInfo(View anchorView, Task task, Context context) {
            if (context == null) return;

            android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(context);
            View popupView = android.view.LayoutInflater.from(context).inflate(R.layout.popup_quick_info, null);
            popupWindow.setContentView(popupView);

            popupWindow.setWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setFocusable(true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            popupWindow.setElevation(24f);
            popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

            // Populate popup
            TextView titleText = popupView.findViewById(R.id.quickInfoTitle);
            TextView typeText = popupView.findViewById(R.id.quickInfoType);
            TextView dateText = popupView.findViewById(R.id.quickInfoDate);
            TextView timeText = popupView.findViewById(R.id.quickInfoTime);
            TextView durationText = popupView.findViewById(R.id.quickInfoDuration);
            View durationRow = popupView.findViewById(R.id.durationRow);
            android.widget.ImageView typeIcon = popupView.findViewById(R.id.typeIcon);
            com.google.android.material.card.MaterialCardView iconContainer = popupView.findViewById(R.id.typeIconContainer);
            TextView alarmText = popupView.findViewById(R.id.quickInfoAlarm);

            boolean isFocusSession = task.isFocusTask();

            if (titleText != null) titleText.setText(task.name);
            if (typeText != null) {
                typeText.setText(isFocusSession ? "Focus Session" : "Task");
                if (context instanceof android.app.Activity) {
                    int color = isFocusSession
                        ? ((android.app.Activity) context).getColor(R.color.primary)
                        : ((android.app.Activity) context).getColor(R.color.success);
                    typeText.setTextColor(color);
                }
            }
            if (dateText != null && task.date != null) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
                    Date date = inputFormat.parse(task.date);
                    if (date != null) {
                        dateText.setText(outputFormat.format(date));
                    }
                } catch (Exception e) {
                    dateText.setText(task.date);
                }
            }
            if (timeText != null) {
                if (isFocusSession) {
                    String timeRange = String.format(Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                            task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                            task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                    timeText.setText(timeRange);
                } else {
                    timeText.setText(String.format(Locale.getDefault(), "%d:%02d %s",
                            task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
                }
            }
            if (durationRow != null && durationText != null && isFocusSession) {
                durationRow.setVisibility(View.VISIBLE);
                // Calculate duration
                int startMins = convertTo24Hour(task.hour, task.amPm) * 60 + task.minute;
                int endMins = convertTo24Hour(task.endHour, task.endAmPm) * 60 + task.endMinute;
                int durationMins = endMins - startMins;
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
            if (typeIcon != null && iconContainer != null && context instanceof android.app.Activity) {
                if (isFocusSession) {
                    typeIcon.setImageResource(R.drawable.ic_focus);
                    iconContainer.setCardBackgroundColor(((android.app.Activity) context).getColor(R.color.primary));
                } else {
                    typeIcon.setImageResource(R.drawable.ic_reminder);
                    iconContainer.setCardBackgroundColor(((android.app.Activity) context).getColor(R.color.success));
                }
            }
            if (alarmText != null) {
                alarmText.setText("✓ Completed");
            }

            popupWindow.showAtLocation(anchorView, android.view.Gravity.CENTER, 0, 0);

            // Add haptic feedback
            anchorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

            // Tap popup to dismiss - no edit functionality for completed tasks
            popupView.setOnClickListener(v -> {
                popupWindow.dismiss();
            });
        }

        private int convertTo24Hour(int hour, String amPm) {
            if (amPm == null) amPm = "AM";
            if (hour == 12) {
                return amPm.equals("AM") ? 0 : 12;
            } else {
                return amPm.equals("PM") ? hour + 12 : hour;
            }
        }
    }
}

