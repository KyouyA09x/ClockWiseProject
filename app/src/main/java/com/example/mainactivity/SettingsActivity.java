package com.example.mainactivity;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;
import androidx.window.layout.FoldingFeature;
import androidx.window.layout.WindowInfoTracker;
import androidx.window.layout.WindowLayoutInfo;
import androidx.window.java.layout.WindowInfoTrackerCallbackAdapter;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.concurrent.Executor;

public class SettingsActivity extends BaseThemedActivity {
    
    private SlidingPaneLayout slidingPaneLayout;
    private SharedPreferences prefs;
    private boolean useSinglePane = false;
    private int lastScreenWidthDp = 0;
    
    // Jetpack WindowManager for foldable detection
    private WindowInfoTrackerCallbackAdapter windowInfoTracker;
    private final Consumer<WindowLayoutInfo> layoutStateChangeCallback = this::onLayoutStateChanged;
    private boolean isFolded = false;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize WindowInfoTracker for foldable detection
        windowInfoTracker = new WindowInfoTrackerCallbackAdapter(
                WindowInfoTracker.getOrCreate(this)
        );
        
        // Check if we should use single pane mode for foldable phones
        useSinglePane = shouldUseSinglePaneForFoldable();
        lastScreenWidthDp = getCurrentScreenWidthDp();
        
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
        isInitialized = true;
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Start listening for window layout changes (fold/unfold events)
        if (windowInfoTracker != null) {
            Executor mainExecutor = ContextCompat.getMainExecutor(this);
            windowInfoTracker.addWindowLayoutInfoListener(this, mainExecutor, layoutStateChangeCallback);
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Stop listening for window layout changes
        if (windowInfoTracker != null) {
            windowInfoTracker.removeWindowLayoutInfoListener(layoutStateChangeCallback);
        }
    }
    
    /**
     * Called when the window layout changes (e.g., phone is folded/unfolded)
     */
    private void onLayoutStateChanged(WindowLayoutInfo windowLayoutInfo) {
        if (!isInitialized) return;
        
        boolean wasFolded = isFolded;
        boolean hasHinge = false;
        boolean isTableTopMode = false;
        
        // Check for folding features
        for (androidx.window.layout.DisplayFeature feature : windowLayoutInfo.getDisplayFeatures()) {
            if (feature instanceof FoldingFeature) {
                FoldingFeature foldingFeature = (FoldingFeature) feature;
                hasHinge = true;
                
                // Check the state of the fold
                FoldingFeature.State state = foldingFeature.getState();
                if (state == FoldingFeature.State.HALF_OPENED) {
                    isTableTopMode = true;
                }
                
                // Determine if phone is in "folded" (narrow screen) mode
                // FLAT = fully open, HALF_OPENED = partially open
                isFolded = false; // If we have a folding feature visible, the device is unfolded
            }
        }
        
        // If no folding features detected, the device might be folded (single screen mode)
        if (!hasHinge) {
            // Check screen width to determine if we're in folded mode
            int currentWidthDp = getCurrentScreenWidthDp();
            isFolded = currentWidthDp < 600;
        }
        
        // If fold state changed, update the layout
        if (wasFolded != isFolded || shouldLayoutChange()) {
            android.util.Log.d("SettingsActivity", "Fold state changed - wasFolded: " + wasFolded + ", isFolded: " + isFolded);
            updateLayoutForFoldState();
        }
    }
    
    /**
     * Check if the layout mode needs to change based on current screen size
     */
    private boolean shouldLayoutChange() {
        int newScreenWidthDp = getCurrentScreenWidthDp();
        boolean shouldBeMultiPane = newScreenWidthDp >= 600;
        boolean isCurrentlyMultiPane = !useSinglePane;
        return shouldBeMultiPane != isCurrentlyMultiPane;
    }
    
    /**
     * Update the layout when fold state changes
     */
    private void updateLayoutForFoldState() {
        int currentWidthDp = getCurrentScreenWidthDp();
        boolean shouldUseSingle = currentWidthDp < 600;
        
        if (shouldUseSingle != useSinglePane) {
            // Need to switch layouts - recreate the activity
            android.util.Log.d("SettingsActivity", "Switching layout mode - recreating activity");
            recreate();
        }
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Check if screen width changed significantly (foldable state change)
        int newScreenWidthDp = getCurrentScreenWidthDp();
        boolean shouldBeMultiPane = newScreenWidthDp >= 600;
        boolean isCurrentlyMultiPane = !useSinglePane;
        
        android.util.Log.d("SettingsActivity", "onConfigurationChanged - width: " + newScreenWidthDp + 
                ", shouldBeMultiPane: " + shouldBeMultiPane + ", isCurrentlyMultiPane: " + isCurrentlyMultiPane);
        
        // If the layout mode needs to change, recreate the activity
        if (shouldBeMultiPane != isCurrentlyMultiPane) {
            recreate();
        }
    }
    
    private int getCurrentScreenWidthDp() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) (metrics.widthPixels / metrics.density);
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
        if (isInMultiWindowMode()) {
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
        if (isPossibleFoldable) {
            return true;
        }
        
        // Regular phones (< 600dp width) - use single pane
        // Tablets and unfolded foldables (>= 600dp) - use split pane
        return widthDp < 600;
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
        android.util.Log.d("SettingsActivity", "openDetailPane called, useSinglePane=" + useSinglePane + 
            ", fragment=" + (fragment != null ? fragment.getClass().getSimpleName() : "null"));
        
        if (useSinglePane) {
            // In single pane mode, navigate to a new fragment
            android.util.Log.d("SettingsActivity", "Using single pane mode - replacing single_settings_container");
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.single_settings_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            // In split pane mode, replace the detail container
            android.util.Log.d("SettingsActivity", "Using split pane mode - replacing detail_container");
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
}
