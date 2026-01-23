package com.example.mainactivity;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * AI-Powered Natural Language Task Creation Bottom Sheet.
 * Allows users to describe tasks in natural language and automatically
 * extracts task name, time, priority, category, and generates subtasks.
 */
public class NaturalLanguageTaskBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText nlInputEditText;
    private MaterialCardView parseResultsCard;
    private MaterialCardView subtasksCard;
    private TextView extractedTaskName;
    private TextView extractedTime;
    private TextView timeContext;
    private TextView extractedPriority;
    private TextView priorityEmoji;
    private TextView confidenceEmoji;
    private TextView confidenceText;
    private Chip categoryChip;
    private LinearLayout subtasksContainer;
    private MaterialButton parseButton;
    private MaterialButton createButton;
    private MaterialCardView timeCard;

    private AIModelHelper aiHelper;
    private AIModelHelper.NLParseResult currentParseResult;
    private List<String> selectedSubtasks = new ArrayList<>();
    
    private OnTaskCreatedListener listener;

    public interface OnTaskCreatedListener {
        void onTaskCreated();
    }

    public void setOnTaskCreatedListener(OnTaskCreatedListener listener) {
        this.listener = listener;
    }

    public static NaturalLanguageTaskBottomSheet newInstance() {
        return new NaturalLanguageTaskBottomSheet();
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_nl_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Expand bottom sheet by default
        if (getDialog() != null) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setSkipCollapsed(true);
        }

        aiHelper = AIModelHelper.getInstance(requireContext());
        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        nlInputEditText = view.findViewById(R.id.nlInputEditText);
        parseResultsCard = view.findViewById(R.id.parseResultsCard);
        subtasksCard = view.findViewById(R.id.subtasksCard);
        extractedTaskName = view.findViewById(R.id.extractedTaskName);
        extractedTime = view.findViewById(R.id.extractedTime);
        timeContext = view.findViewById(R.id.timeContext);
        extractedPriority = view.findViewById(R.id.extractedPriority);
        priorityEmoji = view.findViewById(R.id.priorityEmoji);
        confidenceEmoji = view.findViewById(R.id.confidenceEmoji);
        confidenceText = view.findViewById(R.id.confidenceText);
        categoryChip = view.findViewById(R.id.categoryChip);
        subtasksContainer = view.findViewById(R.id.subtasksContainer);
        parseButton = view.findViewById(R.id.parseButton);
        createButton = view.findViewById(R.id.createButton);
        timeCard = view.findViewById(R.id.timeCard);

        // Close button
        view.findViewById(R.id.closeButton).setOnClickListener(v -> dismiss());
    }

    private void setupListeners() {
        // Text change listener for real-time parsing hints
        nlInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                boolean hasText = s != null && s.length() > 3;
                parseButton.setEnabled(hasText);
                
                // Auto-parse after user stops typing (debounced)
                if (hasText && s.length() > 10) {
                    nlInputEditText.removeCallbacks(parseRunnable);
                    nlInputEditText.postDelayed(parseRunnable, 800);
                }
            }
        });

        // Parse button
        parseButton.setOnClickListener(v -> parseInput());

        // Create button
        createButton.setOnClickListener(v -> createTask());
        
        // Time card click to adjust time
        if (timeCard != null) {
            timeCard.setOnClickListener(v -> showTimeAdjustmentDialog());
        }
    }

    private final Runnable parseRunnable = this::parseInput;

    private void parseInput() {
        String input = nlInputEditText.getText() != null ? nlInputEditText.getText().toString().trim() : "";
        
        if (input.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a task description", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        parseButton.setText("🔄 Parsing...");
        parseButton.setEnabled(false);

        // Parse with AI
        nlInputEditText.postDelayed(() -> {
            currentParseResult = aiHelper.parseNaturalLanguageTask(input);
            displayParseResults();
            
            parseButton.setText("🔍 Parse");
            parseButton.setEnabled(true);
        }, 300); // Small delay for UX
    }

    private void displayParseResults() {
        if (currentParseResult == null) return;

        // Show results card with animation
        parseResultsCard.setVisibility(View.VISIBLE);
        parseResultsCard.setAlpha(0f);
        parseResultsCard.animate().alpha(1f).setDuration(300).start();

        // Confidence
        confidenceEmoji.setText(currentParseResult.getConfidenceEmoji());
        confidenceText.setText(currentParseResult.getConfidenceText());

        // Task name
        extractedTaskName.setText(currentParseResult.extractedTaskName);

        // Time and Date display
        if (currentParseResult.hasTime) {
            extractedTime.setText(currentParseResult.getFormattedTime());
            
            // Build context text including date if available
            String contextText;
            if (currentParseResult.hasDate && !currentParseResult.dateContext.isEmpty()) {
                // Show the date context (Tomorrow, Next Monday, etc.)
                contextText = currentParseResult.dateContext;
            } else {
                switch (currentParseResult.timeContext) {
                    case "before":
                        contextText = "Set 15 min before";
                        break;
                    case "at":
                        contextText = "Exactly at";
                        break;
                    case "by":
                        contextText = "Deadline";
                        break;
                    case "in":
                        contextText = "From now";
                        break;
                    case "time_of_day":
                        contextText = "Suggested time";
                        break;
                    default:
                        contextText = "Today";
                }
            }
            timeContext.setText(contextText);
        } else {
            // Use current time + 1 hour as default
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, 1);
            int hour = cal.get(Calendar.HOUR);
            if (hour == 0) hour = 12;
            int minute = cal.get(Calendar.MINUTE);
            String amPm = cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
            
            currentParseResult.extractedHour = hour;
            currentParseResult.extractedMinute = minute;
            currentParseResult.extractedAmPm = amPm;
            currentParseResult.hasTime = true;
            
            extractedTime.setText(String.format(Locale.getDefault(), "%d:%02d %s", hour, minute, amPm));
            timeContext.setText("Default (1 hour from now)");
        }

        // Priority
        if (currentParseResult.predictedPriority != null) {
            String priority = currentParseResult.predictedPriority.getValue();
            extractedPriority.setText(priority);
            
            switch (priority) {
                case "High":
                    priorityEmoji.setText("🔴");
                    break;
                case "Medium":
                    priorityEmoji.setText("🟡");
                    break;
                case "Low":
                    priorityEmoji.setText("🟢");
                    break;
                default:
                    priorityEmoji.setText("⚪");
            }
        }

        // Category
        if (currentParseResult.predictedCategory != null) {
            String category = currentParseResult.predictedCategory.getValue();
            String emoji = getCategoryEmoji(category);
            categoryChip.setText(emoji + " " + category);
        }

        // Display subtasks
        displaySubtasks();

        // Enable create button
        createButton.setEnabled(true);
    }

    private void displaySubtasks() {
        if (currentParseResult == null || currentParseResult.suggestedSubtasks.isEmpty()) {
            subtasksCard.setVisibility(View.GONE);
            return;
        }

        subtasksCard.setVisibility(View.VISIBLE);
        subtasksCard.setAlpha(0f);
        subtasksCard.animate().alpha(1f).setDuration(300).setStartDelay(100).start();

        subtasksContainer.removeAllViews();
        selectedSubtasks.clear();

        for (String subtask : currentParseResult.suggestedSubtasks) {
            View subtaskView = createSubtaskView(subtask);
            subtasksContainer.addView(subtaskView);
            selectedSubtasks.add(subtask); // Select all by default
        }
    }

    private View createSubtaskView(String subtask) {
        Context context = getContext();
        if (context == null) return new View(requireContext());

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        layout.setPadding(0, 8, 0, 8);

        CheckBox checkBox = new CheckBox(context);
        checkBox.setChecked(true);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedSubtasks.contains(subtask)) {
                    selectedSubtasks.add(subtask);
                }
            } else {
                selectedSubtasks.remove(subtask);
            }
        });

        TextView textView = new TextView(context);
        textView.setText(subtask);
        textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setPadding(8, 0, 0, 0);

        layout.addView(checkBox);
        layout.addView(textView);

        return layout;
    }

    private void showTimeAdjustmentDialog() {
        // Could show a time picker dialog here
        // For now, just show a toast
        Toast.makeText(getContext(), "Tap Create to use this time, or edit your input", Toast.LENGTH_SHORT).show();
    }

    private void createTask() {
        if (currentParseResult == null) {
            Toast.makeText(getContext(), "Please parse your input first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the intelligent date/time validation from NLParseResult
        if (currentParseResult.isInPast()) {
            Toast.makeText(getContext(), "⚠️ Cannot set time in the past", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the main task
        Task newTask = new Task();
        newTask.name = currentParseResult.extractedTaskName;
        newTask.hour = currentParseResult.extractedHour;
        newTask.minute = currentParseResult.extractedMinute;
        newTask.amPm = currentParseResult.extractedAmPm;
        newTask.taskType = "reminder";
        newTask.isAlarmOn = true;
        newTask.vibrationEnabled = true;
        
        // Set date from AI extraction (handles "tomorrow", "next Monday", etc.)
        if (currentParseResult.hasDate && !currentParseResult.extractedDate.isEmpty()) {
            newTask.date = currentParseResult.extractedDate;
        } else {
            // Use the intelligent calendar which handles day offset
            java.util.Calendar taskCal = currentParseResult.getExtractedCalendar();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            newTask.date = sdf.format(taskCal.getTime());
        }
        
        // Set priority
        if (currentParseResult.predictedPriority != null) {
            newTask.urgency = currentParseResult.predictedPriority.getValue();
        } else {
            newTask.urgency = "Medium";
        }
        
        // Set category
        if (currentParseResult.predictedCategory != null) {
            newTask.category = currentParseResult.predictedCategory.getValue();
        }
        
        // Determine time category
        if (currentParseResult.extractedAmPm.equals("AM")) {
            newTask.timeCategory = "morning";
        } else if (currentParseResult.extractedHour == 12 || 
                   (currentParseResult.extractedHour >= 1 && currentParseResult.extractedHour < 6)) {
            newTask.timeCategory = "afternoon";
        } else {
            newTask.timeCategory = "night";
        }
        
        newTask.selectedDays = new boolean[7];

        // Save to repository
        TaskRepository repository = TaskRepository.getInstance();
        repository.addTask(newTask);

        // Schedule alarm
        AlarmHelper.scheduleTaskAlarm(requireContext(), newTask);

        // Create subtasks as linked notes (shown when alarm goes off)
        if (!selectedSubtasks.isEmpty()) {
            createSubtasksAsNotes(newTask);
        }

        // Record for AI learning
        Calendar now = Calendar.getInstance();
        aiHelper.recordTaskCompletion(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.DAY_OF_WEEK));

        // Show success with animation
        Toast.makeText(getContext(), "✨ Task created with AI assistance!", Toast.LENGTH_SHORT).show();

        // Notify listener
        if (listener != null) {
            listener.onTaskCreated();
        }

        dismiss();
    }

    /**
     * Creates AI-suggested subtasks as linked Notes that will be displayed
     * when the task's alarm goes off.
     */
    private void createSubtasksAsNotes(Task parentTask) {
        NoteDao noteDao = TaskDatabase.getInstance(requireContext()).noteDao();
        
        // Build the checklist content
        StringBuilder checklistContent = new StringBuilder();
        for (String subtaskText : selectedSubtasks) {
            // Keep the emoji and format as checklist
            checklistContent.append("☐ ").append(subtaskText).append("\n");
        }
        
        // Create a single note with all subtasks as a checklist
        Note linkedNote = new Note();
        linkedNote.title = "📋 " + parentTask.name + " - Checklist";
        linkedNote.description = checklistContent.toString().trim();
        linkedNote.isChecklist = true;
        linkedNote.linkedTaskId = parentTask.id;
        linkedNote.priority = parentTask.urgency;
        linkedNote.createdTimestamp = System.currentTimeMillis();
        linkedNote.modifiedTimestamp = System.currentTimeMillis();
        
        noteDao.insert(linkedNote);
    }

    private String getCategoryEmoji(String category) {
        switch (category) {
            case "Work": return "💼";
            case "Personal": return "🏠";
            case "Health": return "💪";
            case "Finance": return "💰";
            case "Learning": return "📚";
            default: return "📋";
        }
    }
}
