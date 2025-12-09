package com.example.mainactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                Log.d(TAG, "Boot completed. Enqueuing alarm rescheduling work.");
                OneTimeWorkRequest rescheduleRequest = new OneTimeWorkRequest.Builder(RescheduleAlarmsWorker.class).build();
                WorkManager.getInstance(context.getApplicationContext()).enqueue(rescheduleRequest);
            } catch (Exception e) {
                Log.e(TAG, "Failed to schedule alarm rescheduling via WorkManager", e);
            }
        }
    }
}
