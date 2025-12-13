package com.example.mainactivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
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
    private MaterialButton closeButton;
    private MaterialButton deleteButton;
    private TextView titleText;
    private TextView dateText;

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
            isQuickTask = getArguments().getBoolean("QUICK_TASK", false);
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
        return inflater.inflate(R.layout.bottom_sheet_focus_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Make bottom sheet expanded by default
        if (getDialog() != null) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setSkipCollapsed(true);
        }

        initViews(view);
        setupClickListeners();

        if (isEditMode && editingTask != null) {
            populateFieldsForEditing();
        }
    }

    private void initViews(View view) {
        titleText = view.findViewById(R.id.titleText);
        closeButton = view.findViewById(R.id.closeButton);
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

        // Initialize time displays
        updateStartTimeDisplay();
        updateEndTimeDisplay();
        updateDateLabel();
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

        deleteButton.setOnClickListener(v -> showDeleteConfirmation());

        startTimeRow.setOnClickListener(v -> showStartTimePicker());

        endTimeRow.setOnClickListener(v -> showEndTimePicker());

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
        titleText.setText("Edit Focus Session");
        saveButton.setText("Update");
        deleteButton.setVisibility(View.VISIBLE);

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

            Toast.makeText(requireContext(), "🎯 Focus session updated! Let's crush it!", Toast.LENGTH_SHORT).show();
        } else {
            // Create new Focus Task
            Task focusTask = new Task(taskName, startHour, startMinute, startAmPm,
                                      endHour, endMinute, endAmPm, selectedUrgency);
            focusTask.date = dateStr;
            focusTask.timeCategory = timeCategory;
            focusTask.vibrationEnabled = vibrationSwitch.isChecked();
            focusTask.isAlarmOn = alarmSwitch.isChecked();

            long taskId = taskRepository.addTask(focusTask);
            focusTask.id = (int) taskId;

            if (focusTask.isAlarmOn) {
                AlarmHelper.scheduleFocusTaskAlarms(requireContext(), focusTask);
            }

            Toast.makeText(requireContext(), "🎯 Focus session created! You've got this!", Toast.LENGTH_SHORT).show();
        }

        if (listener != null) {
            listener.onTaskSaved();
        }
        dismiss();
    }

    private boolean isValidTimeRange(int startHour, int startMinute, String startAmPm,
                                     int endHour, int endMinute, String endAmPm) {
        int start24 = convertTo24Hour(startHour, startAmPm) * 60 + startMinute;
        int end24 = convertTo24Hour(endHour, endAmPm) * 60 + endMinute;
        return end24 > start24;
    }

    private int convertTo24Hour(int hour, String amPm) {
        if (amPm.equals("AM")) {
            return hour == 12 ? 0 : hour;
        } else {
            return hour == 12 ? 12 : hour + 12;
        }
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

    private void showDeleteConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Focus Session")
                .setMessage("Are you sure you want to delete this focus session?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask())
                .setNegativeButton("Cancel", null)
                .show();
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
}
