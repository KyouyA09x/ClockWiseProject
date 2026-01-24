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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AddReminderBottomSheet extends BottomSheetDialogFragment {

    public interface OnTaskSavedListener {
        void onTaskSaved();
    }

    private OnTaskSavedListener listener;
    private TaskRepository taskRepository;
    private Calendar selectedDate = Calendar.getInstance();
    private boolean[] selectedDays = new boolean[7];
    private String selectedUrgency = "None";

    // Edit mode
    private boolean isEditMode = false;
    private boolean isQuickTask = false;
    private Task editingTask = null;

    // Views
    private TextInputEditText taskNameEditText;
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker amPmPicker;
    private ChipGroup priorityChipGroup;
    private Chip priorityNone, priorityLow, priorityMedium, priorityHigh;
    private TextView repeatDaysText;
    private MaterialSwitch vibrationSwitch;
    private MaterialSwitch alarmSwitch;
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
    private TextView aiReasonText;
    private Handler aiDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable aiSuggestionRunnable;
    private AIModelHelper aiHelper;

    public static AddReminderBottomSheet newInstance() {
        return new AddReminderBottomSheet();
    }

    public static AddReminderBottomSheet newInstance(Task task) {
        AddReminderBottomSheet fragment = new AddReminderBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable("TASK", task);
        args.putBoolean("EDIT_MODE", true);
        fragment.setArguments(args);
        return fragment;
    }
    
    public static AddReminderBottomSheet newInstance(Task task, boolean hideDate) {
        AddReminderBottomSheet fragment = new AddReminderBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable("TASK", task);
        args.putBoolean("EDIT_MODE", false);
        args.putBoolean("PREFILLED", true);
        args.putBoolean("HIDE_DATE", hideDate); // For quick tasks
        fragment.setArguments(args);
        return fragment;
    }

    public static AddReminderBottomSheet newInstanceWithData(Task task) {
        AddReminderBottomSheet fragment = new AddReminderBottomSheet();
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
        return inflater.inflate(R.layout.bottom_sheet_reminder_modern, container, false);
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
        setupTimePickers();
        setupClickListeners();

        if (isEditMode && editingTask != null) {
            populateFieldsForEditing();
        } else if (getArguments() != null && getArguments().getBoolean("PREFILLED", false)) {
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
        hourPicker = view.findViewById(R.id.hourPicker);
        minutePicker = view.findViewById(R.id.minutePicker);
        amPmPicker = view.findViewById(R.id.amPmPicker);
        priorityChipGroup = view.findViewById(R.id.priorityChipGroup);
        priorityNone = view.findViewById(R.id.priorityNone);
        priorityLow = view.findViewById(R.id.priorityLow);
        priorityMedium = view.findViewById(R.id.priorityMedium);
        priorityHigh = view.findViewById(R.id.priorityHigh);
        repeatDaysText = view.findViewById(R.id.repeatDaysText);
        vibrationSwitch = view.findViewById(R.id.vibrationSwitch);
        alarmSwitch = view.findViewById(R.id.alarmSwitch);
        deleteButton = view.findViewById(R.id.deleteButton);
        dateText = view.findViewById(R.id.dateText);
        alarmSoundText = view.findViewById(R.id.alarmSoundText);
        
        // Initialize AI Suggestion Views
        aiSuggestionCard = view.findViewById(R.id.aiSuggestionCard);
        aiConfidenceText = view.findViewById(R.id.aiConfidenceText);
        chipPrioritySuggestion = view.findViewById(R.id.chipPrioritySuggestion);
        chipCategorySuggestion = view.findViewById(R.id.chipCategorySuggestion);
        chipTimeSuggestion = view.findViewById(R.id.chipTimeSuggestion);
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
                    if (aiConfidenceText != null) aiConfidenceText.setVisibility(View.GONE);
                }
            } else {
                // Hide AI card if disabled
                if (aiSuggestionCard != null) {
                    aiSuggestionCard.setVisibility(View.GONE);
                }
            }
        }

        // Update date display
        updateDateLabel();
    }
    
    /**
     * Configure UI for Quick Task mode - minimal options for fast task creation
     */
    private void setupQuickTaskMode(View view) {
        // Update title
        if (titleText != null) {
            titleText.setText("⚡ Quick Task");
        }
        
        // Show "Today only" indicator
        View quickTaskIndicator = view.findViewById(R.id.quickTaskIndicator);
        if (quickTaskIndicator != null) {
            quickTaskIndicator.setVisibility(View.VISIBLE);
        }
        
        // Hide date picker (already done elsewhere, but ensure)
        if (dateText != null) {
            dateText.setVisibility(View.GONE);
        }
        
        // Find and hide the Date & Time card's date section if possible
        View dateCard = view.findViewById(R.id.dateText);
        if (dateCard != null && dateCard.getParent() != null) {
            // Hide the entire date card wrapper
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
        
        // Hide repeat card
        View repeatCard = view.findViewById(R.id.repeatCard);
        if (repeatCard != null) {
            repeatCard.setVisibility(View.GONE);
        }
        
        // Hide alarm sound card
        View alarmSoundCard = view.findViewById(R.id.alarmSoundCard);
        if (alarmSoundCard != null) {
            alarmSoundCard.setVisibility(View.GONE);
        }
        
        // Set sensible defaults for quick mode
        if (alarmSwitch != null) {
            alarmSwitch.setChecked(true); // Enable alarm by default
        }
    }
    
    /**
     * Setup AI suggestion functionality with debounced text watching
     */
    private void setupAISuggestions() {
        if (taskNameEditText == null || aiSuggestionCard == null) return;
        
        // Add text watcher with debounce for AI suggestions
        taskNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                // Cancel previous runnable
                if (aiSuggestionRunnable != null) {
                    aiDebounceHandler.removeCallbacks(aiSuggestionRunnable);
                }
                
                // Debounce - wait 300ms after user stops typing (reduced from 500ms)
                aiSuggestionRunnable = () -> updateAISuggestions(s.toString());
                aiDebounceHandler.postDelayed(aiSuggestionRunnable, 300);
            }
        });
        
        // Setup suggestion chip click listeners
        if (chipPrioritySuggestion != null) {
            chipPrioritySuggestion.setOnClickListener(v -> applyPrioritySuggestion());
        }
        if (chipTimeSuggestion != null) {
            chipTimeSuggestion.setOnClickListener(v -> applyTimeSuggestion());
        }
    }
    
    /**
     * Update AI suggestions based on task name
     */
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
                if (aiConfidenceText != null) aiConfidenceText.setVisibility(View.GONE);
            }
            return;
        }
        
        // Get AI suggestions
        AIModelHelper.TaskSuggestions suggestions = aiHelper.getTaskSuggestions(taskName);
        
        // Show suggestion card with chips
        if (aiSuggestionCard != null) {
            aiSuggestionCard.setVisibility(View.VISIBLE);
        }
        if (aiConfidenceText != null) aiConfidenceText.setVisibility(View.VISIBLE);
        
        // Update priority suggestion (if enabled)
        if (chipPrioritySuggestion != null && suggestions.priority != null) {
            if (AIModelHelper.isPriorityEnabled(requireContext())) {
                chipPrioritySuggestion.setVisibility(View.VISIBLE);
                String priorityText = suggestions.priority.getEmoji() + " Priority: " + suggestions.priority.getValue();
                chipPrioritySuggestion.setText(priorityText);
                chipPrioritySuggestion.setTag(suggestions.priority.getValue());
                
                // Set chip icon tint based on priority
                int tintColor;
                switch (suggestions.priority.getValue()) {
                    case "High": tintColor = getResources().getColor(R.color.error, null); break;
                    case "Medium": tintColor = getResources().getColor(R.color.warning, null); break;
                    case "Low": tintColor = getResources().getColor(R.color.success, null); break;
                    default: tintColor = getResources().getColor(R.color.text_secondary, null);
                }
                chipPrioritySuggestion.setChipIconTint(android.content.res.ColorStateList.valueOf(tintColor));
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
                chipCategorySuggestion.setTag(suggestions.category.getValue());
            } else {
                chipCategorySuggestion.setVisibility(View.GONE);
            }
        }
        
        // Update time suggestion (if enabled)
        if (chipTimeSuggestion != null && suggestions.timeOfDay != null) {
            if (AIModelHelper.isTimeEnabled(requireContext())) {
                chipTimeSuggestion.setVisibility(View.VISIBLE);
                String timeEmoji = getTimeEmoji(suggestions.timeOfDay.getValue());
                chipTimeSuggestion.setText(timeEmoji + " Best: " + capitalize(suggestions.timeOfDay.getValue()));
                chipTimeSuggestion.setTag(suggestions.timeOfDay.getValue());
            } else {
                chipTimeSuggestion.setVisibility(View.GONE);
            }
        }
        
        // Update confidence text
        if (aiConfidenceText != null && suggestions.priority != null) {
            aiConfidenceText.setText(suggestions.priority.getEmoji() + " " + 
                suggestions.priority.getConfidenceLevel() + " confidence");
        }
        
        // Update reason text
        if (aiReasonText != null && suggestions.priority != null) {
            aiReasonText.setText("💡 " + suggestions.priority.getReason());
        }
    }
    
    /**
     * Apply priority suggestion when chip is clicked
     */
    private void applyPrioritySuggestion() {
        if (chipPrioritySuggestion == null) return;
        
        String priority = (String) chipPrioritySuggestion.getTag();
        if (priority == null) return;
        
        // Update priority selection
        selectedUrgency = priority;
        switch (priority) {
            case "High":
                priorityChipGroup.check(R.id.priorityHigh);
                break;
            case "Medium":
                priorityChipGroup.check(R.id.priorityMedium);
                break;
            case "Low":
                priorityChipGroup.check(R.id.priorityLow);
                break;
            default:
                priorityChipGroup.check(R.id.priorityNone);
        }
        
        Toast.makeText(getContext(), "Applied: " + priority + " priority", Toast.LENGTH_SHORT).show();
        chipPrioritySuggestion.setChecked(true);
    }
    
    /**
     * Apply time suggestion when chip is clicked
     */
    private void applyTimeSuggestion() {
        if (chipTimeSuggestion == null) return;
        
        String timeOfDay = (String) chipTimeSuggestion.getTag();
        if (timeOfDay == null) return;
        
        // Set time based on suggestion
        int hour, amPm;
        switch (timeOfDay) {
            case "morning":
                hour = 9;
                amPm = 0; // AM
                break;
            case "afternoon":
                hour = 2;
                amPm = 1; // PM
                break;
            case "evening":
                hour = 6;
                amPm = 1; // PM
                break;
            default:
                hour = 8;
                amPm = 1; // PM (night)
        }
        
        hourPicker.setValue(hour);
        minutePicker.setValue(0);
        amPmPicker.setValue(amPm);
        
        Toast.makeText(getContext(), "Applied: " + capitalize(timeOfDay) + " time", Toast.LENGTH_SHORT).show();
        chipTimeSuggestion.setChecked(true);
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
    
    private String getTimeEmoji(String time) {
        switch (time) {
            case "morning": return "🌅";
            case "afternoon": return "☀️";
            case "evening": return "🌆";
            default: return "🌙";
        }
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void setupTimePickers() {
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setValue(12);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(i -> String.format("%02d", i));

        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
    }

    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> dismiss());

        // Header save button - checkmark icon in top right
        if (headerSaveButton != null) {
            headerSaveButton.setOnClickListener(v -> saveTask());
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        }

        if (repeatDaysText != null) {
            repeatDaysText.setOnClickListener(v -> showRepeatDialog());
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
        titleText.setText("✏️ Edit Task");
        
        // Update subtitle for edit mode
        if (subtitleText != null) {
            subtitleText.setText("Modify your task details");
        }
        // Show delete button in edit mode
        if (deleteButton != null) {
            deleteButton.setVisibility(View.VISIBLE);
        }

        taskNameEditText.setText(editingTask.name);
        hourPicker.setValue(editingTask.hour);
        minutePicker.setValue(editingTask.minute);
        amPmPicker.setValue(editingTask.amPm != null && editingTask.amPm.equals("PM") ? 1 : 0);

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

        // Set repeat days
        if (editingTask.selectedDays != null) {
            selectedDays = editingTask.selectedDays.clone();
            updateRepeatText();
        }

        vibrationSwitch.setChecked(editingTask.vibrationEnabled);
        alarmSwitch.setChecked(editingTask.isAlarmOn);
        
        // Set alarm sound
        if (editingTask.alarmSound != null) {
            selectedAlarmSound = editingTask.alarmSound;
            if (alarmSoundText != null) {
                alarmSoundText.setText(selectedAlarmSound);
            }
        }
    }
    
    private void populateFieldsFromTask() {
        // Similar to populateFieldsForEditing but without edit mode UI changes
        taskNameEditText.setText(editingTask.name);
        
        // Set time pickers
        hourPicker.setValue(editingTask.hour);
        minutePicker.setValue(editingTask.minute);
        amPmPicker.setValue(editingTask.amPm != null && editingTask.amPm.equals("PM") ? 1 : 0);

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

        // Set repeat days
        if (editingTask.selectedDays != null) {
            selectedDays = editingTask.selectedDays.clone();
            updateRepeatText();
        }

        vibrationSwitch.setChecked(editingTask.vibrationEnabled);
        alarmSwitch.setChecked(editingTask.isAlarmOn);
        
        // Set alarm sound
        if (editingTask.alarmSound != null) {
            selectedAlarmSound = editingTask.alarmSound;
            if (alarmSoundText != null) {
                alarmSoundText.setText(selectedAlarmSound);
            }
        }
    }

    private void saveTask() {
        String taskName = taskNameEditText.getText() != null ? taskNameEditText.getText().toString().trim() : "";

        if (taskName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a task name", Toast.LENGTH_SHORT).show();
            taskNameEditText.requestFocus();
            return;
        }

        int hour = hourPicker.getValue();
        int minute = minutePicker.getValue();
        String amPm = amPmPicker.getDisplayedValues()[amPmPicker.getValue()];

        // Validate time is not in the past for today's tasks
        if (isTimeInPastForToday(hour, minute, amPm)) {
            Toast.makeText(requireContext(), "⚠️ Cannot set time in the past for today", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine time category
        String timeCategory;
        if (amPm.equals("AM")) {
            timeCategory = "morning";
        } else if (hour == 12 || (hour >= 1 && hour < 6)) {
            timeCategory = "afternoon";
        } else {
            timeCategory = "night";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(selectedDate.getTime());

        if (isEditMode && editingTask != null) {
            // Update existing task
            AlarmHelper.cancelTaskAlarm(requireContext(), editingTask);

            editingTask.name = taskName;
            editingTask.hour = hour;
            editingTask.minute = minute;
            editingTask.amPm = amPm;
            editingTask.urgency = selectedUrgency;
            editingTask.selectedDays = selectedDays;
            editingTask.date = dateStr;
            editingTask.timeCategory = timeCategory;
            editingTask.vibrationEnabled = vibrationSwitch.isChecked();
            editingTask.isAlarmOn = alarmSwitch.isChecked();
            editingTask.alarmSound = selectedAlarmSound;

            taskRepository.updateTask(editingTask);

            if (editingTask.isAlarmOn) {
                AlarmHelper.scheduleTaskAlarm(requireContext(), editingTask);
            }

            // Trigger UI refresh IMMEDIATELY before toast
            if (listener != null) {
                listener.onTaskSaved();
            }

            Toast.makeText(requireContext(), "Reminder updated!", Toast.LENGTH_SHORT).show();
        } else {
            // Create new task
            Task newTask = new Task(taskName, hour, minute, amPm, selectedUrgency, selectedDays);
            newTask.date = dateStr;
            newTask.timeCategory = timeCategory;
            newTask.vibrationEnabled = vibrationSwitch.isChecked();
            newTask.isAlarmOn = alarmSwitch.isChecked();
            newTask.alarmSound = selectedAlarmSound;
            
            // AI-powered category detection
            android.content.Context ctx = getContext();
            if (ctx != null && AIModelHelper.isEnabled(ctx)) {
                AIModelHelper aiHelper = AIModelHelper.getInstance(ctx);
                AIModelHelper.AIPrediction categoryPrediction = aiHelper.predictCategory(taskName);
                if (categoryPrediction != null && categoryPrediction.getValue() != null) {
                    newTask.category = categoryPrediction.getValue();
                }
            }

            long taskId = taskRepository.addTask(newTask);
            newTask.id = (int) taskId;

            if (newTask.isAlarmOn) {
                AlarmHelper.scheduleTaskAlarm(requireContext(), newTask);
            }

            // Trigger UI refresh IMMEDIATELY before toast/dismiss
            if (listener != null) {
                listener.onTaskSaved();
            }

            Toast.makeText(requireContext(), "Reminder saved!", Toast.LENGTH_SHORT).show();
        }

        dismiss();
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

    private void showRepeatDialog() {
        String[] daysOfWeek = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Repeat")
                .setMultiChoiceItems(daysOfWeek, selectedDays, (dialog, which, isChecked) -> {
                    selectedDays[which] = isChecked;
                })
                .setPositiveButton("OK", (dialog, which) -> updateRepeatText())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateRepeatText() {
        String[] daysShort = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        ArrayList<String> selectedDayNames = new ArrayList<>();
        int selectedCount = 0;

        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                selectedDayNames.add(daysShort[i]);
                selectedCount++;
            }
        }

        if (selectedCount == 7) {
            repeatDaysText.setText("Every day");
        } else if (selectedCount == 0) {
            repeatDaysText.setText("Never");
        } else {
            repeatDaysText.setText(String.join(", ", selectedDayNames));
        }
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
                "Delete Reminder?",
                "This action cannot be undone. {item} will be permanently removed.",
                editingTask != null ? editingTask.name : "This reminder",
                R.drawable.ic_delete,
                this::deleteTask,
                null
        );
    }

    private void deleteTask() {
        if (editingTask != null) {
            // Cancel alarm first
            AlarmHelper.cancelTaskAlarm(requireContext(), editingTask);
            
            // Delete from database
            taskRepository.deleteTask(editingTask);
            
            Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_SHORT).show();
            
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
