package com.example.mainactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base Activity that automatically handles theme changes.
 * All activities should extend this class to support dynamic theme switching.
 */
public abstract class BaseThemedActivity extends AppCompatActivity {

    public static final String ACTION_THEME_CHANGED = "com.example.mainactivity.THEME_CHANGED";
    
    private int currentThemeMode;
    private String currentThemeColor;
    private boolean isRecreating = false;
    
    private final BroadcastReceiver themeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_THEME_CHANGED.equals(intent.getAction()) && !isRecreating) {
                isRecreating = true;
                recreate();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply theme BEFORE calling super.onCreate()
        ThemeHelper.applyTheme(this);
        setTheme(ThemeHelper.getThemeResource(this));
        
        super.onCreate(savedInstanceState);
        
        // Store current theme settings
        currentThemeMode = ThemeHelper.getThemeMode(this);
        currentThemeColor = ThemeHelper.getThemeColor(this);
        isRecreating = false;
        
        // Register broadcast receiver for theme changes
        try {
            IntentFilter filter = new IntentFilter(ACTION_THEME_CHANGED);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(themeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(themeChangeReceiver, filter);
            }
        } catch (Exception e) {
            // Receiver registration failed, theme changes will still work via onResume
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if theme has changed while activity was paused/stopped
        int newThemeMode = ThemeHelper.getThemeMode(this);
        String newThemeColor = ThemeHelper.getThemeColor(this);
        
        if (!isRecreating && (currentThemeMode != newThemeMode || !currentThemeColor.equals(newThemeColor))) {
            // Theme changed, recreate activity to apply new theme
            isRecreating = true;
            recreate();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        // Update stored theme values in case they changed while stopped
        currentThemeMode = ThemeHelper.getThemeMode(this);
        currentThemeColor = ThemeHelper.getThemeColor(this);
        isRecreating = false;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister broadcast receiver to prevent memory leaks
        try {
            unregisterReceiver(themeChangeReceiver);
        } catch (Exception e) {
            // Receiver might not be registered
        }
    }
    
    /**
     * Call this method after changing theme to immediately notify all activities
     */
    protected void notifyThemeChanged() {
        Intent intent = new Intent(ACTION_THEME_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }
}
