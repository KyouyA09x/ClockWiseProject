package com.example.mainactivity;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Modern Material Design 3 Dialog Helper
 * Inspired by iOS action sheets with Material Design principles
 * 
 * Features:
 * - Clean visual hierarchy
 * - Prominent destructive actions
 * - Smooth animations
 * - Better typography and spacing
 */
public class ModernDialogHelper {

    /**
     * Shows a modern destructive confirmation dialog (for delete actions)
     * Follows Material Design 3 guidelines with iOS-inspired visual hierarchy
     */
    public static void showDestructiveDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @Nullable String itemName,
            @DrawableRes int iconRes,
            @NonNull Runnable onConfirm,
            @Nullable Runnable onCancel
    ) {
        // Build the message with proper formatting
        String formattedMessage = itemName != null 
            ? message.replace("{item}", "\"" + itemName + "\"")
            : message;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(formattedMessage)
                .setIcon(iconRes)
                .setCancelable(true);

        // Set destructive action button (filled, red)
        builder.setPositiveButton("Delete", (dialog, which) -> {
            if (onConfirm != null) {
                onConfirm.run();
            }
        });

        // Set cancel button (text only)
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (onCancel != null) {
                onCancel.run();
            }
        });

        // Create and customize the dialog
        var dialog = builder.create();
        dialog.show();

        // Customize button styles after showing
        customizeDestructiveButtons(dialog);
    }

    /**
     * Shows a modern destructive dialog for bulk operations
     */
    public static void showBulkDestructiveDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            int itemCount,
            @NonNull String deleteButtonText,
            @DrawableRes int iconRes,
            @NonNull Runnable onConfirm,
            @Nullable Runnable onCancel
    ) {
        String formattedMessage = message.replace("{count}", String.valueOf(itemCount));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(formattedMessage)
                .setIcon(iconRes)
                .setCancelable(true);

        builder.setPositiveButton(deleteButtonText, (dialog, which) -> {
            if (onConfirm != null) {
                onConfirm.run();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (onCancel != null) {
                onCancel.run();
            }
        });

        var dialog = builder.create();
        dialog.show();
        customizeDestructiveButtons(dialog);
    }

    /**
     * Shows a modern warning dialog (non-destructive but important)
     */
    public static void showWarningDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @NonNull String confirmText,
            @DrawableRes int iconRes,
            @NonNull Runnable onConfirm,
            @Nullable Runnable onCancel
    ) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(iconRes)
                .setCancelable(true);

        builder.setPositiveButton(confirmText, (dialog, which) -> {
            if (onConfirm != null) {
                onConfirm.run();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (onCancel != null) {
                onCancel.run();
            }
        });

        var dialog = builder.create();
        dialog.show();
        customizeWarningButtons(dialog);
    }

    /**
     * Customizes dialog buttons for destructive actions
     * Makes the delete button prominent with filled red style
     */
    private static void customizeDestructiveButtons(androidx.appcompat.app.AlertDialog dialog) {
        try {
            // Get buttons
            android.widget.Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            android.widget.Button negativeButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);

            Context context = dialog.getContext();
            
            if (positiveButton != null) {
                // Make delete button filled and red (prominent destructive action)
                int errorColor = ContextCompat.getColor(context, R.color.error);
                int onErrorColor = ContextCompat.getColor(context, R.color.on_error);
                
                positiveButton.setBackgroundTintList(ColorStateList.valueOf(errorColor));
                positiveButton.setTextColor(onErrorColor);
                positiveButton.setAllCaps(false);
                
                // Add some padding for better touch target
                int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
                positiveButton.setPadding(padding, padding/2, padding, padding/2);
            }

            if (negativeButton != null) {
                // Make cancel button text-only and neutral
                negativeButton.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                negativeButton.setAllCaps(false);
                
                int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
                negativeButton.setPadding(padding, padding/2, padding, padding/2);
            }

        } catch (Exception e) {
            // Fail gracefully if customization doesn't work
            e.printStackTrace();
        }
    }

    /**
     * Customizes dialog buttons for warning actions
     * Makes both buttons more balanced
     */
    private static void customizeWarningButtons(androidx.appcompat.app.AlertDialog dialog) {
        try {
            android.widget.Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            android.widget.Button negativeButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);

            Context context = dialog.getContext();
            int padding = (int) (16 * context.getResources().getDisplayMetrics().density);

            if (positiveButton != null) {
                positiveButton.setAllCaps(false);
                positiveButton.setPadding(padding, padding/2, padding, padding/2);
            }

            if (negativeButton != null) {
                negativeButton.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                negativeButton.setAllCaps(false);
                negativeButton.setPadding(padding, padding/2, padding, padding/2);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Builder pattern for more complex dialogs
     */
    public static class Builder {
        private final Context context;
        private String title;
        private String message;
        private String positiveText = "OK";
        private String negativeText = "Cancel";
        private int iconRes = 0;
        private Runnable onPositive;
        private Runnable onNegative;
        private boolean isDestructive = false;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setPositiveButton(String text, Runnable action) {
            this.positiveText = text;
            this.onPositive = action;
            return this;
        }

        public Builder setNegativeButton(String text, Runnable action) {
            this.negativeText = text;
            this.onNegative = action;
            return this;
        }

        public Builder setIcon(@DrawableRes int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        public Builder setDestructive(boolean destructive) {
            this.isDestructive = destructive;
            return this;
        }

        public void show() {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(true);

            if (iconRes != 0) {
                builder.setIcon(iconRes);
            }

            builder.setPositiveButton(positiveText, (dialog, which) -> {
                if (onPositive != null) {
                    onPositive.run();
                }
            });

            builder.setNegativeButton(negativeText, (dialog, which) -> {
                if (onNegative != null) {
                    onNegative.run();
                }
            });

            var dialog = builder.create();
            dialog.show();

            if (isDestructive) {
                customizeDestructiveButtons(dialog);
            } else {
                customizeWarningButtons(dialog);
            }
        }
    }
}
