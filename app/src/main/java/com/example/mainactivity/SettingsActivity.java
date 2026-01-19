package com.example.mainactivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * OPTIMIZED: Responsive Settings Architecture for Tablets & Foldables.
 * - Automatically detects screen size using WindowSizeHelper
 * - Uses adaptive layouts (layout-w600dp) for tablets
 * - Implements side-by-side master-detail on large screens
 * - Handles configuration changes smoothly
 * - Properly scales spacing, icons, and touch targets
 */
public class SettingsActivity extends BaseThemedActivity {

    private SlidingPaneLayout slidingPaneLayout;
    private WindowSizeHelper.WindowSizeClass currentWindowSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // SYNC ID: Fixes crash on settings open
        final View root = findViewById(R.id.settingsRoot);
        if (root == null) return;

        // Detect window size for adaptive behavior
        currentWindowSize = WindowSizeHelper.getWidthSizeClass(this);

        // Initialize views FIRST to ensure slidingPaneLayout is ready
        initViews();
        initNavigation();
        setupBackPressedHandler();

        // SAFETY LAYER: Ensures UI readiness before loading components
        root.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.settings_list_container, new SettingsListFragment())
                        .commit();
                
                // On large screens (tablets/foldables), show detail pane by default
                if (WindowSizeHelper.isLargeScreen(this)) {
                    openDetailPane(SettingsDetailFragment.newInstance("Appearance"));
                }
            }
        });
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Explicitly call the activity's method to avoid confusion
                SettingsActivity.this.handleBackPressed();
            }
        });
    }

    private void initNavigation() {
        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        if (toolbar != null) {
            // Set the toolbar as the action bar
            setSupportActionBar(toolbar);

            // Enable the back button
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }

            // Set the navigation click listener (this handles the back button click)
            toolbar.setNavigationOnClickListener(v -> {
                handleBackPressed();
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // This is called when the back arrow in the action bar is clicked
        handleBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // CRITICAL: Explicitly intercept the home button (android.R.id.home)
        // This is the "Navigation Listener" that prevents the app from using
        // the weak default system behavior that can't break the recreation loop
        if (item.getItemId() == android.R.id.home) {
            handleBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleBackPressed() {
        // SIMPLIFIED FIX: Always navigate to MainActivity
        // The previous logic was preventing navigation by checking detail pane state first
        // Now we ALWAYS go home when back is pressed, clearing the entire Settings activity

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        slidingPaneLayout = findViewById(R.id.sliding_pane_layout);
        if (slidingPaneLayout != null) {
            slidingPaneLayout.setLockMode(SlidingPaneLayout.LOCK_MODE_UNLOCKED);
        }
    }

    public void openDetailPane(Fragment fragment) {
        if (isFinishing() || isDestroyed() || fragment == null) return;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_container, fragment)
                .setReorderingAllowed(true)
                .commit();

        if (slidingPaneLayout != null) {
            slidingPaneLayout.openPane();
        }
    }

    @Override
    public void onConfigurationChanged(@androidx.annotation.NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // TABLET/FOLDABLE OPTIMIZATION: Handle configuration changes
        // This is critical for:
        // - Device rotation (portrait ↔ landscape)
        // - Foldable devices (folded ↔ unfolded)
        // - Multi-window/split-screen mode
        // - External display connection

        WindowSizeHelper.WindowSizeClass newWindowSize = WindowSizeHelper.getWidthSizeClass(this);

        // Detect if window size class changed (e.g., folding a foldable device)
        if (currentWindowSize != newWindowSize) {
            currentWindowSize = newWindowSize;

            // On transition to large screen, show detail pane if none is shown
            if (WindowSizeHelper.isLargeScreen(this)) {
                Fragment detailFragment = getSupportFragmentManager().findFragmentById(R.id.detail_container);
                if (detailFragment == null) {
                    openDetailPane(SettingsDetailFragment.newInstance("Appearance"));
                }
            }
        }

        // Force layout recalculation to prevent stretching
        if (slidingPaneLayout != null) {
            slidingPaneLayout.requestLayout();
        }
    }
}
