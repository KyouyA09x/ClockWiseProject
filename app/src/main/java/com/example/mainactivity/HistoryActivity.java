package com.example.mainactivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Activity to display task history organized by year and month sections.
 * Features a year switcher at the top and month-based grouping with statistics.
 */
public class HistoryActivity extends BaseThemedActivity {

    private TextView yearText;
    private ImageButton prevYearButton;
    private ImageButton nextYearButton;
    private LinearLayout monthSectionsContainer;
    private MaterialCardView summaryCard;
    private MaterialCardView emptyState;
    private TextView totalTasksCount;
    private TextView completedTasksCount;
    private TextView focusSessionsCount;
    private TextView productivityScoreText;

    private TaskRepository taskRepository;
    private int currentYear;
    private int minYear;
    private int maxYear;

    // Placeholder data for history when no real data exists
    private static final String[] PLACEHOLDER_TASK_NAMES = {
            "Team standup meeting",
            "Code review session",
            "Project planning",
            "Client presentation",
            "Documentation update",
            "Bug fixes",
            "Feature development",
            "Testing and QA",
            "Sprint retrospective",
            "Design review",
            "Research session",
            "Performance optimization"
    };

    private static final String[] PLACEHOLDER_FOCUS_NAMES = {
            "Deep work: API development",
            "Writing technical docs",
            "Learning new framework",
            "Architecture planning",
            "Creative brainstorming",
            "Study session",
            "UI/UX design work",
            "Data analysis"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initViews();
        setupToolbar();
        
        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);

        // Set current year
        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        minYear = currentYear - 5;  // Show up to 5 years back
        maxYear = currentYear;

        setupYearNavigation();
        loadHistoryForYear(currentYear);
    }

    private void initViews() {
        yearText = findViewById(R.id.yearText);
        prevYearButton = findViewById(R.id.prevYearButton);
        nextYearButton = findViewById(R.id.nextYearButton);
        monthSectionsContainer = findViewById(R.id.monthSectionsContainer);
        summaryCard = findViewById(R.id.summaryCard);
        emptyState = findViewById(R.id.emptyState);
        totalTasksCount = findViewById(R.id.totalTasksCount);
        completedTasksCount = findViewById(R.id.completedTasksCount);
        focusSessionsCount = findViewById(R.id.focusSessionsCount);
        productivityScoreText = findViewById(R.id.productivityScore);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupYearNavigation() {
        updateYearDisplay();

        if (prevYearButton != null) {
            prevYearButton.setOnClickListener(v -> {
                if (currentYear > minYear) {
                    currentYear--;
                    updateYearDisplay();
                    loadHistoryForYear(currentYear);
                }
            });
        }

        if (nextYearButton != null) {
            nextYearButton.setOnClickListener(v -> {
                if (currentYear < maxYear) {
                    currentYear++;
                    updateYearDisplay();
                    loadHistoryForYear(currentYear);
                }
            });
        }
    }

    private void updateYearDisplay() {
        if (yearText != null) {
            yearText.setText(String.valueOf(currentYear));
        }

        // Update button states with visual feedback
        if (prevYearButton != null) {
            prevYearButton.setAlpha(currentYear > minYear ? 1.0f : 0.3f);
            prevYearButton.setEnabled(currentYear > minYear);
        }
        if (nextYearButton != null) {
            nextYearButton.setAlpha(currentYear < maxYear ? 1.0f : 0.3f);
            nextYearButton.setEnabled(currentYear < maxYear);
        }
    }

    private void loadHistoryForYear(int year) {
        if (monthSectionsContainer == null) return;
        monthSectionsContainer.removeAllViews();

        // Get all completed tasks for the year
        List<Task> allTasks = taskRepository.getAllTasks();
        List<Task> yearTasks = new ArrayList<>();

        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

        for (Task task : allTasks) {
            if (task == null || task.date == null) continue;
            if (!task.isComplete) continue;

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.util.Date taskDate = sdf.parse(task.date);
                if (taskDate != null && yearFormat.format(taskDate).equals(String.valueOf(year))) {
                    yearTasks.add(task);
                }
            } catch (Exception e) {
                // Skip invalid dates
            }
        }

        // No placeholder data - app starts empty until user populates via Settings

        if (yearTasks.isEmpty()) {
            showEmptyState();
            return;
        }

        hideEmptyState();
        updateSummary(yearTasks);

        // Group tasks by month sections (Jan-Mar, Apr-Jun, Jul-Sep, Oct-Dec)
        String[][] monthGroups = {
            {"January", "February", "March"},
            {"April", "May", "June"},
            {"July", "August", "September"},
            {"October", "November", "December"}
        };
        String[] groupTitles = {"Jan - Mar", "Apr - Jun", "Jul - Sep", "Oct - Dec"};

        for (int i = 0; i < monthGroups.length; i++) {
            List<Task> groupTasks = getTasksForMonthGroup(yearTasks, monthGroups[i]);
            if (!groupTasks.isEmpty()) {
                View sectionView = createMonthGroupSection(groupTitles[i], groupTasks, monthGroups[i]);
                monthSectionsContainer.addView(sectionView);
            }
        }
    }

    private List<Task> generatePlaceholderData(int year) {
        List<Task> placeholderTasks = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for consistent placeholder data
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Generate tasks for the past months of the current year
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        
        for (int month = 0; month <= currentMonth; month++) {
            // Generate 5-12 tasks per month
            int tasksPerMonth = 5 + random.nextInt(8);
            
            for (int t = 0; t < tasksPerMonth; t++) {
                Task task = new Task();
                
                // 40% chance of focus task
                boolean isFocusTask = random.nextFloat() < 0.4f;
                
                if (isFocusTask) {
                    task.taskType = "focus";
                    task.name = PLACEHOLDER_FOCUS_NAMES[random.nextInt(PLACEHOLDER_FOCUS_NAMES.length)];
                    task.hour = 9 + random.nextInt(8);
                    task.minute = random.nextInt(60);
                    task.amPm = task.hour >= 12 ? "PM" : "AM";
                    if (task.hour > 12) task.hour -= 12;
                    if (task.hour == 0) task.hour = 12;
                    
                    int durationHours = 1 + random.nextInt(3);
                    int endHour24 = (task.amPm.equals("PM") ? task.hour + 12 : task.hour) + durationHours;
                    task.endHour = endHour24 > 12 ? endHour24 - 12 : endHour24;
                    task.endMinute = task.minute;
                    task.endAmPm = endHour24 >= 12 ? "PM" : "AM";
                } else {
                    task.taskType = "reminder";
                    task.name = PLACEHOLDER_TASK_NAMES[random.nextInt(PLACEHOLDER_TASK_NAMES.length)];
                    task.hour = 7 + random.nextInt(14);
                    task.minute = random.nextInt(60);
                    task.amPm = task.hour >= 12 ? "PM" : "AM";
                    if (task.hour > 12) task.hour -= 12;
                    if (task.hour == 0) task.hour = 12;
                }
                
                // Set date to a random day in this month
                cal.set(Calendar.MONTH, month);
                int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                int day = 1 + random.nextInt(maxDay);
                
                // Don't go past today
                if (month == currentMonth) {
                    int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                    day = Math.min(day, today);
                }
                
                cal.set(Calendar.DAY_OF_MONTH, day);
                task.date = dateFormat.format(cal.getTime());
                
                // Set priority
                String[] priorities = {"None", "Low", "Medium", "High"};
                task.urgency = priorities[random.nextInt(priorities.length)];
                
                task.isComplete = true;
                task.id = -(month * 100 + t + 1); // Negative IDs for placeholder
                
                placeholderTasks.add(task);
            }
        }
        
        return placeholderTasks;
    }

    private List<Task> getTasksForMonthGroup(List<Task> tasks, String[] months) {
        List<Task> groupTasks = new ArrayList<>();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());

        for (Task task : tasks) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.util.Date taskDate = sdf.parse(task.date);
                if (taskDate != null) {
                    String monthName = monthFormat.format(taskDate);
                    for (String month : months) {
                        if (month.equals(monthName)) {
                            groupTasks.add(task);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // Skip
            }
        }

        // Sort by date (newest first)
        Collections.sort(groupTasks, (t1, t2) -> {
            if (t1.date == null || t2.date == null) return 0;
            return t2.date.compareTo(t1.date);
        });

        return groupTasks;
    }

    private View createMonthGroupSection(String groupTitle, List<Task> tasks, String[] monthNames) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View sectionView = inflater.inflate(R.layout.item_history_quarter_section, null, false);

        TextView quarterTitleText = sectionView.findViewById(R.id.quarterTitle);
        TextView taskCountText = sectionView.findViewById(R.id.quarterTaskCount);
        LinearLayout taskListContainer = sectionView.findViewById(R.id.quarterTaskList);
        ImageView expandIcon = sectionView.findViewById(R.id.expandIcon);
        View headerLayout = sectionView.findViewById(R.id.sectionHeader);

        if (quarterTitleText != null) {
            quarterTitleText.setText(groupTitle);
        }

        // Count tasks and focus sessions
        int focusCount = 0;
        int taskCount = 0;
        for (Task task : tasks) {
            if (task.isFocusTask()) {
                focusCount++;
            } else {
                taskCount++;
            }
        }

        if (taskCountText != null) {
            String countText = String.format(Locale.getDefault(), 
                "%d task%s • %d focus session%s",
                taskCount, taskCount != 1 ? "s" : "",
                focusCount, focusCount != 1 ? "s" : "");
            taskCountText.setText(countText);
        }

        // Group by individual months within this group
        Map<String, List<Task>> monthMap = groupTasksByMonth(tasks);

        // Add month subsections in order
        for (String monthName : monthNames) {
            List<Task> monthTasks = monthMap.get(monthName);
            if (monthTasks != null && !monthTasks.isEmpty()) {
                View monthHeader = createMonthHeader(monthName, monthTasks.size());
                taskListContainer.addView(monthHeader);

                for (Task task : monthTasks) {
                    View taskView = createHistoryTaskItem(task);
                    taskListContainer.addView(taskView);
                }
            }
        }

        // Make section expandable/collapsible
        final boolean[] isExpanded = {true}; // Start expanded
        if (headerLayout != null && expandIcon != null) {
            headerLayout.setOnClickListener(v -> {
                isExpanded[0] = !isExpanded[0];
                
                // Animate rotation
                expandIcon.animate()
                    .rotation(isExpanded[0] ? 0 : -90)
                    .setDuration(200)
                    .start();
                
                // Show/hide content
                taskListContainer.setVisibility(isExpanded[0] ? View.VISIBLE : View.GONE);
            });
        }

        return sectionView;
    }

    private Map<String, List<Task>> groupTasksByMonth(List<Task> tasks) {
        Map<String, List<Task>> monthMap = new HashMap<>();
        SimpleDateFormat monthNameFormat = new SimpleDateFormat("MMMM", Locale.getDefault());

        for (Task task : tasks) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.util.Date taskDate = sdf.parse(task.date);
                if (taskDate != null) {
                    String monthName = monthNameFormat.format(taskDate);
                    if (!monthMap.containsKey(monthName)) {
                        monthMap.put(monthName, new ArrayList<>());
                    }
                    monthMap.get(monthName).add(task);
                }
            } catch (Exception e) {
                // Skip
            }
        }

        return monthMap;
    }

    private View createMonthHeader(String monthName, int count) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View headerView = inflater.inflate(R.layout.item_history_month_header, null, false);

        TextView monthText = headerView.findViewById(R.id.monthName);
        TextView countText = headerView.findViewById(R.id.monthCount);

        if (monthText != null) {
            monthText.setText(monthName);
        }
        if (countText != null) {
            countText.setText(count + " completed");
        }

        return headerView;
    }

    private View createHistoryTaskItem(Task task) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View taskView = inflater.inflate(R.layout.item_history_task, null, false);

        TextView taskNameText = taskView.findViewById(R.id.historyTaskName);
        TextView taskDateText = taskView.findViewById(R.id.historyTaskDate);
        TextView taskTimeText = taskView.findViewById(R.id.historyTaskTime);
        TextView taskTypeChip = taskView.findViewById(R.id.historyTaskType);
        View priorityIndicator = taskView.findViewById(R.id.priorityIndicator);

        if (taskNameText != null) {
            taskNameText.setText(task.name);
        }

        if (taskDateText != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
                java.util.Date date = inputFormat.parse(task.date);
                if (date != null) {
                    taskDateText.setText(outputFormat.format(date));
                }
            } catch (Exception e) {
                taskDateText.setText(task.date);
            }
        }

        if (taskTimeText != null) {
            if (task.isFocusTask()) {
                String timeRange = String.format(Locale.getDefault(), "%d:%02d %s - %d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                        task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                taskTimeText.setText(timeRange);
            } else {
                String time = String.format(Locale.getDefault(), "%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM");
                taskTimeText.setText(time);
            }
        }

        if (taskTypeChip != null) {
            if (task.isFocusTask()) {
                taskTypeChip.setText("Focus");
                taskTypeChip.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        getColor(R.color.primary)));
            } else {
                taskTypeChip.setText("Task");
                taskTypeChip.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        getColor(R.color.success)));
            }
        }

        if (priorityIndicator != null) {
            String priority = task.urgency != null ? task.urgency : "None";
            int color;
            switch (priority) {
                case "High":
                    color = getColor(R.color.priority_border_high);
                    break;
                case "Medium":
                    color = getColor(R.color.priority_border_medium);
                    break;
                case "Low":
                    color = getColor(R.color.priority_border_low);
                    break;
                default:
                    color = getColor(R.color.outline_light);
                    break;
            }
            priorityIndicator.setBackgroundColor(color);
        }

        // Show quick info popup on long press
        taskView.setOnLongClickListener(v -> {
            showTaskQuickInfo(v, task);
            return true;
        });

        return taskView;
    }

    private void showTaskQuickInfo(View anchorView, Task task) {
        // Show quick info popup similar to other screens
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_quick_info, null);
        popupWindow.setContentView(popupView);

        popupWindow.setWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(24f);
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        // Populate popup
        TextView titleText = popupView.findViewById(R.id.quickInfoTitle);
        TextView typeText = popupView.findViewById(R.id.quickInfoType);
        TextView dateText = popupView.findViewById(R.id.quickInfoDate);
        TextView timeText = popupView.findViewById(R.id.quickInfoTime);
        TextView durationText = popupView.findViewById(R.id.quickInfoDuration);
        View durationRow = popupView.findViewById(R.id.durationRow);
        ImageView typeIcon = popupView.findViewById(R.id.typeIcon);
        MaterialCardView iconContainer = popupView.findViewById(R.id.typeIconContainer);
        TextView alarmText = popupView.findViewById(R.id.quickInfoAlarm);

        boolean isFocusSession = task.isFocusTask();

        if (titleText != null) titleText.setText(task.name);
        if (typeText != null) {
            typeText.setText(isFocusSession ? "Focus Session" : "Task");
            typeText.setTextColor(isFocusSession ? getColor(R.color.primary) : getColor(R.color.success));
        }
        if (dateText != null && task.date != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
                java.util.Date date = inputFormat.parse(task.date);
                if (date != null) {
                    dateText.setText(outputFormat.format(date));
                }
            } catch (Exception e) {
                dateText.setText(task.date);
            }
        }
        if (timeText != null) {
            if (isFocusSession) {
                String timeRange = String.format(Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                        task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                timeText.setText(timeRange);
            } else {
                timeText.setText(String.format(Locale.getDefault(), "%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
            }
        }
        if (durationRow != null && durationText != null && isFocusSession) {
            durationRow.setVisibility(View.VISIBLE);
            // Calculate duration
            int startMins = convertTo24Hour(task.hour, task.amPm) * 60 + task.minute;
            int endMins = convertTo24Hour(task.endHour, task.endAmPm) * 60 + task.endMinute;
            int durationMins = endMins - startMins;
            if (durationMins < 0) durationMins += 24 * 60;
            int hours = durationMins / 60;
            int mins = durationMins % 60;
            String durationStr;
            if (hours > 0 && mins > 0) {
                durationStr = hours + " hr " + mins + " min";
            } else if (hours > 0) {
                durationStr = hours + " hour" + (hours > 1 ? "s" : "");
            } else {
                durationStr = mins + " minutes";
            }
            durationText.setText(durationStr);
        } else if (durationRow != null) {
            durationRow.setVisibility(View.GONE);
        }
        if (typeIcon != null && iconContainer != null) {
            if (isFocusSession) {
                typeIcon.setImageResource(R.drawable.ic_focus);
                iconContainer.setCardBackgroundColor(getColor(R.color.primary));
            } else {
                typeIcon.setImageResource(R.drawable.ic_reminder);
                iconContainer.setCardBackgroundColor(getColor(R.color.success));
            }
        }
        if (alarmText != null) {
            alarmText.setText("✓ Completed");
        }

        popupWindow.showAtLocation(anchorView, android.view.Gravity.CENTER, 0, 0);

        // Add haptic feedback
        anchorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

        // Tap popup to edit - allow editing completed tasks
        popupView.setOnClickListener(v -> {
            popupWindow.dismiss();
            openTaskForEditing(task);
        });
    }

    private void openTaskForEditing(Task task) {
        if (task.isFocusTask()) {
            Intent intent = new Intent(this, EditFocusTaskActivity.class);
            intent.putExtra("task_id", task.id);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, EditTaskActivity.class);
            intent.putExtra("task_id", task.id);
            startActivity(intent);
        }
    }

    private int convertTo24Hour(int hour, String amPm) {
        if (amPm == null) amPm = "AM";
        if (hour == 12) {
            return amPm.equals("AM") ? 0 : 12;
        } else {
            return amPm.equals("PM") ? hour + 12 : hour;
        }
    }

    private void updateSummary(List<Task> tasks) {
        int total = tasks.size();
        int completed = 0;
        int focus = 0;

        for (Task task : tasks) {
            if (task.isComplete) completed++;
            if (task.isFocusTask()) focus++;
        }

        if (totalTasksCount != null) {
            totalTasksCount.setText(String.valueOf(total));
        }
        if (completedTasksCount != null) {
            completedTasksCount.setText(String.valueOf(completed));
        }
        if (focusSessionsCount != null) {
            focusSessionsCount.setText(String.valueOf(focus));
        }
        
        // Calculate productivity score (simple formula based on completion rate and focus time)
        if (productivityScoreText != null) {
            int score = 0;
            if (total > 0) {
                // Base score from completion
                score = (completed * 100) / total;
                // Bonus for focus sessions (up to 20 points)
                int focusBonus = Math.min(20, focus * 4);
                score = Math.min(100, score + focusBonus);
            }
            productivityScoreText.setText(score + "%");
        }

        if (summaryCard != null) {
            summaryCard.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
        }
        if (summaryCard != null) {
            summaryCard.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }
}
