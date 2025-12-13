package com.example.mainactivity;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotepadFragment extends Fragment {

    private View emptyStateNotes;
    private LinearLayout notesContainer;
    private ExtendedFloatingActionButton fabAddNote;
    private NoteDao noteDao;
    private List<Note> notesList = new ArrayList<>();
    private boolean isSelectionMode = false;
    private final List<Note> selectedNotes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notepad, container, false);

        initViews(view);
        noteDao = TaskDatabase.getInstance(requireContext()).noteDao();
        
        fabAddNote.setOnClickListener(v -> showAddNoteDialog(null));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isSelectionMode) {
            refreshNotes();
        }
    }

    private void initViews(View view) {
        emptyStateNotes = view.findViewById(R.id.emptyStateNotes);
        notesContainer = view.findViewById(R.id.notesContainer);
        fabAddNote = view.findViewById(R.id.fabAddNote);
    }

    public void enableSelectionMode() {
        if (noteDao == null || getContext() == null) return;

        notesList = noteDao.getAllNotes();
        
        // Check if notepad is empty
        if (notesList.isEmpty()) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Notepad Empty")
                    .setMessage("No notes available to convert. Would you like to create a new note?")
                    .setPositiveButton("Create Note", (dialog, which) -> showAddNoteDialog(null))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        isSelectionMode = true;
        selectedNotes.clear();
        refreshNotesInSelectionMode();
    }

    private void refreshNotesInSelectionMode() {
        notesContainer.removeAllViews();
        emptyStateNotes.setVisibility(View.GONE);
        notesContainer.setVisibility(View.VISIBLE);

        for (Note note : notesList) {
            View noteView = createNoteViewWithCheckbox(note);
            notesContainer.addView(noteView);
        }

        // Change FAB to confirm button
        fabAddNote.setText("Convert Selected");
        fabAddNote.setIcon(null);
        fabAddNote.setOnClickListener(v -> convertSelectedNotesToTasks());
    }

    private View createNoteViewWithCheckbox(Note note) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View noteView = inflater.inflate(R.layout.note_item, notesContainer, false);

        // Set proper layout params with margins
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (12 * getResources().getDisplayMetrics().density);
        params.setMargins(0, margin, 0, margin);
        noteView.setLayoutParams(params);

        TextView titleView = noteView.findViewById(R.id.noteTitle);
        TextView descriptionView = noteView.findViewById(R.id.noteDescription);
        ImageView deleteButton = noteView.findViewById(R.id.deleteNoteButton);

        // Hide delete button and show checkbox
        deleteButton.setVisibility(View.GONE);

        // Add checkbox programmatically
        com.google.android.material.checkbox.MaterialCheckBox checkbox = new com.google.android.material.checkbox.MaterialCheckBox(getContext());
        checkbox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Add checkbox to the note card
        if (noteView instanceof com.google.android.material.card.MaterialCardView) {
            LinearLayout contentLayout = (LinearLayout) ((com.google.android.material.card.MaterialCardView) noteView).getChildAt(0);
            LinearLayout headerLayout = (LinearLayout) contentLayout.getChildAt(0);
            headerLayout.addView(checkbox);
        }

        titleView.setText(note.title != null && !note.title.isEmpty() ? note.title : "Untitled");
        descriptionView.setText(note.description != null && !note.description.isEmpty() ? note.description : "No description");

        // Handle checkbox selection
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedNotes.contains(note)) {
                    selectedNotes.add(note);
                }
            } else {
                selectedNotes.remove(note);
            }
        });

        // Make the whole card clickable to toggle checkbox
        noteView.setOnClickListener(v -> checkbox.setChecked(!checkbox.isChecked()));

        return noteView;
    }

    private void convertSelectedNotesToTasks() {
        if (selectedNotes.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one note", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert selected notes to tasks
        for (Note note : selectedNotes) {
            // Create a reminder task from note
            Task task = new Task();
            task.name = note.title != null && !note.title.isEmpty() ? note.title : "Task from Note";
            task.taskType = "reminder";
            task.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            task.hour = 9;
            task.minute = 0;
            task.amPm = "AM";
            task.isAlarmOn = true;
            // Use note priority for task urgency to match colors
            task.urgency = note.priority != null ? note.priority : "None";
            task.selectedDays = new boolean[7]; // No repeat by default

            TaskRepository.getInstance().addTask(task);
            
            // Mark note as converted to task
            note.isConvertedToTask = true;
            noteDao.update(note);
        }

        Toast.makeText(getContext(), selectedNotes.size() + " note(s) converted to task(s)", Toast.LENGTH_SHORT).show();
        
        // Exit selection mode
        exitSelectionMode();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedNotes.clear();
        fabAddNote.setText("Add Note");
        fabAddNote.setIcon(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_fab));
        fabAddNote.setOnClickListener(v -> showAddNoteDialog(null));
        refreshNotes();
    }

    public void refreshNotes() {
        if (noteDao == null || getContext() == null) return;

        notesList = noteDao.getAllNotes();
        notesContainer.removeAllViews();

        if (notesList.isEmpty()) {
            emptyStateNotes.setVisibility(View.VISIBLE);
            notesContainer.setVisibility(View.GONE);
        } else {
            emptyStateNotes.setVisibility(View.GONE);
            notesContainer.setVisibility(View.VISIBLE);

            for (Note note : notesList) {
                View noteView = createNoteView(note);
                notesContainer.addView(noteView);
            }
        }
    }

    private View createNoteView(Note note) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View noteView = inflater.inflate(R.layout.note_item, notesContainer, false);

        // Set proper layout params with margins
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (12 * getResources().getDisplayMetrics().density);
        params.setMargins(0, margin, 0, margin);
        noteView.setLayoutParams(params);

        TextView titleView = noteView.findViewById(R.id.noteTitle);
        TextView descriptionView = noteView.findViewById(R.id.noteDescription);
        TextView timestampView = noteView.findViewById(R.id.noteTimestamp);
        ImageView deleteButton = noteView.findViewById(R.id.deleteNoteButton);
        View priorityIndicator = noteView.findViewById(R.id.notePriorityIndicator);
        TextView taskTag = noteView.findViewById(R.id.noteTaskTag);

        titleView.setText(note.title != null && !note.title.isEmpty() ? note.title : "Untitled");
        descriptionView.setText(note.description != null && !note.description.isEmpty() ? note.description : "No description");

        // Show task tag based on task type
        if (taskTag != null && note.taskType != null && !note.taskType.equals("None")) {
            taskTag.setVisibility(View.VISIBLE);
            if (note.taskType.equals("Reminder")) {
                taskTag.setText("⏰ Reminder");
            } else if (note.taskType.equals("Focus Task")) {
                taskTag.setText("🎯 Focus");
            }
        }

        // Set priority color
        if (priorityIndicator != null) {
            String notePriority = note.priority != null ? note.priority : "None";
            switch (notePriority) {
                case "High":
                    priorityIndicator.setBackgroundResource(R.drawable.red_circle);
                    break;
                case "Medium":
                    priorityIndicator.setBackgroundResource(R.drawable.yellow_circle);
                    break;
                case "Low":
                    priorityIndicator.setBackgroundResource(R.drawable.green_circle);
                    break;
                default:
                    priorityIndicator.setVisibility(View.GONE);
                    break;
            }
        }

        // Apply formatting to description
        applyFormatting(descriptionView, note);

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        String timestamp = "Modified: " + sdf.format(new Date(note.modifiedTimestamp));
        timestampView.setText(timestamp);

        // Click to edit
        noteView.setOnClickListener(v -> showAddNoteDialog(note));

        // Delete button
        deleteButton.setOnClickListener(v -> showDeleteConfirmation(note));

        return noteView;
    }

    private void applyFormatting(TextView textView, Note note) {
        // Apply text size
        textView.setTextSize(note.textSize);

        // Apply font family
        switch (note.fontFamily) {
            case "serif":
                textView.setTypeface(Typeface.SERIF);
                break;
            case "monospace":
                textView.setTypeface(Typeface.MONOSPACE);
                break;
            default:
                textView.setTypeface(Typeface.DEFAULT);
                break;
        }

        // Apply text color
        try {
            textView.setTextColor(Color.parseColor(note.textColor));
        } catch (IllegalArgumentException e) {
            textView.setTextColor(Color.BLACK);
        }

        // Apply text style
        int style = Typeface.NORMAL;
        if (note.isBold && note.isItalic) {
            style = Typeface.BOLD_ITALIC;
        } else if (note.isBold) {
            style = Typeface.BOLD;
        } else if (note.isItalic) {
            style = Typeface.ITALIC;
        }
        textView.setTypeface(textView.getTypeface(), style);

        // Apply underline
        if (note.isUnderline) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
        }
    }

    private void showAddNoteDialog(Note noteToEdit) {
        AddNoteDialog dialog = new AddNoteDialog(requireContext(), noteToEdit, this::refreshNotes);
        dialog.show();
    }

    private void showDeleteConfirmation(Note note) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    noteDao.delete(note);
                    refreshNotes();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
