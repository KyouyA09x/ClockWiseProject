package com.example.mainactivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AlarmHelper {

    private static final String TAG = "AlarmHelper";

    public static void scheduleTaskAlarm(Context context, Task task) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("taskName", task.name);
        intent.putExtra("taskId", task.id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();

        // Set the date from the task
        if (task.date != null && !task.date.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                calendar.setTime(sdf.parse(task.date));
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + task.date, e);
                return; // Don't schedule if date is invalid
            }
        }

        // Set the time
        int hourOfDay = task.hour;
        if (task.amPm.equals("PM") && task.hour != 12) {
            hourOfDay += 12;
        }
        if (task.amPm.equals("AM") && task.hour == 12) {
            hourOfDay = 0; // Midnight
        }
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, task.minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the calculated time is in the past, schedule it for the next day.
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            Log.d(TAG, "Alarm time is in the past, scheduling for next day at " + calendar.getTime());
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.d(TAG, "Alarm scheduled for task '" + task.name + "' at " + calendar.getTime());
    }

    public static void cancelTaskAlarm(Context context, Task task) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Alarm canceled for task '" + task.name + "'");
    }

    public static void rescheduleAllAlarms(Context context, List<Task> tasks) {
        Log.d(TAG, "Rescheduling " + tasks.size() + " tasks.");
        for (Task task : tasks) {
            if (task.isAlarmOn) {
                scheduleTaskAlarm(context, task);
            }
        }
    }
}
