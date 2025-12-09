package com.example.mainactivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        String taskName = intent.getStringExtra("taskName");
        int taskId = intent.getIntExtra("taskId", 0);

        if (taskName == null) {
            Log.e(TAG, "Task name is null");
            taskName = "Task";
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("task_alarms", "Task Alarms", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for task alarms");
            notificationManager.createNotificationChannel(channel);
        }

        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, taskId, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "task_alarms")
                // Use a system icon to ensure it works. Replace with your own valid notification icon later.
                .setSmallIcon(android.R.drawable.ic_dialog_info) 
                .setContentTitle("ClockWise Task")
                .setContentText(taskName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            notificationManager.notify(taskId, builder.build());
            Log.d(TAG, "Notification posted for task: " + taskName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to post notification", e);
        }
    }
}
