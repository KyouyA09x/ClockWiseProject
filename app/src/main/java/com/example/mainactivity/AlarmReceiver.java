package com.example.mainactivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

@SuppressLint("MissingPermission")
public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "task_notification_channel";
    private static final String CHANNEL_NAME = "Task Notifications";

    public static final String ACTION_IGNORE = "com.example.mainactivity.ACTION_IGNORE";
    public static final String ACTION_SNOOZE = "com.example.mainactivity.ACTION_SNOOZE";
    public static final String ACTION_CANCEL = "com.example.mainactivity.ACTION_CANCEL";
    public static final String ACTION_KEEP_GOING = "com.example.mainactivity.ACTION_KEEP_GOING";
    public static final String ACTION_DONE = "com.example.mainactivity.ACTION_DONE";

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_NAME = "task_name";
    public static final String EXTRA_TASK_TIME = "task_time";
    public static final String EXTRA_VIBRATION_ENABLED = "vibration_enabled";
    public static final String EXTRA_ALARM_TYPE = "alarm_type";
    public static final String EXTRA_TASK_TYPE = "task_type";
    public static final String EXTRA_END_TIME = "end_time";
    public static final String EXTRA_EXTEND_MINUTES = "extend_minutes";

    // Alarm types for Focus Task
    public static final String ALARM_TYPE_FOCUS_START = "focus_start";
    public static final String ALARM_TYPE_FOCUS_END = "focus_end";
    public static final String ALARM_TYPE_REMINDER = "reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Handle notification action buttons
        if (ACTION_IGNORE.equals(action)) {
            handleIgnore(context, intent);
            return;
        } else if (ACTION_SNOOZE.equals(action)) {
            handleSnooze(context, intent);
            return;
        } else if (ACTION_CANCEL.equals(action)) {
            handleCancel(context, intent);
            return;
        } else if (ACTION_KEEP_GOING.equals(action)) {
            handleKeepGoing(context, intent);
            return;
        } else if (ACTION_DONE.equals(action)) {
            handleDone(context, intent);
            return;
        }

        // Handle alarm trigger
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        String taskName = intent.getStringExtra(EXTRA_TASK_NAME);
        String taskTime = intent.getStringExtra(EXTRA_TASK_TIME);
        boolean vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, false);
        String alarmType = intent.getStringExtra(EXTRA_ALARM_TYPE);
        String taskType = intent.getStringExtra(EXTRA_TASK_TYPE);
        String endTime = intent.getStringExtra(EXTRA_END_TIME);

        if (taskName == null) {
            taskName = "Task Reminder";
        }
        if (taskTime == null) {
            taskTime = "";
        }

        createNotificationChannel(context);

        // Show appropriate notification based on task type
        if ("focus".equals(taskType)) {
            showFocusTaskNotification(context, taskId, taskName, taskTime, endTime, alarmType, vibrationEnabled);
        } else {
            showNotification(context, taskId, taskName, taskTime, vibrationEnabled);
        }
    }

    private void handleKeepGoing(Context context, Intent intent) {
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        int extendMinutes = intent.getIntExtra(EXTRA_EXTEND_MINUTES, 30); // Default 30 minutes

        // Dismiss current notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(taskId);
        }

        // Get task and extend it
        TaskRepository repository = TaskRepository.getInstance();
        repository.initialize(context);
        Task task = repository.getTaskById(taskId);

        if (task != null && task.isFocusTask()) {
            AlarmHelper.extendFocusTask(context, task, extendMinutes);
        }
    }

    private void handleDone(Context context, Intent intent) {
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);

        // Dismiss notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(taskId);
        }

        // Mark task as complete - it will now appear in History instead of main screen
        TaskRepository repository = TaskRepository.getInstance();
        repository.initialize(context);
        Task task = repository.getTaskById(taskId);
        if (task != null) {
            task.isComplete = true;
            task.isAlarmOn = false; // Turn off alarm since task is done
            repository.updateTask(task);

            // Cancel any remaining alarms for this task
            if (task.isFocusTask()) {
                AlarmHelper.cancelFocusTaskAlarms(context, task);
            } else {
                AlarmHelper.cancelTaskAlarm(context, task);
            }

            // Send broadcast to refresh MainActivity immediately
            Intent broadcastIntent = new Intent(MainActivity.ACTION_TASK_COMPLETED);
            context.sendBroadcast(broadcastIntent);
        }
    }

    private void handleIgnore(Context context, Intent intent) {
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        // Just dismiss the notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(taskId);
        }
    }

    private void handleSnooze(Context context, Intent intent) {
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        String taskName = intent.getStringExtra(EXTRA_TASK_NAME);
        String taskTime = intent.getStringExtra(EXTRA_TASK_TIME);
        boolean vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, false);

        // Dismiss current notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(taskId);
        }

        // Schedule snooze for 5 minutes later
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
            snoozeIntent.putExtra(EXTRA_TASK_ID, taskId);
            snoozeIntent.putExtra(EXTRA_TASK_NAME, taskName);
            snoozeIntent.putExtra(EXTRA_TASK_TIME, taskTime);
            snoozeIntent.putExtra(EXTRA_VIBRATION_ENABLED, vibrationEnabled);

            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId + 10000, // Different request code to avoid conflicts
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, snoozePendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, snoozePendingIntent);
                }
            } catch (SecurityException e) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, snoozePendingIntent);
            }
        }
    }

    private void handleCancel(Context context, Intent intent) {
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);

        // Dismiss notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(taskId);
        }

        // Cancel the alarm completely
        TaskRepository repository = TaskRepository.getInstance();
        repository.initialize(context);
        Task task = repository.getTaskById(taskId);
        if (task != null) {
            task.isAlarmOn = false;
            repository.updateTask(task);
            AlarmHelper.cancelTaskAlarm(context, task);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // HIGH importance for heads-up notification
            );
            channel.setDescription("Notifications for task reminders");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private void showNotificationInternal(Context context, int taskId, String taskName, String taskTime, boolean vibrationEnabled) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create Ignore action
        Intent ignoreIntent = new Intent(context, AlarmReceiver.class);
        ignoreIntent.setAction(ACTION_IGNORE);
        ignoreIntent.putExtra(EXTRA_TASK_ID, taskId);
        PendingIntent ignorePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId + 1000,
                ignoreIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create Snooze action
        Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra(EXTRA_TASK_ID, taskId);
        snoozeIntent.putExtra(EXTRA_TASK_NAME, taskName);
        snoozeIntent.putExtra(EXTRA_TASK_TIME, taskTime);
        snoozeIntent.putExtra(EXTRA_VIBRATION_ENABLED, vibrationEnabled);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId + 2000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create Cancel action
        Intent cancelIntent = new Intent(context, AlarmReceiver.class);
        cancelIntent.setAction(ACTION_CANCEL);
        cancelIntent.putExtra(EXTRA_TASK_ID, taskId);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                context,
                taskId + 3000,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create Done action - marks task as complete and moves to History
        Intent doneIntent = new Intent(context, AlarmReceiver.class);
        doneIntent.setAction(ACTION_DONE);
        doneIntent.putExtra(EXTRA_TASK_ID, taskId);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId + 6000,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Task Reminder: " + taskName)
                .setContentText("Time: " + taskTime + " - Don't forget your task!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Task: " + taskName + "\nTime: " + taskTime + "\n\nReminder: Please complete your task!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // For pre-Oreo devices
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(false) // Make it persistent - won't dismiss on tap
                .setOngoing(true) // Make it persistent - can't be swiped away
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lockscreen
                .addAction(android.R.drawable.ic_menu_send, "Done", donePendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ignore", ignorePendingIntent);

        // Set defaults based on vibration setting
        if (vibrationEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL); // Sound + Vibration + Lights
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_LIGHTS); // Sound + Lights only
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(taskId, builder.build());
        }
    }

    private void showNotification(Context context, int taskId, String taskName, String taskTime, boolean vibrationEnabled) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // Permission not granted, cannot show notification
            }
        }
        showNotificationInternal(context, taskId, taskName, taskTime, vibrationEnabled);
    }

    private void showFocusTaskNotification(Context context, int taskId, String taskName,
                                           String startTime, String endTime, String alarmType,
                                           boolean vibrationEnabled) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, taskId, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title;
        String message;
        NotificationCompat.Builder builder;

        if (ALARM_TYPE_FOCUS_START.equals(alarmType)) {
            // START notification
            title = "🎯 Focus Time: " + taskName;
            message = "Time to start! Focus session: " + startTime + " to " + endTime;

            // Create Ignore action
            Intent ignoreIntent = new Intent(context, AlarmReceiver.class);
            ignoreIntent.setAction(ACTION_IGNORE);
            ignoreIntent.putExtra(EXTRA_TASK_ID, taskId);
            PendingIntent ignorePendingIntent = PendingIntent.getBroadcast(
                    context, taskId + 1000, ignoreIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("📝 Task: " + taskName +
                                    "\n⏰ Start: " + startTime +
                                    "\n🏁 End: " + endTime +
                                    "\n\n💪 Stay focused and give it your best!"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Got it!", ignorePendingIntent);

        } else {
            // END notification with Keep Going button
            title = "⏰ Time's Up: " + taskName;
            message = "Your focus session has ended. Great work!";

            // Create Done action - marks task complete
            Intent doneIntent = new Intent(context, AlarmReceiver.class);
            doneIntent.setAction(ACTION_DONE);
            doneIntent.putExtra(EXTRA_TASK_ID, taskId);
            PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                    context, taskId + 6000, doneIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Create Keep Going +15 min action
            Intent keepGoing15Intent = new Intent(context, AlarmReceiver.class);
            keepGoing15Intent.setAction(ACTION_KEEP_GOING);
            keepGoing15Intent.putExtra(EXTRA_TASK_ID, taskId);
            keepGoing15Intent.putExtra(EXTRA_EXTEND_MINUTES, 15);
            PendingIntent keepGoing15PendingIntent = PendingIntent.getBroadcast(
                    context, taskId + 4000, keepGoing15Intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Create Keep Going +30 min action
            Intent keepGoing30Intent = new Intent(context, AlarmReceiver.class);
            keepGoing30Intent.setAction(ACTION_KEEP_GOING);
            keepGoing30Intent.putExtra(EXTRA_TASK_ID, taskId);
            keepGoing30Intent.putExtra(EXTRA_EXTEND_MINUTES, 30);
            PendingIntent keepGoing30PendingIntent = PendingIntent.getBroadcast(
                    context, taskId + 5000, keepGoing30Intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("📝 Task: " + taskName +
                                    "\n⏰ Session ended!" +
                                    "\n\n🎉 Great job! Want to keep going?"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .addAction(android.R.drawable.ic_menu_send, "Done", donePendingIntent)
                    .addAction(android.R.drawable.ic_menu_add, "+15 min", keepGoing15PendingIntent)
                    .addAction(android.R.drawable.ic_menu_add, "+30 min", keepGoing30PendingIntent);
        }

        // Set defaults based on vibration setting
        if (vibrationEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_LIGHTS);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(taskId, builder.build());
        }
    }
}

