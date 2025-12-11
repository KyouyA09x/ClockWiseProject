package com.example.mainactivity;

import android.app.Application;

public class ClockWiseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Apply theme before any activity is created
        ThemeHelper.applyTheme(this);
    }
}
