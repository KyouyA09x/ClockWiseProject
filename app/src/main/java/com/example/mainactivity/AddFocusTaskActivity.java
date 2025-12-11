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

    // Start time pickers
    private NumberPicker startHourPicker;
    private NumberPicker startMinutePicker;
    private NumberPicker startAmPmPicker;

    // End time pickers
    private NumberPicker endHourPicker;
    private NumberPicker endMinutePicker;
    private NumberPicker endAmPmPicker;

    private EditText labelEditText;
    private ImageButton clearLabelButton;
    private LinearLayout dateRow;
    private TextView dateValue;
    private LinearLayout urgencyLayout;
    private TextView urgencyValueText;
    private LinearLayout vibrationLayout;
    private SwitchCompat vibrationSwitch;

    private Calendar selectedDate = Calendar.getInstance();
    private String selectedUrgency = "None";
    private TaskRepository taskRepository;

    // Edit mode variables
    private boolean isEditMode = false;
    private Task editingTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);

        setTheme(ThemeHelper.getThemeResource(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_focus_task);

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        initViews();
        setupTimePickers();
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
        clearLabelButton.setVisibility(editingTask.name != null && !editingTask.name.isEmpty() ? View.VISIBLE : View.GONE);

        // Set start time
        startHourPicker.setValue(editingTask.hour);
        startMinutePicker.setValue(editingTask.minute);
        startAmPmPicker.setValue(editingTask.amPm != null && editingTask.amPm.equals("PM") ? 1 : 0);

        // Set end time
        endHourPicker.setValue(editingTask.endHour);
        endMinutePicker.setValue(editingTask.endMinute);
        endAmPmPicker.setValue(editingTask.endAmPm != null && editingTask.endAmPm.equals("PM") ? 1 : 0);

        // Set urgency
        if (editingTask.urgency != null && !editingTask.urgency.equals("None")) {
            selectedUrgency = editingTask.urgency;
            urgencyValueText.setText(selectedUrgency);
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

        // Set vibration
        vibrationSwitch.setChecked(editingTask.vibrationEnabled);

        // Change save button text to indicate update
        saveButton.setText("Update");
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
        // clearLabelButton = findViewById(R.id.clearLabelButton);
        // dateRow = findViewById(R.id.dateRow);
        // dateValue = findViewById(R.id.dateValue);
        // urgencyLayout = findViewById(R.id.urgencyLayout);
        // urgencyValueText = findViewById(R.id.urgencyValueText);
        // vibrationLayout = findViewById(R.id.vibrationLayout);
        // vibrationSwitch = findViewById(R.id.vibrationSwitch);

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
        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> saveTask());

        dateRow.setOnClickListener(v -> showDatePicker());

        urgencyLayout.setOnClickListener(v -> showUrgencyDialog());

        vibrationLayout.setOnClickListener(v -> vibrationSwitch.toggle());

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, isChecked ? "Vibration ON" : "Vibration OFF", Toast.LENGTH_SHORT).show();
        });

        clearLabelButton.setOnClickListener(v -> labelEditText.setText(""));

        labelEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearLabelButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void saveTask() {
        String taskName = labelEditText.getText().toString().trim();

        if (taskName.isEmpty()) {
            Toast.makeText(this, "Please enter a task name", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
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
            editingTask.vibrationEnabled = vibrationSwitch.isChecked();

            taskRepository.updateTask(editingTask);

            // Schedule new alarms if enabled
            if (editingTask.isAlarmOn) {
                AlarmHelper.scheduleFocusTaskAlarms(this, editingTask);
            }

            Toast.makeText(this, "Focus Task updated!", Toast.LENGTH_SHORT).show();
        } else {
            // Create new Focus Task
            Task focusTask = new Task(taskName, startHour, startMinute, startAmPm,
                                      endHour, endMinute, endAmPm, selectedUrgency);
            focusTask.date = dateStr;
            focusTask.timeCategory = timeCategory;
            focusTask.vibrationEnabled = vibrationSwitch.isChecked();

            // Save to database
            long taskId = taskRepository.addTask(focusTask);
            focusTask.id = (int) taskId;

            // Schedule both alarms (start and end)
            AlarmHelper.scheduleFocusTaskAlarms(this, focusTask);

            Toast.makeText(this, "Focus Task created: " + startHour + ":" +
                    String.format("%02d", startMinute) + " " + startAmPm + " to " +
                    endHour + ":" + String.format("%02d", endMinute) + " " + endAmPm,
                    Toast.LENGTH_LONG).show();
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
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        dateValue.setText(sdf.format(selectedDate.getTime()));
    }

    private void showUrgencyDialog() {
        String[] urgencyLevels = {"None", "Low", "Medium", "High"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View customTitleView = inflater.inflate(R.layout.dialog_custom_title, null);
        TextView dialogTitle = customTitleView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Priority");
        ImageButton dialogCancelButton = customTitleView.findViewById(R.id.dialogCancelButton);
        builder.setCustomTitle(customTitleView);

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
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
        dialog.show();
    }
}

