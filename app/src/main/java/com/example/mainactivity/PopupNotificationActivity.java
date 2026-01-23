package com.example.mainactivity;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Transparent activity that shows popup notifications for tasks and focus sessions.
 * This is used instead of an overlay service to avoid background service restrictions on newer Android versions.
 */
public class PopupNotificationActivity extends Activity {

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_NAME = "task_name";
    public static final String EXTRA_TASK_NOTE = "task_note";
    public static final String EXTRA_PRIORITY = "priority";
    public static final String EXTRA_TASK_TYPE = "task_type";
    public static final String EXTRA_TASK_HOUR = "task_hour";
    public static final String EXTRA_TASK_MINUTE = "task_minute";
    public static final String EXTRA_TASK_AMPM = "task_ampm";
    public static final String EXTRA_END_HOUR = "end_hour";
    public static final String EXTRA_END_MINUTE = "end_minute";
    public static final String EXTRA_END_AMPM = "end_ampm";
    public static final String EXTRA_FROM_NOTIFICATION = "from_notification";

    private static final String PREFS_NAME = "popup_session_prefs";
    private static final String KEY_SESSION_START = "session_start_";
    private static final String KEY_TOTAL_EXTENSIONS = "total_extensions_";
    
    private static final String CHANNEL_ID = "active_task_channel";
    private static final String CHANNEL_NAME = "Active Tasks";
    private static final int ONGOING_NOTIFICATION_ID_OFFSET = 50000;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private SharedPreferences prefs;
    private int taskId;
    private String taskName;
    private String taskType;
    private String taskNote;
    private String priority;
    private int hour, minute, endHour, endMinute;
    private String ampm, endAmPm;

    // Motivational messages
    private static final String[] MOTIVATIONAL_MESSAGES = {
        "🔥 You're doing amazing! Keep that momentum going!",
        "💪 Deep work pays off. You've got this!",
        "🌟 Every minute of focus brings you closer to success!",
        "🚀 You're in the zone! Keep pushing forward!",
        "🎯 Stay focused. Great things are happening!",
        "✨ Your dedication is inspiring. Keep it up!"
    };

    private static final String[] END_SESSION_MESSAGES = {
        "🎉 Incredible session! You focused for %s!",
        "🏆 Amazing work! %s of deep concentration!",
        "⭐ Fantastic! %s well spent on focused work!",
        "👏 Great job! %s of productivity complete!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make activity show over lock screen and as an overlay
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        timerHandler = new Handler(Looper.getMainLooper());

        Intent intent = getIntent();
        taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        taskName = intent.getStringExtra(EXTRA_TASK_NAME);
        taskNote = intent.getStringExtra(EXTRA_TASK_NOTE);
        priority = intent.getStringExtra(EXTRA_PRIORITY);
        taskType = intent.getStringExtra(EXTRA_TASK_TYPE);
        hour = intent.getIntExtra(EXTRA_TASK_HOUR, 0);
        minute = intent.getIntExtra(EXTRA_TASK_MINUTE, 0);
        ampm = intent.getStringExtra(EXTRA_TASK_AMPM);
        endHour = intent.getIntExtra(EXTRA_END_HOUR, 0);
        endMinute = intent.getIntExtra(EXTRA_END_MINUTE, 0);
        endAmPm = intent.getStringExtra(EXTRA_END_AMPM);

        if (taskName == null) {
            taskName = "Task Reminder";
        }
        
        // Cancel the trigger notification now that popup is shown
        cancelTriggerNotification();

        // Play alarm sound for ALL notifications (both preview and actual tasks)
        AlarmSoundHelper.playAlarmSound(this);
        android.util.Log.d("PopupNotification", "🔔 Playing alarm sound (taskId=" + taskId + ", type=" + taskType + ")");

        if ("focus".equals(taskType)) {
            showFocusSessionPopup();
        } else {
            showTaskPopup();
        }
    }
    
    /**
     * Cancel the notification that triggered this popup
     */
    private void cancelTriggerNotification() {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && taskId != -1) {
            notificationManager.cancel(taskId);
        }
    }

    private void showTaskPopup() {
        setContentView(R.layout.popup_task_reminder);

        // Make background clickable to dismiss
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setOnClickListener(v -> finishAndRemoveTask());
        }

        // Prevent clicks on the card from dismissing
        View card = findViewById(R.id.popupIconContainer);
        if (card != null) {
            View parent = (View) card.getParent();
            while (parent != null && parent != rootView) {
                parent.setOnClickListener(v -> {}); // Consume click
                if (parent.getParent() instanceof View) {
                    parent = (View) parent.getParent();
                } else {
                    break;
                }
            }
        }

        // Populate views
        TextView titleText = findViewById(R.id.popupTitle);
        TextView timeText = findViewById(R.id.popupTime);
        TextView descText = findViewById(R.id.popupDescription);
        TextView dateText = findViewById(R.id.popupDate);
        TextView priorityBadge = findViewById(R.id.priorityBadge);

        if (titleText != null) titleText.setText(taskName);
        if (timeText != null) {
            timeText.setText(String.format(Locale.getDefault(), "%d:%02d %s",
                    hour, minute, ampm != null ? ampm : "AM"));
        }

        if (descText != null) {
            if (taskNote != null && !taskNote.isEmpty()) {
                descText.setText(taskNote);
            } else {
                descText.setText("It's time to complete this task. Stay focused and productive!");
            }
        }

        if (dateText != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            dateText.setText("Today, " + dateFormat.format(new Date()));
        }

        // Set priority badge
        setPriorityBadge(priorityBadge, priority);
        
        // Load and display linked notes/checklist if any
        loadLinkedNotes();

        // Setup snooze buttons
        setupSnoozeButton(findViewById(R.id.snooze5Button), 5);
        setupSnoozeButton(findViewById(R.id.snooze15Button), 15);
        setupSnoozeButton(findViewById(R.id.snooze1hButton), 60);

        // Setup action buttons
        MaterialButton dismissBtn = findViewById(R.id.dismissButton);
        MaterialButton startBtn = findViewById(R.id.startButton);
        MaterialButton completeBtn = findViewById(R.id.completeButton);

        if (dismissBtn != null) {
            dismissBtn.setOnClickListener(v -> finishAndRemoveTask());
        }

        if (startBtn != null) {
            startBtn.setOnClickListener(v -> {
                // Show persistent notification for ongoing task
                showOngoingTaskNotification();
                Toast.makeText(this, "🎯 Let's do this! Good luck!", Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
            });
        }

        if (completeBtn != null) {
            completeBtn.setOnClickListener(v -> {
                markTaskComplete();
                cancelOngoingNotification();
                Toast.makeText(this, "✅ Task completed! Great job!", Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
            });
        }
    }
    
    /**
     * Load and display any notes linked to this task (AI-generated checklists)
     */
    private void loadLinkedNotes() {
        android.util.Log.d("PopupNotification", "loadLinkedNotes called with taskId=" + taskId);
        
        if (taskId <= 0) {
            android.util.Log.d("PopupNotification", "Invalid taskId, skipping linked notes load");
            return;
        }
        
        try {
            NoteDao noteDao = TaskDatabase.getInstance(this).noteDao();
            java.util.List<Note> linkedNotes = noteDao.getNotesForTask(taskId);
            
            android.util.Log.d("PopupNotification", "Found " + (linkedNotes != null ? linkedNotes.size() : 0) + " linked notes for taskId=" + taskId);
            
            if (linkedNotes != null && !linkedNotes.isEmpty()) {
                View linkedNotesCard = findViewById(R.id.linkedNotesCard);
                TextView linkedNotesContent = findViewById(R.id.linkedNotesContent);
                
                android.util.Log.d("PopupNotification", "linkedNotesCard=" + linkedNotesCard + ", linkedNotesContent=" + linkedNotesContent);
                
                if (linkedNotesCard != null && linkedNotesContent != null) {
                    // Build the checklist content
                    StringBuilder content = new StringBuilder();
                    for (Note note : linkedNotes) {
                        if (note.description != null && !note.description.isEmpty()) {
                            content.append(note.description);
                            if (linkedNotes.indexOf(note) < linkedNotes.size() - 1) {
                                content.append("\n");
                            }
                        }
                    }
                    
                    if (content.length() > 0) {
                        linkedNotesContent.setText(content.toString());
                        linkedNotesCard.setVisibility(View.VISIBLE);
                        android.util.Log.d("PopupNotification", "Showing linked notes card with content length=" + content.length());
                    }
                } else {
                    android.util.Log.w("PopupNotification", "Could not find linkedNotesCard or linkedNotesContent views");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PopupNotification", "Error loading linked notes", e);
        }
    }

    private void showFocusSessionPopup() {
        setContentView(R.layout.popup_focus_session);

        // Make background semi-transparent but not dismissable by click for focus sessions
        View rootView = findViewById(android.R.id.content);
        
        // Start session timer
        long sessionStart = getSessionStartTime(taskId);
        if (sessionStart == 0) {
            sessionStart = System.currentTimeMillis();
            saveSessionStartTime(taskId, sessionStart);
        }

        // Populate views
        TextView titleText = findViewById(R.id.popupTitle);
        TextView statusText = findViewById(R.id.popupStatus);
        TextView timerDisplay = findViewById(R.id.timerDisplay);
        TextView progressText = findViewById(R.id.sessionProgress);
        TextView motivationalMsg = findViewById(R.id.motivationalMessage);
        MaterialButton closeBtn = findViewById(R.id.closeButton);

        if (titleText != null) titleText.setText(taskName);
        if (statusText != null) statusText.setText("In Progress");

        // Random motivational message
        if (motivationalMsg != null) {
            int msgIndex = (int) (Math.random() * MOTIVATIONAL_MESSAGES.length);
            motivationalMsg.setText(MOTIVATIONAL_MESSAGES[msgIndex]);
        }

        // Calculate scheduled duration
        int scheduledMins = calculateDuration(hour, minute, ampm, endHour, endMinute, endAmPm);

        // Start timer updates
        final long finalSessionStart = sessionStart;
        final TextView finalTimerDisplay = timerDisplay;
        final TextView finalProgressText = progressText;
        
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - finalSessionStart;
                int extensions = getTotalExtensions(taskId);
                updateTimerDisplay(finalTimerDisplay, elapsed);
                updateProgress(finalProgressText, elapsed, scheduledMins + extensions);
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);

        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                // Just close the popup but keep the session running with a notification
                showOngoingFocusNotification();
                stopTimer();
                finishAndRemoveTask();
            });
        }

        // Extension buttons
        final TextView finalMotivationalMsg = motivationalMsg;
        setupExtendButton(findViewById(R.id.extend5Button), 5, finalMotivationalMsg);
        setupExtendButton(findViewById(R.id.extend15Button), 15, finalMotivationalMsg);
        setupExtendButton(findViewById(R.id.extend30Button), 30, finalMotivationalMsg);

        // Pause and End buttons
        MaterialButton pauseBtn = findViewById(R.id.pauseButton);
        MaterialButton endBtn = findViewById(R.id.endSessionButton);

        if (pauseBtn != null) {
            pauseBtn.setOnClickListener(v -> {
                // Show notification so user can resume
                showOngoingFocusNotification();
                Toast.makeText(this, "⏸ Session paused. Tap notification to resume!", Toast.LENGTH_SHORT).show();
                stopTimer();
                finishAndRemoveTask();
            });
        }

        final long sessionStartFinal = sessionStart;
        if (endBtn != null) {
            endBtn.setOnClickListener(v -> {
                long elapsed = System.currentTimeMillis() - sessionStartFinal;
                String duration = formatDuration(elapsed);

                int msgIdx = (int) (Math.random() * END_SESSION_MESSAGES.length);
                String message = String.format(END_SESSION_MESSAGES[msgIdx], duration);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                clearSessionData(taskId);
                cancelOngoingNotification();
                markTaskComplete();
                stopTimer();
                finishAndRemoveTask();
            });
        }
    }

    private void setupSnoozeButton(MaterialButton button, int minutes) {
        if (button != null) {
            button.setOnClickListener(v -> {
                snoozeTask(minutes);
                showPostponedNotification(minutes);
                Toast.makeText(this, "⏰ Reminder set for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
            });
        }
    }

    private void setupExtendButton(MaterialButton button, int minutes, TextView msgView) {
        if (button != null) {
            button.setOnClickListener(v -> {
                addExtension(taskId, minutes);
                extendFocusTask(minutes);
                // Update the ongoing notification
                showOngoingFocusNotification();
                Toast.makeText(this, "⏱ Extended by " + minutes + " minutes. Keep going!", Toast.LENGTH_SHORT).show();
                if (msgView != null) {
                    msgView.setText("🔥 Added " + minutes + " more minutes! You're on fire!");
                }
            });
        }
    }

    // ========== Persistent Notification Methods ==========
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // Low so it doesn't make sound but stays visible
            );
            channel.setDescription("Shows ongoing tasks and focus sessions");
            channel.setShowBadge(true);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showOngoingTaskNotification() {
        createNotificationChannel();
        
        // Intent to reopen this popup when notification is clicked
        Intent popupIntent = new Intent(this, PopupNotificationActivity.class);
        popupIntent.putExtra(EXTRA_TASK_ID, taskId);
        popupIntent.putExtra(EXTRA_TASK_NAME, taskName);
        popupIntent.putExtra(EXTRA_TASK_NOTE, taskNote);
        popupIntent.putExtra(EXTRA_PRIORITY, priority);
        popupIntent.putExtra(EXTRA_TASK_TYPE, taskType);
        popupIntent.putExtra(EXTRA_TASK_HOUR, hour);
        popupIntent.putExtra(EXTRA_TASK_MINUTE, minute);
        popupIntent.putExtra(EXTRA_TASK_AMPM, ampm);
        popupIntent.putExtra(EXTRA_FROM_NOTIFICATION, true);
        popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            taskId + ONGOING_NOTIFICATION_ID_OFFSET,
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("📋 " + taskName)
            .setContentText("Tap to view task details")
            .setOngoing(true) // Cannot be dismissed
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(taskId + ONGOING_NOTIFICATION_ID_OFFSET, builder.build());
        }
    }

    private void showOngoingFocusNotification() {
        createNotificationChannel();
        
        // Calculate elapsed time
        long sessionStart = getSessionStartTime(taskId);
        long elapsed = System.currentTimeMillis() - sessionStart;
        String elapsedStr = formatDuration(elapsed);
        int extensions = getTotalExtensions(taskId);
        
        // Intent to reopen this popup when notification is clicked
        Intent popupIntent = new Intent(this, PopupNotificationActivity.class);
        popupIntent.putExtra(EXTRA_TASK_ID, taskId);
        popupIntent.putExtra(EXTRA_TASK_NAME, taskName);
        popupIntent.putExtra(EXTRA_TASK_NOTE, taskNote);
        popupIntent.putExtra(EXTRA_PRIORITY, priority);
        popupIntent.putExtra(EXTRA_TASK_TYPE, taskType);
        popupIntent.putExtra(EXTRA_TASK_HOUR, hour);
        popupIntent.putExtra(EXTRA_TASK_MINUTE, minute);
        popupIntent.putExtra(EXTRA_TASK_AMPM, ampm);
        popupIntent.putExtra(EXTRA_END_HOUR, endHour);
        popupIntent.putExtra(EXTRA_END_MINUTE, endMinute);
        popupIntent.putExtra(EXTRA_END_AMPM, endAmPm);
        popupIntent.putExtra(EXTRA_FROM_NOTIFICATION, true);
        popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            taskId + ONGOING_NOTIFICATION_ID_OFFSET,
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String contentText = "Focused for " + elapsedStr;
        if (extensions > 0) {
            contentText += " (+" + extensions + " min extended)";
        }
        contentText += " • Tap to view";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_focus)
            .setContentTitle("🎯 " + taskName)
            .setContentText(contentText)
            .setOngoing(true) // Cannot be dismissed
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingIntent)
            .setUsesChronometer(true)
            .setWhen(sessionStart);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(taskId + ONGOING_NOTIFICATION_ID_OFFSET, builder.build());
        }
    }

    private void cancelOngoingNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(taskId + ONGOING_NOTIFICATION_ID_OFFSET);
        }
    }

    /**
     * Shows a notification indicating that the reminder has been postponed.
     * Tapping this notification will reopen the popup.
     */
    private void showPostponedNotification(int minutes) {
        createNotificationChannel();
        
        // Create a separate channel for postponed notifications (higher importance for visibility)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel postponedChannel = new NotificationChannel(
                "postponed_reminder_channel",
                "Postponed Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            postponedChannel.setDescription("Shows when a reminder has been postponed");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(postponedChannel);
            }
        }
        
        // Intent to reopen this popup when notification is clicked
        Intent popupIntent = new Intent(this, PopupNotificationActivity.class);
        popupIntent.putExtra(EXTRA_TASK_ID, taskId);
        popupIntent.putExtra(EXTRA_TASK_NAME, taskName);
        popupIntent.putExtra(EXTRA_TASK_NOTE, taskNote);
        popupIntent.putExtra(EXTRA_PRIORITY, priority);
        popupIntent.putExtra(EXTRA_TASK_TYPE, taskType);
        popupIntent.putExtra(EXTRA_TASK_HOUR, hour);
        popupIntent.putExtra(EXTRA_TASK_MINUTE, minute);
        popupIntent.putExtra(EXTRA_TASK_AMPM, ampm);
        popupIntent.putExtra(EXTRA_END_HOUR, endHour);
        popupIntent.putExtra(EXTRA_END_MINUTE, endMinute);
        popupIntent.putExtra(EXTRA_END_AMPM, endAmPm);
        popupIntent.putExtra(EXTRA_FROM_NOTIFICATION, true);
        popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Use a different request code offset for postponed notifications
        int postponedNotificationId = taskId + 60000;
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            postponedNotificationId,
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String timeText;
        if (minutes >= 60) {
            int hours = minutes / 60;
            int mins = minutes % 60;
            if (mins > 0) {
                timeText = hours + " hr " + mins + " min";
            } else {
                timeText = hours + " hour" + (hours > 1 ? "s" : "");
            }
        } else {
            timeText = minutes + " minute" + (minutes > 1 ? "s" : "");
        }

        boolean isFocus = "focus".equals(taskType);
        String emoji = isFocus ? "🎯" : "⏰";
        String contentTitle = emoji + " Reminder postponed";
        String contentText = "\"" + taskName + "\" will remind you in " + timeText + ". Tap to view now.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "postponed_reminder_channel")
            .setSmallIcon(isFocus ? R.drawable.ic_focus : R.drawable.ic_reminder)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(postponedNotificationId, builder.build());
        }
    }

    // ========== Helper Methods ==========

    private void setPriorityBadge(TextView badge, String priority) {
        if (badge == null) return;
        if (priority == null) priority = "None";

        switch (priority) {
            case "High":
                badge.setVisibility(View.VISIBLE);
                badge.setText("HIGH");
                badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.error)));
                break;
            case "Medium":
                badge.setVisibility(View.VISIBLE);
                badge.setText("MEDIUM");
                badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.warning)));
                break;
            case "Low":
                badge.setVisibility(View.VISIBLE);
                badge.setText("LOW");
                badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.success)));
                break;
            default:
                badge.setVisibility(View.GONE);
                break;
        }
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

    private void updateProgress(TextView progressText, long elapsedMs, int totalScheduledMins) {
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

    private int calculateDuration(int startHour, int startMin, String startAmPm,
                                   int endHour, int endMin, String endAmPm) {
        int startMins = convertTo24Hour(startHour, startAmPm) * 60 + startMin;
        int endMins = convertTo24Hour(endHour, endAmPm) * 60 + endMin;
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
        prefs.edit().putLong(KEY_SESSION_START + taskId, startTime).apply();
    }

    private long getSessionStartTime(int taskId) {
        return prefs.getLong(KEY_SESSION_START + taskId, 0);
    }

    private void addExtension(int taskId, int minutes) {
        int current = prefs.getInt(KEY_TOTAL_EXTENSIONS + taskId, 0);
        prefs.edit().putInt(KEY_TOTAL_EXTENSIONS + taskId, current + minutes).apply();
    }

    private int getTotalExtensions(int taskId) {
        return prefs.getInt(KEY_TOTAL_EXTENSIONS + taskId, 0);
    }

    private void clearSessionData(int taskId) {
        prefs.edit()
                .remove(KEY_SESSION_START + taskId)
                .remove(KEY_TOTAL_EXTENSIONS + taskId)
                .apply();
    }

    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void markTaskComplete() {
        try {
            TaskRepository repository = TaskRepository.getInstance();
            if (repository != null) {
                repository.initialize(this);
                Task task = repository.getTaskById(taskId);
                if (task != null) {
                    task.isComplete = true;
                    task.isAlarmOn = false;
                    repository.updateTask(task);
                    Intent broadcastIntent = new Intent(MainActivity.ACTION_TASK_COMPLETED);
                    sendBroadcast(broadcastIntent);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PopupNotification", "Error marking task complete: " + e.getMessage());
        }
    }

    private void snoozeTask(int minutes) {
        try {
            TaskRepository repository = TaskRepository.getInstance();
            if (repository != null) {
                repository.initialize(this);
                Task task = repository.getTaskById(taskId);
                if (task != null) {
                    AlarmHelper.snoozeTask(this, task, minutes);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PopupNotification", "Error snoozing task: " + e.getMessage());
        }
    }

    private void extendFocusTask(int minutes) {
        try {
            TaskRepository repository = TaskRepository.getInstance();
            if (repository != null) {
                repository.initialize(this);
                Task task = repository.getTaskById(taskId);
                if (task != null && task.isFocusTask()) {
                    AlarmHelper.extendFocusTask(this, task, minutes);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PopupNotification", "Error extending focus task: " + e.getMessage());
        }
    }

    @Override
    public void finish() {
        // Stop alarm sound when activity is finishing
        AlarmSoundHelper.stopAlarmSound();
        android.util.Log.d("PopupNotification", "🔇 Stopping alarm sound on finish()");
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        
        // Stop alarm sound if it's playing - safety net
        AlarmSoundHelper.stopAlarmSound();
    }

    /**
     * Launch the popup notification activity
     */
    public static void show(Context context, int taskId, String taskName,
                            String taskNote, String priority, String taskType,
                            int hour, int minute, String ampm,
                            int endHour, int endMinute, String endAmPm) {
        Intent intent = new Intent(context, PopupNotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                        Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_TASK_NAME, taskName);
        intent.putExtra(EXTRA_TASK_NOTE, taskNote);
        intent.putExtra(EXTRA_PRIORITY, priority);
        intent.putExtra(EXTRA_TASK_TYPE, taskType);
        intent.putExtra(EXTRA_TASK_HOUR, hour);
        intent.putExtra(EXTRA_TASK_MINUTE, minute);
        intent.putExtra(EXTRA_TASK_AMPM, ampm);
        intent.putExtra(EXTRA_END_HOUR, endHour);
        intent.putExtra(EXTRA_END_MINUTE, endMinute);
        intent.putExtra(EXTRA_END_AMPM, endAmPm);
        context.startActivity(intent);
    }
}
