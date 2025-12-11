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
    private Task editingTask = null;

    // Views
    private TextInputEditText taskNameEditText;
    private NumberPicker startHourPicker;
    private NumberPicker startMinutePicker;
    private NumberPicker startAmPmPicker;
    private NumberPicker endHourPicker;
    private NumberPicker endMinutePicker;
    private NumberPicker endAmPmPicker;
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
            editingTask = getArguments().getParcelable("TASK");
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
        setupTimePickers();
        setupClickListeners();

        if (isEditMode && editingTask != null) {
            populateFieldsForEditing();
        }
    }

    private void initViews(View view) {
        titleText = view.findViewById(R.id.titleText);
        closeButton = view.findViewById(R.id.closeButton);
        taskNameEditText = view.findViewById(R.id.taskNameEditText);
        
        startHourPicker = view.findViewById(R.id.startHourPicker);
        startMinutePicker = view.findViewById(R.id.startMinutePicker);
        startAmPmPicker = view.findViewById(R.id.startAmPmPicker);
        
        endHourPicker = view.findViewById(R.id.endHourPicker);
        endMinutePicker = view.findViewById(R.id.endMinutePicker);
        endAmPmPicker = view.findViewById(R.id.endAmPmPicker);
        
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

        // Update date display
        updateDateLabel();
    }

    private void setupTimePickers() {
        // Start time picker setup
        startHourPicker.setMinValue(1);
        startHourPicker.setMaxValue(12);
        startHourPicker.setValue(9); // Default to 9 AM

        startMinutePicker.setMinValue(0);
        startMinutePicker.setMaxValue(59);
        startMinutePicker.setFormatter(i -> String.format("%02d", i));
        startMinutePicker.setValue(0);

        startAmPmPicker.setMinValue(0);
        startAmPmPicker.setMaxValue(1);
        startAmPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        startAmPmPicker.setValue(0); // AM

        // End time picker setup
        endHourPicker.setMinValue(1);
        endHourPicker.setMaxValue(12);
        endHourPicker.setValue(10); // Default to 10 AM (1 hour later)

        endMinutePicker.setMinValue(0);
        endMinutePicker.setMaxValue(59);
        endMinutePicker.setFormatter(i -> String.format("%02d", i));
        endMinutePicker.setValue(0);

        endAmPmPicker.setMinValue(0);
        endAmPmPicker.setMaxValue(1);
        endAmPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        endAmPmPicker.setValue(0); // AM
    }

    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> dismiss());

        saveButton.setOnClickListener(v -> saveTask());

        deleteButton.setOnClickListener(v -> showDeleteConfirmation());

        if (dateText != null) {
            dateText.setOnClickListener(v -> showDatePicker());
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
        titleText.setText("Edit Focus Task");
        saveButton.setText("Update");
        deleteButton.setVisibility(View.VISIBLE);

        taskNameEditText.setText(editingTask.name);
        
        // Set start time
        startHourPicker.setValue(editingTask.hour);
        startMinutePicker.setValue(editingTask.minute);
        startAmPmPicker.setValue(editingTask.amPm != null && editingTask.amPm.equals("PM") ? 1 : 0);

        // Set end time
        endHourPicker.setValue(editingTask.endHour);
        endMinutePicker.setValue(editingTask.endMinute);
        endAmPmPicker.setValue(editingTask.endAmPm != null && editingTask.endAmPm.equals("PM") ? 1 : 0);

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
            Toast.makeText(requireContext(), "Please enter a task name", Toast.LENGTH_SHORT).show();
            taskNameEditText.requestFocus();
            return;
        }

        // Get start time
        int startHour = startHourPicker.getValue();
        int startMinute = startMinutePicker.getValue();
        String startAmPm = startAmPmPicker.getDisplayedValues()[startAmPmPicker.getValue()];

        // Get end time
        int endHour = endHourPicker.getValue();
        int endMinute = endMinutePicker.getValue();
        String endAmPm = endAmPmPicker.getDisplayedValues()[endAmPmPicker.getValue()];

        // Validate time range
        if (!isValidTimeRange(startHour, startMinute, startAmPm, endHour, endMinute, endAmPm)) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
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

            Toast.makeText(requireContext(), "Focus Task updated!", Toast.LENGTH_SHORT).show();
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

            Toast.makeText(requireContext(), "Focus Task saved!", Toast.LENGTH_SHORT).show();
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

    private void showDeleteConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Focus Task")
                .setMessage("Are you sure you want to delete this focus task?")
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
            
            Toast.makeText(requireContext(), "Focus Task deleted", Toast.LENGTH_SHORT).show();
            
            if (listener != null) {
                listener.onTaskSaved();
            }
            dismiss();
        }
    }
}
