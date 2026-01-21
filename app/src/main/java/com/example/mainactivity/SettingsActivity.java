package com.example.mainactivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends BaseThemedActivity {
    
    private SlidingPaneLayout slidingPaneLayout;
    private SwitchMaterial floatingButtonSwitch;
    private SharedPreferences prefs;
    private boolean useSinglePane = false;
    
    private static final int REQUEST_OVERLAY_PERMISSION = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if we should use single pane mode for foldable phones
        useSinglePane = shouldUseSinglePaneForFoldable();
        
        if (useSinglePane) {
            // Use single pane layout for foldables in closed/single screen mode
            setContentView(R.layout.activity_settings_single);
            loadSinglePaneSettings();
        } else {
            // Use split pane layout for tablets or foldables in split screen
            setContentView(R.layout.activity_settings);
            loadSplitPaneSettings();
        }

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
    }
    
    /**
     * Determines if single pane should be used for foldable phones.
     * Returns true ONLY for foldable phones when NOT in split screen mode.
     */
    private boolean shouldUseSinglePaneForFoldable() {
        // Get screen dimensions
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float widthDp = metrics.widthPixels / metrics.density;
        float heightDp = metrics.heightPixels / metrics.density;
        
        // Check if in multi-window mode (split screen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
            return false; // Use split pane in multi-window mode
        }
        
        // Detect foldable: A foldable typically has unusual aspect ratios
        // or specific screen configurations when folded vs unfolded
        boolean isPossibleFoldable = false;
        
        // Check for foldable characteristics:
        // 1. Screen width between 320-420dp when folded (typical narrow foldable screen)
        // 2. Not a regular phone (regular phones are < 600dp but have normal aspect ratios)
        
        float aspectRatio = Math.max(widthDp, heightDp) / Math.min(widthDp, heightDp);
        
        // Foldable phones in closed state often have:
        // - Width around 300-420dp  
        // - Unusual aspect ratios (very tall/narrow)
        if (widthDp >= 300 && widthDp <= 450 && aspectRatio > 2.0) {
            isPossibleFoldable = true;
        }
        
        // For foldable in single screen mode (not unfolded), use single pane
        // This prevents the stretched settings on narrow foldable screens
        if (isPossibleFoldable && widthDp < 500) {
            return true;
        }
        
        // Regular phones (< 600dp width) - use single pane
        if (widthDp < 600) {
            return true;
        }
        
        // Tablets and unfolded foldables (>= 600dp) - use split pane
        return false;
    }
    
    private void loadSinglePaneSettings() {
        // Load single pane settings fragment
        android.widget.FrameLayout singleContainer = findViewById(R.id.single_settings_container);
        if (singleContainer != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.single_settings_container, new SettingsSinglePaneFragment())
                    .commitNow();
        }
    }
    
    private void loadSplitPaneSettings() {
        // Initialize SlidingPaneLayout
        slidingPaneLayout = findViewById(R.id.sliding_pane_layout);
        
        if (slidingPaneLayout != null) {
            // Load the settings list fragment
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_list_container, new SettingsListFragment())
                    .commitNow();
            
            // Load the default detail fragment (Notifications)
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.detail_container, SettingsDetailFragment.newInstance("Notifications"))
                    .commitNow();
        }
    }
    
    /**
     * Opens the detail pane with the specified fragment.
     * Called by SettingsListFragment when a category is selected.
     */
    public void openDetailPane(Fragment fragment) {
        if (useSinglePane) {
            // In single pane mode, navigate to a new fragment
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.single_settings_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            // In split pane mode, replace the detail container
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.detail_container, fragment)
                    .commitNow();
            
            // Open the detail pane on small screens
            if (slidingPaneLayout != null && slidingPaneLayout.isSlideable()) {
                slidingPaneLayout.openPane();
            }
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