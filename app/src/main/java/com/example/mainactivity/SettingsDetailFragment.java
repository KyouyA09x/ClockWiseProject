package com.example.mainactivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

/**
 * CRITICAL FIX: Settings Detail module.
 * Purpose: Restores all functional sections and implements the "Blindfold" sync guard.
 * Fixes: Infinite flashing loop and missing 'layout_settings_developer' symbol.
 */
public class SettingsDetailFragment extends Fragment {
    private static final String ARG_TYPE = "category";
    private String settingType;
    
    // THE "BLINDFOLD" FIX: Suppresses automated listener triggers during setup
    private boolean isSystemUpdating = true;

    // DEBOUNCE MECHANISM: Prevents rapid-fire events
    private boolean isThemeChangePending = false;
    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingThemeChange = null;

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

        // Switch Logic: Determine which container to show
        View appearanceSection = view.findViewById(R.id.layout_appearance);
        View notificationSection = view.findViewById(R.id.layout_notifications);
        View developerSection = view.findViewById(R.id.layout_developer);

        if (appearanceSection != null) appearanceSection.setVisibility("Appearance".equals(settingType) ? View.VISIBLE : View.GONE);
        if (notificationSection != null) notificationSection.setVisibility("Notifications".equals(settingType) ? View.VISIBLE : View.GONE);
        if (developerSection != null) developerSection.setVisibility("Developer Options".equals(settingType) || "Developer Tools".equals(settingType) ? View.VISIBLE : View.GONE);

        // BREAK THE LOOP: Begin silent initialization
        isSystemUpdating = true;
        
        initAppearanceSection(view);
        initDeveloperSection(view);
        initNotificationSection(view);

        // Setup complete: Re-enable interaction triggers
        isSystemUpdating = false;
    }

    private void initAppearanceSection(View v) {
        View lightBtn = v.findViewById(R.id.btnThemeLight);
        View darkBtn = v.findViewById(R.id.btnThemeDark);
        View autoBtn = v.findViewById(R.id.btnThemeAuto);

        if (lightBtn != null) {
            lightBtn.setOnClickListener(view -> {
                if (!isThemeChangePending) {
                    triggerThemeApply(AppCompatDelegate.MODE_NIGHT_NO, ThemeHelper.MODE_LIGHT);
                }
            });
        }
        if (darkBtn != null) {
            darkBtn.setOnClickListener(view -> {
                if (!isThemeChangePending) {
                    triggerThemeApply(AppCompatDelegate.MODE_NIGHT_YES, ThemeHelper.MODE_DARK);
                }
            });
        }
        if (autoBtn != null) {
            autoBtn.setOnClickListener(view -> {
                if (!isThemeChangePending) {
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
            previewTask.setOnClickListener(view -> Toast.makeText(getContext(), "🔔 Task Notification Preview", Toast.LENGTH_SHORT).show());
        }
        if (previewFocus != null) {
            previewFocus.setOnClickListener(view -> Toast.makeText(getContext(), "🎯 Focus Session Preview", Toast.LENGTH_SHORT).show());
        }
        if (previewQuickInfo != null) {
            previewQuickInfo.setOnClickListener(view -> Toast.makeText(getContext(), "👆 Quick Info Preview", Toast.LENGTH_SHORT).show());
        }
    }
}
