package com.example.mainactivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

import static android.content.Context.MODE_PRIVATE;

/**
 * Single pane settings fragment for foldable phones when not in split screen mode.
 * Combines all settings in one scrollable view without the split screen layout.
 */
public class SettingsSinglePaneFragment extends Fragment {
    
    private static final int REQUEST_OVERLAY_PERMISSION = 1234;
    
    private boolean isSystemUpdating = true;
    private boolean isThemeChangePending = false;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingThemeChange = null;
    
    // Floating button switch
    private SwitchMaterial floatingButtonSwitch;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Use the detail fragment layout which has all settings combined
        return inflater.inflate(R.layout.fragment_settings_single_pane, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize preferences
        prefs = requireActivity().getSharedPreferences("settings", MODE_PRIVATE);
        
        isSystemUpdating = true;
        
        initAppearanceSection(view);
        initDeveloperSection(view);
        initNotificationSection(view);
        initAIFeaturesSection(view);

        isSystemUpdating = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (debounceHandler != null && pendingThemeChange != null) {
            debounceHandler.removeCallbacks(pendingThemeChange);
        }
        isThemeChangePending = false;
    }

    private void initAppearanceSection(View v) {
        View lightBtn = v.findViewById(R.id.btnThemeLight);
        View darkBtn = v.findViewById(R.id.btnThemeDark);
        View autoBtn = v.findViewById(R.id.btnThemeAuto);

        // Get current theme mode to highlight the selected button
        int currentMode = ThemeHelper.getThemeMode(requireActivity());
        
        // Set selected state for current mode
        if (lightBtn != null) lightBtn.setSelected(currentMode == ThemeHelper.MODE_LIGHT);
        if (darkBtn != null) darkBtn.setSelected(currentMode == ThemeHelper.MODE_DARK);
        if (autoBtn != null) autoBtn.setSelected(currentMode == ThemeHelper.MODE_AUTO);

        if (lightBtn != null) {
            lightBtn.setOnClickListener(view -> {
                if (!isThemeChangePending) {
                    // Update selected state
                    lightBtn.setSelected(true);
                    if (darkBtn != null) darkBtn.setSelected(false);
                    if (autoBtn != null) autoBtn.setSelected(false);
                    triggerThemeApply(AppCompatDelegate.MODE_NIGHT_NO, ThemeHelper.MODE_LIGHT);
                }
            });
        }
        if (darkBtn != null) {
            darkBtn.setOnClickListener(view -> {
                if (!isThemeChangePending) {
                    // Update selected state
                    if (lightBtn != null) lightBtn.setSelected(false);
                    darkBtn.setSelected(true);
                    if (autoBtn != null) autoBtn.setSelected(false);
                    triggerThemeApply(AppCompatDelegate.MODE_NIGHT_YES, ThemeHelper.MODE_DARK);
                }
            });
        }
        if (autoBtn != null) {
            autoBtn.setOnClickListener(view -> {
                if (!isThemeChangePending) {
                    // Update selected state
                    if (lightBtn != null) lightBtn.setSelected(false);
                    if (darkBtn != null) darkBtn.setSelected(false);
                    autoBtn.setSelected(true);
                    triggerThemeApply(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeHelper.MODE_AUTO);
                }
            });
        }

        RadioGroup group = v.findViewById(R.id.themeRadioGroup);
        if (group != null) {
            group.setOnCheckedChangeListener(null);
            
            String currentColor = ThemeHelper.getThemeColor(requireActivity());
            int selectedId = R.id.radioDefault;
            if (ThemeHelper.COLOR_CYAN.equals(currentColor)) selectedId = R.id.radioCyan;
            else if (ThemeHelper.COLOR_GREEN.equals(currentColor)) selectedId = R.id.radioGreen;
            else if (ThemeHelper.COLOR_PURPLE.equals(currentColor)) selectedId = R.id.radioPurple;
            else if (ThemeHelper.COLOR_ORANGE.equals(currentColor)) selectedId = R.id.radioOrange;

            group.check(selectedId);

            final String[] lastAppliedColor = {currentColor};

            group.setOnCheckedChangeListener((rg, checkedId) -> {
                if (isSystemUpdating || isThemeChangePending) return;

                String color = ThemeHelper.COLOR_DEFAULT;
                if (checkedId == R.id.radioCyan) color = ThemeHelper.COLOR_CYAN;
                else if (checkedId == R.id.radioGreen) color = ThemeHelper.COLOR_GREEN;
                else if (checkedId == R.id.radioPurple) color = ThemeHelper.COLOR_PURPLE;
                else if (checkedId == R.id.radioOrange) color = ThemeHelper.COLOR_ORANGE;

                if (color.equals(lastAppliedColor[0])) return;

                isSystemUpdating = true;
                isThemeChangePending = true;
                lastAppliedColor[0] = color;

                rg.setEnabled(false);

                if (pendingThemeChange != null) {
                    debounceHandler.removeCallbacks(pendingThemeChange);
                }

                final String selectedColor = color;
                ThemeHelper.setThemeColor(requireActivity(), selectedColor);

                if (getActivity() instanceof BaseThemedActivity) {
                    ((BaseThemedActivity) getActivity()).notifyThemeChanged();
                }

                pendingThemeChange = () -> {
                    if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().recreate();
                    }
                };

                debounceHandler.postDelayed(pendingThemeChange, 300);
            });

            v.postDelayed(() -> {
                isSystemUpdating = false;
                group.setEnabled(true);
            }, 500);
        }
    }

    private void triggerThemeApply(int delegateMode, int helperMode) {
        if (isSystemUpdating || isThemeChangePending) return;

        isSystemUpdating = true;
        isThemeChangePending = true;

        if (pendingThemeChange != null) {
            debounceHandler.removeCallbacks(pendingThemeChange);
        }

        ThemeHelper.setThemeMode(requireActivity(), helperMode);
        AppCompatDelegate.setDefaultNightMode(delegateMode);

        if (getActivity() instanceof BaseThemedActivity) {
            ((BaseThemedActivity) getActivity()).notifyThemeChanged();
        }

        pendingThemeChange = () -> {
            if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                getActivity().recreate();
            }
        };

        debounceHandler.postDelayed(pendingThemeChange, 300);
    }

    private void initDeveloperSection(View v) {
        View populateBtn = v.findViewById(R.id.btnPopulateData);
        View clearBtn = v.findViewById(R.id.btnClearData);

        if (populateBtn != null) {
            populateBtn.setOnClickListener(view -> {
                new TestDataGenerator(requireContext()).populateTestData();
                Toast.makeText(getContext(), "Test Data Injected", Toast.LENGTH_SHORT).show();
            });
        }

        if (clearBtn != null) {
            clearBtn.setOnClickListener(view -> {
                new TestDataGenerator(requireContext()).clearAllData();
                Toast.makeText(getContext(), "Database Cleared", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void initNotificationSection(View v) {
        View previewTask = v.findViewById(R.id.btnPreviewTask);
        View previewFocus = v.findViewById(R.id.btnPreviewFocus);
        View previewQuickInfo = v.findViewById(R.id.btnPreviewQuickInfo);

        if (previewTask != null) {
            previewTask.setOnClickListener(view -> {
                if (android.os.Build.VERSION.SDK_INT >= 35 && OverlayNotificationService.canDrawOverlays(requireContext())) {
                    OverlayNotificationService.showNotificationWithTime(
                        requireContext(), -1, "Review Project Proposal",
                        "This is a preview of how your task notification will appear!",
                        "High", "reminder", 10, 30, "AM", 0, 0, "AM"
                    );
                } else {
                    PopupNotificationActivity.show(
                        requireContext(), -1, "Review Project Proposal",
                        "This is a preview of how your task notification will appear!",
                        "High", "reminder", 10, 30, "AM", 0, 0, "AM"
                    );
                }
            });
        }
        if (previewFocus != null) {
            previewFocus.setOnClickListener(view -> {
                if (android.os.Build.VERSION.SDK_INT >= 35 && OverlayNotificationService.canDrawOverlays(requireContext())) {
                    OverlayNotificationService.showNotificationWithTime(
                        requireContext(), -2, "Deep Work: Project Planning",
                        "Focus session preview with timer and controls!",
                        "High", "focus", 2, 0, "PM", 4, 0, "PM"
                    );
                } else {
                    PopupNotificationActivity.show(
                        requireContext(), -2, "Deep Work: Project Planning",
                        "Focus session preview with timer and controls!",
                        "High", "focus", 2, 0, "PM", 4, 0, "PM"
                    );
                }
            });
        }
        if (previewQuickInfo != null) {
            previewQuickInfo.setOnClickListener(this::showPreviewQuickInfoPopup);
        }
        
        // Setup floating button toggle
        setupFloatingButtonToggle(v);
    }
    
    /**
     * Initialize AI Features section with all toggles
     */
    private void initAIFeaturesSection(View v) {
        // Master AI Toggle
        SwitchMaterial smartSwitch = v.findViewById(R.id.smartSuggestionsSwitch);
        
        // Individual AI Feature Toggles
        SwitchMaterial prioritySwitch = v.findViewById(R.id.aiPrioritySwitch);
        SwitchMaterial categorySwitch = v.findViewById(R.id.aiCategorySwitch);
        SwitchMaterial timeSwitch = v.findViewById(R.id.aiTimeSwitch);
        SwitchMaterial durationSwitch = v.findViewById(R.id.aiDurationSwitch);
        SwitchMaterial insightsSwitch = v.findViewById(R.id.aiInsightsSwitch);
        
        // Feature card views for enabling/disabling
        View priorityCard = v.findViewById(R.id.aiPriorityCard);
        View categoryCard = v.findViewById(R.id.aiCategoryCard);
        View timeCard = v.findViewById(R.id.aiTimeCard);
        View durationCard = v.findViewById(R.id.aiDurationCard);
        View insightsCard = v.findViewById(R.id.aiInsightsCard);
        
        // Load saved preferences
        boolean masterEnabled = prefs.getBoolean("smart_suggestions_enabled", true);
        boolean priorityEnabled = prefs.getBoolean("ai_priority_enabled", true);
        boolean categoryEnabled = prefs.getBoolean("ai_category_enabled", true);
        boolean timeEnabled = prefs.getBoolean("ai_time_enabled", true);
        boolean durationEnabled = prefs.getBoolean("ai_duration_enabled", true);
        boolean insightsEnabled = prefs.getBoolean("ai_insights_enabled", true);
        
        // Set initial states
        if (smartSwitch != null) smartSwitch.setChecked(masterEnabled);
        if (prioritySwitch != null) prioritySwitch.setChecked(priorityEnabled);
        if (categorySwitch != null) categorySwitch.setChecked(categoryEnabled);
        if (timeSwitch != null) timeSwitch.setChecked(timeEnabled);
        if (durationSwitch != null) durationSwitch.setChecked(durationEnabled);
        if (insightsSwitch != null) insightsSwitch.setChecked(insightsEnabled);
        
        // Update sub-feature cards state based on master toggle
        updateAICardsState(masterEnabled, priorityCard, categoryCard, timeCard, durationCard, insightsCard,
                          prioritySwitch, categorySwitch, timeSwitch, durationSwitch, insightsSwitch);
        
        // Master AI Toggle listener
        if (smartSwitch != null) {
            smartSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSystemUpdating) {
                    prefs.edit().putBoolean("smart_suggestions_enabled", isChecked).apply();
                    updateAICardsState(isChecked, priorityCard, categoryCard, timeCard, durationCard, insightsCard,
                                      prioritySwitch, categorySwitch, timeSwitch, durationSwitch, insightsSwitch);
                    Toast.makeText(getContext(), 
                        isChecked ? "🧠 AI Assistant enabled" : "AI Assistant disabled", 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Individual feature toggle listeners
        if (prioritySwitch != null) {
            prioritySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSystemUpdating) {
                    prefs.edit().putBoolean("ai_priority_enabled", isChecked).apply();
                }
            });
        }
        
        if (categorySwitch != null) {
            categorySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSystemUpdating) {
                    prefs.edit().putBoolean("ai_category_enabled", isChecked).apply();
                }
            });
        }
        
        if (timeSwitch != null) {
            timeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSystemUpdating) {
                    prefs.edit().putBoolean("ai_time_enabled", isChecked).apply();
                }
            });
        }
        
        if (durationSwitch != null) {
            durationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSystemUpdating) {
                    prefs.edit().putBoolean("ai_duration_enabled", isChecked).apply();
                }
            });
        }
        
        if (insightsSwitch != null) {
            insightsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSystemUpdating) {
                    prefs.edit().putBoolean("ai_insights_enabled", isChecked).apply();
                }
            });
        }
    }
    
    /**
     * Update AI feature cards state based on master toggle
     */
    private void updateAICardsState(boolean enabled, View priorityCard, View categoryCard, 
                                    View timeCard, View durationCard, View insightsCard,
                                    SwitchMaterial prioritySwitch, SwitchMaterial categorySwitch,
                                    SwitchMaterial timeSwitch, SwitchMaterial durationSwitch,
                                    SwitchMaterial insightsSwitch) {
        float alpha = enabled ? 1.0f : 0.5f;
        
        if (priorityCard != null) {
            priorityCard.setAlpha(alpha);
            if (prioritySwitch != null) prioritySwitch.setEnabled(enabled);
        }
        if (categoryCard != null) {
            categoryCard.setAlpha(alpha);
            if (categorySwitch != null) categorySwitch.setEnabled(enabled);
        }
        if (timeCard != null) {
            timeCard.setAlpha(alpha);
            if (timeSwitch != null) timeSwitch.setEnabled(enabled);
        }
        if (durationCard != null) {
            durationCard.setAlpha(alpha);
            if (durationSwitch != null) durationSwitch.setEnabled(enabled);
        }
        if (insightsCard != null) {
            insightsCard.setAlpha(alpha);
            if (insightsSwitch != null) insightsSwitch.setEnabled(enabled);
        }
    }
    
    private void setupFloatingButtonToggle(View v) {
        floatingButtonSwitch = v.findViewById(R.id.floatingButtonSwitch);
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
                if (!Settings.canDrawOverlays(requireContext())) {
                    // Need to request overlay permission
                    floatingButtonSwitch.setChecked(false);
                    showOverlayPermissionDialog();
                    return;
                }
                
                // Permission granted, start service
                startFloatingButtonService();
                prefs.edit().putBoolean("floating_button_enabled", true).apply();
                Toast.makeText(requireContext(), "Floating button enabled", Toast.LENGTH_SHORT).show();
            } else {
                // Stop service
                stopFloatingButtonService();
                prefs.edit().putBoolean("floating_button_enabled", false).apply();
                Toast.makeText(requireContext(), "Floating button disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showOverlayPermissionDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Overlay Permission Required")
                .setMessage("The floating button requires permission to display over other apps. This allows quick access to ClockWise actions from anywhere on your device.")
                .setIcon(R.drawable.ic_flash)
                .setPositiveButton("Grant Permission", (dialog, which) -> requestOverlayPermission())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + requireContext().getPackageName()));
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(requireContext())) {
                // Permission granted
                if (floatingButtonSwitch != null) {
                    floatingButtonSwitch.setChecked(true);
                }
                startFloatingButtonService();
                prefs.edit().putBoolean("floating_button_enabled", true).apply();
                Toast.makeText(requireContext(), "Floating button enabled", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(requireContext(), "Overlay permission is required for floating button", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void startFloatingButtonService() {
        try {
            // Double-check overlay permission before starting
            if (!Settings.canDrawOverlays(requireContext())) {
                Toast.makeText(requireContext(), "Overlay permission not granted", Toast.LENGTH_SHORT).show();
                if (floatingButtonSwitch != null) {
                    floatingButtonSwitch.setChecked(false);
                }
                prefs.edit().putBoolean("floating_button_enabled", false).apply();
                return;
            }
            
            Intent serviceIntent = new Intent(requireContext(), FloatingButtonService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent);
            } else {
                requireContext().startService(serviceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to start floating button: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (floatingButtonSwitch != null) {
                floatingButtonSwitch.setChecked(false);
            }
            prefs.edit().putBoolean("floating_button_enabled", false).apply();
        }
    }
    
    private void stopFloatingButtonService() {
        Intent serviceIntent = new Intent(requireContext(), FloatingButtonService.class);
        requireContext().stopService(serviceIntent);
    }
    
    private void showPreviewQuickInfoPopup(android.view.View anchorView) {
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(requireContext());
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
            typeText.setTextColor(requireContext().getColor(R.color.primary));
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
            iconContainer.setCardBackgroundColor(requireContext().getColor(R.color.primary));
        }
        
        popupWindow.showAtLocation(anchorView, android.view.Gravity.CENTER, 0, 0);
    }
}
