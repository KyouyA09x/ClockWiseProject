package com.example.mainactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            Log.e(TAG, "Context or intent is null");
            return;
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device booted, rescheduling alarms");
            try {
                // Reschedule all alarms after device reboot
                AlarmHelper.rescheduleAllAlarms(context);
                Log.d(TAG, "Alarms rescheduled successfully after boot");
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling alarms after boot", e);
            }
        }
    }
}

