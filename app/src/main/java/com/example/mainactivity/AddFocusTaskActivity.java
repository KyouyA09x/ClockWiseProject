package com.example.mainactivity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddFocusTaskActivity extends AppCompatActivity {

    private Button cancelButton;
    private Button saveButton;

    // Time display views
    private LinearLayout startTimeRow;
    private LinearLayout endTimeRow;
    private TextView startTimeDisplay;
    private TextView endTimeDisplay;

    // Time values
    private int startHour = 9;
    private int startMinute = 0;
    private String startAmPm = "AM";
    private int endHour = 10;
    private int endMinute = 0;
    private String endAmPm = "AM";

    private EditText labelEditText;
    private TextView repeatDaysText;
    private SwitchCompat alarmSwitch;
    private boolean[] selectedDays = new boolean[7];

    private Calendar selectedDate = Calendar.getInstance();
    private String selectedUrgency = "None";
    private TaskRepository taskRepository;

    // Edit mode variables
    private boolean isEditMode = false;
    private Task editingTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        setTheme(ThemeHelper.getThemeResource(this));
        setContentView(R.layout.activity_add_focus_task);

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        initViews();
        setupClickListeners();

        // Check if we're in edit mode
        checkEditMode();
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra("EDIT_MODE", false)) {
            isEditMode = true;
            editingTask = intent.getParcelableExtra("TASK");
            if (editingTask != null) {
                populateFieldsForEditing();
            }
        }
    }

    private void populateFieldsForEditing() {
        // Set task name
        labelEditText.setText(editingTask.name);

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

        // Set urgency
        if (editingTask.urgency != null && !editingTask.urgency.equals("None")) {
            selectedUrgency = editingTask.urgency;
        }

        // Set date
        if (editingTask.date != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate.setTime(sdf.parse(editingTask.date));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Set repeat days
        if (editingTask.selectedDays != null) {
            selectedDays = editingTask.selectedDays.clone();
            updateRepeatText();
        }

        // Set alarm
        alarmSwitch.setChecked(editingTask.isAlarmOn);

        // Change save button text to indicate update
        saveButton.setText("Update");
    }

    private void initViews() {
        cancelButton = findViewById(R.id.cancelButton);
        saveButton = findViewById(R.id.saveButton);

        startTimeRow = findViewById(R.id.startTimeRow);
        endTimeRow = findViewById(R.id.endTimeRow);
        startTimeDisplay = findViewById(R.id.startTimeDisplay);
        endTimeDisplay = findViewById(R.id.endTimeDisplay);

        labelEditText = findViewById(R.id.taskNameEditText);
        repeatDaysText = findViewById(R.id.repeatDaysText);
        alarmSwitch = findViewById(R.id.alarmSwitch);

        // Initialize time displays
        updateStartTimeDisplay();
        updateEndTimeDisplay();
        updateRepeatText();
    }



    private void setupClickListeners() {
        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> saveTask());

        startTimeRow.setOnClickListener(v -> showStartTimePicker());

        endTimeRow.setOnClickListener(v -> showEndTimePicker());

        repeatDaysText.setOnClickListener(v -> showRepeatDialog());

        alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, isChecked ? "🔔 Alerts enabled - we'll keep you on track!" : "🔕 Alerts disabled", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveTask() {
        String taskName = labelEditText.getText().toString().trim();

        if (taskName.isEmpty()) {
            Toast.makeText(this, "Please name your focus session 📝", Toast.LENGTH_SHORT).show();
            labelEditText.requestFocus();
            return;
        }

        // Validate time range
        if (!isValidTimeRange(startHour, startMinute, startAmPm, endHour, endMinute, endAmPm)) {
            Toast.makeText(this, "⏰ End time must be after start time", Toast.LENGTH_SHORT).show();
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
            // Cancel old alarms first
            AlarmHelper.cancelFocusTaskAlarms(this, editingTask);

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
            editingTask.selectedDays = selectedDays;
            editingTask.isAlarmOn = alarmSwitch.isChecked();

            taskRepository.updateTask(editingTask);

            // Schedule new alarms if enabled
            if (editingTask.isAlarmOn) {
                AlarmHelper.scheduleFocusTaskAlarms(this, editingTask);
            }

            Toast.makeText(this, "🎯 Focus session updated! Let's crush it!", Toast.LENGTH_SHORT).show();
        } else {
            // Create new Focus Task
            Task focusTask = new Task(taskName, startHour, startMinute, startAmPm,
                                      endHour, endMinute, endAmPm, selectedUrgency);
            focusTask.date = dateStr;
            focusTask.timeCategory = timeCategory;
            focusTask.selectedDays = selectedDays;
            focusTask.isAlarmOn = alarmSwitch.isChecked();

            // Save to database
            long taskId = taskRepository.addTask(focusTask);
            focusTask.id = (int) taskId;

            // Schedule both alarms (start and end)
            if (focusTask.isAlarmOn) {
                AlarmHelper.scheduleFocusTaskAlarms(this, focusTask);
            }

            Toast.makeText(this, "🎯 Focus session created! You've got this!", Toast.LENGTH_SHORT).show();
        }

        setResult(RESULT_OK);
        finish();
    }

    private boolean isValidTimeRange(int startHour, int startMinute, String startAmPm,
                                     int endHour, int endMinute, String endAmPm) {
        // Convert to 24-hour format for comparison
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

    private void showStartTimePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
        
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
        
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
    
    private void updateStartTimeDisplay() {
        String timeText = String.format("%d:%02d %s", startHour, startMinute, startAmPm);
        startTimeDisplay.setText(timeText);
    }
    
    private void updateEndTimeDisplay() {
        String timeText = String.format("%d:%02d %s", endHour, endMinute, endAmPm);
        endTimeDisplay.setText(timeText);
    }
    
    private void showRepeatDialog() {
        String[] daysOfWeek = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        
        new AlertDialog.Builder(this)
                .setTitle("🔁 Choose your focus days")
                .setMultiChoiceItems(daysOfWeek, selectedDays, (dialog, which, isChecked) -> {
                    selectedDays[which] = isChecked;
                })
                .setPositiveButton("Done", (dialog, which) -> updateRepeatText())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void updateRepeatText() {
        String[] daysShort = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        java.util.ArrayList<String> selectedDayNames = new java.util.ArrayList<>();
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
}

