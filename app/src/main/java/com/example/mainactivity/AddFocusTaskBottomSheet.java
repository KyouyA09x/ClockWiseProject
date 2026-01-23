package com.example.mainactivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddFocusTaskBottomSheet extends BottomSheetDialogFragment {

    public interface OnTaskSavedListener {
        void onTaskSaved();
    }

    private OnTaskSavedListener listener;
    private TaskRepository taskRepository;
    private Calendar selectedDate = Calendar.getInstance();
    private String selectedUrgency = "None";

    // Edit mode
    private boolean isEditMode = false;
    private boolean isQuickTask = false;
    private Task editingTask = null;

    // Time values
    private int startHour = 9;
    private int startMinute = 0;
    private String startAmPm = "AM";
    private int endHour = 10;
    private int endMinute = 0;
    private String endAmPm = "AM";

    // Views
    private TextInputEditText taskNameEditText;
    private View startTimeRow;
    private View endTimeRow;
    private TextView startTimeDisplay;
    private TextView endTimeDisplay;
    private ChipGroup priorityChipGroup;
    private Chip priorityNone, priorityLow, priorityMedium, priorityHigh;
    private MaterialSwitch vibrationSwitch;
    private MaterialSwitch alarmSwitch;
    private MaterialButton saveButton;
    private MaterialButton headerSaveButton;
    private MaterialButton closeButton;
    private MaterialButton deleteButton;
    private TextView titleText;
    private TextView subtitleText;
    private TextView dateText;
    private TextView alarmSoundText;
    private String selectedAlarmSound = "Default Alarm";
    
    // AI Suggestion Views
    private View aiSuggestionCard;
    private TextView aiConfidenceText;
    private Chip chipPrioritySuggestion;
    private Chip chipCategorySuggestion;
    private Chip chipTimeSuggestion;
    private Chip chipDurationSuggestion;
    private TextView aiReasonText;
    private Handler aiDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable aiSuggestionRunnable;
    private AIModelHelper aiHelper;

    public static AddFocusTaskBottomSheet newInstance() {
        return new AddFocusTaskBottomSheet();
    }

    public static AddFocusTaskBottomSheet newInstance(Task task) {
        AddFocusTaskBottomSheet fragment = new AddFocusTaskBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable("TASK", task);
        args.putBoolean("EDIT_MODE", true);
        fragment.setArguments(args);
        return fragment;
    }
    
    public static AddFocusTaskBottomSheet newInstance(Task task, boolean hideDate) {
        AddFocusTaskBottomSheet fragment = new AddFocusTaskBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable("TASK", task);
        args.putBoolean("EDIT_MODE", false);
        args.putBoolean("PREFILLED", true);
        args.putBoolean("HIDE_DATE", hideDate); // For quick tasks
        fragment.setArguments(args);
        return fragment;
    }

    public static AddFocusTaskBottomSheet newInstanceWithData(Task task) {
        AddFocusTaskBottomSheet fragment = new AddFocusTaskBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable("TASK", task);
        args.putBoolean("EDIT_MODE", false); // Not edit mode - creating new task with pre-filled data
        args.putBoolean("PREFILLED", true);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnTaskSavedListener(OnTaskSavedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(requireContext());

        if (getArguments() != null) {
            isEditMode = getArguments().getBoolean("EDIT_MODE", false);
            isQuickTask = getArguments().getBoolean("QUICK_TASK", false) || getArguments().getBoolean("HIDE_DATE", false);
            editingTask = getArguments().getParcelable("TASK");
            
            // If it's a quick task or edit mode with task data, load the task
            if (editingTask != null) {
                if (editingTask.date != null && !editingTask.date.isEmpty()) {
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                        selectedDate.setTime(sdf.parse(editingTask.date));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_focus_task_modern, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Make bottom sheet expanded by default
        if (getDialog() != null) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setSkipCollapsed(true);
            
            // Configure for large screens - make dialog more compact
            if (getActivity() != null && WindowSizeHelper.isLargeScreen(getActivity())) {
                configureForLargeScreen(dialog);
            }
        }

        initViews(view);
        setupClickListeners();

        if (isEditMode && editingTask != null) {
            populateFieldsForEditing();
        } else if (getArguments() != null && getArguments().getBoolean("PREFILLED", false) && editingTask != null) {
            populateFieldsFromTask();
        }
    }
    
    /**
     * Configures the dialog for large screens (foldables unfolded, tablets).
     * Makes the dialog appear as a compact centered popup instead of full-width bottom sheet.
     */
    private void configureForLargeScreen(BottomSheetDialog dialog) {
        if (dialog == null || dialog.getWindow() == null || getActivity() == null) return;
        
        // Set maximum width for the dialog
        float density = getResources().getDisplayMetrics().density;
        int maxWidthPx = (int) (400 * density); // 400dp max width
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int dialogWidth = Math.min(maxWidthPx, (int) (screenWidth * 0.85));
        
        android.view.Window window = dialog.getWindow();
        android.view.WindowManager.LayoutParams params = window.getAttributes();
        params.width = dialogWidth;
        params.gravity = android.view.Gravity.CENTER;
        window.setAttributes(params);
        
        // Make background transparent for rounded corners effect
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    private void initViews(View view) {
        titleText = view.findViewById(R.id.titleText);
        subtitleText = view.findViewById(R.id.subtitleText);
        closeButton = view.findViewById(R.id.closeButton);
        headerSaveButton = view.findViewById(R.id.headerSaveButton);
        taskNameEditText = view.findViewById(R.id.taskNameEditText);
        
        startTimeRow = view.findViewById(R.id.startTimeRow);
        endTimeRow = view.findViewById(R.id.endTimeRow);
        startTimeDisplay = view.findViewById(R.id.startTimeDisplay);
        endTimeDisplay = view.findViewById(R.id.endTimeDisplay);
        
        priorityChipGroup = view.findViewById(R.id.priorityChipGroup);
        priorityNone = view.findViewById(R.id.priorityNone);
        priorityLow = view.findViewById(R.id.priorityLow);
        priorityMedium = view.findViewById(R.id.priorityMedium);
        priorityHigh = view.findViewById(R.id.priorityHigh);
        
        vibrationSwitch = view.findViewById(R.id.vibrationSwitch);
        alarmSwitch = view.findViewById(R.id.alarmSwitch);
        saveButton = view.findViewById(R.id.saveButton);
        deleteButton = view.findViewById(R.id.deleteButton);
        dateText = view.findViewById(R.id.dateText);
        alarmSoundText = view.findViewById(R.id.alarmSoundText);
        
        // Initialize AI Suggestion Views
        aiSuggestionCard = view.findViewById(R.id.aiSuggestionCard);
        aiConfidenceText = view.findViewById(R.id.aiConfidenceText);
        chipPrioritySuggestion = view.findViewById(R.id.chipPrioritySuggestion);
        chipCategorySuggestion = view.findViewById(R.id.chipCategorySuggestion);
        chipTimeSuggestion = view.findViewById(R.id.chipTimeSuggestion);
        chipDurationSuggestion = view.findViewById(R.id.chipDurationSuggestion);
        aiReasonText = view.findViewById(R.id.aiReasonText);
        
        // Handle Quick Task Mode - simplify UI
        if (isQuickTask) {
            setupQuickTaskMode(view);
        } else {
            // Initialize AI Helper - always initialize to show/hide based on settings
            aiHelper = AIModelHelper.getInstance(requireContext());
            if (AIModelHelper.isEnabled(requireContext())) {
                setupAISuggestions();
                // Show a subtle hint that AI is available
                if (aiSuggestionCard != null) {
                    aiSuggestionCard.setVisibility(View.VISIBLE);
                    if (aiReasonText != null) {
                        aiReasonText.setText("💡 Start typing to get AI suggestions...");
                    }
                    // Hide chips until suggestions are ready
                    if (chipPrioritySuggestion != null) chipPrioritySuggestion.setVisibility(View.GONE);
                    if (chipCategorySuggestion != null) chipCategorySuggestion.setVisibility(View.GONE);
                    if (chipTimeSuggestion != null) chipTimeSuggestion.setVisibility(View.GONE);
                    if (chipDurationSuggestion != null) chipDurationSuggestion.setVisibility(View.GONE);
                    if (aiConfidenceText != null) aiConfidenceText.setVisibility(View.GONE);
                }
            } else {
                if (aiSuggestionCard != null) {
                    aiSuggestionCard.setVisibility(View.GONE);
                }
            }
        }

        // Initialize time displays
        updateStartTimeDisplay();
        updateEndTimeDisplay();
        updateDateLabel();
    }
    
    /**
     * Configure UI for Quick Focus mode - minimal options for fast session creation
     */
    private void setupQuickTaskMode(View view) {
        // Update title
        if (titleText != null) {
            titleText.setText("🎯 Quick Focus");
        }
        
        // Show "Today only" indicator
        View quickTaskIndicator = view.findViewById(R.id.quickTaskIndicator);
        if (quickTaskIndicator != null) {
            quickTaskIndicator.setVisibility(View.VISIBLE);
        }
        
        // Hide date picker
        if (dateText != null) {
            dateText.setVisibility(View.GONE);
        }
        
        // Hide date card section
        View dateCard = view.findViewById(R.id.dateText);
        if (dateCard != null && dateCard.getParent() != null) {
            View dateCardParent = (View) dateCard.getParent();
            if (dateCardParent != null && dateCardParent.getParent() != null) {
                View dateSection = (View) dateCardParent.getParent();
                if (dateSection != null) {
                    dateSection.setVisibility(View.GONE);
                }
            }
        }
        
        // Hide AI suggestions card in quick mode
        if (aiSuggestionCard != null) {
            aiSuggestionCard.setVisibility(View.GONE);
        }
        
        // Hide priority card - use default Medium priority
        View priorityCard = view.findViewById(R.id.priorityCard);
        if (priorityCard != null) {
            priorityCard.setVisibility(View.GONE);
        }
        selectedUrgency = "Medium"; // Default to medium priority
        
        // Hide options card entirely for quick mode
        View optionsCard = view.findViewById(R.id.optionsCard);
        if (optionsCard != null) {
            optionsCard.setVisibility(View.GONE);
        }
        
        // Set sensible defaults for quick mode
        if (alarmSwitch != null) {
            alarmSwitch.setChecked(true); // Enable alarm by default
        }
        
        // Update save button text
        if (saveButton != null) {
            saveButton.setText("🎯 Start Quick Focus");
        }
    }
    
    /**
     * Setup AI suggestion functionality with debounced text watching
     */
    private void setupAISuggestions() {
        if (taskNameEditText == null || aiSuggestionCard == null) return;
        
        taskNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (aiSuggestionRunnable != null) {
                    aiDebounceHandler.removeCallbacks(aiSuggestionRunnable);
                }
                aiSuggestionRunnable = () -> updateAISuggestions(s.toString());
                aiDebounceHandler.postDelayed(aiSuggestionRunnable, 300);
            }
        });
        
        if (chipPrioritySuggestion != null) {
            chipPrioritySuggestion.setOnClickListener(v -> applyPrioritySuggestion());
        }
        if (chipDurationSuggestion != null) {
            chipDurationSuggestion.setOnClickListener(v -> applyDurationSuggestion());
        }
    }
    
    private void updateAISuggestions(String taskName) {
        if (aiHelper == null || getContext() == null) return;
        
        // Check if task name is too short
        if (taskName == null || taskName.trim().length() < 2) {
            // Show hint state
            if (aiSuggestionCard != null && AIModelHelper.isEnabled(requireContext())) {
                aiSuggestionCard.setVisibility(View.VISIBLE);
                if (aiReasonText != null) aiReasonText.setText("💡 Start typing to get AI suggestions...");
                if (chipPrioritySuggestion != null) chipPrioritySuggestion.setVisibility(View.GONE);
                if (chipCategorySuggestion != null) chipCategorySuggestion.setVisibility(View.GONE);
                if (chipTimeSuggestion != null) chipTimeSuggestion.setVisibility(View.GONE);
                if (chipDurationSuggestion != null) chipDurationSuggestion.setVisibility(View.GONE);
                if (aiConfidenceText != null) aiConfidenceText.setVisibility(View.GONE);
            }
            return;
        }
        
        AIModelHelper.TaskSuggestions suggestions = aiHelper.getTaskSuggestions(taskName);
        
        if (aiSuggestionCard != null) aiSuggestionCard.setVisibility(View.VISIBLE);
        if (aiConfidenceText != null) aiConfidenceText.setVisibility(View.VISIBLE);
        
        // Update priority suggestion (if enabled)
        if (chipPrioritySuggestion != null && suggestions.priority != null) {
            if (AIModelHelper.isPriorityEnabled(requireContext())) {
                chipPrioritySuggestion.setVisibility(View.VISIBLE);
                String priorityText = suggestions.priority.getEmoji() + " Priority: " + suggestions.priority.getValue();
                chipPrioritySuggestion.setText(priorityText);
                chipPrioritySuggestion.setTag(suggestions.priority.getValue());
            } else {
                chipPrioritySuggestion.setVisibility(View.GONE);
            }
        }
        
        // Update category suggestion (if enabled)
        if (chipCategorySuggestion != null && suggestions.category != null) {
            if (AIModelHelper.isCategoryEnabled(requireContext())) {
                chipCategorySuggestion.setVisibility(View.VISIBLE);
                String categoryEmoji = getCategoryEmoji(suggestions.category.getValue());
                chipCategorySuggestion.setText(categoryEmoji + " " + suggestions.category.getValue());
            } else {
                chipCategorySuggestion.setVisibility(View.GONE);
            }
        }
        
        // Update duration suggestion (if enabled - special for focus tasks)
        if (chipDurationSuggestion != null && suggestions.focusDuration != null) {
            if (AIModelHelper.isDurationEnabled(requireContext())) {
                chipDurationSuggestion.setVisibility(View.VISIBLE);
                chipDurationSuggestion.setText("⏱️ " + suggestions.focusDuration.getValue() + " min");
                chipDurationSuggestion.setTag(Integer.parseInt(suggestions.focusDuration.getValue()));
            } else {
                chipDurationSuggestion.setVisibility(View.GONE);
            }
        }
        
        // Update confidence and reason
        if (aiConfidenceText != null && suggestions.priority != null) {
            aiConfidenceText.setText(suggestions.priority.getEmoji() + " " + suggestions.priority.getConfidenceLevel() + " confidence");
        }
        if (aiReasonText != null && suggestions.focusDuration != null) {
            aiReasonText.setText("💡 " + suggestions.focusDuration.getReason());
        }
    }
    
    private void applyPrioritySuggestion() {
        if (chipPrioritySuggestion == null) return;
        String priority = (String) chipPrioritySuggestion.getTag();
        if (priority == null) return;
        
        selectedUrgency = priority;
        switch (priority) {
            case "High": priorityChipGroup.check(R.id.priorityHigh); break;
            case "Medium": priorityChipGroup.check(R.id.priorityMedium); break;
            case "Low": priorityChipGroup.check(R.id.priorityLow); break;
            default: priorityChipGroup.check(R.id.priorityNone);
        }
        Toast.makeText(getContext(), "Applied: " + priority + " priority", Toast.LENGTH_SHORT).show();
        chipPrioritySuggestion.setChecked(true);
    }
    
    private void applyDurationSuggestion() {
        if (chipDurationSuggestion == null) return;
        Integer duration = (Integer) chipDurationSuggestion.getTag();
        if (duration == null) return;
        
        // Calculate end time based on suggested duration
        int totalStartMinutes = convertTo24Hour(startHour, startAmPm) * 60 + startMinute;
        int totalEndMinutes = totalStartMinutes + duration;
        
        int endHour24 = (totalEndMinutes / 60) % 24;
        int endMin = totalEndMinutes % 60;
        
        // Convert back to 12-hour format
        if (endHour24 == 0) {
            endHour = 12;
            endAmPm = "AM";
        } else if (endHour24 < 12) {
            endHour = endHour24;
            endAmPm = "AM";
        } else if (endHour24 == 12) {
            endHour = 12;
            endAmPm = "PM";
        } else {
            endHour = endHour24 - 12;
            endAmPm = "PM";
        }
        endMinute = endMin;
        
        updateEndTimeDisplay();
        Toast.makeText(getContext(), "Applied: " + duration + " min duration", Toast.LENGTH_SHORT).show();
        chipDurationSuggestion.setChecked(true);
    }
    
    private int convertTo24Hour(int hour, String amPm) {
        if (amPm.equals("AM")) {
            return hour == 12 ? 0 : hour;
        } else {
            return hour == 12 ? 12 : hour + 12;
        }
    }
    
    private String getCategoryEmoji(String category) {
        switch (category) {
            case "Work": return "💼";
            case "Personal": return "🏠";
            case "Health": return "💪";
            case "Finance": return "💰";
            case "Learning": return "📚";
            default: return "📋";
        }
    }

    private void updateStartTimeDisplay() {
        String timeText = String.format("%d:%02d %s", startHour, startMinute, startAmPm);
        if (startTimeDisplay != null) {
            startTimeDisplay.setText(timeText);
        }
    }

    private void updateEndTimeDisplay() {
        String timeText = String.format("%d:%02d %s", endHour, endMinute, endAmPm);
        if (endTimeDisplay != null) {
            endTimeDisplay.setText(timeText);
        }
    }

    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> dismiss());

        saveButton.setOnClickListener(v -> saveTask());
        
        // Header save button - same action as bottom save button
        if (headerSaveButton != null) {
            headerSaveButton.setOnClickListener(v -> saveTask());
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        }

        if (startTimeRow != null) {
            startTimeRow.setOnClickListener(v -> showStartTimePicker());
        }

        if (endTimeRow != null) {
            endTimeRow.setOnClickListener(v -> showEndTimePicker());
        }

        if (alarmSoundText != null) {
            alarmSoundText.setOnClickListener(v -> showAlarmSoundPicker());
        }

        if (dateText != null) {
            if (isQuickTask) {
                // Hide date picker for quick tasks - date is locked to today
                dateText.setVisibility(View.GONE);
            } else {
                dateText.setOnClickListener(v -> showDatePicker());
            }
        }

        // Priority chip selection
        priorityChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedUrgency = "None";
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.priorityNone) {
                selectedUrgency = "None";
            } else if (checkedId == R.id.priorityLow) {
                selectedUrgency = "Low";
            } else if (checkedId == R.id.priorityMedium) {
                selectedUrgency = "Medium";
            } else if (checkedId == R.id.priorityHigh) {
                selectedUrgency = "High";
                // Auto-enable vibration for high priority
                vibrationSwitch.setChecked(true);
            }
        });
    }

    private void populateFieldsForEditing() {
        titleText.setText("✏️ Edit Focus Session");
        
        // Update subtitle for edit mode
        if (subtitleText != null) {
            subtitleText.setText("Modify your focus session details");
        }
        
        // Show save button with update text
        if (saveButton != null) {
            saveButton.setText("✓ Save Changes");
            saveButton.setVisibility(View.VISIBLE);
        }
        
        // Show delete button in edit mode
        if (deleteButton != null) {
            deleteButton.setVisibility(View.VISIBLE);
        }

        taskNameEditText.setText(editingTask.name);
        
        // Set start time
        startHour = editingTask.hour;
        startMinute = editingTask.minute;
        startAmPm = editingTask.amPm != null ? editingTask.amPm : "AM";
        updateStartTimeDisplay();

        // Set end time
        endHour = editingTask.endHour;
        endMinute = editingTask.endMinute;
        endAmPm = editingTask.endAmPm != null ? editingTask.endAmPm : "AM";
        updateEndTimeDisplay();

        // Set priority chip
        if (editingTask.urgency != null) {
            selectedUrgency = editingTask.urgency;
            switch (editingTask.urgency.toLowerCase()) {
                case "low":
                    priorityLow.setChecked(true);
                    break;
                case "medium":
                    priorityMedium.setChecked(true);
                    break;
                case "high":
                    priorityHigh.setChecked(true);
                    break;
                default:
                    priorityNone.setChecked(true);
                    break;
            }
        }

        // Set date
        if (editingTask.date != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate.setTime(sdf.parse(editingTask.date));
                updateDateLabel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        vibrationSwitch.setChecked(editingTask.vibrationEnabled);
        alarmSwitch.setChecked(editingTask.isAlarmOn);
    }
    
    private void populateFieldsFromTask() {
        // Similar to populateFieldsForEditing but without edit mode UI changes
        taskNameEditText.setText(editingTask.name);
        
        // Set start time
        startHour = editingTask.hour;
        startMinute = editingTask.minute;
        startAmPm = editingTask.amPm != null ? editingTask.amPm : "AM";
        updateStartTimeDisplay();

        // Set end time
        endHour = editingTask.endHour;
        endMinute = editingTask.endMinute;
        endAmPm = editingTask.endAmPm != null ? editingTask.endAmPm : "AM";
        updateEndTimeDisplay();

        // Set priority chip
        if (editingTask.urgency != null) {
            selectedUrgency = editingTask.urgency;
            switch (editingTask.urgency.toLowerCase()) {
                case "low":
                    priorityLow.setChecked(true);
                    break;
                case "medium":
                    priorityMedium.setChecked(true);
                    break;
                case "high":
                    priorityHigh.setChecked(true);
                    break;
                default:
                    priorityNone.setChecked(true);
                    break;
            }
        }

        // Set date
        if (editingTask.date != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate.setTime(sdf.parse(editingTask.date));
                updateDateLabel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        vibrationSwitch.setChecked(editingTask.vibrationEnabled);
        alarmSwitch.setChecked(editingTask.isAlarmOn);
    }

    private void saveTask() {
        String taskName = taskNameEditText.getText() != null ? taskNameEditText.getText().toString().trim() : "";

        if (taskName.isEmpty()) {
            Toast.makeText(requireContext(), "Please name your focus session 📝", Toast.LENGTH_SHORT).show();
            taskNameEditText.requestFocus();
            return;
        }

        // Validate start time is not in the past for today
        if (isTimeInPastForToday(startHour, startMinute, startAmPm)) {
            Toast.makeText(requireContext(), "⚠️ Start time cannot be in the past for today", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate time range
        if (!isValidTimeRange(startHour, startMinute, startAmPm, endHour, endMinute, endAmPm)) {
            Toast.makeText(requireContext(), "⏰ End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(selectedDate.getTime());

        // Determine time category based on start time
        String timeCategory;
        if (startAmPm.equals("AM")) {
            timeCategory = "morning";
        } else if (startHour == 12 || (startHour >= 1 && startHour < 6)) {
            timeCategory = "afternoon";
        } else {
            timeCategory = "night";
        }

        if (isEditMode && editingTask != null) {
            // Update existing task
            AlarmHelper.cancelFocusTaskAlarms(requireContext(), editingTask);

            editingTask.name = taskName;
            editingTask.hour = startHour;
            editingTask.minute = startMinute;
            editingTask.amPm = startAmPm;
            editingTask.endHour = endHour;
            editingTask.endMinute = endMinute;
            editingTask.endAmPm = endAmPm;
            editingTask.urgency = selectedUrgency;
            editingTask.date = dateStr;
            editingTask.timeCategory = timeCategory;
            editingTask.vibrationEnabled = vibrationSwitch.isChecked();
            editingTask.isAlarmOn = alarmSwitch.isChecked();

            taskRepository.updateTask(editingTask);

            if (editingTask.isAlarmOn) {
                AlarmHelper.scheduleFocusTaskAlarms(requireContext(), editingTask);
            }

            // Trigger UI refresh IMMEDIATELY before toast
            if (listener != null) {
                listener.onTaskSaved();
            }

            Toast.makeText(requireContext(), "🎯 Focus session updated! Let's crush it!", Toast.LENGTH_SHORT).show();
        } else {
            // Create new Focus Task
            Task focusTask = new Task(taskName, startHour, startMinute, startAmPm,
                                      endHour, endMinute, endAmPm, selectedUrgency);
            focusTask.date = dateStr;
            focusTask.timeCategory = timeCategory;
            focusTask.vibrationEnabled = vibrationSwitch.isChecked();
            focusTask.isAlarmOn = alarmSwitch.isChecked();
            
            // AI-powered category detection
            android.content.Context ctx = getContext();
            if (ctx != null && AIModelHelper.isEnabled(ctx)) {
                AIModelHelper aiHelper = AIModelHelper.getInstance(ctx);
                AIModelHelper.AIPrediction categoryPrediction = aiHelper.predictCategory(taskName);
                if (categoryPrediction != null && categoryPrediction.getValue() != null) {
                    focusTask.category = categoryPrediction.getValue();
                }
            }

            long taskId = taskRepository.addTask(focusTask);
            focusTask.id = (int) taskId;

            if (focusTask.isAlarmOn) {
                AlarmHelper.scheduleFocusTaskAlarms(requireContext(), focusTask);
            }

            // Trigger UI refresh IMMEDIATELY before toast
            if (listener != null) {
                listener.onTaskSaved();
            }

            Toast.makeText(requireContext(), "🎯 Focus session created! You've got this!", Toast.LENGTH_SHORT).show();
        }

        dismiss();
    }

    private boolean isValidTimeRange(int startHour, int startMinute, String startAmPm,
                                     int endHour, int endMinute, String endAmPm) {
        int start24 = convertTo24Hour(startHour, startAmPm) * 60 + startMinute;
        int end24 = convertTo24Hour(endHour, endAmPm) * 60 + endMinute;
        return end24 > start24;
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        if (dateText != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            dateText.setText(sdf.format(selectedDate.getTime()));
        }
    }

    private void showStartTimePicker() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_time_picker, null);
        
        NumberPicker hourPicker = dialogView.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = dialogView.findViewById(R.id.minutePicker);
        NumberPicker amPmPicker = dialogView.findViewById(R.id.amPmPicker);
        
        // Setup pickers
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setValue(startHour);
        
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(i -> String.format("%02d", i));
        minutePicker.setValue(startMinute);
        
        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        amPmPicker.setValue(startAmPm.equals("PM") ? 1 : 0);
        
        builder.setView(dialogView)
               .setTitle("⏰ When do you start?")
               .setPositiveButton("Set", (dialog, which) -> {
                   startHour = hourPicker.getValue();
                   startMinute = minutePicker.getValue();
                   startAmPm = amPmPicker.getDisplayedValues()[amPmPicker.getValue()];
                   updateStartTimeDisplay();
               })
               .setNegativeButton("Cancel", null)
               .show();
    }

    private void showEndTimePicker() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_time_picker, null);
        
        NumberPicker hourPicker = dialogView.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = dialogView.findViewById(R.id.minutePicker);
        NumberPicker amPmPicker = dialogView.findViewById(R.id.amPmPicker);
        
        // Setup pickers
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setValue(endHour);
        
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(i -> String.format("%02d", i));
        minutePicker.setValue(endMinute);
        
        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        amPmPicker.setValue(endAmPm.equals("PM") ? 1 : 0);
        
        builder.setView(dialogView)
               .setTitle("⏰ When do you finish?")
               .setPositiveButton("Set", (dialog, which) -> {
                   endHour = hourPicker.getValue();
                   endMinute = minutePicker.getValue();
                   endAmPm = amPmPicker.getDisplayedValues()[amPmPicker.getValue()];
                   updateEndTimeDisplay();
               })
               .setNegativeButton("Cancel", null)
               .show();
    }

    private void showAlarmSoundPicker() {
        android.media.RingtoneManager manager = new android.media.RingtoneManager(requireContext());
        manager.setType(android.media.RingtoneManager.TYPE_ALARM);
        android.database.Cursor cursor = manager.getCursor();

        java.util.Map<String, android.net.Uri> ringtones = new java.util.TreeMap<>();
        while (cursor.moveToNext()) {
            String title = cursor.getString(android.media.RingtoneManager.TITLE_COLUMN_INDEX);
            android.net.Uri uri = manager.getRingtoneUri(cursor.getPosition());
            ringtones.put(title, uri);
        }

        String[] ringtoneNames = ringtones.keySet().toArray(new String[0]);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Select Alarm Sound");
        
        builder.setItems(ringtoneNames, (dialog, which) -> {
            selectedAlarmSound = ringtoneNames[which];
            if (alarmSoundText != null) {
                alarmSoundText.setText(selectedAlarmSound);
            }
            
            // Play a preview of the selected sound
            try {
                android.net.Uri uri = ringtones.get(selectedAlarmSound);
                android.media.Ringtone ringtone = android.media.RingtoneManager.getRingtone(requireContext(), uri);
                ringtone.play();
                
                // Stop after 2 seconds
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (ringtone.isPlaying()) {
                        ringtone.stop();
                    }
                }, 2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirmation() {
        ModernDialogHelper.showDestructiveDialog(
                requireContext(),
                "Delete Focus Session?",
                "This action cannot be undone. {item} will be permanently removed.",
                editingTask != null ? editingTask.name : "This focus session",
                R.drawable.ic_delete,
                this::deleteTask,
                null
        );
    }

    private void deleteTask() {
        if (editingTask != null) {
            // Cancel alarms first
            AlarmHelper.cancelFocusTaskAlarms(requireContext(), editingTask);
            
            // Delete from database
            taskRepository.deleteTask(editingTask);
            
            Toast.makeText(requireContext(), "Focus session deleted", Toast.LENGTH_SHORT).show();
            
            if (listener != null) {
                listener.onTaskSaved();
            }
            dismiss();
        }
    }
    
    /**
     * Check if the selected time is in the past for today's date.
     * @return true if selected date is today AND selected time is before current time
     */
    private boolean isTimeInPastForToday(int hour, int minute, String amPm) {
        // Check if selected date is today
        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar selected = (java.util.Calendar) selectedDate.clone();
        
        boolean isToday = today.get(java.util.Calendar.YEAR) == selected.get(java.util.Calendar.YEAR) &&
                          today.get(java.util.Calendar.DAY_OF_YEAR) == selected.get(java.util.Calendar.DAY_OF_YEAR);
        
        if (!isToday) {
            return false; // Not today, so time can be anything
        }
        
        // Convert selected time to 24-hour format for comparison
        int selectedHour24 = hour;
        if (amPm.equals("PM") && hour != 12) {
            selectedHour24 = hour + 12;
        } else if (amPm.equals("AM") && hour == 12) {
            selectedHour24 = 0;
        }
        
        int currentHour = today.get(java.util.Calendar.HOUR_OF_DAY);
        int currentMinute = today.get(java.util.Calendar.MINUTE);
        
        // Check if selected time is in the past
        if (selectedHour24 < currentHour) {
            return true;
        } else if (selectedHour24 == currentHour && minute < currentMinute) {
            return true;
        }
        
        return false;
    }
}
