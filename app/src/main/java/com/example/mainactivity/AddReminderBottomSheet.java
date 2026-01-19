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
    private MaterialButton saveButton;
    private MaterialButton closeButton;
    private MaterialButton deleteButton;
    private TextView titleText;
    private TextView dateText;

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
        return inflater.inflate(R.layout.bottom_sheet_reminder, container, false);
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
        } else if (getArguments() != null && getArguments().getBoolean("PREFILLED", false)) {
            populateFieldsFromTask();
        }
    }

    private void initViews(View view) {
        titleText = view.findViewById(R.id.titleText);
        closeButton = view.findViewById(R.id.closeButton);
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
        saveButton = view.findViewById(R.id.saveButton);
        deleteButton = view.findViewById(R.id.deleteButton);
        dateText = view.findViewById(R.id.dateText);

        // Update date display
        updateDateLabel();
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

        saveButton.setOnClickListener(v -> saveTask());

        deleteButton.setOnClickListener(v -> showDeleteConfirmation());

        repeatDaysText.setOnClickListener(v -> showRepeatDialog());

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
        titleText.setText("Edit Reminder");
        saveButton.setText("Update");

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
}
