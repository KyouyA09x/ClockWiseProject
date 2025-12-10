package com.example.mainactivity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
    private NumberPicker startHourPicker, startMinutePicker, startAmPmPicker;
    private NumberPicker endHourPicker, endMinutePicker, endAmPmPicker;
    private EditText labelEditText;
    private ImageButton clearLabelButton;
    private LinearLayout urgencyLayout;
    private TextView urgencyValueText;
    private LinearLayout vibrationLayout;
    private SwitchCompat vibrationSwitch;
    private LinearLayout dateRow;
    private TextView dateValue;
    private TextView addTaskTitle;

    private Calendar selectedDate = Calendar.getInstance();
    private String selectedUrgency = "None";
    private TaskRepository taskRepository;
    private Task existingTask;
    private int taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_focus_task);

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        // Get task ID from intent
        taskId = getIntent().getIntExtra("task_id", -1);
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
        populateWithExistingTask();
        setupListeners();
    }

    private void initViews() {
        cancelButton = findViewById(R.id.cancelButton);
        saveButton = findViewById(R.id.saveButton);

        startHourPicker = findViewById(R.id.startHourPicker);
        startMinutePicker = findViewById(R.id.startMinutePicker);
        startAmPmPicker = findViewById(R.id.startAmPmPicker);

        endHourPicker = findViewById(R.id.endHourPicker);
        endMinutePicker = findViewById(R.id.endMinutePicker);
        endAmPmPicker = findViewById(R.id.endAmPmPicker);

        labelEditText = findViewById(R.id.labelEditText);
        clearLabelButton = findViewById(R.id.clearLabelButton);
        urgencyLayout = findViewById(R.id.urgencyLayout);
        urgencyValueText = findViewById(R.id.urgencyValueText);
        vibrationLayout = findViewById(R.id.vibrationLayout);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        dateRow = findViewById(R.id.dateRow);
        dateValue = findViewById(R.id.dateValue);
        addTaskTitle = findViewById(R.id.addTaskTitle);

        // Change title to "Edit Focus Task"
        if (addTaskTitle != null) {
            addTaskTitle.setText("Edit Focus Task");
        }

        // Setup pickers
        setupTimePickers();
    }

    private void setupTimePickers() {
        // Start time pickers
        startHourPicker.setMinValue(1);
        startHourPicker.setMaxValue(12);
        startMinutePicker.setMinValue(0);
        startMinutePicker.setMaxValue(59);
        startMinutePicker.setFormatter(i -> String.format("%02d", i));
        startAmPmPicker.setMinValue(0);
        startAmPmPicker.setMaxValue(1);
        startAmPmPicker.setDisplayedValues(new String[]{"AM", "PM"});

        // End time pickers
        endHourPicker.setMinValue(1);
        endHourPicker.setMaxValue(12);
        endMinutePicker.setMinValue(0);
        endMinutePicker.setMaxValue(59);
        endMinutePicker.setFormatter(i -> String.format("%02d", i));
        endAmPmPicker.setMinValue(0);
        endAmPmPicker.setMaxValue(1);
        endAmPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
    }

    private void populateWithExistingTask() {
        // Set task name
        labelEditText.setText(existingTask.name);

        // Set start time
        startHourPicker.setValue(existingTask.hour);
        startMinutePicker.setValue(existingTask.minute);
        startAmPmPicker.setValue(existingTask.amPm != null && existingTask.amPm.equals("PM") ? 1 : 0);

        // Set end time
        endHourPicker.setValue(existingTask.endHour);
        endMinutePicker.setValue(existingTask.endMinute);
        endAmPmPicker.setValue(existingTask.endAmPm != null && existingTask.endAmPm.equals("PM") ? 1 : 0);

        // Set urgency
        selectedUrgency = existingTask.urgency != null ? existingTask.urgency : "None";
        urgencyValueText.setText(selectedUrgency);

        // Set vibration
        vibrationSwitch.setChecked(existingTask.vibrationEnabled);

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
        updateDateLabel();
    }

    private void setupListeners() {
        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> saveTask());

        dateRow.setOnClickListener(v -> showDatePicker());

        urgencyLayout.setOnClickListener(v -> showUrgencyDialog());

        vibrationLayout.setOnClickListener(v -> vibrationSwitch.toggle());

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Vibration is " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        clearLabelButton.setOnClickListener(v -> labelEditText.setText(""));
    }

    private void saveTask() {
        String taskName = labelEditText.getText().toString();
        if (taskName.isEmpty()) {
            Toast.makeText(this, "Please enter a task name", Toast.LENGTH_SHORT).show();
            return;
        }

        int startHour = startHourPicker.getValue();
        int startMinute = startMinutePicker.getValue();
        String startAmPm = startAmPmPicker.getDisplayedValues()[startAmPmPicker.getValue()];

        int endHour = endHourPicker.getValue();
        int endMinute = endMinutePicker.getValue();
        String endAmPm = endAmPmPicker.getDisplayedValues()[endAmPmPicker.getValue()];

        // Validate time range
        if (!isValidTimeRange(startHour, startMinute, startAmPm, endHour, endMinute, endAmPm)) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
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
        existingTask.vibrationEnabled = vibrationSwitch.isChecked();

        // Update date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        existingTask.date = sdf.format(selectedDate.getTime());

        // Update time category
        if (startAmPm.equals("AM")) {
            existingTask.timeCategory = "morning";
        } else if (startHour == 12 || (startHour >= 1 && startHour < 6)) {
            existingTask.timeCategory = "afternoon";
        } else {
            existingTask.timeCategory = "night";
        }

        // Save to database
        taskRepository.updateTask(existingTask);

        // Reschedule alarms if enabled
        if (existingTask.isAlarmOn) {
            AlarmHelper.scheduleFocusTaskAlarms(this, existingTask);
        }

        Toast.makeText(this, "Focus Task updated!", Toast.LENGTH_SHORT).show();
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

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
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
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        dateValue.setText(sdf.format(selectedDate.getTime()));
    }

    private void showUrgencyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View customTitleView = inflater.inflate(R.layout.dialog_custom_title, null);
        TextView dialogTitle = customTitleView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Priority");
        ImageButton dialogCancelButton = customTitleView.findViewById(R.id.dialogCancelButton);
        builder.setCustomTitle(customTitleView);

        String[] urgencyLevels = {"None", "Low", "Medium", "High"};
        builder.setItems(urgencyLevels, (dialog, which) -> {
            selectedUrgency = urgencyLevels[which];
            urgencyValueText.setText(selectedUrgency);

            // Auto-enable vibration for high priority
            if (selectedUrgency.equals("High")) {
                vibrationSwitch.setChecked(true);
            }
        });

        AlertDialog dialog = builder.create();
        dialogCancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.show();
    }
}

