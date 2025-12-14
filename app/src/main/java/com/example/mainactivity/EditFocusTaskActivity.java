package com.example.mainactivity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditFocusTaskActivity extends AppCompatActivity {

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
    private Task existingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_focus_task);

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        // Get task ID from intent
        int taskId = getIntent().getIntExtra("task_id", -1);
        if (taskId == -1) {
            Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load existing task
        existingTask = taskRepository.getTaskById(taskId);
        if (existingTask == null) {
            Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        populateWithExistingTask();
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

    private void updateStartTimeDisplay() {
        String timeText = String.format(Locale.getDefault(), "%d:%02d %s", startHour, startMinute, startAmPm);
        startTimeDisplay.setText(timeText);
    }
    
    private void updateEndTimeDisplay() {
        String timeText = String.format(Locale.getDefault(), "%d:%02d %s", endHour, endMinute, endAmPm);
        endTimeDisplay.setText(timeText);
    }

    private void populateWithExistingTask() {
        // Set task name
        labelEditText.setText(existingTask.name);

        // Set start time
        startHour = existingTask.hour;
        startMinute = existingTask.minute;
        startAmPm = existingTask.amPm != null ? existingTask.amPm : "AM";
        updateStartTimeDisplay();

        // Set end time
        endHour = existingTask.endHour;
        endMinute = existingTask.endMinute;
        endAmPm = existingTask.endAmPm != null ? existingTask.endAmPm : "AM";
        updateEndTimeDisplay();

        // Set urgency
        selectedUrgency = existingTask.urgency != null ? existingTask.urgency : "None";

        // Set repeat days
        if (existingTask.selectedDays != null) {
            selectedDays = existingTask.selectedDays.clone();
            updateRepeatText();
        }

        // Set alarm
        alarmSwitch.setChecked(existingTask.isAlarmOn);

        // Set date
        if (existingTask.date != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf.parse(existingTask.date);
                if (date != null) {
                    selectedDate.setTime(date);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupClickListeners() {
        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> saveTask());

        startTimeRow.setOnClickListener(v -> showStartTimePicker());

        endTimeRow.setOnClickListener(v -> showEndTimePicker());

        repeatDaysText.setOnClickListener(v -> showRepeatDialog());

        alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, isChecked ? "🔔 Alerts enabled - we'll keep you on track!" : "🔕 Alerts disabled", Toast.LENGTH_SHORT).show()
        );
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

        // Cancel old alarms first
        AlarmHelper.cancelFocusTaskAlarms(this, existingTask);

        // Update task
        existingTask.name = taskName;
        existingTask.hour = startHour;
        existingTask.minute = startMinute;
        existingTask.amPm = startAmPm;
        existingTask.endHour = endHour;
        existingTask.endMinute = endMinute;
        existingTask.endAmPm = endAmPm;
        existingTask.urgency = selectedUrgency;
        existingTask.date = dateStr;
        existingTask.timeCategory = timeCategory;
        existingTask.selectedDays = selectedDays;
        existingTask.isAlarmOn = alarmSwitch.isChecked();

        taskRepository.updateTask(existingTask);

        // Schedule new alarms if enabled
        if (existingTask.isAlarmOn) {
            AlarmHelper.scheduleFocusTaskAlarms(this, existingTask);
        }

        Toast.makeText(this, "🎯 Focus session updated! Let's crush it!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private boolean isValidTimeRange(int startHour, int startMinute, String startAmPm,
                                     int endHour, int endMinute, String endAmPm) {
        int startTotal = convertTo24Hour(startHour, startAmPm) * 60 + startMinute;
        int endTotal = convertTo24Hour(endHour, endAmPm) * 60 + endMinute;
        return endTotal > startTotal;
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
        minutePicker.setFormatter(i -> String.format(Locale.getDefault(), "%02d", i));
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
        minutePicker.setFormatter(i -> String.format(Locale.getDefault(), "%02d", i));
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
    
    private void showRepeatDialog() {
        String[] daysOfWeek = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        
        new AlertDialog.Builder(this)
                .setTitle("🔁 Choose your focus days")
                .setMultiChoiceItems(daysOfWeek, selectedDays, (dialog, which, isChecked) -> 
                    selectedDays[which] = isChecked
                )
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

