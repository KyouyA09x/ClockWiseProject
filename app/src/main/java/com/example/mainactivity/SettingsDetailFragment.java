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
 * CRITICAL FIX: Settings Detail module.
 * Purpose: Restores all functional sections and implements the "Blindfold" sync guard.
 * Fixes: Infinite flashing loop and missing 'layout_settings_developer' symbol.
 */
public class SettingsDetailFragment extends Fragment {
    private static final String ARG_TYPE = "category";
    private static final int REQUEST_OVERLAY_PERMISSION = 1234;
    private String settingType;
    
    // THE "BLINDFOLD" FIX: Suppresses automated listener triggers during setup
    private boolean isSystemUpdating = true;

    // DEBOUNCE MECHANISM: Prevents rapid-fire events
    private boolean isThemeChangePending = false;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingThemeChange = null;
    
    // Floating button switch
    private SwitchMaterial floatingButtonSwitch;
    private SharedPreferences prefs;
    
    public static SettingsDetailFragment newInstance(String category) {
        SettingsDetailFragment fragment = new SettingsDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            settingType = getArguments().getString(ARG_TYPE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up any pending callbacks to prevent memory leaks
        if (debounceHandler != null && pendingThemeChange != null) {
            debounceHandler.removeCallbacks(pendingThemeChange);
        }
        isThemeChangePending = false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Ensure root is present to avoid NPE
        if (view.findViewById(R.id.settingsRoot) == null) return;

        // Initialize preferences
        prefs = requireActivity().getSharedPreferences("settings", MODE_PRIVATE);

        // Get all section views
        View appearanceSection = view.findViewById(R.id.layout_appearance);
        View notificationSection = view.findViewById(R.id.layout_notifications);
        View developerSection = view.findViewById(R.id.layout_developer);
        View taskPreferencesSection = view.findViewById(R.id.layout_task_preferences);
        View dataStorageSection = view.findViewById(R.id.layout_data_storage);
        View aboutHelpSection = view.findViewById(R.id.layout_about_help);

        // Hide all sections first
        if (appearanceSection != null) appearanceSection.setVisibility(View.GONE);
        if (notificationSection != null) notificationSection.setVisibility(View.GONE);
        if (developerSection != null) developerSection.setVisibility(View.GONE);
        if (taskPreferencesSection != null) taskPreferencesSection.setVisibility(View.GONE);
        if (dataStorageSection != null) dataStorageSection.setVisibility(View.GONE);
        if (aboutHelpSection != null) aboutHelpSection.setVisibility(View.GONE);

        // Show only the selected category (iPadOS-style master-detail)
        SettingsCategory category = SettingsCategory.fromString(settingType);
        android.util.Log.d("SettingsDetail", "Category selected: " + settingType + " -> " + category);
        
        switch (category) {
            case NOTIFICATIONS:
                if (notificationSection != null) notificationSection.setVisibility(View.VISIBLE);
                break;
            case APPEARANCE:
                if (appearanceSection != null) appearanceSection.setVisibility(View.VISIBLE);
                break;
            case TASK_PREFERENCES:
                if (taskPreferencesSection != null) taskPreferencesSection.setVisibility(View.VISIBLE);
                break;
            case DATA_STORAGE:
                if (dataStorageSection != null) dataStorageSection.setVisibility(View.VISIBLE);
                break;
            case ABOUT_HELP:
                if (aboutHelpSection != null) aboutHelpSection.setVisibility(View.VISIBLE);
                break;
            case DEVELOPER:
                if (developerSection != null) developerSection.setVisibility(View.VISIBLE);
                break;
            default:
                // Default to notifications
                if (notificationSection != null) notificationSection.setVisibility(View.VISIBLE);
                break;
        }

        // BREAK THE LOOP: Begin silent initialization
        isSystemUpdating = true;
        
        initAppearanceSection(view);
        initDeveloperSection(view);
        initNotificationSection(view);
        initTaskPreferencesSection(view);
        initDataStorageSection(view);
        initAboutHelpSection(view);

        // Setup complete: Re-enable interaction triggers
        isSystemUpdating = false;
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
            // CRITICAL: Remove any existing listener first to prevent double-attachment
            group.setOnCheckedChangeListener(null);
            
            // Set the currently selected theme based on saved preference
            String currentColor = ThemeHelper.getThemeColor(requireActivity());
            int selectedId = R.id.radioDefault;
            if (ThemeHelper.COLOR_CYAN.equals(currentColor)) selectedId = R.id.radioCyan;
            else if (ThemeHelper.COLOR_GREEN.equals(currentColor)) selectedId = R.id.radioGreen;
            else if (ThemeHelper.COLOR_PURPLE.equals(currentColor)) selectedId = R.id.radioPurple;
            else if (ThemeHelper.COLOR_ORANGE.equals(currentColor)) selectedId = R.id.radioOrange;

            // Check the correct radio button WITHOUT triggering the listener
            group.check(selectedId);

            // Track the last selected color to prevent unnecessary recreate calls
            final String[] lastAppliedColor = {currentColor};

            // Now set up the listener with comprehensive guards
            group.setOnCheckedChangeListener((rg, checkedId) -> {
                // GUARD 1: Block during system updates
                if (isSystemUpdating) return;

                // GUARD 2: Block if a theme change is already pending
                if (isThemeChangePending) return;

                // Determine the selected color
                String color = ThemeHelper.COLOR_DEFAULT;
                if (checkedId == R.id.radioCyan) color = ThemeHelper.COLOR_CYAN;
                else if (checkedId == R.id.radioGreen) color = ThemeHelper.COLOR_GREEN;
                else if (checkedId == R.id.radioPurple) color = ThemeHelper.COLOR_PURPLE;
                else if (checkedId == R.id.radioOrange) color = ThemeHelper.COLOR_ORANGE;

                // GUARD 3: Check if color actually changed (prevent re-selecting same color)
                if (color.equals(lastAppliedColor[0])) {
                    return;
                }

                // Set flags immediately to prevent re-entry
                isSystemUpdating = true;
                isThemeChangePending = true;
                lastAppliedColor[0] = color;

                // Disable the radio group to prevent additional clicks
                rg.setEnabled(false);

                // Cancel any pending theme change
                if (pendingThemeChange != null) {
                    debounceHandler.removeCallbacks(pendingThemeChange);
                }


                // Save the color immediately
                final String selectedColor = color;
                ThemeHelper.setThemeColor(requireActivity(), selectedColor);

                // Broadcast theme change to all activities
                if (getActivity() instanceof BaseThemedActivity) {
                    ((BaseThemedActivity) getActivity()).notifyThemeChanged();
                }

                // DEBOUNCED REFRESH: Wait 300ms before recreating activity
                pendingThemeChange = () -> {
                    if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().recreate();
                    }
                };

                debounceHandler.postDelayed(pendingThemeChange, 300);
            });

            // Post a delayed task to re-enable user interaction after initialization
            v.postDelayed(() -> {
                isSystemUpdating = false;
                group.setEnabled(true);
            }, 500);
        }
    }


    private void triggerThemeApply(int delegateMode, int helperMode) {
        // GUARD: Prevent execution if a change is already pending
        if (isSystemUpdating || isThemeChangePending) return;

        // Set flags to prevent re-entry
        isSystemUpdating = true;
        isThemeChangePending = true;

        // Cancel any pending theme change
        if (pendingThemeChange != null) {
            debounceHandler.removeCallbacks(pendingThemeChange);
        }

        // Save the theme mode
        ThemeHelper.setThemeMode(requireActivity(), helperMode);
        AppCompatDelegate.setDefaultNightMode(delegateMode);

        // Broadcast theme change to all activities
        if (getActivity() instanceof BaseThemedActivity) {
            ((BaseThemedActivity) getActivity()).notifyThemeChanged();
        }

        // DEBOUNCED REFRESH: Wait 300ms before recreating activity
        pendingThemeChange = () -> {
            if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                getActivity().recreate();
            }
        };

        debounceHandler.postDelayed(pendingThemeChange, 300);
    }

    @Deprecated
    private void executeSafeRefresh() {
        // This method is deprecated - use the debounced approach in triggerThemeApply instead
        if (getActivity() == null || getActivity().isFinishing()) return;

        // 250ms DELAY: Prevents "flashing" before UI refresh
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                getActivity().recreate();
            }
        }, 250);
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
                // Use overlay for A15+, activity for A14 and below
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
                // Use overlay for A15+, activity for A14 and below
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
            previewQuickInfo.setOnClickListener(view -> showPreviewQuickInfoPopup(view));
        }
        
        // Setup floating button toggle
        setupFloatingButtonToggle(v);
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
    
    /**
     * Initialize Task Preferences section with all AI feature controls
     */
    private void initTaskPreferencesSection(View v) {
        // Master AI Toggle
        com.google.android.material.switchmaterial.SwitchMaterial smartSwitch = 
            v.findViewById(R.id.smartSuggestionsSwitch);
        
        // Individual AI Feature Toggles
        com.google.android.material.switchmaterial.SwitchMaterial prioritySwitch = 
            v.findViewById(R.id.aiPrioritySwitch);
        com.google.android.material.switchmaterial.SwitchMaterial categorySwitch = 
            v.findViewById(R.id.aiCategorySwitch);
        com.google.android.material.switchmaterial.SwitchMaterial timeSwitch = 
            v.findViewById(R.id.aiTimeSwitch);
        com.google.android.material.switchmaterial.SwitchMaterial durationSwitch = 
            v.findViewById(R.id.aiDurationSwitch);
        com.google.android.material.switchmaterial.SwitchMaterial insightsSwitch = 
            v.findViewById(R.id.aiInsightsSwitch);
        
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
        
        // Update sub-feature cards visibility based on master toggle
        updateAIFeatureCardsState(masterEnabled, priorityCard, categoryCard, timeCard, durationCard, insightsCard);
        
        // Master AI Toggle listener
        if (smartSwitch != null) {
            smartSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSystemUpdating) {
                    prefs.edit().putBoolean("smart_suggestions_enabled", isChecked).apply();
                    updateAIFeatureCardsState(isChecked, priorityCard, categoryCard, timeCard, durationCard, insightsCard);
                    android.widget.Toast.makeText(getContext(), 
                        isChecked ? "🧠 AI Assistant enabled" : "AI Assistant disabled", 
                        android.widget.Toast.LENGTH_SHORT).show();
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
        
        // Duration Slider
        com.google.android.material.slider.Slider durationSlider = v.findViewById(R.id.durationSlider);
        android.widget.TextView durationText = v.findViewById(R.id.durationValueText);
        if (durationSlider != null && durationText != null) {
            int savedDuration = prefs.getInt("default_focus_duration", 60);
            durationSlider.setValue(savedDuration);
            durationText.setText(savedDuration + " minutes");
            
            durationSlider.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser && !isSystemUpdating) {
                    int duration = (int) value;
                    durationText.setText(duration + " minutes");
                    prefs.edit().putInt("default_focus_duration", duration).apply();
                }
            });
        }
        
        // Priority Radio Group
        android.widget.RadioGroup priorityGroup = v.findViewById(R.id.priorityRadioGroup);
        if (priorityGroup != null) {
            String savedPriority = prefs.getString("default_priority", "None");
            int selectedId = R.id.priorityNone;
            switch (savedPriority) {
                case "Low": selectedId = R.id.priorityLow; break;
                case "Medium": selectedId = R.id.priorityMedium; break;
                case "High": selectedId = R.id.priorityHigh; break;
            }
            priorityGroup.check(selectedId);
            
            priorityGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (!isSystemUpdating) {
                    String priority = "None";
                    if (checkedId == R.id.priorityLow) priority = "Low";
                    else if (checkedId == R.id.priorityMedium) priority = "Medium";
                    else if (checkedId == R.id.priorityHigh) priority = "High";
                    prefs.edit().putString("default_priority", priority).apply();
                }
            });
        }
    }
    
    /**
     * Update AI feature cards state based on master toggle
     */
    private void updateAIFeatureCardsState(boolean enabled, View... cards) {
        float alpha = enabled ? 1.0f : 0.5f;
        for (View card : cards) {
            if (card != null) {
                card.setAlpha(alpha);
                card.setEnabled(enabled);
                // Also disable the switches inside
                com.google.android.material.switchmaterial.SwitchMaterial switchView = 
                    card.findViewById(card.getId() == R.id.aiPriorityCard ? R.id.aiPrioritySwitch :
                                      card.getId() == R.id.aiCategoryCard ? R.id.aiCategorySwitch :
                                      card.getId() == R.id.aiTimeCard ? R.id.aiTimeSwitch :
                                      card.getId() == R.id.aiDurationCard ? R.id.aiDurationSwitch :
                                      R.id.aiInsightsSwitch);
                if (switchView != null) {
                    switchView.setEnabled(enabled);
                }
            }
        }
    }
    
    /**
     * Initialize Data & Storage section
     */
    private void initDataStorageSection(View v) {
        // Storage Info
        android.widget.TextView storageInfo = v.findViewById(R.id.storageInfoText);
        if (storageInfo != null) {
            TaskRepository repo = TaskRepository.getInstance();
            repo.initialize(requireContext());
            int taskCount = 0;
            int focusCount = 0;
            
            if (repo.morningTasks != null) taskCount += repo.morningTasks.size();
            if (repo.afternoonTasks != null) taskCount += repo.afternoonTasks.size();
            if (repo.nightTasks != null) taskCount += repo.nightTasks.size();
            
            // Count focus sessions
            for (Task t : repo.morningTasks != null ? repo.morningTasks : new java.util.ArrayList<Task>()) {
                if (t.isFocusTask()) focusCount++;
            }
            for (Task t : repo.afternoonTasks != null ? repo.afternoonTasks : new java.util.ArrayList<Task>()) {
                if (t.isFocusTask()) focusCount++;
            }
            for (Task t : repo.nightTasks != null ? repo.nightTasks : new java.util.ArrayList<Task>()) {
                if (t.isFocusTask()) focusCount++;
            }
            
            storageInfo.setText("Tasks: " + (taskCount - focusCount) + " | Focus Sessions: " + focusCount);
        }
        
        // Export Data Button
        com.google.android.material.button.MaterialButton exportBtn = v.findViewById(R.id.btnExportData);
        if (exportBtn != null) {
            exportBtn.setOnClickListener(view -> {
                android.widget.Toast.makeText(getContext(), "Export feature coming soon!", 
                    android.widget.Toast.LENGTH_SHORT).show();
            });
        }
        
        // Clear Completed Button
        com.google.android.material.button.MaterialButton clearCompletedBtn = v.findViewById(R.id.btnClearCompleted);
        if (clearCompletedBtn != null) {
            clearCompletedBtn.setOnClickListener(view -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Clear Completed Tasks")
                    .setMessage("This will remove all completed tasks. Continue?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        TaskRepository repo = TaskRepository.getInstance();
                        repo.clearCompletedTasks(requireContext());
                        android.widget.Toast.makeText(getContext(), "Completed tasks cleared", 
                            android.widget.Toast.LENGTH_SHORT).show();
                        initDataStorageSection(v); // Refresh count
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
        
        // Clear All Data Button
        com.google.android.material.button.MaterialButton clearAllBtn = v.findViewById(R.id.btnClearAllDataStorage);
        if (clearAllBtn != null) {
            clearAllBtn.setOnClickListener(view -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Clear All Data")
                    .setMessage("⚠️ This will permanently delete ALL tasks and focus sessions. This cannot be undone!")
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        TaskRepository repo = TaskRepository.getInstance();
                        repo.clearAllTasks(requireContext());
                        android.widget.Toast.makeText(getContext(), "All data cleared", 
                            android.widget.Toast.LENGTH_SHORT).show();
                        initDataStorageSection(v); // Refresh count
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
        
        // Clear AI Data Button
        com.google.android.material.button.MaterialButton clearAIBtn = v.findViewById(R.id.btnClearAIData);
        if (clearAIBtn != null) {
            clearAIBtn.setOnClickListener(view -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Clear AI Learning Data")
                    .setMessage("This will reset all AI-learned patterns and preferences. The AI will start learning your habits from scratch.")
                    .setPositiveButton("Clear AI Data", (dialog, which) -> {
                        AIModelHelper.getInstance(requireContext()).clearAllData();
                        android.widget.Toast.makeText(getContext(), "AI learning data cleared", 
                            android.widget.Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }
    
    /**
     * Initialize About & Help section
     */
    private void initAboutHelpSection(View v) {
        // Version Text
        android.widget.TextView versionText = v.findViewById(R.id.versionText);
        if (versionText != null) {
            try {
                String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                versionText.setText("Version " + versionName);
            } catch (Exception e) {
                versionText.setText("Version 1.0.0");
            }
        }
        
        // FAQ Button
        com.google.android.material.button.MaterialButton faqBtn = v.findViewById(R.id.btnFaq);
        if (faqBtn != null) {
            faqBtn.setOnClickListener(view -> {
                showFaqDialog();
            });
        }
        
        // Feedback Button
        com.google.android.material.button.MaterialButton feedbackBtn = v.findViewById(R.id.btnFeedback);
        if (feedbackBtn != null) {
            feedbackBtn.setOnClickListener(view -> {
                android.widget.Toast.makeText(getContext(), "Feedback feature coming soon!", 
                    android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    /**
     * Show FAQ dialog
     */
    private void showFaqDialog() {
        String faq = "📌 How to create a task?\n" +
            "Tap the + button and choose 'Add Task'.\n\n" +
            "📌 How to create a Focus Session?\n" +
            "Tap the + button and choose 'Add Focus Session'. Set start/end times.\n\n" +
            "📌 What are Smart Suggestions?\n" +
            "AI-powered feature that suggests optimal times and priorities based on your task names.\n\n" +
            "📌 How does time-aware sorting work?\n" +
            "Focus sessions are sorted by current time of day - morning tasks appear first in the morning, etc.";
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("FAQ & Tips")
            .setMessage(faq)
            .setPositiveButton("Got it", null)
            .show();
    }
}