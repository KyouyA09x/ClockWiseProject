package com.example.mainactivity;

import android.app.Application;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TaskRepository.getInstance(this);
    }
}
