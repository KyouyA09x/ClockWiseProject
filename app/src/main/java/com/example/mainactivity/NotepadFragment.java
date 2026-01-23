package com.example.mainactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private ExtendedFloatingActionButton fabConvertSelected;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabCancelConvert;
    private NoteDao noteDao;
    private List<Note> notesList = new ArrayList<>();
    private boolean isSelectionMode = false;
    private final List<Note> selectedNotes = new ArrayList<>();
    
    // Broadcast receiver to refresh notes when added from floating button
    private final BroadcastReceiver notesRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.mainactivity.REFRESH_TASKS".equals(intent.getAction())) {
                refreshNotes();
            }
        }
    };

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
        // Register broadcast receiver for notes refresh
        IntentFilter filter = new IntentFilter("com.example.mainactivity.REFRESH_TASKS");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(notesRefreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(notesRefreshReceiver, filter);
        }
        if (!isSelectionMode) {
            refreshNotes();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        try {
            requireContext().unregisterReceiver(notesRefreshReceiver);
        } catch (Exception ignored) {}
    }

    private void initViews(View view) {
        emptyStateNotes = view.findViewById(R.id.emptyStateNotes);
        notesContainer = view.findViewById(R.id.notesContainer);
        fabAddNote = view.findViewById(R.id.fabAddNote);
        fabConvertSelected = view.findViewById(R.id.fabConvertSelected);
        fabCancelConvert = view.findViewById(R.id.fabCancelConvert);
        
        // Setup Convert and Cancel button listeners
        if (fabConvertSelected != null) {
            fabConvertSelected.setOnClickListener(v -> convertSelectedNotesToTasks());
        }
        if (fabCancelConvert != null) {
            fabCancelConvert.setOnClickListener(v -> {
                isSelectionMode = false;
                selectedNotes.clear();
                // Hide Convert and Cancel FABs, show Add Note FAB
                if (fabConvertSelected != null) fabConvertSelected.setVisibility(View.GONE);
                if (fabCancelConvert != null) fabCancelConvert.setVisibility(View.GONE);
                if (fabAddNote != null) fabAddNote.setVisibility(View.VISIBLE);
                refreshNotes();
            });
        }
        
        // Setup dynamic FAB positioning and content padding
        setupDynamicPadding(view);
    }
    
    private void setupDynamicPadding(View view) {
        if (fabAddNote == null) return;
        
        View contentContainer = view.findViewById(R.id.notepadContentContainer);
        
        // Post to ensure layout is complete
        view.post(() -> {
            // Get MainActivity to access bottom navigation
            android.app.Activity activity = getActivity();
            if (!(activity instanceof MainActivity)) return;
            
            MainActivity mainActivity = (MainActivity) activity;
            
            // Handle window insets for dynamic positioning
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                );
                
                // Get display metrics
                float density = getResources().getDisplayMetrics().density;
                
                // Calculate bottom navigation bar height - FAB needs to clear this
                int bottomNavHeight = 0;
                try {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                        mainActivity.findViewById(R.id.bottomNavigation);
                    if (bottomNav != null) {
                        bottomNav.post(() -> {
                            int navHeight = bottomNav.getHeight();
                            if (navHeight == 0) {
                                navHeight = (int) (56 * density);
                            }
                            updateFabPosition(navHeight, systemBars.bottom, density);
                            updateContentPadding(contentContainer, navHeight, systemBars.bottom, density);
                        });
                    } else {
                        bottomNavHeight = (int) (56 * density);
                        updateFabPosition(bottomNavHeight, systemBars.bottom, density);
                        updateContentPadding(contentContainer, bottomNavHeight, systemBars.bottom, density);
                    }
                } catch (Exception e) {
                    bottomNavHeight = (int) (56 * density);
                    updateFabPosition(bottomNavHeight, systemBars.bottom, density);
                    updateContentPadding(contentContainer, bottomNavHeight, systemBars.bottom, density);
                }
                
                return insets;
            });
            
            // Trigger insets application
            androidx.core.view.ViewCompat.requestApplyInsets(view);
        });
    }
    
    private void updateContentPadding(View container, int bottomNavHeight, int systemBarsBottom, float density) {
        if (container == null) return;
        
        // Reserve space for FAB + bottom nav + system bars
        int fabSpace = (int) (80 * density); // FAB height + margin
        int totalBottomPadding = bottomNavHeight + systemBarsBottom + fabSpace;
        
        container.setPadding(
            container.getPaddingLeft(),
            container.getPaddingTop(),
            container.getPaddingRight(),
            totalBottomPadding
        );
    }
    
    private void updateFabPosition(int bottomNavHeight, int systemBarsBottom, float density) {
        if (fabAddNote == null) return;
        
        // Standard FAB margin (16dp)
        int fabMargin = (int) (16 * density);
        
        // FAB needs to be ABOVE the bottom nav!
        // Total offset from screen bottom = system nav bar + bottom nav bar + margin
        int bottomOffset = systemBarsBottom + bottomNavHeight + fabMargin;
        
        // Update FAB positioning
        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams fabParams = 
            (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) fabAddNote.getLayoutParams();
        fabParams.bottomMargin = bottomOffset;
        fabParams.rightMargin = fabMargin;
        fabAddNote.setLayoutParams(fabParams);
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
        // Reload notes from database to get current state
        if (noteDao != null) {
            notesList = noteDao.getActiveNotes();
        }
        
        notesContainer.removeAllViews();
        emptyStateNotes.setVisibility(View.GONE);
        notesContainer.setVisibility(View.VISIBLE);

        for (Note note : notesList) {
            View noteView = createNoteViewWithCheckbox(note);
            notesContainer.addView(noteView);
        }

        // Hide Add Note FAB, show Convert and Cancel FABs in selection mode
        if (fabAddNote != null) {
            fabAddNote.setVisibility(View.GONE);
        }
        if (fabConvertSelected != null) {
            fabConvertSelected.setVisibility(View.VISIBLE);
        }
        if (fabCancelConvert != null) {
            fabCancelConvert.setVisibility(View.VISIBLE);
        }
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

        // Exit selection mode first
        isSelectionMode = false;
        
        // Convert each selected note, showing bottom sheet for configuration
        convertNextNote(0);
    }
    
    private void convertNextNote(int index) {
        if (index >= selectedNotes.size()) {
            // All notes converted, show success message and reset
            Toast.makeText(getContext(), selectedNotes.size() + " note(s) converted to task(s)", Toast.LENGTH_SHORT).show();
            selectedNotes.clear();
            
            // Restore Add Note FAB, hide Convert/Cancel FABs
            if (fabAddNote != null) {
                fabAddNote.setVisibility(View.VISIBLE);
            }
            if (fabConvertSelected != null) {
                fabConvertSelected.setVisibility(View.GONE);
            }
            if (fabCancelConvert != null) {
                fabCancelConvert.setVisibility(View.GONE);
            }
            
            // Refresh MainActivity to show converted tasks in Current/Upcoming
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshAllFragments();
            }
            
            refreshNotes();
            return;
        }
        
        Note note = selectedNotes.get(index);
        
        // Show task type chooser dialog to let user pick Focus Session or Reminder
        showTaskTypeChooserForNote(note, index);
    }
    
    private void showTaskTypeChooserForNote(Note note, int index) {
        if (getContext() == null) return;
        
        // Create dialog
        android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.setContentView(R.layout.dialog_task_type_chooser);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }
        
        // Get views
        com.google.android.material.card.MaterialCardView reminderOption = dialog.findViewById(R.id.reminderOption);
        com.google.android.material.card.MaterialCardView focusOption = dialog.findViewById(R.id.focusTaskOption);
        
        // Reminder option click
        if (reminderOption != null) {
            reminderOption.setOnClickListener(v -> {
                dialog.dismiss();
                convertNoteToReminder(note, index);
            });
        }
        
        // Focus Session option click
        if (focusOption != null) {
            focusOption.setOnClickListener(v -> {
                dialog.dismiss();
                convertNoteToFocusSession(note, index);
            });
        }
        
        dialog.show();
    }
    
    private void convertNoteToReminder(Note note, int index) {
        // Create task from note with note type and priority matching
        Task task = new Task();
        task.name = note.title != null && !note.title.isEmpty() ? note.title : "Task from Note";
        task.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        task.hour = 9;
        task.minute = 0;
        task.amPm = "AM";
        task.isAlarmOn = true;
        // Match note priority to task urgency
        task.urgency = note.priority != null ? note.priority : "None";
        task.selectedDays = new boolean[7];
        task.taskType = "reminder";
        
        // Show Reminder bottom sheet - allow date modification
        AddReminderBottomSheet bottomSheet = AddReminderBottomSheet.newInstanceWithData(task);
        bottomSheet.setOnTaskSavedListener(() -> {
            // Mark note as converted
            note.isConvertedToTask = true;
            noteDao.update(note);
            
            // Refresh MainActivity to show task immediately
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshAllFragments();
            }
            
            // Convert next note
            convertNextNote(index + 1);
        });
        bottomSheet.show(getParentFragmentManager(), "AddReminderBottomSheet");
    }
    
    private void convertNoteToFocusSession(Note note, int index) {
        // Create task from note with note type and priority matching
        Task task = new Task();
        task.name = note.title != null && !note.title.isEmpty() ? note.title : "Task from Note";
        task.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        task.hour = 9;
        task.minute = 0;
        task.amPm = "AM";
        task.isAlarmOn = true;
        // Match note priority to task urgency
        task.urgency = note.priority != null ? note.priority : "None";
        task.selectedDays = new boolean[7];
        task.taskType = "focus";
        task.endHour = 10;
        task.endMinute = 0;
        task.endAmPm = "AM";
        
        // Show Focus Task bottom sheet - allow date modification
        AddFocusTaskBottomSheet bottomSheet = AddFocusTaskBottomSheet.newInstanceWithData(task);
        bottomSheet.setOnTaskSavedListener(() -> {
            // Mark note as converted
            note.isConvertedToTask = true;
            noteDao.update(note);
            
            // Refresh MainActivity to show task immediately
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshAllFragments();
            }
            
            // Convert next note
            convertNextNote(index + 1);
        });
        bottomSheet.show(getParentFragmentManager(), "AddFocusTaskBottomSheet");
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

        notesList = noteDao.getActiveNotes(); // Only show non-deleted notes
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
        TextView dueDateView = noteView.findViewById(R.id.noteDueDate);
        ImageView deleteButton = noteView.findViewById(R.id.deleteNoteButton);
        View priorityIndicator = noteView.findViewById(R.id.notePriorityIndicator);
        TextView taskTag = noteView.findViewById(R.id.noteTaskTag);

        titleView.setText(note.title != null && !note.title.isEmpty() ? note.title : "Untitled");
        descriptionView.setText(note.description != null && !note.description.isEmpty() ? note.description : "No description");

        // Show due date if set
        if (dueDateView != null && note.dueDate != null && !note.dueDate.isEmpty()) {
            dueDateView.setVisibility(View.VISIBLE);
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(note.dueDate);
                if (date != null) {
                    dueDateView.setText("Due: " + outputFormat.format(date));
                }
            } catch (Exception e) {
                dueDateView.setText("Due: " + note.dueDate);
            }
        } else if (dueDateView != null) {
            dueDateView.setVisibility(View.GONE);
        }

        // Show task tag based on task type
        if (taskTag != null && note.taskType != null && !note.taskType.equals("None")) {
            taskTag.setVisibility(View.VISIBLE);
            if (note.taskType.equals("Reminder")) {
                taskTag.setText("⏰ Task");
            } else if (note.taskType.equals("Focus Task")) {
                taskTag.setText("🎯 Focus");
            }
        }

        // Set priority color and border
        if (priorityIndicator != null) {
            String notePriority = note.priority != null ? note.priority : "None";
            int borderColor;
            switch (notePriority) {
                case "High":
                    priorityIndicator.setBackgroundResource(R.drawable.red_circle);
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_high);
                    break;
                case "Medium":
                    priorityIndicator.setBackgroundResource(R.drawable.yellow_circle);
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_medium);
                    break;
                case "Low":
                    priorityIndicator.setBackgroundResource(R.drawable.green_circle);
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_low);
                    break;
                default:
                    priorityIndicator.setVisibility(View.GONE);
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_none);
                    break;
            }
            // Set border color on the card
            if (noteView instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) noteView).setStrokeColor(borderColor);
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

        // Long press with 3D touch effect for context menu
        noteView.setOnLongClickListener(v -> {
            // Apply 3D touch scale effect
            v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start();
                    
                    // Haptic feedback
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    
                    // Show context menu
                    showNoteContextMenu(note, v);
                })
                .start();
            return true;
        });

        // Delete button
        deleteButton.setOnClickListener(v -> showDeleteConfirmation(note, noteView));

        return noteView;
    }

    private void applyFormatting(TextView textView, Note note) {
        // Apply text size (using SP for proper DPI scaling)
        textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, note.textSize);

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
    
    public void showAddNoteDialog() {
        showAddNoteDialog(null);
    }

    private void showNoteContextMenu(Note note, View anchorView) {
        String[] options = {"✏️ Edit", "🗑️ Delete", "📋 Convert to Task"};
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(note.title != null && !note.title.isEmpty() ? note.title : "Note Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            showAddNoteDialog(note);
                            break;
                        case 1: // Delete
                            showDeleteConfirmation(note, anchorView);
                            break;
                        case 2: // Convert to Task
                            convertNoteToTask(note);
                            break;
                    }
                })
                .show();
    }

    private void convertNoteToTask(Note note) {
        // Create a task from this note
        Task task = new Task();
        task.name = note.title != null && !note.title.isEmpty() ? note.title : "Task from Note";
        task.taskType = "reminder";
        task.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        task.hour = 9;
        task.minute = 0;
        task.amPm = "AM";
        task.isAlarmOn = true;
        task.urgency = note.priority != null ? note.priority : "None";
        task.selectedDays = new boolean[7];

        TaskRepository.getInstance().addTask(task);
        
        // Mark note as converted
        note.isConvertedToTask = true;
        noteDao.update(note);
        
        Toast.makeText(getContext(), "Note converted to task!", Toast.LENGTH_SHORT).show();
        refreshNotes();
    }

    private void showDeleteConfirmation(Note note) {
        showDeleteConfirmation(note, null);
    }
    
    private void showDeleteConfirmation(Note note, View noteView) {
        String itemName = (note.title != null && !note.title.isEmpty()) ? note.title : "This note";
        
        ModernDialogHelper.showNonDestructiveDialog(
                requireContext(),
                "Move to Trash?",
                "{item} will be moved to the trash bin. You can restore it later.",
                itemName,
                "Move to Trash",
                R.drawable.ic_delete,
                () -> {
                    if (noteView != null) {
                        // Animate slide-to-right deletion
                        animateNoteDeletion(noteView, () -> {
                            // Update in background thread
                            new Thread(() -> {
                                noteDao.softDelete(note.id, System.currentTimeMillis());
                                
                                // Refresh on main thread
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        refreshNotes();
                                        android.widget.Toast.makeText(getContext(), "Note moved to trash", android.widget.Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }).start();
                        });
                    } else {
                        // Update in background thread
                        new Thread(() -> {
                            noteDao.softDelete(note.id, System.currentTimeMillis());
                            
                            // Refresh on main thread
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    refreshNotes();
                                    android.widget.Toast.makeText(getContext(), "Note moved to trash", android.widget.Toast.LENGTH_SHORT).show();
                                });
                            }
                        }).start();
                    }
                },
                null
        );
    }
    
    private void animateNoteDeletion(View noteView, Runnable onComplete) {
        noteView.animate()
            .translationX(noteView.getWidth())
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction(onComplete)
            .start();
    }
    
    /**
     * Formats checklist text for display with visual checkboxes
     * Converts [ ] to ☐ (unchecked) and [x] to ☑ (checked)
     */
    private String formatChecklistForDisplay(String checklistText) {
        if (checklistText == null) return "";
        
        StringBuilder formatted = new StringBuilder();
        String[] lines = checklistText.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("[x]") || line.startsWith("[X]")) {
                // Checked item
                formatted.append("☑ ").append(line.substring(3).trim());
            } else if (line.startsWith("[ ]")) {
                // Unchecked item
                formatted.append("☐ ").append(line.substring(3).trim());
            } else {
                // Regular line
                formatted.append(line);
            }
            
            if (i < lines.length - 1) {
                formatted.append("\n");
            }
        }
        
        return formatted.toString();
    }
}
