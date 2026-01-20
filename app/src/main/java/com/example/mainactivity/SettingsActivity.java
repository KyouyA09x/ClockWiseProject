package com.example.mainactivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends BaseThemedActivity {
    
    private SwitchMaterial floatingButtonSwitch;
    private SharedPreferences prefs;
    
    private static final int REQUEST_OVERLAY_PERMISSION = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Load single pane settings fragment
        android.widget.FrameLayout singleContainer = findViewById(R.id.single_settings_container);
        if (singleContainer != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.single_settings_container, new SettingsSinglePaneFragment())
                    .commitNow();
        }
    }
    
    private void setupFloatingButtonToggle() {
        if (floatingButtonSwitch == null) return;
        
        // Set initial state from preferences WITHOUT triggering the listener
        boolean isEnabled = prefs.getBoolean("floating_button_enabled", false);
        
        // Set the checked state without listener first
        floatingButtonSwitch.setOnCheckedChangeListener(null);
        floatingButtonSwitch.setChecked(isEnabled);
        
        // NOW set up the listener
        floatingButtonSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Check overlay permission
                if (!Settings.canDrawOverlays(this)) {
                    // Need to request overlay permission
                    floatingButtonSwitch.setChecked(false);
                    showOverlayPermissionDialog();
                    return;
                }
                
                // Permission granted, start service
                startFloatingButtonService();
                prefs.edit().putBoolean("floating_button_enabled", true).apply();
                Toast.makeText(this, "Floating button enabled", Toast.LENGTH_SHORT).show();
            } else {
                // Stop service
                stopFloatingButtonService();
                prefs.edit().putBoolean("floating_button_enabled", false).apply();
                Toast.makeText(this, "Floating button disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showOverlayPermissionDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("The floating button requires permission to display over other apps. This allows quick access to ClockWise actions from anywhere on your device.")
                .setIcon(R.drawable.ic_flash)
                .setPositiveButton("Grant Permission", (dialog, which) -> requestOverlayPermission())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted
                if (floatingButtonSwitch != null) {
                    floatingButtonSwitch.setChecked(true);
                }
                startFloatingButtonService();
                prefs.edit().putBoolean("floating_button_enabled", true).apply();
                Toast.makeText(this, "Floating button enabled", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Overlay permission is required for floating button", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void startFloatingButtonService() {
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    
    private void stopFloatingButtonService() {
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        stopService(serviceIntent);
    }
}