package com.example.mainactivity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.DatePickerDialog; // <--- Added Import
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat; // <--- Added Import
import java.util.ArrayList;
import java.util.Calendar; // <--- Added Import
import java.util.Locale; // <--- Added Import
import java.util.Map;
import java.util.TreeMap;

public class AddTaskActivity extends AppCompatActivity {

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
    private LinearLayout soundLayout;
    private TextView soundValueText;
    private LinearLayout snoozeLayout;
    private LinearLayout urgencyLayout;
    private TextView urgencyValueText;
    private TextView repeatInfoText;
    private LinearLayout vibrationLayout;
    private SwitchCompat vibrationSwitch;

    // **** NEW DATE VARIABLES ****
    private LinearLayout dateRow; // Changed to LinearLayout to match your style
    private TextView dateValue;
    private Calendar selectedDate = Calendar.getInstance();

    private boolean[] selectedDays = new boolean[7];
    private String[] daysOfWeek = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private String selectedUrgency = "None";
    private TaskRepository taskRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        cancelButton = findViewById(R.id.cancelButton);
        saveButton = findViewById(R.id.saveButton);
        hourPicker = findViewById(R.id.hourPicker);
        minutePicker = findViewById(R.id.minutePicker);
        amPmPicker = findViewById(R.id.amPmPicker);
        snoozeSwitch = findViewById(R.id.snoozeSwitch);
        repeatLayout = findViewById(R.id.repeatLayout);
        repeatValueText = findViewById(R.id.repeatValueText);
        labelEditText = findViewById(R.id.labelEditText);
        clearLabelButton = findViewById(R.id.clearLabelButton);
        soundLayout = findViewById(R.id.soundLayout);
        soundValueText = findViewById(R.id.soundValueText);
        snoozeLayout = findViewById(R.id.snoozeLayout);
        urgencyLayout = findViewById(R.id.urgencyLayout);
        urgencyValueText = findViewById(R.id.urgencyValueText);
        repeatInfoText = findViewById(R.id.repeatInfoText);
        vibrationLayout = findViewById(R.id.vibrationLayout);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);

        // **** FIND VIEWS FOR DATE ****
        dateRow = findViewById(R.id.dateRow);
        dateValue = findViewById(R.id.dateValue);

        // **** SET UP DATE CLICK LISTENER ****
        updateDateLabel(); // Set initial text to Today
        dateRow.setOnClickListener(v -> showDatePicker());

        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> {
            String taskName = labelEditText.getText().toString();
            int hour = hourPicker.getValue();
            int minute = minutePicker.getValue();
            String amPm = amPmPicker.getDisplayedValues()[amPmPicker.getValue()];

            Task newTask = new Task(taskName, hour, minute, amPm, selectedUrgency, selectedDays);

            // **** SAVE THE DATE TO THE TASK ****
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            newTask.date = sdf.format(selectedDate.getTime());

            // Set time category for the task
            if (amPm.equals("AM")) {
                newTask.timeCategory = "morning";
            } else if (hour == 12 || (hour >= 1 && hour < 6)) {
                newTask.timeCategory = "afternoon";
            } else {
                newTask.timeCategory = "night";
            }

            // Save vibration setting
            newTask.vibrationEnabled = vibrationSwitch.isChecked();

            // Save to database and get the ID
            long taskId = taskRepository.addTask(newTask);
            newTask.id = (int) taskId;

            // Schedule alarm notification for this task
            AlarmHelper.scheduleTaskAlarm(this, newTask);

            setResult(RESULT_OK);
            finish();
        });

        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setValue(12);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(i -> String.format("%02d", i));

        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});

        repeatLayout.setOnClickListener(v -> showRepeatDialog());
        soundLayout.setOnClickListener(v -> showSoundDialog());
        urgencyLayout.setOnClickListener(v -> showUrgencyDialog());

        snoozeLayout.setOnClickListener(v -> snoozeSwitch.toggle());

        snoozeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Snooze is ON", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Snooze is OFF", Toast.LENGTH_SHORT).show();
            }
        });

        // Vibration toggle setup
        vibrationLayout.setOnClickListener(v -> vibrationSwitch.toggle());

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Vibration is ON", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Vibration is OFF", Toast.LENGTH_SHORT).show();
            }
        });

        labelEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    clearLabelButton.setVisibility(View.VISIBLE);
                } else {
                    clearLabelButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearLabelButton.setOnClickListener(v -> labelEditText.setText(""));
    }

    // **** NEW METHOD: SHOW DATE PICKER ****
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

    // **** NEW METHOD: UPDATE LABEL TEXT ****
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

        builder.setPositiveButton("OK", (dialog, which) -> {
            updateRepeatText();
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialogCancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.show();
    }

    private void updateRepeatText() {
        ArrayList<String> selectedDayNames = new ArrayList<>();
        int selectedCount = 0;
        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                selectedDayNames.add(daysOfWeek[i].substring(0, 3));
                selectedCount++;
            }
        }

        if (selectedCount == 7) {
            repeatValueText.setText("Every day");
            repeatInfoText.setVisibility(View.VISIBLE);
        } else if (selectedCount == 0) {
            repeatValueText.setText("Never");
            repeatInfoText.setVisibility(View.GONE);
        } else {
            repeatValueText.setText(String.join(", ", selectedDayNames));
            repeatInfoText.setVisibility(View.GONE);
        }
    }

    private void showSoundDialog() {
        RingtoneManager manager = new RingtoneManager(this);
        manager.setType(RingtoneManager.TYPE_ALARM);
        Cursor cursor = manager.getCursor();

        Map<String, Uri> ringtones = new TreeMap<>();
        while (cursor.moveToNext()) {
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            Uri uri = manager.getRingtoneUri(cursor.getPosition());
            ringtones.put(title, uri);
        }

        String[] ringtoneNames = ringtones.keySet().toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View customTitleView = inflater.inflate(R.layout.dialog_custom_title, null);
        TextView dialogTitle = customTitleView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Sound");
        ImageButton dialogCancelButton = customTitleView.findViewById(R.id.dialogCancelButton);
        builder.setCustomTitle(customTitleView);

        builder.setItems(ringtoneNames, (dialog, which) -> {
            String selectedRingtone = ringtoneNames[which];
            soundValueText.setText(selectedRingtone);
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialogCancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.show();
    }

    private void showUrgencyDialog() {
        String[] urgencyLevels = {"Low", "Medium", "High"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View customTitleView = inflater.inflate(R.layout.dialog_custom_title, null);
        TextView dialogTitle = customTitleView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Urgency");
        ImageButton dialogCancelButton = customTitleView.findViewById(R.id.dialogCancelButton);
        builder.setCustomTitle(customTitleView);

        builder.setItems(urgencyLevels, (dialog, which) -> {
            selectedUrgency = urgencyLevels[which];
            urgencyValueText.setText(selectedUrgency);

            // Auto-enable vibration for High priority tasks
            if (selectedUrgency.equals("High")) {
                vibrationSwitch.setChecked(true);
                Toast.makeText(this, "Vibration enabled for high priority task", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialogCancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.show();
    }
}
