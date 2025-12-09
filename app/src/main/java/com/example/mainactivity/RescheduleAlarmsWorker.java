package com.example.mainactivity;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class RescheduleAlarmsWorker extends Worker {

    public RescheduleAlarmsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        TaskDatabase database = TaskDatabase.getDatabase(getApplicationContext());
        List<Task> tasks = database.taskDao().getAllTasksSync();
        AlarmHelper.rescheduleAllAlarms(getApplicationContext(), tasks);
        return Result.success();
    }
}
