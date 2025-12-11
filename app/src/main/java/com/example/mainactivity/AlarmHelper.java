package com.example.mainactivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmHelper {
    private static final String TAG = "AlarmHelper";

    public static void scheduleTaskAlarm(Context context, Task task) {
        if (!task.isAlarmOn) {
            cancelTaskAlarm(context, task);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id);
        intent.putExtra(AlarmReceiver.EXTRA_TASK_NAME, task.name);
        intent.putExtra(AlarmReceiver.EXTRA_TASK_TIME, String.format(Locale.getDefault(), "%d:%02d %s", task.hour, task.minute, task.amPm));
        intent.putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, task.vibrationEnabled);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Calculate the alarm time
        Calendar calendar = Calendar.getInstance();

        // Parse the task date
        if (task.date != null && !task.date.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date taskDate = sdf.parse(task.date);
                if (taskDate != null) {
                    Calendar taskCal = Calendar.getInstance();
                    taskCal.setTime(taskDate);
                    calendar.set(Calendar.YEAR, taskCal.get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, taskCal.get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, taskCal.get(Calendar.DAY_OF_MONTH));
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + e.getMessage());
            }
        }

        // Set the time
        int hour24 = task.hour;
        if (task.amPm.equals("PM") && task.hour != 12) {
            hour24 += 12;
        } else if (task.amPm.equals("AM") && task.hour == 12) {
            hour24 = 0;
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour24);
        calendar.set(Calendar.MINUTE, task.minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the time has already passed for today, don't schedule (unless it's a repeating task)
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            boolean hasRepeat = false;
            if (task.selectedDays != null) {
                for (boolean day : task.selectedDays) {
                    if (day) {
                        hasRepeat = true;
                        break;
                    }
                }
            }

            if (!hasRepeat) {
                Log.d(TAG, "Task time has passed and not repeating, skipping alarm");
                return;
            }

            // Find next occurrence for repeating tasks
            calendar = findNextOccurrence(task, calendar);
            if (calendar == null) {
                return;
            }
        }

        Log.d(TAG, "Scheduling alarm for task: " + task.name + " at " + calendar.getTime());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    // Fall back to inexact alarm
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to schedule exact alarms: " + e.getMessage());
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private static Calendar findNextOccurrence(Task task, Calendar baseCalendar) {
        if (task.selectedDays == null) return null;

        Calendar cal = (Calendar) baseCalendar.clone();
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0-based (Sunday = 0)

        for (int i = 0; i < 7; i++) {
            int checkDay = (currentDayOfWeek + i) % 7;
            if (task.selectedDays[checkDay]) {
                cal.add(Calendar.DAY_OF_MONTH, i);
                if (i == 0 && cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    // If today is selected but time passed, check next week
                    continue;
                }
                return cal;
            }
        }

        // If we're here, wrap around to next week
        for (int i = 0; i < 7; i++) {
            if (task.selectedDays[i]) {
                cal = (Calendar) baseCalendar.clone();
                int daysToAdd = (i - currentDayOfWeek + 7) % 7;
                if (daysToAdd == 0) daysToAdd = 7;
                cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
                return cal;
            }
        }

        return null;
    }

    public static void cancelTaskAlarm(Context context, Task task) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Cancelled alarm for task: " + task.name);
    }

    public static void rescheduleAllAlarms(Context context) {
        try {
            TaskRepository repository = TaskRepository.getInstance();
            repository.initialize(context);

            if (repository.morningTasks != null) {
                for (Task task : repository.morningTasks) {
                    if (task != null && task.isAlarmOn) {
                        try {
                            if (task.isFocusTask()) {
                                scheduleFocusTaskAlarms(context, task);
                            } else {
                                scheduleTaskAlarm(context, task);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error rescheduling task: " + task.name, e);
                        }
                    }
                }
            }

            if (repository.afternoonTasks != null) {
                for (Task task : repository.afternoonTasks) {
                    if (task != null && task.isAlarmOn) {
                        try {
                            if (task.isFocusTask()) {
                                scheduleFocusTaskAlarms(context, task);
                            } else {
                                scheduleTaskAlarm(context, task);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error rescheduling task: " + task.name, e);
                        }
                    }
                }
            }

            if (repository.nightTasks != null) {
                for (Task task : repository.nightTasks) {
                    if (task != null && task.isAlarmOn) {
                        try {
                            if (task.isFocusTask()) {
                                scheduleFocusTaskAlarms(context, task);
                            } else {
                                scheduleTaskAlarm(context, task);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error rescheduling task: " + task.name, e);
                        }
                    }
                }
            }

            Log.d(TAG, "All alarms rescheduled successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in rescheduleAllAlarms", e);
        }
    }

    /**
     * Schedule both start and end alarms for a Focus Task
     */
    public static void scheduleFocusTaskAlarms(Context context, Task task) {
        if (!task.isAlarmOn || !task.isFocusTask()) {
            cancelFocusTaskAlarms(context, task);
            return;
        }

        // Schedule START alarm
        scheduleAlarmAtTime(context, task, task.hour, task.minute, task.amPm,
                           task.id, AlarmReceiver.ALARM_TYPE_FOCUS_START);

        // Schedule END alarm (with any extensions)
        int endMinuteTotal = task.endMinute + task.extendedMinutes;
        int extraHours = endMinuteTotal / 60;
        int finalEndMinute = endMinuteTotal % 60;
        int finalEndHour = task.endHour + extraHours;
        String finalEndAmPm = task.endAmPm;

        // Handle hour overflow
        if (finalEndHour > 12) {
            finalEndHour = finalEndHour - 12;
            finalEndAmPm = finalEndAmPm.equals("AM") ? "PM" : "AM";
        }

        scheduleAlarmAtTime(context, task, finalEndHour, finalEndMinute, finalEndAmPm,
                           task.id + 50000, AlarmReceiver.ALARM_TYPE_FOCUS_END);

        Log.d(TAG, "Scheduled Focus Task alarms: Start at " + task.getStartTimeFormatted() +
              ", End at " + finalEndHour + ":" + String.format("%02d", finalEndMinute) + " " + finalEndAmPm);
    }

    private static void scheduleAlarmAtTime(Context context, Task task, int hour, int minute,
                                            String amPm, int requestCode, String alarmType) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id);
        intent.putExtra(AlarmReceiver.EXTRA_TASK_NAME, task.name);
        intent.putExtra(AlarmReceiver.EXTRA_TASK_TIME, String.format(Locale.getDefault(),
                "%d:%02d %s", hour, minute, amPm));
        intent.putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, task.vibrationEnabled);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, alarmType);
        intent.putExtra(AlarmReceiver.EXTRA_TASK_TYPE, task.taskType);
        intent.putExtra(AlarmReceiver.EXTRA_END_TIME, task.getEndTimeFormatted());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Calculate alarm time
        Calendar calendar = Calendar.getInstance();

        if (task.date != null && !task.date.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date taskDate = sdf.parse(task.date);
                if (taskDate != null) {
                    Calendar taskCal = Calendar.getInstance();
                    taskCal.setTime(taskDate);
                    calendar.set(Calendar.YEAR, taskCal.get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, taskCal.get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, taskCal.get(Calendar.DAY_OF_MONTH));
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + e.getMessage());
            }
        }

        int hour24 = hour;
        if (amPm.equals("PM") && hour != 12) {
            hour24 += 12;
        } else if (amPm.equals("AM") && hour == 12) {
            hour24 = 0;
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour24);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            Log.d(TAG, "Alarm time has passed, skipping: " + alarmType);
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "No permission for exact alarms: " + e.getMessage());
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    public static void cancelFocusTaskAlarms(Context context, Task task) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Cancel start alarm
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent startPendingIntent = PendingIntent.getBroadcast(
                context, task.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(startPendingIntent);

        // Cancel end alarm
        PendingIntent endPendingIntent = PendingIntent.getBroadcast(
                context, task.id + 50000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(endPendingIntent);

        Log.d(TAG, "Cancelled Focus Task alarms for: " + task.name);
    }

    /**
     * Extend a Focus Task's end time by specified minutes
     */
    public static void extendFocusTask(Context context, Task task, int additionalMinutes) {
        task.extendedMinutes += additionalMinutes;

        TaskRepository repository = TaskRepository.getInstance();
        repository.initialize(context);
        repository.updateTask(task);

        // Cancel old end alarm and schedule new one
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent oldEndPendingIntent = PendingIntent.getBroadcast(
                    context, task.id + 50000, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(oldEndPendingIntent);
        }

        // Calculate new end time
        int endMinuteTotal = task.endMinute + task.extendedMinutes;
        int extraHours = endMinuteTotal / 60;
        int finalEndMinute = endMinuteTotal % 60;
        int finalEndHour = task.endHour + extraHours;
        String finalEndAmPm = task.endAmPm;

        if (finalEndHour > 12) {
            finalEndHour = finalEndHour - 12;
            finalEndAmPm = finalEndAmPm.equals("AM") ? "PM" : "AM";
        }

        scheduleAlarmAtTime(context, task, finalEndHour, finalEndMinute, finalEndAmPm,
                           task.id + 50000, AlarmReceiver.ALARM_TYPE_FOCUS_END);

        Log.d(TAG, "Extended Focus Task to: " + finalEndHour + ":" +
              String.format("%02d", finalEndMinute) + " " + finalEndAmPm);
    }
}

