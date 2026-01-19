package com.example.mainactivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends BaseThemedActivity {

    private MaterialCardView lightModeButton;
    private MaterialCardView darkModeButton;
    private MaterialCardView autoModeButton;

    private RadioGroup themeColorGroup;
    private RadioButton radioDefault;
    private RadioButton radioCyan;
    private RadioButton radioGreen;
    private RadioButton radioPurple;
    private RadioButton radioOrange;
    
    private SwitchMaterial floatingButtonSwitch;
    private SharedPreferences prefs;
    
    private static final int REQUEST_OVERLAY_PERMISSION = 1234;
    
    private boolean isInitializing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        initViews();
        updateThemeModeUI();
        updateThemeColorUI();
        setupListeners();
        setupFloatingButtonToggle();
        setupTestDataButtons();
        setupNotificationPreviewButtons();
        isInitializing = false;
    }

    private void initViews() {
        lightModeButton = findViewById(R.id.lightModeButton);
        darkModeButton = findViewById(R.id.darkModeButton);
        autoModeButton = findViewById(R.id.autoModeButton);

        themeColorGroup = findViewById(R.id.themeRadioGroup);
        radioDefault = findViewById(R.id.defaultThemeRadio);
        radioCyan = findViewById(R.id.cyanThemeRadio);
        radioGreen = findViewById(R.id.greenThemeRadio);
        radioPurple = findViewById(R.id.purpleThemeRadio);
        radioOrange = findViewById(R.id.orangeThemeRadio);
        
        floatingButtonSwitch = findViewById(R.id.floatingButtonSwitch);
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        // Need to request overlay permission
                        floatingButtonSwitch.setChecked(false);
                        showOverlayPermissionDialog();
                        return;
                    }
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
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    requestOverlayPermission();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // Permission granted
                    floatingButtonSwitch.setChecked(true);
                    startFloatingButtonService();
                    prefs.edit().putBoolean("floating_button_enabled", true).apply();
                    Toast.makeText(this, "Floating button enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied
                    Toast.makeText(this, "Overlay permission is required for floating button", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void startFloatingButtonService() {
        android.util.Log.d("SettingsActivity", "startFloatingButtonService called");
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.util.Log.d("SettingsActivity", "Starting foreground service");
            startForegroundService(serviceIntent);
        } else {
            android.util.Log.d("SettingsActivity", "Starting regular service");
            startService(serviceIntent);
        }
        android.util.Log.d("SettingsActivity", "Service start command sent");
    }
    
    private void stopFloatingButtonService() {
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        stopService(serviceIntent);
    }

    private void setupListeners() {
        if (lightModeButton != null) {
            lightModeButton.setOnClickListener(v -> {
                int currentMode = ThemeHelper.getThemeMode(this);
                if (currentMode != ThemeHelper.MODE_LIGHT) {
                    ThemeHelper.setThemeMode(this, ThemeHelper.MODE_LIGHT);
                    notifyThemeChanged();
                    recreate();
                }
            });
        }

        if (darkModeButton != null) {
            darkModeButton.setOnClickListener(v -> {
                int currentMode = ThemeHelper.getThemeMode(this);
                if (currentMode != ThemeHelper.MODE_DARK) {
                    ThemeHelper.setThemeMode(this, ThemeHelper.MODE_DARK);
                    notifyThemeChanged();
                    recreate();
                }
            });
        }

        if (autoModeButton != null) {
            autoModeButton.setOnClickListener(v -> {
                int currentMode = ThemeHelper.getThemeMode(this);
                if (currentMode != ThemeHelper.MODE_AUTO) {
                    ThemeHelper.setThemeMode(this, ThemeHelper.MODE_AUTO);
                    notifyThemeChanged();
                    recreate();
                }
            });
        }

        if (themeColorGroup != null) {
            themeColorGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (isInitializing) return;
                
                String currentColor = ThemeHelper.getThemeColor(this);
                String newColor;
                
                if (checkedId == R.id.cyanThemeRadio) {
                    newColor = ThemeHelper.COLOR_CYAN;
                } else if (checkedId == R.id.greenThemeRadio) {
                    newColor = ThemeHelper.COLOR_GREEN;
                } else if (checkedId == R.id.purpleThemeRadio) {
                    newColor = ThemeHelper.COLOR_PURPLE;
                } else if (checkedId == R.id.orangeThemeRadio) {
                    newColor = ThemeHelper.COLOR_ORANGE;
                } else {
                    newColor = ThemeHelper.COLOR_DEFAULT;
                }
                
                if (!currentColor.equals(newColor)) {
                    ThemeHelper.setThemeColor(this, newColor);
                    notifyThemeChanged();
                    recreate();
                }
            });
        }
    }

    private void updateThemeModeUI() {
        int mode = ThemeHelper.getThemeMode(this);
        int outlineColor = MaterialColors.getColor(this, android.R.attr.colorControlNormal, getColor(R.color.outline_light));
        int primaryColor = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, getColor(R.color.blue_primary));

        if (lightModeButton != null) lightModeButton.setStrokeColor(ColorStateList.valueOf(outlineColor));
        if (darkModeButton != null) darkModeButton.setStrokeColor(ColorStateList.valueOf(outlineColor));
        if (autoModeButton != null) autoModeButton.setStrokeColor(ColorStateList.valueOf(outlineColor));

        switch (mode) {
            case ThemeHelper.MODE_LIGHT:
                if (lightModeButton != null) lightModeButton.setStrokeColor(ColorStateList.valueOf(primaryColor));
                break;
            case ThemeHelper.MODE_DARK:
                if (darkModeButton != null) darkModeButton.setStrokeColor(ColorStateList.valueOf(primaryColor));
                break;
            case ThemeHelper.MODE_AUTO:
            default:
                if (autoModeButton != null) autoModeButton.setStrokeColor(ColorStateList.valueOf(primaryColor));
                break;
        }
    }

    private void updateThemeColorUI() {
        String color = ThemeHelper.getThemeColor(this);
        if (themeColorGroup == null) return;

        switch (color) {
            case ThemeHelper.COLOR_CYAN:
                if (radioCyan != null) radioCyan.setChecked(true);
                break;
            case ThemeHelper.COLOR_GREEN:
                if (radioGreen != null) radioGreen.setChecked(true);
                break;
            case ThemeHelper.COLOR_PURPLE:
                if (radioPurple != null) radioPurple.setChecked(true);
                break;
            case ThemeHelper.COLOR_ORANGE:
                if (radioOrange != null) radioOrange.setChecked(true);
                break;
            case ThemeHelper.COLOR_DEFAULT:
            default:
                if (radioDefault != null) radioDefault.setChecked(true);
                break;
        }
    }

    private void setupTestDataButtons() {
        com.google.android.material.button.MaterialButton populateButton = 
            findViewById(R.id.populateTestDataButton);
        com.google.android.material.button.MaterialButton clearButton = 
            findViewById(R.id.clearAllDataButton);

        if (populateButton != null) {
            populateButton.setOnClickListener(v -> 
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Populate Test Data")
                    .setMessage("This will add sample tasks, focus sessions, and notes to test the app. Continue?")
                    .setPositiveButton("Populate", (dialog, which) -> {
                        TestDataGenerator generator = new TestDataGenerator(this);
                        generator.populateTestData();
                        android.widget.Toast.makeText(this, 
                            "Test data populated successfully!", 
                            android.widget.Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show()
            );
        }

        if (clearButton != null) {
            clearButton.setOnClickListener(v -> 
                ModernDialogHelper.showBulkDestructiveDialog(
                    this,
                    "Clear All Data?",
                    "⚠️ This will permanently delete ALL tasks and notes. This action cannot be undone. Continue?",
                    0,  // No count needed for this type
                    "Delete All",
                    R.drawable.ic_delete,
                    () -> {
                        TestDataGenerator generator = new TestDataGenerator(this);
                        generator.clearAllData();
                        android.widget.Toast.makeText(this, 
                            "All data cleared!", 
                            android.widget.Toast.LENGTH_SHORT).show();
                    },
                    null
                )
            );
        }
    }
    
    private void setupNotificationPreviewButtons() {
        com.google.android.material.button.MaterialButton previewTaskButton = 
            findViewById(R.id.previewTaskPopupButton);
        com.google.android.material.button.MaterialButton previewFocusButton = 
            findViewById(R.id.previewFocusPopupButton);
        com.google.android.material.button.MaterialButton previewQuickInfoButton = 
            findViewById(R.id.previewQuickInfoPopupButton);
        
        if (previewTaskButton != null) {
            previewTaskButton.setOnClickListener(v -> {
                // Use overlay for A15+, activity for A14 and below
                if (android.os.Build.VERSION.SDK_INT >= 35 && OverlayNotificationService.canDrawOverlays(this)) {
                    OverlayNotificationService.showNotificationWithTime(
                        this, -1, "Review Project Proposal",
                        "This is a preview of how your task notification will appear!",
                        "High", "reminder", 10, 30, "AM", 0, 0, "AM"
                    );
                } else {
                    PopupNotificationActivity.show(
                        this, -1, "Review Project Proposal",
                        "This is a preview of how your task notification will appear!",
                        "High", "reminder", 10, 30, "AM", 0, 0, "AM"
                    );
                }
            });
        }
        
        if (previewFocusButton != null) {
            previewFocusButton.setOnClickListener(v -> {
                // Use overlay for A15+, activity for A14 and below
                if (android.os.Build.VERSION.SDK_INT >= 35 && OverlayNotificationService.canDrawOverlays(this)) {
                    OverlayNotificationService.showNotificationWithTime(
                        this, -2, "Deep Work: Project Planning",
                        "Focus session preview with timer and controls!",
                        "High", "focus", 2, 0, "PM", 4, 0, "PM"
                    );
                } else {
                    PopupNotificationActivity.show(
                        this, -2, "Deep Work: Project Planning",
                        "Focus session preview with timer and controls!",
                        "High", "focus", 2, 0, "PM", 4, 0, "PM"
                    );
                }
            });
        }
        
        if (previewQuickInfoButton != null) {
            previewQuickInfoButton.setOnClickListener(this::showPreviewQuickInfoPopup);
        }
    }
    
    private void showPreviewQuickInfoPopup(android.view.View anchorView) {
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(this);
        android.view.View popupView = getLayoutInflater().inflate(R.layout.popup_quick_info, null);
        popupWindow.setContentView(popupView);
        
        popupWindow.setWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(24f);
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        
        android.widget.TextView titleText = popupView.findViewById(R.id.quickInfoTitle);
        android.widget.TextView typeText = popupView.findViewById(R.id.quickInfoType);
        android.widget.TextView dateText = popupView.findViewById(R.id.quickInfoDate);
        android.widget.TextView timeText = popupView.findViewById(R.id.quickInfoTime);
        android.widget.TextView durationText = popupView.findViewById(R.id.quickInfoDuration);
        android.view.View durationRow = popupView.findViewById(R.id.durationRow);
        android.widget.ImageView typeIcon = popupView.findViewById(R.id.typeIcon);
        com.google.android.material.card.MaterialCardView iconContainer = popupView.findViewById(R.id.typeIconContainer);
        
        if (titleText != null) titleText.setText("Review Project Proposal");
        if (typeText != null) {
            typeText.setText("Focus Session");
            typeText.setTextColor(getColor(R.color.primary));
        }
        if (dateText != null) {
            java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault());
            dateText.setText(displayFormat.format(new java.util.Date()));
        }
        if (timeText != null) {
            timeText.setText("3:00 PM → 5:00 PM");
        }
        if (durationRow != null && durationText != null) {
            durationRow.setVisibility(android.view.View.VISIBLE);
            durationText.setText("2 hours");
        }
        if (typeIcon != null) {
            typeIcon.setImageResource(R.drawable.ic_focus);
        }
        if (iconContainer != null) {
            iconContainer.setCardBackgroundColor(getColor(R.color.primary));
        }
        
        popupWindow.showAtLocation(anchorView, android.view.Gravity.CENTER, 0, 0);
    }
    
    private void setupTrashBinButton() {
        View trashBinButton = findViewById(R.id.trashBinButton);
        if (trashBinButton != null) {
            trashBinButton.setOnClickListener(v -> {
                startActivity(new Intent(SettingsActivity.this, TrashBinActivity.class));
            });
        }
    }
}