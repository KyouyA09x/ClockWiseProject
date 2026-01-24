package com.example.mainactivity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

public class AddNoteDialog extends Dialog {

    private TextInputEditText noteTitleInput;
    private TextInputEditText noteDescriptionInput;
    private MaterialButton editNoteButton;
    private MaterialButton taskTypeButton;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    private MaterialButton addChecklistItemButton;
    private TextView dateDisplay;

    private Note currentNote;
    private NoteDao noteDao;
    private OnNoteSavedListener listener;

    // Formatting properties
    private String fontFamily = "default";
    private int textSize = 16;
    private String textColor = "#000000";
    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isUnderline = false;
    private boolean isChecklist = false;
    private String priority = "None";
    private String taskType = "None"; // "None", "Reminder", "Focus Task"
    
    // Task scheduling properties (used when converting note to task)
    private String taskDate;
    private int taskHour = 9;
    private int taskMinute = 0;
    private String taskAmPm = "AM";

    public interface OnNoteSavedListener {
        void onNoteSaved();
    }

    public AddNoteDialog(@NonNull Context context, Note note, OnNoteSavedListener listener) {
        super(context);
        this.currentNote = note;
        this.listener = listener;
        this.noteDao = TaskDatabase.getInstance(context).noteDao();

        if (currentNote != null) {
            // Load existing formatting
            fontFamily = currentNote.fontFamily;
            textSize = currentNote.textSize;
            textColor = currentNote.textColor;
            isBold = currentNote.isBold;
            isItalic = currentNote.isItalic;
            isUnderline = currentNote.isUnderline;
            isChecklist = currentNote.isChecklist;
            priority = currentNote.priority != null ? currentNote.priority : "None";
            taskType = currentNote.taskType != null ? currentNote.taskType : "None";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_add_note);

        // Make dialog full width and adjust to screen
        if (getWindow() != null) {
            android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            getWindow().setAttributes(params);
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        initViews();
        setupListeners();

        if (currentNote != null) {
            // Edit mode
            noteTitleInput.setText(currentNote.title);
            noteDescriptionInput.setText(currentNote.description);
            updateTaskTypeButton(); // Show task type
        } else {
            updateTaskTypeButton(); // Show default task type
        }

        // Apply formatting to description preview
        applyFormattingToInput();
    }

    private void initViews() {
        noteTitleInput = findViewById(R.id.noteTitleInput);
        noteDescriptionInput = findViewById(R.id.noteDescriptionInput);
        dateDisplay = findViewById(R.id.noteDateDisplay);
        editNoteButton = findViewById(R.id.editNoteButton);
        taskTypeButton = findViewById(R.id.taskTypeButton);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        
        // Initialize checklist button
        addChecklistItemButton = findViewById(R.id.addChecklistItemButton);
        updateChecklistButtonVisibility();
    }

    private void setupListeners() {
        editNoteButton.setOnClickListener(v -> showEditNoteOptionsMenu());

        taskTypeButton.setOnClickListener(v -> showTaskTypeDialog());

        saveButton.setOnClickListener(v -> saveNote());
        cancelButton.setOnClickListener(v -> dismiss());
        
        // Checklist item button
        if (addChecklistItemButton != null) {
            addChecklistItemButton.setOnClickListener(v -> insertChecklistItem());
        }
    }
    
    private void showEditNoteOptionsMenu() {
        String[] options = {"✨ Text Formatting", "🎯 Set Priority"};
        
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Edit Note Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Text Formatting
                            showTextFormatDialog();
                            break;
                        case 1: // Priority
                            showPriorityDialog();
                            break;
                    }
                })
                .show();
    }
    
    private void showDatePicker() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    java.util.Calendar selectedDate = java.util.Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    taskDate = sdf.format(selectedDate.getTime());
                    updateTaskDateButton();
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
    
    private void showTimePicker() {
        int currentHour = taskHour;
        if (taskAmPm.equals("PM") && currentHour != 12) {
            currentHour += 12;
        } else if (taskAmPm.equals("AM") && currentHour == 12) {
            currentHour = 0;
        }
        
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    taskMinute = minute;
                    if (hourOfDay == 0) {
                        taskHour = 12;
                        taskAmPm = "AM";
                    } else if (hourOfDay < 12) {
                        taskHour = hourOfDay;
                        taskAmPm = "AM";
                    } else if (hourOfDay == 12) {
                        taskHour = 12;
                        taskAmPm = "PM";
                    } else {
                        taskHour = hourOfDay - 12;
                        taskAmPm = "PM";
                    }
                    updateTaskTimeButton();
                },
                currentHour,
                taskMinute,
                false
        );
        timePickerDialog.show();
    }
    
    private void updateTaskDateButton() {
        if (taskTypeButton == null) return;
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(taskDate);
            
            // Check if it's today
            java.text.SimpleDateFormat todayFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            String today = todayFormat.format(new java.util.Date());
            
            if (taskDate.equals(today)) {
                taskTypeButton.setText("📅 Today");
            } else {
                taskTypeButton.setText("📅 " + outputFormat.format(date));
            }
        } catch (Exception e) {
            taskTypeButton.setText("📅 " + taskDate);
        }
    }
    
    private void updateTaskTimeButton() {
        if (taskTypeButton == null) return;
        taskTypeButton.setText(String.format(java.util.Locale.getDefault(), "🕐 %d:%02d %s", taskHour, taskMinute, taskAmPm));
    }

    private void showPriorityDialog() {
        Dialog priorityDialog = new Dialog(getContext());
        priorityDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        priorityDialog.setContentView(R.layout.dialog_priority_selector);
        
        if (priorityDialog.getWindow() != null) {
            priorityDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            priorityDialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Setup click listeners for each priority option
        priorityDialog.findViewById(R.id.priorityNone).setOnClickListener(v -> {
            priority = "None";
            updatePriorityButton();
            priorityDialog.dismiss();
        });

        priorityDialog.findViewById(R.id.priorityLow).setOnClickListener(v -> {
            priority = "Low";
            updatePriorityButton();
            priorityDialog.dismiss();
        });

        priorityDialog.findViewById(R.id.priorityMedium).setOnClickListener(v -> {
            priority = "Medium";
            updatePriorityButton();
            priorityDialog.dismiss();
        });

        priorityDialog.findViewById(R.id.priorityHigh).setOnClickListener(v -> {
            priority = "High";
            updatePriorityButton();
            priorityDialog.dismiss();
        });

        priorityDialog.findViewById(R.id.cancelButton).setOnClickListener(v -> priorityDialog.dismiss());

        priorityDialog.show();
    }

    private void updatePriorityButton() {
        taskTypeButton.setText("Priority: " + priority);
        switch (priority) {
            case "High":
                taskTypeButton.setIconResource(R.drawable.ic_priority_high);
                break;
            case "Medium":
                taskTypeButton.setIconResource(R.drawable.ic_priority_medium);
                break;
            case "Low":
                taskTypeButton.setIconResource(R.drawable.ic_priority_low);
                break;
            default:
                taskTypeButton.setIconResource(R.drawable.ic_priority_low);
                break;
        }
    }

    private void showTaskTypeDialog() {
        Dialog taskTypeDialog = new Dialog(getContext());
        taskTypeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        taskTypeDialog.setContentView(R.layout.dialog_task_type_selector);
        
        if (taskTypeDialog.getWindow() != null) {
            taskTypeDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            taskTypeDialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Setup click listeners for each task type option
        taskTypeDialog.findViewById(R.id.taskTypeNone).setOnClickListener(v -> {
            taskType = "None";
            updateTaskTypeButton();
            taskTypeDialog.dismiss();
        });

        taskTypeDialog.findViewById(R.id.taskTypeReminder).setOnClickListener(v -> {
            taskType = "Reminder";
            updateTaskTypeButton();
            taskTypeDialog.dismiss();
        });

        taskTypeDialog.findViewById(R.id.taskTypeFocus).setOnClickListener(v -> {
            taskType = "Focus Task";
            updateTaskTypeButton();
            taskTypeDialog.dismiss();
        });

        taskTypeDialog.findViewById(R.id.cancelButton).setOnClickListener(v -> taskTypeDialog.dismiss());

        taskTypeDialog.show();
    }

    private void updateTaskTypeButton() {
        if (taskType.equals("None")) {
            taskTypeButton.setText("Note Type: Just a Note");
            taskTypeButton.setIconResource(R.drawable.ic_edit);
        } else if (taskType.equals("Reminder")) {
            taskTypeButton.setText("Note Type: ⏰ Task");
            taskTypeButton.setIconResource(R.drawable.ic_reminder);
        } else {
            taskTypeButton.setText("Note Type: 🎯 Focus Task");
            taskTypeButton.setIconResource(R.drawable.ic_focus);
        }
    }

    private void showTextFormatDialog() {
        Dialog formatDialog = new Dialog(getContext());
        formatDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        formatDialog.setContentView(R.layout.dialog_text_format);
        
        // Set dialog to be full width with proper styling
        if (formatDialog.getWindow() != null) {
            formatDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            formatDialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Initialize views
        android.widget.Spinner fontFamilySpinner = formatDialog.findViewById(R.id.fontFamilySpinner);
        TextView fontPreviewText = formatDialog.findViewById(R.id.fontPreviewText);
        Slider textSizeSlider = formatDialog.findViewById(R.id.textSizeSlider);
        TextView textSizeValue = formatDialog.findViewById(R.id.textSizeValue);
        MaterialButton colorBlack = formatDialog.findViewById(R.id.colorBlack);
        MaterialButton colorBlue = formatDialog.findViewById(R.id.colorBlue);
        MaterialButton colorRed = formatDialog.findViewById(R.id.colorRed);
        MaterialButton colorGreen = formatDialog.findViewById(R.id.colorGreen);
        MaterialButton boldButton = formatDialog.findViewById(R.id.boldButton);
        MaterialButton italicButton = formatDialog.findViewById(R.id.italicButton);
        MaterialButton underlineButton = formatDialog.findViewById(R.id.underlineButton);
        MaterialButton checklistToggle = formatDialog.findViewById(R.id.checklistToggle);
        MaterialButton applyButton = formatDialog.findViewById(R.id.applyFormatButton);
        MaterialButton cancelFormatButton = formatDialog.findViewById(R.id.cancelFormatButton);

        // Setup checklist toggle
        if (checklistToggle != null) {
            checklistToggle.setText(isChecklist ? "Checklist" : "Plain Text");
            checklistToggle.setIconResource(isChecklist ? R.drawable.ic_reminder : R.drawable.ic_edit);
            checklistToggle.setOnClickListener(v -> {
                isChecklist = !isChecklist;
                checklistToggle.setText(isChecklist ? "Checklist" : "Plain Text");
                checklistToggle.setIconResource(isChecklist ? R.drawable.ic_reminder : R.drawable.ic_edit);
                updateChecklistButtonVisibility();
            });
        }

        // Setup font family spinner
        String[] fontFamilies = {"Default", "Serif", "Monospace", "Sans Serif", "Cursive", "Casual", "Elegant"};
        android.widget.ArrayAdapter<String> fontAdapter = new android.widget.ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, fontFamilies);
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontFamilySpinner.setAdapter(fontAdapter);

        // Set current font family
        int fontPosition = 0;
        if (fontFamily.equals("serif")) fontPosition = 1;
        else if (fontFamily.equals("monospace")) fontPosition = 2;
        else if (fontFamily.equals("sans-serif")) fontPosition = 3;
        else if (fontFamily.equals("cursive")) fontPosition = 4;
        else if (fontFamily.equals("casual")) fontPosition = 5;
        else if (fontFamily.equals("elegant")) fontPosition = 6;
        fontFamilySpinner.setSelection(fontPosition);

        // Update preview text based on selections
        updateFontPreview(fontPreviewText);

        textSizeSlider.setValue(textSize);
        textSizeValue.setText(textSize + " sp");

        // Font family selection
        fontFamilySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                switch (position) {
                    case 0:
                        fontFamily = "default";
                        break;
                    case 1:
                        fontFamily = "serif";
                        break;
                    case 2:
                        fontFamily = "monospace";
                        break;
                    case 3:
                        fontFamily = "sans-serif";
                        break;
                    case 4:
                        fontFamily = "cursive";
                        break;
                    case 5:
                        fontFamily = "casual";
                        break;
                    case 6:
                        fontFamily = "elegant";
                        break;
                }
                updateFontPreview(fontPreviewText);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Text size slider
        textSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            textSize = (int) value;
            textSizeValue.setText(textSize + " sp");
            updateFontPreview(fontPreviewText);
        });

        // Color buttons
        colorBlack.setOnClickListener(v -> {
            textColor = "#000000";
            updateFontPreview(fontPreviewText);
        });
        colorBlue.setOnClickListener(v -> {
            textColor = "#1565C0";
            updateFontPreview(fontPreviewText);
        });
        colorRed.setOnClickListener(v -> {
            textColor = "#D32F2F";
            updateFontPreview(fontPreviewText);
        });
        colorGreen.setOnClickListener(v -> {
            textColor = "#388E3C";
            updateFontPreview(fontPreviewText);
        });

        // Style buttons
        boldButton.setOnClickListener(v -> {
            isBold = !isBold;
            boldButton.setBackgroundColor(isBold ? Color.parseColor("#E0E0E0") : Color.TRANSPARENT);
            updateFontPreview(fontPreviewText);
        });
        italicButton.setOnClickListener(v -> {
            isItalic = !isItalic;
            italicButton.setBackgroundColor(isItalic ? Color.parseColor("#E0E0E0") : Color.TRANSPARENT);
            updateFontPreview(fontPreviewText);
        });
        underlineButton.setOnClickListener(v -> {
            isUnderline = !isUnderline;
            underlineButton.setBackgroundColor(isUnderline ? Color.parseColor("#E0E0E0") : Color.TRANSPARENT);
            updateFontPreview(fontPreviewText);
        });

        // Set initial button states
        if (isBold) boldButton.setBackgroundColor(Color.parseColor("#E0E0E0"));
        if (isItalic) italicButton.setBackgroundColor(Color.parseColor("#E0E0E0"));
        if (isUnderline) underlineButton.setBackgroundColor(Color.parseColor("#E0E0E0"));

        applyButton.setOnClickListener(v -> {
            applyFormattingToInput();
            formatDialog.dismiss();
        });

        cancelFormatButton.setOnClickListener(v -> formatDialog.dismiss());

        formatDialog.show();
    }

    private void updateFontPreview(TextView previewText) {
        if (previewText == null) return;

        // Apply text size
        previewText.setTextSize(textSize);

        // Apply font family
        Typeface typeface;
        switch (fontFamily) {
            case "serif":
                typeface = Typeface.SERIF;
                break;
            case "monospace":
                typeface = Typeface.MONOSPACE;
                break;
            case "sans-serif":
                typeface = Typeface.SANS_SERIF;
                break;
            case "cursive":
                typeface = Typeface.create("cursive", Typeface.NORMAL);
                break;
            case "casual":
                typeface = Typeface.create("casual", Typeface.NORMAL);
                break;
            case "elegant":
                typeface = Typeface.create("serif", Typeface.NORMAL);
                break;
            default:
                typeface = Typeface.DEFAULT;
                break;
        }

        // Apply text color
        try {
            previewText.setTextColor(Color.parseColor(textColor));
        } catch (IllegalArgumentException e) {
            previewText.setTextColor(Color.BLACK);
        }

        // Apply text style
        int style = Typeface.NORMAL;
        if (isBold && isItalic) {
            style = Typeface.BOLD_ITALIC;
        } else if (isBold) {
            style = Typeface.BOLD;
        } else if (isItalic) {
            style = Typeface.ITALIC;
        }
        previewText.setTypeface(Typeface.create(typeface, style));

        // Apply underline
        if (isUnderline) {
            previewText.setPaintFlags(previewText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            previewText.setPaintFlags(previewText.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
        }
    }

    private void applyFormattingToInput() {
        if (noteDescriptionInput == null) return;

        // Apply text size (using SP for proper DPI scaling)
        noteDescriptionInput.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSize);

        // Apply font family
        switch (fontFamily) {
            case "serif":
                noteDescriptionInput.setTypeface(Typeface.SERIF);
                break;
            case "monospace":
                noteDescriptionInput.setTypeface(Typeface.MONOSPACE);
                break;
            default:
                noteDescriptionInput.setTypeface(Typeface.DEFAULT);
                break;
        }

        // Apply text color
        try {
            noteDescriptionInput.setTextColor(Color.parseColor(textColor));
        } catch (IllegalArgumentException e) {
            noteDescriptionInput.setTextColor(Color.BLACK);
        }

        // Apply text style
        int style = Typeface.NORMAL;
        if (isBold && isItalic) {
            style = Typeface.BOLD_ITALIC;
        } else if (isBold) {
            style = Typeface.BOLD;
        } else if (isItalic) {
            style = Typeface.ITALIC;
        }
        noteDescriptionInput.setTypeface(noteDescriptionInput.getTypeface(), style);

        // Apply underline
        if (isUnderline) {
            noteDescriptionInput.setPaintFlags(noteDescriptionInput.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            noteDescriptionInput.setPaintFlags(noteDescriptionInput.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        }
    }

    private void saveNote() {
        String title = noteTitleInput.getText() != null ? noteTitleInput.getText().toString().trim() : "";
        String description = noteDescriptionInput.getText() != null ? noteDescriptionInput.getText().toString().trim() : "";

        if (title.isEmpty() && description.isEmpty()) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Empty Note")
                    .setMessage("Please enter a title or description")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // If Task type is selected, create both a Note and a Task
        if (!taskType.equals("None")) {
            createTaskFromNote(title, description);
        }

        if (currentNote == null) {
            // Create new note
            currentNote = new Note();
        }

        currentNote.title = title;
        currentNote.description = description;
        currentNote.modifiedTimestamp = System.currentTimeMillis();
        currentNote.fontFamily = fontFamily;
        currentNote.textSize = textSize;
        currentNote.textColor = textColor;
        currentNote.isBold = isBold;
        currentNote.isItalic = isItalic;
        currentNote.isUnderline = isUnderline;
        currentNote.isChecklist = isChecklist;
        currentNote.priority = priority;
        currentNote.taskType = taskType;
        currentNote.isConvertedToTask = !taskType.equals("None");

        if (currentNote.id == 0) {
            noteDao.insert(currentNote);
        } else {
            noteDao.update(currentNote);
        }

        if (listener != null) {
            listener.onNoteSaved();
        }

        dismiss();
    }
    
    private void createTaskFromNote(String title, String description) {
        Task task = new Task();
        task.name = title.isEmpty() ? "Task from Note" : title;
        task.taskType = taskType.equals("Focus Task") ? "focus" : "reminder";
        task.date = taskDate;
        task.hour = taskHour;
        task.minute = taskMinute;
        task.amPm = taskAmPm;
        task.isAlarmOn = true;
        task.urgency = priority;
        task.selectedDays = new boolean[7];
        task.vibrationEnabled = false;
        
        // Set note content for context when task notifies
        task.noteContent = description;
        
        // For focus tasks, set default end time (1 hour later)
        if (taskType.equals("Focus Task")) {
            int endHour24 = taskHour;
            if (taskAmPm.equals("PM") && taskHour != 12) {
                endHour24 += 12;
            } else if (taskAmPm.equals("AM") && taskHour == 12) {
                endHour24 = 0;
            }
            endHour24 += 1; // Add 1 hour
            
            if (endHour24 >= 24) {
                endHour24 = 23;
                task.endMinute = 59;
            } else {
                task.endMinute = taskMinute;
            }
            
            if (endHour24 == 0) {
                task.endHour = 12;
                task.endAmPm = "AM";
            } else if (endHour24 < 12) {
                task.endHour = endHour24;
                task.endAmPm = "AM";
            } else if (endHour24 == 12) {
                task.endHour = 12;
                task.endAmPm = "PM";
            } else {
                task.endHour = endHour24 - 12;
                task.endAmPm = "PM";
            }
        }
        
        // Determine time category
        int hour24 = taskHour;
        if (taskAmPm.equals("PM") && taskHour != 12) {
            hour24 += 12;
        } else if (taskAmPm.equals("AM") && taskHour == 12) {
            hour24 = 0;
        }
        
        if (hour24 < 12) {
            task.timeCategory = "morning";
        } else if (hour24 < 17) {
            task.timeCategory = "afternoon";
        } else {
            task.timeCategory = "night";
        }
        
        // Insert task and schedule alarm
        TaskRepository repository = TaskRepository.getInstance();
        repository.initialize(getContext());
        repository.addTask(task);
        
        // Schedule alarm
        if (taskType.equals("Focus Task")) {
            AlarmHelper.scheduleFocusTaskAlarms(getContext(), task);
        } else {
            AlarmHelper.scheduleTaskAlarm(getContext(), task);
        }
    }
    private void updateChecklistButtonVisibility() {
        if (addChecklistItemButton != null) {
            addChecklistItemButton.setVisibility(isChecklist ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }
    private void insertChecklistItem() {
        if (noteDescriptionInput == null) return;
        String currentText = noteDescriptionInput.getText() != null ? 
            noteDescriptionInput.getText().toString() : "";
        int cursorPosition = noteDescriptionInput.getSelectionStart();
        String checklistPrefix = "[ ] ";
        StringBuilder newText = new StringBuilder(currentText);
        if (cursorPosition > 0 && cursorPosition <= currentText.length() && 
            currentText.charAt(cursorPosition - 1) != '\n') {
            newText.insert(cursorPosition, "\n" + checklistPrefix);
            cursorPosition += checklistPrefix.length() + 1;
        } else {
            newText.insert(cursorPosition, checklistPrefix);
            cursorPosition += checklistPrefix.length();
        }
        noteDescriptionInput.setText(newText.toString());
        noteDescriptionInput.setSelection(cursorPosition);
        noteDescriptionInput.requestFocus();
    }
}
