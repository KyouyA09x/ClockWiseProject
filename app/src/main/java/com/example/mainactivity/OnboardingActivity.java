package com.example.mainactivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class OnboardingActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private static final String PREFS_NAME = "ClockWisePrefs";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";

    private ImageView notificationCheck;
    private ImageView alarmCheck;
    private ImageView batteryCheck;
    private ImageView overlayCheck;
    private Button grantPermissionsButton;
    private TextView statusText;

    private boolean notificationPermissionGranted = false;
    private boolean alarmPermissionGranted = false;
    private boolean batteryOptimizationDisabled = false;
    private boolean overlayPermissionGranted = false;

    // Activity result launchers
    private ActivityResultLauncher<Intent> alarmPermissionLauncher;
    private ActivityResultLauncher<Intent> batteryOptimizationLauncher;
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Initialize activity result launchers
            initializeActivityResultLaunchers();

            // Always refresh permission status first
            checkCurrentPermissions();

            // If onboarding was completed but permissions were revoked, reset onboarding
            if (isOnboardingComplete() && !areAllPermissionsGranted()) {
                resetOnboarding();
            }

            // Check if onboarding was already completed and permissions are still granted
            if (isOnboardingComplete() && areAllPermissionsGranted()) {
                proceedToApp();
                return;
            }

            setContentView(R.layout.activity_onboarding);
            setupBackPressHandler();
            initViews();
            checkCurrentPermissions();
            setupClickListeners();
        } catch (Exception e) {
            android.util.Log.e("OnboardingActivity", "Error in onCreate", e);
            // If onboarding fails, try to proceed to app anyway
            try {
                proceedToApp();
            } catch (Exception ex) {
                android.util.Log.e("OnboardingActivity", "Fatal error", ex);
                Toast.makeText(this, "Error starting app: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void initializeActivityResultLaunchers() {
        alarmPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    checkCurrentPermissions();
                    updateUI();
                }
        );

        batteryOptimizationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    checkCurrentPermissions();
                    updateUI();
                }
        );

        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    checkCurrentPermissions();
                    updateUI();
                }
        );
    }

    private void resetOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, false).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check permissions when returning from settings
        checkCurrentPermissions();
        updateUI();
    }

    private void initViews() {
        notificationCheck = findViewById(R.id.notificationCheck);
        alarmCheck = findViewById(R.id.alarmCheck);
        batteryCheck = findViewById(R.id.batteryCheck);
        overlayCheck = findViewById(R.id.overlayCheck);
        grantPermissionsButton = findViewById(R.id.grantPermissionsButton);
        statusText = findViewById(R.id.statusText);

        LinearLayout notificationRow = findViewById(R.id.notificationPermissionRow);
        LinearLayout alarmRow = findViewById(R.id.alarmPermissionRow);
        LinearLayout batteryRow = findViewById(R.id.batteryPermissionRow);
        LinearLayout overlayRow = findViewById(R.id.overlayPermissionRow);

        if (notificationRow != null) {
            notificationRow.setOnClickListener(v -> requestNotificationPermission());
        }
        if (alarmRow != null) {
            alarmRow.setOnClickListener(v -> requestAlarmPermission());
        }
        if (batteryRow != null) {
            batteryRow.setOnClickListener(v -> requestBatteryOptimization());
        }
        if (overlayRow != null) {
            overlayRow.setOnClickListener(v -> requestOverlayPermission());
        }
    }

    private void setupClickListeners() {
        grantPermissionsButton.setOnClickListener(v -> {
            if (areAllPermissionsGranted()) {
                completeOnboarding();
            } else {
                requestNextMissingPermission();
            }
        });
    }

    private void checkCurrentPermissions() {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            notificationPermissionGranted = true; // Not needed below Android 13
        }

        // Check exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmPermissionGranted = alarmManager != null && alarmManager.canScheduleExactAlarms();
        } else {
            alarmPermissionGranted = true; // Not needed below Android 12
        }

        // Check battery optimization
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            batteryOptimizationDisabled = powerManager.isIgnoringBatteryOptimizations(getPackageName());
        }

        // Check overlay permission
        overlayPermissionGranted = Settings.canDrawOverlays(this);

        updateUI();
    }

    private void updateUI() {
        // Update checkboxes with null checks
        if (notificationCheck != null) {
            notificationCheck.setImageResource(notificationPermissionGranted ?
                    android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);
        }
        if (alarmCheck != null) {
            alarmCheck.setImageResource(alarmPermissionGranted ?
                    android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);
        }
        if (batteryCheck != null) {
            batteryCheck.setImageResource(batteryOptimizationDisabled ?
                    android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);
        }
        if (overlayCheck != null) {
            overlayCheck.setImageResource(overlayPermissionGranted ?
                    android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);
        }

        // Update button text and status with null checks
        if (areAllPermissionsGranted()) {
            if (grantPermissionsButton != null) {
                grantPermissionsButton.setText("Continue to App");
                grantPermissionsButton.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
            }
            if (statusText != null) {
                statusText.setText("All permissions granted! Tap to continue.");
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            }
        } else {
            if (grantPermissionsButton != null) {
                grantPermissionsButton.setText("Grant Permissions");
                grantPermissionsButton.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));
            }
            if (statusText != null) {
                statusText.setText("All permissions are required to use the app");
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.tab_indicator_text));
            }
        }
    }

    private boolean areAllPermissionsGranted() {
        return notificationPermissionGranted && alarmPermissionGranted && batteryOptimizationDisabled && overlayPermissionGranted;
    }

    private void requestNextMissingPermission() {
        if (!notificationPermissionGranted) {
            requestNotificationPermission();
        } else if (!alarmPermissionGranted) {
            requestAlarmPermission();
        } else if (!batteryOptimizationDisabled) {
            requestBatteryOptimization();
        } else if (!overlayPermissionGranted) {
            requestOverlayPermission();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    showPermissionRationale("Notification Permission",
                            "Notifications are required to remind you about your tasks. Please grant this permission.",
                            () -> ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    NOTIFICATION_PERMISSION_CODE));
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE);
                }
            } else {
                notificationPermissionGranted = true;
                updateUI();
            }
        }
    }

    @SuppressLint("NewApi")
    private void requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                showPermissionRationale("Exact Alarm Permission",
                        "Exact alarms are required to notify you at the precise time you set for your tasks.",
                        () -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            alarmPermissionLauncher.launch(intent);
                        });
            } else {
                alarmPermissionGranted = true;
                updateUI();
            }
        }
    }

    @SuppressLint("BatteryLife")
    private void requestBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            showPermissionRationale("Background Running",
                    "To ensure you receive task reminders even when the app is closed, please disable battery optimization for this app.",
                    () -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        batteryOptimizationLauncher.launch(intent);
                    });
        } else {
            batteryOptimizationDisabled = true;
            updateUI();
        }
    }

    private void requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            showPermissionRationale("Display Over Apps",
                    "This allows ClockWise to show popup notifications when tasks are due, even over other apps.",
                    () -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        overlayPermissionLauncher.launch(intent);
                    });
        } else {
            overlayPermissionGranted = true;
            updateUI();
        }
    }

    private void showPermissionRationale(String title, String message, Runnable onAccept) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Grant", (dialog, which) -> onAccept.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                notificationPermissionGranted = true;
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission is required", Toast.LENGTH_SHORT).show();
                // If permanently denied, direct to settings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.POST_NOTIFICATIONS)) {
                        showSettingsDialog();
                    }
                }
            }
            updateUI();
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Notification permission was denied. Please enable it in app settings to continue.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean isOnboardingComplete() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false);
    }

    private void completeOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply();
        proceedToApp();
    }

    private void proceedToApp() {
        Intent intent = new Intent(OnboardingActivity.this, SplashActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!areAllPermissionsGranted()) {
                    Toast.makeText(OnboardingActivity.this,
                            "Please grant all permissions to continue", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });
    }
}

