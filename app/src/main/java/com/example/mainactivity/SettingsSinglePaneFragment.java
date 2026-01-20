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
 * Single pane settings fragment for foldable phones when not in split screen mode.
 * Combines all settings in one scrollable view without the split screen layout.
 */
public class SettingsSinglePaneFragment extends Fragment {
    
    private boolean isSystemUpdating = true;
    private boolean isThemeChangePending = false;
    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingThemeChange = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Use the detail fragment layout which has all settings combined
        return inflater.inflate(R.layout.fragment_settings_single_pane, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        isSystemUpdating = true;
        
        initAppearanceSection(view);
        initDeveloperSection(view);
        initNotificationSection(view);

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
            previewQuickInfo.setOnClickListener(view -> showPreviewQuickInfoPopup(view));
        }
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
