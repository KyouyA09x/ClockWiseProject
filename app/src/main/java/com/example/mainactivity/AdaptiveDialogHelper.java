package com.example.mainactivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Helper class for showing dialogs that adapt to screen size.
 * On large screens (unfolded foldables/tablets), shows compact centered dialogs.
 * On compact screens (phones), shows full-width bottom sheets.
 */
public class AdaptiveDialogHelper {
    
    // Maximum width for dialogs on large screens (in dp)
    private static final int MAX_DIALOG_WIDTH_DP = 400;
    
    /**
     * Shows a bottom sheet dialog fragment adaptively based on screen size.
     * On large screens, it configures the dialog to be compact and centered.
     */
    public static void showAdaptiveBottomSheet(
            FragmentActivity activity,
            BottomSheetDialogFragment bottomSheet,
            String tag) {
        
        if (activity == null || activity.isFinishing()) return;
        
        FragmentManager fm = activity.getSupportFragmentManager();
        
        // Check if this is a large screen
        boolean isLargeScreen = WindowSizeHelper.isLargeScreen(activity);
        
        if (isLargeScreen) {
            // For large screens, we'll configure the bottom sheet to appear more compact
            bottomSheet.show(fm, tag);
            
            // Post to configure after dialog is created
            activity.getWindow().getDecorView().post(() -> {
                Dialog dialog = bottomSheet.getDialog();
                if (dialog != null && dialog.getWindow() != null) {
                    configureForLargeScreen(activity, dialog);
                }
            });
        } else {
            // Standard bottom sheet for compact screens
            bottomSheet.show(fm, tag);
        }
    }
    
    /**
     * Configures a dialog for large screen display with compact width.
     */
    public static void configureForLargeScreen(Activity activity, Dialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        
        Window window = dialog.getWindow();
        
        // Calculate max width in pixels
        float density = activity.getResources().getDisplayMetrics().density;
        int maxWidthPx = (int) (MAX_DIALOG_WIDTH_DP * density);
        
        // Get screen width
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        
        // Use the smaller of max width or 90% of screen width
        int dialogWidth = Math.min(maxWidthPx, (int) (screenWidth * 0.9));
        
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = dialogWidth;
        params.gravity = Gravity.CENTER;
        
        window.setAttributes(params);
        
        // Make the dialog look more like a floating card
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
    
    /**
     * Configure a standard AlertDialog to be adaptive.
     */
    public static void configureAlertDialogForLargeScreen(Activity activity, Dialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        
        boolean isLargeScreen = WindowSizeHelper.isLargeScreen(activity);
        
        if (isLargeScreen) {
            Window window = dialog.getWindow();
            
            // Calculate max width in pixels
            float density = activity.getResources().getDisplayMetrics().density;
            int maxWidthPx = (int) (MAX_DIALOG_WIDTH_DP * density);
            
            // Get screen width
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            
            // Use the smaller of max width or 80% of screen width
            int dialogWidth = Math.min(maxWidthPx, (int) (screenWidth * 0.8));
            
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = dialogWidth;
            params.gravity = Gravity.CENTER;
            
            window.setAttributes(params);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
    
    /**
     * Helper to set proper dimensions for the dialog content view.
     */
    public static void setAdaptiveDialogContentWidth(Activity activity, View contentView) {
        if (contentView == null) return;
        
        boolean isLargeScreen = WindowSizeHelper.isLargeScreen(activity);
        
        if (isLargeScreen) {
            // Calculate max width in pixels
            float density = activity.getResources().getDisplayMetrics().density;
            int maxWidthPx = (int) (MAX_DIALOG_WIDTH_DP * density);
            
            ViewGroup.LayoutParams params = contentView.getLayoutParams();
            if (params != null) {
                params.width = maxWidthPx;
                contentView.setLayoutParams(params);
            }
        }
    }
    
    /**
     * Check if the current context is on a large screen.
     */
    public static boolean isLargeScreen(Context context) {
        if (context instanceof Activity) {
            return WindowSizeHelper.isLargeScreen((Activity) context);
        }
        return false;
    }
}
