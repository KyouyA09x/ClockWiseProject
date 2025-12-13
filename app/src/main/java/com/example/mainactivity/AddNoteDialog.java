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
    private MaterialButton textFormatButton;
    private MaterialButton taskTypeButton;
    private MaterialButton priorityButton;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;
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
            updatePriorityButton(); // Show current priority
            updateTaskTypeButton(); // Show task type
        } else {
            updatePriorityButton(); // Show default priority
            updateTaskTypeButton(); // Show default task type
        }

        // Apply formatting to description preview
        applyFormattingToInput();
    }

    private void initViews() {
        noteTitleInput = findViewById(R.id.noteTitleInput);
        noteDescriptionInput = findViewById(R.id.noteDescriptionInput);
        dateDisplay = findViewById(R.id.noteDateDisplay);
        textFormatButton = findViewById(R.id.textFormatButton);
        taskTypeButton = findViewById(R.id.taskTypeButton);
        priorityButton = findViewById(R.id.priorityButton);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        
        // Set current date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault());
        dateDisplay.setText(sdf.format(new java.util.Date()));
    }

    private void setupListeners() {
        textFormatButton.setOnClickListener(v -> showTextFormatDialog());

        taskTypeButton.setOnClickListener(v -> showTaskTypeDialog());

        priorityButton.setOnClickListener(v -> showPriorityDialog());

        saveButton.setOnClickListener(v -> saveNote());
        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void showPriorityDialog() {
        String[] priorities = {"None", "Low", "Medium", "High"};
        int currentIndex = 0;
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i].equals(priority)) {
                currentIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Select Priority")
                .setSingleChoiceItems(priorities, currentIndex, (dialog, which) -> {
                    priority = priorities[which];
                    updatePriorityButton();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePriorityButton() {
        priorityButton.setText("Priority: " + priority);
        switch (priority) {
            case "High":
                priorityButton.setIconResource(R.drawable.ic_priority_high);
                break;
            case "Medium":
                priorityButton.setIconResource(R.drawable.ic_priority_medium);
                break;
            case "Low":
                priorityButton.setIconResource(R.drawable.ic_priority_low);
                break;
            default:
                priorityButton.setIconResource(R.drawable.ic_priority_low);
                break;
        }
    }

    private void showTaskTypeDialog() {
        String[] taskTypes = {"None", "⏰ Reminder", "🎯 Focus Task"};
        int currentIndex = 0;
        if (taskType.equals("Reminder")) currentIndex = 1;
        else if (taskType.equals("Focus Task")) currentIndex = 2;

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Configure as Task")
                .setSingleChoiceItems(taskTypes, currentIndex, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            taskType = "None";
                            break;
                        case 1:
                            taskType = "Reminder";
                            break;
                        case 2:
                            taskType = "Focus Task";
                            break;
                    }
                    updateTaskTypeButton();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTaskTypeButton() {
        if (taskType.equals("None")) {
            taskTypeButton.setText("Task Type: None");
            taskTypeButton.setIconResource(R.drawable.ic_reminder);
        } else if (taskType.equals("Reminder")) {
            taskTypeButton.setText("Task Type: ⏰ Reminder");
            taskTypeButton.setIconResource(R.drawable.ic_reminder);
        } else {
            taskTypeButton.setText("Task Type: 🎯 Focus Task");
            taskTypeButton.setIconResource(R.drawable.ic_focus);
        }
    }

    private void showTextFormatDialog() {
        Dialog formatDialog = new Dialog(getContext());
        formatDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        formatDialog.setContentView(R.layout.dialog_text_format);

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

        // Apply text size
        noteDescriptionInput.setTextSize(textSize);

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
}
