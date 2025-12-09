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
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_NAME = "task_name";
    public static final String EXTRA_TASK_TIME = "task_time";

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
        }

        // Handle alarm trigger
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        String taskName = intent.getStringExtra(EXTRA_TASK_NAME);
        String taskTime = intent.getStringExtra(EXTRA_TASK_TIME);

        if (taskName == null) {
            taskName = "Task Reminder";
        }
        if (taskTime == null) {
            taskTime = "";
        }

        createNotificationChannel(context);
        showNotification(context, taskId, taskName, taskTime);
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
    private void showNotificationInternal(Context context, int taskId, String taskName, String taskTime) {
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
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lockscreen
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ignore", ignorePendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 5min", snoozePendingIntent)
                .addAction(android.R.drawable.ic_delete, "Cancel", cancelPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(taskId, builder.build());
        }
    }

    private void showNotification(Context context, int taskId, String taskName, String taskTime) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // Permission not granted, cannot show notification
            }
        }
        showNotificationInternal(context, taskId, taskName, taskTime);
    }
}

