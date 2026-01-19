package com.example.mainactivity;

import android.app.Activity;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

public class WindowSizeHelper {
    public enum WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

    public static WindowSizeClass getWidthSizeClass(Activity activity) {
        WindowMetrics metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity);
        
        float widthDp = metrics.getBounds().width() / 
                activity.getResources().getDisplayMetrics().density;
        
        // Standard Android adaptive breakpoints:
        // Compact: < 600dp (Phones)
        // Medium: 600dp - 840dp (Foldables/Small Tablets)
        // Expanded: > 840dp (Large Tablets)
        
        if (widthDp < 600f) {
            return WindowSizeClass.COMPACT;
        } else if (widthDp < 840f) {
            return WindowSizeClass.MEDIUM;
        } else {
            return WindowSizeClass.EXPANDED;
        }
    }
    
    public static boolean isLargeScreen(Activity activity) {
        return getWidthSizeClass(activity) != WindowSizeClass.COMPACT;
    }
}
