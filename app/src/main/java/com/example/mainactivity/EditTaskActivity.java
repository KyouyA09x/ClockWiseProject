package com.example.mainactivity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.DatePickerDialog;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditTaskActivity extends AppCompatActivity {

    private Button cancelButton;
    private Button saveButton;
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker amPmPicker;
    private SwitchCompat snoozeSwitch;
    private LinearLayout repeatLayout;
    private TextView repeatValueText;
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
    private boolean[] selectedDays = new boolean[7];
    private String[] daysOfWeek = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private String selectedUrgency = "None";
    private TaskRepository taskRepository;
    private Task existingTask;
    private int taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);

        setTheme(ThemeHelper.getThemeResource(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

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
        hourPicker = findViewById(R.id.hourPicker);
        minutePicker = findViewById(R.id.minutePicker);
        amPmPicker = findViewById(R.id.amPmPicker);
        // snoozeSwitch = findViewById(R.id.snoozeSwitch);
        // repeatLayout = findViewById(R.id.repeatLayout);
        repeatValueText = findViewById(R.id.repeatDaysText);
        labelEditText = findViewById(R.id.labelEditText);
        // clearLabelButton = findViewById(R.id.clearLabelButton);
        // urgencyLayout = findViewById(R.id.urgencyLayout);
        // urgencyValueText = findViewById(R.id.urgencyValueText);
        // vibrationLayout = findViewById(R.id.vibrationLayout);
        // vibrationSwitch = findViewById(R.id.vibrationSwitch);
        // dateRow = findViewById(R.id.dateRow);
        // dateValue = findViewById(R.id.dateValue);
        addTaskTitle = findViewById(R.id.addTaskTitle);

        // Change title to "Edit Task"
        if (addTaskTitle != null) {
            addTaskTitle.setText("Edit Task");
        }

        // Setup pickers
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(i -> String.format("%02d", i));

        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
    }

    private void populateWithExistingTask() {
        // Set task name
        labelEditText.setText(existingTask.name);

        // Set time
        hourPicker.setValue(existingTask.hour);
        minutePicker.setValue(existingTask.minute);
        amPmPicker.setValue(existingTask.amPm != null && existingTask.amPm.equals("PM") ? 1 : 0);

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

        // Set selected days
        if (existingTask.selectedDays != null) {
            selectedDays = existingTask.selectedDays.clone();
            updateRepeatText();
        }
    }

    private void setupListeners() {
        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> saveTask());

        dateRow.setOnClickListener(v -> showDatePicker());

        repeatLayout.setOnClickListener(v -> showRepeatDialog());

        urgencyLayout.setOnClickListener(v -> showUrgencyDialog());

        vibrationLayout.setOnClickListener(v -> vibrationSwitch.toggle());

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Vibration is " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

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

        clearLabelButton.setOnClickListener(v -> labelEditText.setText(""));
    }

    private void saveTask() {
        String taskName = labelEditText.getText().toString();
        if (taskName.isEmpty()) {
            Toast.makeText(this, "Please enter a task name", Toast.LENGTH_SHORT).show();
            return;
        }

        int hour = hourPicker.getValue();
        int minute = minutePicker.getValue();
        String amPm = amPmPicker.getDisplayedValues()[amPmPicker.getValue()];

        // Cancel old alarm first
        AlarmHelper.cancelTaskAlarm(this, existingTask);

        // Update task
        existingTask.name = taskName;
        existingTask.hour = hour;
        existingTask.minute = minute;
        existingTask.amPm = amPm;
        existingTask.urgency = selectedUrgency;
        existingTask.selectedDays = selectedDays;
        existingTask.vibrationEnabled = vibrationSwitch.isChecked();

        // Update date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        existingTask.date = sdf.format(selectedDate.getTime());

        // Update time category
        if (amPm.equals("AM")) {
            existingTask.timeCategory = "morning";
        } else if (hour == 12 || (hour >= 1 && hour < 6)) {
            existingTask.timeCategory = "afternoon";
        } else {
            existingTask.timeCategory = "night";
        }

        // Save to database
        taskRepository.updateTask(existingTask);

        // Reschedule alarm if enabled
        if (existingTask.isAlarmOn) {
            AlarmHelper.scheduleTaskAlarm(this, existingTask);
        }

        Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
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

    private void showRepeatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View customTitleView = inflater.inflate(R.layout.dialog_custom_title, null);
        TextView dialogTitle = customTitleView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Repeat");
        ImageButton dialogCancelButton = customTitleView.findViewById(R.id.dialogCancelButton);
        builder.setCustomTitle(customTitleView);

        builder.setMultiChoiceItems(daysOfWeek, selectedDays, (dialog, which, isChecked) -> {
            selectedDays[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> updateRepeatText());
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialogCancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.show();
    }

    private void updateRepeatText() {
        ArrayList<String> selectedDayNames = new ArrayList<>();
        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                selectedDayNames.add(daysOfWeek[i].substring(0, 3));
            }
        }
        if (selectedDayNames.isEmpty()) {
            repeatValueText.setText("Never");
        } else if (selectedDayNames.size() == 7) {
            repeatValueText.setText("Every day");
        } else {
            repeatValueText.setText(String.join(", ", selectedDayNames));
        }
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

