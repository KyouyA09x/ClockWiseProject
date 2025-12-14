package com.example.mainactivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manages popup notifications for tasks and focus sessions.
 * Handles display, user interaction tracking, and session timing.
 */
public class PopupNotificationManager {

    private static final String PREFS_NAME = "popup_notification_prefs";
    private static final String KEY_SESSION_START_TIME = "session_start_time_";
    private static final String KEY_TOTAL_EXTENSION_MINS = "total_extension_";
    private static final String KEY_SNOOZE_COUNT = "snooze_count_";

    private final Context context;
    private final SharedPreferences prefs;
    private Dialog currentDialog;
    private Handler timerHandler;
    private Runnable timerRunnable;

    // Motivational messages for focus sessions
    private static final String[] MOTIVATIONAL_MESSAGES = {
        "🔥 You're doing amazing! Keep that momentum going!",
        "💪 Deep work pays off. You've got this!",
        "🌟 Every minute of focus brings you closer to success!",
        "🚀 You're in the zone! Keep pushing forward!",
        "🎯 Stay focused. Great things are happening!",
        "✨ Your dedication is inspiring. Keep it up!",
        "🏆 Champions are made in moments like these!",
        "💡 Your focus today creates tomorrow's success!"
    };

    // Motivational messages when ending session
    private static final String[] END_SESSION_ENCOURAGEMENTS = {
        "You've been focused for %s! That's incredible dedication!",
        "Wow, %s of deep work! You should be proud!",
        "Amazing session! %s of focused productivity!",
        "%s well spent. Take a well-deserved break!",
        "Great job staying focused for %s! You're crushing it!"
    };

    public PopupNotificationManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.timerHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Shows a popup notification for a task reminder.
     */
    public void showTaskPopup(Activity activity, Task task, OnTaskActionListener listener) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_task_reminder);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(params);
        }

        // Populate views
        TextView titleText = dialog.findViewById(R.id.popupTitle);
        TextView typeText = dialog.findViewById(R.id.popupType);
        TextView timeText = dialog.findViewById(R.id.popupTime);
        TextView descText = dialog.findViewById(R.id.popupDescription);
        TextView dateText = dialog.findViewById(R.id.popupDate);
        TextView priorityBadge = dialog.findViewById(R.id.priorityBadge);
        MaterialCardView iconContainer = dialog.findViewById(R.id.popupIconContainer);

        if (titleText != null) titleText.setText(task.name);
        if (timeText != null) {
            timeText.setText(String.format(Locale.getDefault(), "%d:%02d %s",
                    task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
        }
        if (descText != null) {
            String desc = task.noteContent != null && !task.noteContent.isEmpty()
                    ? task.noteContent
                    : "It's time to complete this task. Stay focused and productive!";
            descText.setText(desc);
        }
        if (dateText != null) {
            SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = inputFormat.parse(task.date);
                if (date != null) {
                    // Check if today
                    String today = inputFormat.format(new Date());
                    if (task.date.equals(today)) {
                        dateText.setText("Today, " + new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date));
                    } else {
                        dateText.setText(displayFormat.format(date));
                    }
                }
            } catch (Exception e) {
                dateText.setText(task.date);
            }
        }

        // Set priority badge
        if (priorityBadge != null) {
            String priority = task.urgency != null ? task.urgency : "None";
            switch (priority) {
                case "High":
                    priorityBadge.setVisibility(View.VISIBLE);
                    priorityBadge.setText("HIGH");
                    priorityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            activity.getColor(R.color.error)));
                    break;
                case "Medium":
                    priorityBadge.setVisibility(View.VISIBLE);
                    priorityBadge.setText("MEDIUM");
                    priorityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            activity.getColor(R.color.warning)));
                    break;
                case "Low":
                    priorityBadge.setVisibility(View.VISIBLE);
                    priorityBadge.setText("LOW");
                    priorityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            activity.getColor(R.color.success)));
                    break;
                default:
                    priorityBadge.setVisibility(View.GONE);
                    break;
            }
        }

        // Setup snooze buttons
        MaterialButton snooze5 = dialog.findViewById(R.id.snooze5Button);
        MaterialButton snooze15 = dialog.findViewById(R.id.snooze15Button);
        MaterialButton snooze1h = dialog.findViewById(R.id.snooze1hButton);

        if (snooze5 != null) {
            snooze5.setOnClickListener(v -> {
                incrementSnoozeCount(task.id);
                if (listener != null) listener.onSnooze(task, 5);
                Toast.makeText(activity, "⏰ Reminder set for 5 minutes", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }
        if (snooze15 != null) {
            snooze15.setOnClickListener(v -> {
                incrementSnoozeCount(task.id);
                if (listener != null) listener.onSnooze(task, 15);
                Toast.makeText(activity, "⏰ Reminder set for 15 minutes", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }
        if (snooze1h != null) {
            snooze1h.setOnClickListener(v -> {
                incrementSnoozeCount(task.id);
                if (listener != null) listener.onSnooze(task, 60);
                Toast.makeText(activity, "⏰ Reminder set for 1 hour", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        // Setup action buttons
        MaterialButton dismissBtn = dialog.findViewById(R.id.dismissButton);
        MaterialButton startBtn = dialog.findViewById(R.id.startButton);
        MaterialButton completeBtn = dialog.findViewById(R.id.completeButton);

        if (dismissBtn != null) {
            dismissBtn.setOnClickListener(v -> {
                if (listener != null) listener.onDismiss(task);
                dialog.dismiss();
            });
        }
        if (startBtn != null) {
            startBtn.setOnClickListener(v -> {
                if (listener != null) listener.onStartWorking(task);
                Toast.makeText(activity, "🎯 Let's do this!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }
        if (completeBtn != null) {
            completeBtn.setOnClickListener(v -> {
                if (listener != null) listener.onComplete(task);
                Toast.makeText(activity, "✅ Task completed! Great job!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        currentDialog = dialog;
        dialog.show();
    }

    /**
     * Shows a popup notification for a focus session.
     */
    public void showFocusSessionPopup(Activity activity, Task task, boolean isSessionStart, OnFocusSessionActionListener listener) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_focus_session);
        dialog.setCancelable(false); // Focus session popup should not be easily dismissed

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(params);
        }

        // Get or start session timer
        long sessionStartTime = getSessionStartTime(task.id);
        if (sessionStartTime == 0) {
            sessionStartTime = System.currentTimeMillis();
            saveSessionStartTime(task.id, sessionStartTime);
        }

        // Populate views
        TextView titleText = dialog.findViewById(R.id.popupTitle);
        TextView statusText = dialog.findViewById(R.id.popupStatus);
        TextView timerDisplay = dialog.findViewById(R.id.timerDisplay);
        TextView progressText = dialog.findViewById(R.id.sessionProgress);
        TextView motivationalMsg = dialog.findViewById(R.id.motivationalMessage);
        MaterialButton closeBtn = dialog.findViewById(R.id.closeButton);

        if (titleText != null) titleText.setText(task.name);
        if (statusText != null) {
            statusText.setText(isSessionStart ? "Starting now" : "In Progress");
        }

        // Random motivational message
        if (motivationalMsg != null) {
            int index = (int) (Math.random() * MOTIVATIONAL_MESSAGES.length);
            motivationalMsg.setText(MOTIVATIONAL_MESSAGES[index]);
        }

        // Calculate scheduled duration
        int scheduledDurationMins = calculateFocusDuration(task);

        // Start timer update
        final long finalSessionStart = sessionStartTime;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timerDisplay != null && dialog.isShowing()) {
                    long elapsed = System.currentTimeMillis() - finalSessionStart;
                    int totalExtensions = getTotalExtensionMinutes(task.id);
                    int totalScheduledMins = scheduledDurationMins + totalExtensions;
                    
                    updateTimerDisplay(timerDisplay, elapsed);
                    updateProgressText(progressText, elapsed, totalScheduledMins);
                    
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);

        // Close button
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                stopTimer();
                dialog.dismiss();
            });
        }

        // Extension buttons
        MaterialButton extend5 = dialog.findViewById(R.id.extend5Button);
        MaterialButton extend15 = dialog.findViewById(R.id.extend15Button);
        MaterialButton extend30 = dialog.findViewById(R.id.extend30Button);

        View.OnClickListener extendListener = v -> {
            int minutes = 5;
            if (v.getId() == R.id.extend15Button) minutes = 15;
            else if (v.getId() == R.id.extend30Button) minutes = 30;
            
            addExtensionTime(task.id, minutes);
            if (listener != null) listener.onExtend(task, minutes);
            Toast.makeText(activity, "⏱ Extended by " + minutes + " minutes. Keep going!", Toast.LENGTH_SHORT).show();
            
            // Update motivational message
            if (motivationalMsg != null) {
                motivationalMsg.setText("🔥 Added " + minutes + " more minutes! You're on fire!");
            }
        };

        if (extend5 != null) extend5.setOnClickListener(extendListener);
        if (extend15 != null) extend15.setOnClickListener(extendListener);
        if (extend30 != null) extend30.setOnClickListener(extendListener);

        // Pause and End buttons
        MaterialButton pauseBtn = dialog.findViewById(R.id.pauseButton);
        MaterialButton endBtn = dialog.findViewById(R.id.endSessionButton);

        if (pauseBtn != null) {
            pauseBtn.setOnClickListener(v -> {
                if (listener != null) listener.onPause(task);
                Toast.makeText(activity, "⏸ Session paused. Take a quick break!", Toast.LENGTH_SHORT).show();
                stopTimer();
                dialog.dismiss();
            });
        }

        if (endBtn != null) {
            final long sessionStart = sessionStartTime;
            endBtn.setOnClickListener(v -> {
                long elapsed = System.currentTimeMillis() - sessionStart;
                String durationStr = formatDuration(elapsed);
                
                // Show encouraging end message
                showEndSessionConfirmation(activity, task, durationStr, () -> {
                    clearSessionData(task.id);
                    if (listener != null) listener.onComplete(task);
                    stopTimer();
                    dialog.dismiss();
                });
            });
        }

        currentDialog = dialog;
        dialog.show();
    }

    private void showEndSessionConfirmation(Activity activity, Task task, String duration, Runnable onConfirm) {
        Dialog confirmDialog = new Dialog(activity);
        confirmDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        confirmDialog.setContentView(R.layout.dialog_notification_preview);

        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView titleText = confirmDialog.findViewById(R.id.notificationTitle);
        TextView typeText = confirmDialog.findViewById(R.id.notificationType);
        TextView messageText = confirmDialog.findViewById(R.id.notificationMessage);
        MaterialButton dismissBtn = confirmDialog.findViewById(R.id.dismissButton);
        MaterialButton actionBtn = confirmDialog.findViewById(R.id.actionButton);

        if (titleText != null) titleText.setText("Great Session! 🎉");
        if (typeText != null) {
            typeText.setText("Focus Session Complete");
            typeText.setTextColor(activity.getColor(R.color.success));
        }
        if (messageText != null) {
            int msgIndex = (int) (Math.random() * END_SESSION_ENCOURAGEMENTS.length);
            String message = String.format(END_SESSION_ENCOURAGEMENTS[msgIndex], duration);
            messageText.setText(message);
        }

        if (dismissBtn != null) {
            dismissBtn.setText("Keep Going");
            dismissBtn.setOnClickListener(v -> confirmDialog.dismiss());
        }
        if (actionBtn != null) {
            actionBtn.setText("End Session");
            actionBtn.setOnClickListener(v -> {
                confirmDialog.dismiss();
                onConfirm.run();
            });
        }

        confirmDialog.show();
    }

    private void updateTimerDisplay(TextView display, long elapsedMs) {
        if (display == null) return;
        long seconds = elapsedMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                hours, minutes % 60, seconds % 60);
        display.setText(time);
    }

    private void updateProgressText(TextView progressText, long elapsedMs, int totalScheduledMins) {
        if (progressText == null || totalScheduledMins == 0) return;
        
        long elapsedMins = elapsedMs / 60000;
        int percentage = Math.min(100, (int) ((elapsedMins * 100) / totalScheduledMins));
        
        if (percentage >= 100) {
            progressText.setText("🎯 Goal reached! Great job!");
        } else {
            progressText.setText(percentage + "% of scheduled time");
        }
    }

    private String formatDuration(long elapsedMs) {
        long seconds = elapsedMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d hr %d min", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d minutes", minutes);
        } else {
            return "less than a minute";
        }
    }

    private int calculateFocusDuration(Task task) {
        int startMins = convertTo24Hour(task.hour, task.amPm) * 60 + task.minute;
        int endMins = convertTo24Hour(task.endHour, task.endAmPm) * 60 + task.endMinute;
        int duration = endMins - startMins;
        if (duration < 0) duration += 24 * 60;
        return duration;
    }

    private int convertTo24Hour(int hour, String amPm) {
        if (amPm == null) amPm = "AM";
        if (hour == 12) {
            return amPm.equals("AM") ? 0 : 12;
        } else {
            return amPm.equals("PM") ? hour + 12 : hour;
        }
    }

    // Session data persistence
    private void saveSessionStartTime(int taskId, long startTime) {
        prefs.edit().putLong(KEY_SESSION_START_TIME + taskId, startTime).apply();
    }

    private long getSessionStartTime(int taskId) {
        return prefs.getLong(KEY_SESSION_START_TIME + taskId, 0);
    }

    private void addExtensionTime(int taskId, int minutes) {
        int current = prefs.getInt(KEY_TOTAL_EXTENSION_MINS + taskId, 0);
        prefs.edit().putInt(KEY_TOTAL_EXTENSION_MINS + taskId, current + minutes).apply();
    }

    private int getTotalExtensionMinutes(int taskId) {
        return prefs.getInt(KEY_TOTAL_EXTENSION_MINS + taskId, 0);
    }

    private void incrementSnoozeCount(int taskId) {
        int count = prefs.getInt(KEY_SNOOZE_COUNT + taskId, 0);
        prefs.edit().putInt(KEY_SNOOZE_COUNT + taskId, count + 1).apply();
    }

    public int getSnoozeCount(int taskId) {
        return prefs.getInt(KEY_SNOOZE_COUNT + taskId, 0);
    }

    private void clearSessionData(int taskId) {
        prefs.edit()
                .remove(KEY_SESSION_START_TIME + taskId)
                .remove(KEY_TOTAL_EXTENSION_MINS + taskId)
                .apply();
    }

    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    public void dismissCurrent() {
        if (currentDialog != null && currentDialog.isShowing()) {
            stopTimer();
            currentDialog.dismiss();
        }
    }

    // Listener interfaces
    public interface OnTaskActionListener {
        void onSnooze(Task task, int minutes);
        void onDismiss(Task task);
        void onStartWorking(Task task);
        void onComplete(Task task);
    }

    public interface OnFocusSessionActionListener {
        void onExtend(Task task, int minutes);
        void onPause(Task task);
        void onComplete(Task task);
    }
}
