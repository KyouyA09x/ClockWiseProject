package com.example.mainactivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OverlayNotificationService extends Service {

    private static final String FOREGROUND_CHANNEL_ID = "overlay_service_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 99999;

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

    private static final String PREFS_NAME = "overlay_session_prefs";
    private static final String KEY_SESSION_START = "session_start_";
    private static final String KEY_TOTAL_EXTENSIONS = "total_extensions_";

    private WindowManager windowManager;
    private View overlayView;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private SharedPreferences prefs;

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
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        timerHandler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Start as foreground service for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createForegroundNotificationChannel();
            Notification notification = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("ClockWise")
                .setContentText("Showing popup notification...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
            startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        }
    }
    
    private void createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Required for showing popup overlays");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        String taskName = intent.getStringExtra(EXTRA_TASK_NAME);
        String taskNote = intent.getStringExtra(EXTRA_TASK_NOTE);
        String priority = intent.getStringExtra(EXTRA_PRIORITY);
        String taskType = intent.getStringExtra(EXTRA_TASK_TYPE);
        int hour = intent.getIntExtra(EXTRA_TASK_HOUR, 0);
        int minute = intent.getIntExtra(EXTRA_TASK_MINUTE, 0);
        String ampm = intent.getStringExtra(EXTRA_TASK_AMPM);
        int endHour = intent.getIntExtra(EXTRA_END_HOUR, 0);
        int endMinute = intent.getIntExtra(EXTRA_END_MINUTE, 0);
        String endAmPm = intent.getStringExtra(EXTRA_END_AMPM);
        
        // Store current task info for notifications
        currentTaskId = taskId;
        currentTaskName = taskName;
        currentTaskType = taskType;

        if (taskName == null) {
            taskName = "Task Reminder";
        }

        // Play alarm sound for ALL notifications (both actual tasks and previews)
        AlarmSoundHelper.playAlarmSound(this);
        android.util.Log.d("OverlayNotification", "🔔 Playing alarm sound (taskId=" + taskId + ", type=" + taskType + ")");

        if ("focus".equals(taskType)) {
            showFocusSessionOverlay(taskId, taskName, taskNote, priority, hour, minute, ampm, endHour, endMinute, endAmPm);
        } else {
            showTaskOverlay(taskId, taskName, taskNote, priority, hour, minute, ampm);
        }

        return START_NOT_STICKY;
    }

    private void showTaskOverlay(int taskId, String taskName, String taskNote, String priority, 
                                  int hour, int minute, String ampm) {
        removeExistingOverlay();

        // Use ContextThemeWrapper to provide proper theme for Material components
        android.content.Context themedContext = new android.view.ContextThemeWrapper(this, R.style.Theme_MainActivity);
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        overlayView = inflater.inflate(R.layout.popup_task_reminder, null);

        WindowManager.LayoutParams params = createWindowParams();

        // Populate views
        TextView titleText = overlayView.findViewById(R.id.popupTitle);
        TextView timeText = overlayView.findViewById(R.id.popupTime);
        TextView descText = overlayView.findViewById(R.id.popupDescription);
        TextView dateText = overlayView.findViewById(R.id.popupDate);
        TextView priorityBadge = overlayView.findViewById(R.id.priorityBadge);
        MaterialCardView iconContainer = overlayView.findViewById(R.id.popupIconContainer);

        titleText.setText(taskName);
        timeText.setText(String.format(Locale.getDefault(), "%d:%02d %s", 
                hour, minute, ampm != null ? ampm : "AM"));
        
        if (taskNote != null && !taskNote.isEmpty()) {
            descText.setText(taskNote);
        } else {
            descText.setText("It's time to complete this task. Stay focused and productive!");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        dateText.setText("Today, " + dateFormat.format(new Date()).split(", ")[1]);

        // Set priority badge
        setPriorityBadge(priorityBadge, priority);

        // Setup snooze buttons
        setupSnoozeButton(overlayView.findViewById(R.id.snooze5Button), taskId, 5);
        setupSnoozeButton(overlayView.findViewById(R.id.snooze15Button), taskId, 15);
        setupSnoozeButton(overlayView.findViewById(R.id.snooze1hButton), taskId, 60);

        // Setup action buttons
        MaterialButton dismissBtn = overlayView.findViewById(R.id.dismissButton);
        MaterialButton startBtn = overlayView.findViewById(R.id.startButton);
        MaterialButton completeBtn = overlayView.findViewById(R.id.completeButton);

        dismissBtn.setOnClickListener(v -> dismissOverlay());
        
        startBtn.setOnClickListener(v -> {
            Toast.makeText(this, "🎯 Let's do this! Good luck!", Toast.LENGTH_SHORT).show();
            dismissOverlay();
        });
        
        completeBtn.setOnClickListener(v -> {
            markTaskComplete(taskId);
            Toast.makeText(this, "✅ Task completed! Great job!", Toast.LENGTH_SHORT).show();
            dismissOverlay();
        });

        addOverlayToWindow(params);
    }

    private void showFocusSessionOverlay(int taskId, String taskName, String taskNote, String priority,
                                          int hour, int minute, String ampm, 
                                          int endHour, int endMinute, String endAmPm) {
        removeExistingOverlay();

        // Use ContextThemeWrapper to provide proper theme for Material components
        android.content.Context themedContext = new android.view.ContextThemeWrapper(this, R.style.Theme_MainActivity);
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        overlayView = inflater.inflate(R.layout.popup_focus_session, null);

        WindowManager.LayoutParams params = createWindowParams();

        // Start session timer
        long sessionStart = getSessionStartTime(taskId);
        if (sessionStart == 0) {
            sessionStart = System.currentTimeMillis();
            saveSessionStartTime(taskId, sessionStart);
        }

        // Populate views
        TextView titleText = overlayView.findViewById(R.id.popupTitle);
        TextView statusText = overlayView.findViewById(R.id.popupStatus);
        TextView timerDisplay = overlayView.findViewById(R.id.timerDisplay);
        TextView progressText = overlayView.findViewById(R.id.sessionProgress);
        TextView motivationalMsg = overlayView.findViewById(R.id.motivationalMessage);
        MaterialButton closeBtn = overlayView.findViewById(R.id.closeButton);

        titleText.setText(taskName);
        statusText.setText("In Progress");

        // Random motivational message
        int msgIndex = (int) (Math.random() * MOTIVATIONAL_MESSAGES.length);
        motivationalMsg.setText(MOTIVATIONAL_MESSAGES[msgIndex]);

        // Calculate scheduled duration
        int scheduledMins = calculateDuration(hour, minute, ampm, endHour, endMinute, endAmPm);

        // Start timer updates
        final long finalSessionStart = sessionStart;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (overlayView != null) {
                    long elapsed = System.currentTimeMillis() - finalSessionStart;
                    int extensions = getTotalExtensions(taskId);
                    updateTimerDisplay(timerDisplay, elapsed);
                    updateProgress(progressText, elapsed, scheduledMins + extensions);
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);

        closeBtn.setOnClickListener(v -> {
            stopTimer();
            dismissOverlay();
        });

        // Extension buttons
        setupExtendButton(overlayView.findViewById(R.id.extend5Button), taskId, 5, motivationalMsg);
        setupExtendButton(overlayView.findViewById(R.id.extend15Button), taskId, 15, motivationalMsg);
        setupExtendButton(overlayView.findViewById(R.id.extend30Button), taskId, 30, motivationalMsg);

        // Pause and End buttons
        MaterialButton pauseBtn = overlayView.findViewById(R.id.pauseButton);
        MaterialButton endBtn = overlayView.findViewById(R.id.endSessionButton);

        pauseBtn.setOnClickListener(v -> {
            Toast.makeText(this, "⏸ Session paused. Take a quick break!", Toast.LENGTH_SHORT).show();
            stopTimer();
            dismissOverlay();
        });

        final long sessionStartFinal = sessionStart;
        endBtn.setOnClickListener(v -> {
            long elapsed = System.currentTimeMillis() - sessionStartFinal;
            String duration = formatDuration(elapsed);
            
            int msgIdx = (int) (Math.random() * END_SESSION_MESSAGES.length);
            String message = String.format(END_SESSION_MESSAGES[msgIdx], duration);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            
            clearSessionData(taskId);
            markTaskComplete(taskId);
            stopTimer();
            dismissOverlay();
        });

        addOverlayToWindow(params);
    }

    private void setupSnoozeButton(MaterialButton button, int taskId, int minutes) {
        if (button != null) {
            button.setOnClickListener(v -> {
                snoozeTask(taskId, minutes);
                showPostponedNotification(taskId, minutes);
                Toast.makeText(this, "⏰ Reminder set for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
                dismissOverlay();
            });
        }
    }
    
    // Store current task info for notifications
    private int currentTaskId;
    private String currentTaskName;
    private String currentTaskType;

    private void setupExtendButton(MaterialButton button, int taskId, int minutes, TextView msgView) {
        if (button != null) {
            button.setOnClickListener(v -> {
                addExtension(taskId, minutes);
                extendFocusTask(taskId, minutes);
                Toast.makeText(this, "⏱ Extended by " + minutes + " minutes. Keep going!", Toast.LENGTH_SHORT).show();
                if (msgView != null) {
                    msgView.setText("🔥 Added " + minutes + " more minutes! You're on fire!");
                }
            });
        }
    }

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

    private WindowManager.LayoutParams createWindowParams() {
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;
        return params;
    }

    private void removeExistingOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                // Ignore
            }
            overlayView = null;
        }
    }

    private void addOverlayToWindow(WindowManager.LayoutParams params) {
        try {
            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            android.util.Log.e("OverlayNotification", "Failed to add overlay: " + e.getMessage());
            stopSelf();
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

    private void markTaskComplete(int taskId) {
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
            android.util.Log.e("OverlayNotification", "Error marking task complete: " + e.getMessage());
        }
    }

    private void snoozeTask(int taskId, int minutes) {
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
            android.util.Log.e("OverlayNotification", "Error snoozing task: " + e.getMessage());
        }
    }

    private void extendFocusTask(int taskId, int minutes) {
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
            android.util.Log.e("OverlayNotification", "Error extending focus task: " + e.getMessage());
        }
    }

    private void dismissOverlay() {
        // Stop alarm sound immediately when dismissing (for preview mode)
        if (currentTaskId < 0) {
            AlarmSoundHelper.stopAlarmSound();
            android.util.Log.d("OverlayNotification", "🔇 Stopping alarm sound on dismiss");
        }
        
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
                overlayView = null;
            } catch (Exception e) {
                // Ignore
            }
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();
        
        // Stop alarm sound if playing - safety net for ALL tasks
        AlarmSoundHelper.stopAlarmSound();
        android.util.Log.d("OverlayNotification", "🔇 Stopping alarm sound on destroy (taskId=" + currentTaskId + ")");
        
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return android.provider.Settings.canDrawOverlays(context);
        }
        return true;
    }
    
    /**
     * Shows a notification indicating that the reminder has been postponed.
     * Tapping this notification will reopen the popup.
     */
    private void showPostponedNotification(int taskId, int minutes) {
        // Create a separate channel for postponed notifications
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
        
        // Intent to reopen overlay when notification is clicked
        Intent serviceIntent = new Intent(this, OverlayNotificationService.class);
        serviceIntent.putExtra(EXTRA_TASK_ID, currentTaskId);
        serviceIntent.putExtra(EXTRA_TASK_NAME, currentTaskName);
        serviceIntent.putExtra(EXTRA_TASK_TYPE, currentTaskType);
        
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getService(
            this,
            taskId + 60000,
            serviceIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
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

        boolean isFocus = "focus".equals(currentTaskType);
        String emoji = isFocus ? "🎯" : "⏰";
        String contentTitle = emoji + " Reminder postponed";
        String contentText = "\"" + currentTaskName + "\" will remind you in " + timeText + ". Tap to view now.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "postponed_reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(taskId + 60000, builder.build());
        }
    }

    public static void showNotification(Context context, int taskId, String taskName, 
                                         String taskNote, String priority, String taskType) {
        if (!canDrawOverlays(context)) {
            return;
        }

        Intent intent = new Intent(context, OverlayNotificationService.class);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_TASK_NAME, taskName);
        intent.putExtra(EXTRA_TASK_NOTE, taskNote);
        intent.putExtra(EXTRA_PRIORITY, priority);
        intent.putExtra(EXTRA_TASK_TYPE, taskType);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void showNotificationWithTime(Context context, int taskId, String taskName, 
                                                  String taskNote, String priority, String taskType,
                                                  int hour, int minute, String ampm,
                                                  int endHour, int endMinute, String endAmPm) {
        if (!canDrawOverlays(context)) {
            return;
        }

        Intent intent = new Intent(context, OverlayNotificationService.class);
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
