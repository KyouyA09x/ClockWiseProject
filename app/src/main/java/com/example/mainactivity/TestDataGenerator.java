package com.example.mainactivity;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Utility class for generating test data to populate the app
 * for testing purposes.
 */
public class TestDataGenerator {

    private static final String[] TASK_NAMES = {
            "Team meeting",
            "Review project proposal",
            "Send weekly report",
            "Call with client",
            "Update documentation",
            "Code review session",
            "Plan sprint backlog",
            "Design review",
            "Submit expense report",
            "Prepare presentation",
            "Doctor appointment",
            "Pick up groceries",
            "Gym workout",
            "Read emails",
            "Pay bills"
    };

    private static final String[] FOCUS_SESSION_NAMES = {
            "Deep work: Feature development",
            "Writing technical docs",
            "Learning new framework",
            "Bug fixing session",
            "Research and planning",
            "Creative brainstorming",
            "Study session",
            "Project architecture design",
            "Performance optimization",
            "Testing and QA",
            "Course study time",
            "Writing blog post",
            "UI design work"
    };

    private static final String[] NOTE_TITLES = {
            "Meeting Notes",
            "Project Ideas",
            "To-Do List",
            "Quick Thoughts",
            "Shopping List",
            "Goals for the Week",
            "Book Recommendations",
            "Travel Plans",
            "Recipe Ideas",
            "Learning Resources"
    };

    private static final String[] NOTE_DESCRIPTIONS = {
            "Remember to follow up on the key points discussed.",
            "These are some initial ideas that need further exploration.",
            "Complete these tasks by end of the week.",
            "Just some random thoughts to capture for later.",
            "Items to pick up from the store.",
            "Focus on achieving these goals this week.",
            "Books that were recommended by colleagues.",
            "Places to visit and things to do.",
            "Try out these new recipes over the weekend.",
            "Useful links and resources for learning."
    };

    private static final String[] TASK_NOTES = {
            "Don't forget to prepare the presentation slides beforehand.",
            "Make sure to review the agenda before the meeting.",
            "Remember to bring your laptop charger.",
            "Check email for any updates before starting.",
            "Take notes during the session for future reference.",
            "Follow up with team members after completion.",
            "Set up the meeting room 10 minutes early.",
            "Prepare questions to ask during the discussion."
    };

    private static final String[] PRIORITIES = {"None", "Low", "Medium", "High"};
    private static final String[] TASK_TYPES = {"None", "Reminder", "Focus Task"};

    private static final String[] CHECKLIST_ITEMS = {
            "[ ] Buy groceries\n[x] Clean the house\n[ ] Pay bills\n[ ] Call mom",
            "[ ] Review code\n[ ] Write tests\n[x] Update documentation\n[ ] Deploy to staging",
            "[x] Morning workout\n[ ] Read 30 pages\n[ ] Meditate\n[x] Journal",
            "[ ] Pick up dry cleaning\n[ ] Mail package\n[x] Book appointment\n[ ] Get haircut",
            "[ ] Research topic\n[ ] Create outline\n[ ] Write first draft\n[ ] Review and edit"
    };

    private final Context context;
    private final TaskRepository taskRepository;
    private final NoteDao noteDao;
    private final Random random;
    private final SimpleDateFormat dateFormat;

    public TestDataGenerator(Context context) {
        this.context = context;
        this.taskRepository = TaskRepository.getInstance();
        this.taskRepository.initialize(context);
        this.noteDao = TaskDatabase.getInstance(context).noteDao();
        this.random = new Random();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    /**
     * Populates the app with comprehensive test data including:
     * - Today's ongoing tasks
     * - Today's completed tasks
     * - Upcoming tasks (future dates)
     * - Historical completed tasks (past months for history view)
     * - Notes with checklists
     */
    public void populateTestData() {
        // Generate tasks for TODAY (current)
        generateTodayTasks(4);
        generateTodayFocusSessions(2);
        
        // Generate COMPLETED tasks for today
        generateCompletedTodayTasks(3);
        
        // Generate UPCOMING tasks (future dates)
        generateUpcomingTasks(6);
        generateUpcomingFocusSessions(4);
        
        // Generate HISTORICAL completed tasks (past months for history)
        generateHistoricalTasks(40);
        
        // Generate notes
        generateNotes(6);
    }

    /**
     * Generate tasks for today (incomplete)
     */
    private void generateTodayTasks(int count) {
        Calendar calendar = Calendar.getInstance();
        String today = dateFormat.format(calendar.getTime());
        
        for (int i = 0; i < count; i++) {
            Task task = createBaseTask();
            task.name = TASK_NAMES[i % TASK_NAMES.length];
            task.date = today;
            
            // Spread across morning, afternoon, night
            int hour24;
            if (i % 3 == 0) {
                hour24 = 8 + random.nextInt(4); // Morning: 8-11 AM
                task.timeCategory = "morning";
            } else if (i % 3 == 1) {
                hour24 = 12 + random.nextInt(5); // Afternoon: 12-4 PM
                task.timeCategory = "afternoon";
            } else {
                hour24 = 17 + random.nextInt(4); // Night: 5-8 PM
                task.timeCategory = "night";
            }
            
            setTaskTime(task, hour24);
            task.urgency = PRIORITIES[1 + random.nextInt(3)]; // Low, Medium, or High
            task.taskType = "reminder";
            task.isComplete = false;
            
            taskRepository.addTask(task);
        }
    }

    /**
     * Generate focus sessions for today (incomplete)
     */
    private void generateTodayFocusSessions(int count) {
        Calendar calendar = Calendar.getInstance();
        String today = dateFormat.format(calendar.getTime());
        
        for (int i = 0; i < count; i++) {
            Task focusTask = createBaseFocusTask();
            focusTask.name = FOCUS_SESSION_NAMES[i % FOCUS_SESSION_NAMES.length];
            focusTask.date = today;
            
            int startHour24 = 9 + (i * 3); // Spaced out: 9 AM, 12 PM, etc.
            if (startHour24 > 18) startHour24 = 14;
            
            setFocusTaskTime(focusTask, startHour24, 1 + random.nextInt(2));
            focusTask.urgency = PRIORITIES[2 + random.nextInt(2)]; // Medium or High
            focusTask.isComplete = false;
            
            if (startHour24 < 12) {
                focusTask.timeCategory = "morning";
            } else if (startHour24 < 17) {
                focusTask.timeCategory = "afternoon";
            } else {
                focusTask.timeCategory = "night";
            }
            
            taskRepository.addTask(focusTask);
        }
    }

    /**
     * Generate completed tasks for today (for progress tracker)
     */
    private void generateCompletedTodayTasks(int count) {
        Calendar calendar = Calendar.getInstance();
        String today = dateFormat.format(calendar.getTime());
        
        for (int i = 0; i < count; i++) {
            Task task = createBaseTask();
            task.name = TASK_NAMES[(i + 5) % TASK_NAMES.length] + " ✓";
            task.date = today;
            
            int hour24 = 7 + (i * 2); // Earlier in the day
            setTaskTime(task, hour24);
            
            if (hour24 < 12) {
                task.timeCategory = "morning";
            } else if (hour24 < 17) {
                task.timeCategory = "afternoon";
            } else {
                task.timeCategory = "night";
            }
            
            task.urgency = PRIORITIES[random.nextInt(4)];
            task.taskType = "reminder";
            task.isComplete = true; // Already completed
            
            taskRepository.addTask(task);
        }
    }

    /**
     * Generate upcoming tasks for future dates
     */
    private void generateUpcomingTasks(int count) {
        Calendar calendar = Calendar.getInstance();
        
        for (int i = 0; i < count; i++) {
            Task task = createBaseTask();
            task.name = TASK_NAMES[random.nextInt(TASK_NAMES.length)];
            
            // Set date to 1-14 days in future
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, 1 + random.nextInt(14));
            task.date = dateFormat.format(calendar.getTime());
            
            int hour24 = 8 + random.nextInt(12);
            setTaskTime(task, hour24);
            
            if (hour24 < 12) {
                task.timeCategory = "morning";
            } else if (hour24 < 17) {
                task.timeCategory = "afternoon";
            } else {
                task.timeCategory = "night";
            }
            
            task.urgency = PRIORITIES[random.nextInt(4)];
            task.taskType = "reminder";
            task.isComplete = false;
            
            if (random.nextBoolean()) {
                task.noteContent = TASK_NOTES[random.nextInt(TASK_NOTES.length)];
            }
            
            taskRepository.addTask(task);
        }
    }

    /**
     * Generate upcoming focus sessions for future dates
     */
    private void generateUpcomingFocusSessions(int count) {
        Calendar calendar = Calendar.getInstance();
        
        for (int i = 0; i < count; i++) {
            Task focusTask = createBaseFocusTask();
            focusTask.name = FOCUS_SESSION_NAMES[random.nextInt(FOCUS_SESSION_NAMES.length)];
            
            // Set date to 1-10 days in future
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, 1 + random.nextInt(10));
            focusTask.date = dateFormat.format(calendar.getTime());
            
            int startHour24 = 9 + random.nextInt(8);
            setFocusTaskTime(focusTask, startHour24, 1 + random.nextInt(3));
            
            if (startHour24 < 12) {
                focusTask.timeCategory = "morning";
            } else if (startHour24 < 17) {
                focusTask.timeCategory = "afternoon";
            } else {
                focusTask.timeCategory = "night";
            }
            
            focusTask.urgency = PRIORITIES[1 + random.nextInt(3)];
            focusTask.isComplete = false;
            
            taskRepository.addTask(focusTask);
        }
    }

    /**
     * Generate historical completed tasks for past months (for History view)
     */
    private void generateHistoricalTasks(int count) {
        Calendar calendar = Calendar.getInstance();
        
        // Generate tasks spread across the past 6 months
        for (int i = 0; i < count; i++) {
            boolean isFocusSession = random.nextBoolean();
            Task task;
            
            if (isFocusSession) {
                task = createBaseFocusTask();
                task.name = FOCUS_SESSION_NAMES[random.nextInt(FOCUS_SESSION_NAMES.length)];
            } else {
                task = createBaseTask();
                task.name = TASK_NAMES[random.nextInt(TASK_NAMES.length)];
                task.taskType = "reminder";
            }
            
            // Set date to random day in past 6 months
            calendar.setTime(new Date());
            int daysBack = 1 + random.nextInt(180); // 1-180 days ago
            calendar.add(Calendar.DAY_OF_YEAR, -daysBack);
            task.date = dateFormat.format(calendar.getTime());
            
            int hour24 = 8 + random.nextInt(12);
            if (isFocusSession) {
                setFocusTaskTime(task, hour24, 1 + random.nextInt(3));
            } else {
                setTaskTime(task, hour24);
            }
            
            if (hour24 < 12) {
                task.timeCategory = "morning";
            } else if (hour24 < 17) {
                task.timeCategory = "afternoon";
            } else {
                task.timeCategory = "night";
            }
            
            task.urgency = PRIORITIES[random.nextInt(4)];
            task.isComplete = true; // All historical tasks are completed
            task.isAlarmOn = false; // Don't trigger alarms for past tasks
            
            if (random.nextInt(3) == 0) {
                task.noteContent = TASK_NOTES[random.nextInt(TASK_NOTES.length)];
            }
            
            taskRepository.addTask(task);
        }
    }

    /**
     * Create a base task with common defaults
     */
    private Task createBaseTask() {
        Task task = new Task();
        task.vibrationEnabled = random.nextBoolean();
        task.isAlarmOn = true;
        task.selectedDays = new boolean[7];
        return task;
    }

    /**
     * Create a base focus task with common defaults
     */
    private Task createBaseFocusTask() {
        Task task = createBaseTask();
        task.taskType = "focus";
        return task;
    }

    /**
     * Set task time in 12-hour format
     */
    private void setTaskTime(Task task, int hour24) {
        int minute = random.nextBoolean() ? 0 : 30;
        
        if (hour24 == 0) {
            task.hour = 12;
            task.amPm = "AM";
        } else if (hour24 < 12) {
            task.hour = hour24;
            task.amPm = "AM";
        } else if (hour24 == 12) {
            task.hour = 12;
            task.amPm = "PM";
        } else {
            task.hour = hour24 - 12;
            task.amPm = "PM";
        }
        task.minute = minute;
    }

    /**
     * Set focus task start and end times
     */
    private void setFocusTaskTime(Task task, int startHour24, int durationHours) {
        int startMinute = random.nextBoolean() ? 0 : 30;
        
        // Set start time
        if (startHour24 == 0) {
            task.hour = 12;
            task.amPm = "AM";
        } else if (startHour24 < 12) {
            task.hour = startHour24;
            task.amPm = "AM";
        } else if (startHour24 == 12) {
            task.hour = 12;
            task.amPm = "PM";
        } else {
            task.hour = startHour24 - 12;
            task.amPm = "PM";
        }
        task.minute = startMinute;
        
        // Set end time
        int endHour24 = startHour24 + durationHours;
        
        if (endHour24 == 0) {
            task.endHour = 12;
            task.endAmPm = "AM";
        } else if (endHour24 < 12) {
            task.endHour = endHour24;
            task.endAmPm = "AM";
        } else if (endHour24 == 12) {
            task.endHour = 12;
            task.endAmPm = "PM";
        } else if (endHour24 < 24) {
            task.endHour = endHour24 - 12;
            task.endAmPm = "PM";
        } else {
            task.endHour = endHour24 - 24;
            task.endAmPm = "AM";
        }
        task.endMinute = startMinute;
    }

    /**
     * Generate notes with some being checklists
     */
    private void generateNotes(int count) {
        for (int i = 0; i < count; i++) {
            Note note = new Note();
            note.title = NOTE_TITLES[i % NOTE_TITLES.length];
            
            // Make some notes checklists
            boolean makeChecklist = (i % 3 == 0);
            note.isChecklist = makeChecklist;
            
            if (makeChecklist) {
                note.description = CHECKLIST_ITEMS[i % CHECKLIST_ITEMS.length];
            } else {
                note.description = NOTE_DESCRIPTIONS[i % NOTE_DESCRIPTIONS.length];
            }
            
            // Random timestamps (within last 7 days)
            long now = System.currentTimeMillis();
            long randomOffset = (long) (random.nextDouble() * 7 * 24 * 60 * 60 * 1000);
            note.createdTimestamp = now - randomOffset;
            note.modifiedTimestamp = now - (long) (random.nextDouble() * randomOffset);
            
            // Random formatting
            note.fontFamily = "default";
            note.textSize = 14 + random.nextInt(6);
            note.textColor = "#000000";
            note.isBold = random.nextBoolean() && !makeChecklist;
            note.isItalic = random.nextBoolean() && !makeChecklist;
            note.isUnderline = false;
            
            // Random priority and task type
            note.priority = PRIORITIES[random.nextInt(PRIORITIES.length)];
            note.taskType = TASK_TYPES[random.nextInt(TASK_TYPES.length)];
            note.isConvertedToTask = note.taskType.equals("Reminder") || note.taskType.equals("Focus Task");
            
            // Insert note
            new Thread(() -> noteDao.insert(note)).start();
        }
    }

    /**
     * Clears all test data (tasks and notes)
     */
    public void clearAllData() {
        // Clear all tasks
        List<Task> allTasks = taskRepository.getAllTasks();
        if (allTasks != null) {
            for (Task task : allTasks) {
                // Cancel any scheduled alarms first
                if (task.isFocusTask()) {
                    AlarmHelper.cancelFocusTaskAlarms(context, task);
                } else {
                    AlarmHelper.cancelTaskAlarm(context, task);
                }
                taskRepository.deleteTask(task);
            }
        }
        
        // Clear all notes
        new Thread(() -> {
            List<Note> allNotes = noteDao.getAllNotes();
            if (allNotes != null) {
                for (Note note : allNotes) {
                    noteDao.delete(note);
                }
            }
        }).start();
    }
}
