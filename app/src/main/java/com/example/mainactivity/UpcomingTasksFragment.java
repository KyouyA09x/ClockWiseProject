package com.example.mainactivity;

import android.content.Intent;
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

import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpcomingTasksFragment extends Fragment {

    private View emptyStateCard;
    private LinearLayout focusSessionsSection;
    private LinearLayout focusSessionsContainer;
    private TextView focusSessionsCount;
    private LinearLayout tasksSection;
    private LinearLayout tasksContainer;
    private TextView tasksCount;
    private TaskRepository taskRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upcoming_tasks, container, false);

        initViews(view);
        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(requireContext());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshTasks();
    }

    private void initViews(View view) {
        emptyStateCard = view.findViewById(R.id.emptyStateCard);
        focusSessionsSection = view.findViewById(R.id.focusSessionsSection);
        focusSessionsContainer = view.findViewById(R.id.focusSessionsContainer);
        focusSessionsCount = view.findViewById(R.id.focusSessionsCount);
        tasksSection = view.findViewById(R.id.tasksSection);
        tasksContainer = view.findViewById(R.id.tasksContainer);
        tasksCount = view.findViewById(R.id.tasksCount);

        // Setup dynamic padding
        setupDynamicPadding(view);
    }

    private void setupDynamicPadding(View view) {
        LinearLayout container = view.findViewById(R.id.upcomingRootContainer);
        if (container == null) return;

        view.post(() -> {
            android.app.Activity activity = getActivity();
            if (!(activity instanceof MainActivity)) return;

            MainActivity mainActivity = (MainActivity) activity;

            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                );

                float density = getResources().getDisplayMetrics().density;

                int bottomNavHeight = 0;
                try {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        mainActivity.findViewById(R.id.bottomNavigation);
                    if (bottomNav != null) {
                        bottomNavHeight = bottomNav.getHeight();
                        if (bottomNavHeight == 0) {
                            bottomNavHeight = (int) (56 * density);
                        }
                    }
                } catch (Exception e) {
                    bottomNavHeight = (int) (56 * density);
                }

                int fabsReservedSpace = (int) ((56 + 8 + 56 + 16) * density); // Two regular FABs + spacing + margin
                int totalBottomPadding = bottomNavHeight + systemBars.bottom + fabsReservedSpace;

                container.setPadding(
                    container.getPaddingLeft(),
                    container.getPaddingTop(),
                    container.getPaddingRight(),
                    totalBottomPadding
                );

                return insets;
            });

            androidx.core.view.ViewCompat.requestApplyInsets(view);
        });
    }

    public void refreshTasks() {
        if (taskRepository == null || getContext() == null) return;

        // Ensure repository is fully initialized
        if (taskRepository.morningTasks == null || taskRepository.afternoonTasks == null || taskRepository.nightTasks == null) {
            taskRepository.initialize(requireContext());
        }

        // Safety: Create empty lists if still null (should never happen)
        ArrayList<Task> morningTasks = taskRepository.morningTasks != null ? taskRepository.morningTasks : new ArrayList<>();
        ArrayList<Task> afternoonTasks = taskRepository.afternoonTasks != null ? taskRepository.afternoonTasks : new ArrayList<>();
        ArrayList<Task> nightTasks = taskRepository.nightTasks != null ? taskRepository.nightTasks : new ArrayList<>();

        // Clear containers
        if (focusSessionsContainer != null) focusSessionsContainer.removeAllViews();
        if (tasksContainer != null) tasksContainer.removeAllViews();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        // Separate lists for focus sessions and regular tasks
        ArrayList<Task> upcomingFocusSessions = new ArrayList<>();
        ArrayList<Task> upcomingTasks = new ArrayList<>();

        // Collect tasks
        collectUpcomingItems(morningTasks, todayDate, upcomingFocusSessions, upcomingTasks);
        collectUpcomingItems(afternoonTasks, todayDate, upcomingFocusSessions, upcomingTasks);
        collectUpcomingItems(nightTasks, todayDate, upcomingFocusSessions, upcomingTasks);

        // No placeholder data - app starts empty until user populates via Settings

        // Sort by date and time
        sortTasksByDateTime(upcomingFocusSessions);
        sortTasksByDateTime(upcomingTasks);

        // Group by date
        Map<String, List<Task>> focusSessionsByDate = groupByDate(upcomingFocusSessions);
        Map<String, List<Task>> tasksByDate = groupByDate(upcomingTasks);

        // Display focus sessions
        if (!upcomingFocusSessions.isEmpty() && focusSessionsSection != null) {
            focusSessionsSection.setVisibility(View.VISIBLE);
            if (focusSessionsCount != null) {
                focusSessionsCount.setText(String.valueOf(upcomingFocusSessions.size()));
            }
            displayGroupedItems(focusSessionsContainer, focusSessionsByDate, true);
        } else if (focusSessionsSection != null) {
            focusSessionsSection.setVisibility(View.GONE);
        }

        // Display regular tasks
        if (!upcomingTasks.isEmpty() && tasksSection != null) {
            tasksSection.setVisibility(View.VISIBLE);
            if (tasksCount != null) {
                tasksCount.setText(String.valueOf(upcomingTasks.size()));
            }
            displayGroupedItems(tasksContainer, tasksByDate, false);
        } else if (tasksSection != null) {
            tasksSection.setVisibility(View.GONE);
        }

        // Show empty state if nothing (this shouldn't happen now with placeholder data)
        if (upcomingFocusSessions.isEmpty() && upcomingTasks.isEmpty()) {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
        } else {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.GONE);
        }
    }

    private void generatePlaceholderUpcomingTasks(ArrayList<Task> focusSessions, ArrayList<Task> regularTasks) {
        String[] taskNames = {
            "Team meeting",
            "Review project proposal",
            "Client call",
            "Submit report",
            "Doctor appointment"
        };
        String[] focusNames = {
            "Deep work: Feature development",
            "Writing documentation",
            "Learning session",
            "Creative brainstorming"
        };
        String[] priorities = {"None", "Low", "Medium", "High"};

        java.util.Random random = new java.util.Random(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        // Generate 5 upcoming regular tasks
        for (int i = 0; i < 5; i++) {
            Task task = new Task();
            task.id = -(i + 1); // Negative ID for placeholder
            task.name = taskNames[i % taskNames.length];
            task.taskType = "reminder";
            
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, 1 + random.nextInt(7)); // 1-7 days in future
            task.date = sdf.format(cal.getTime());
            
            int hour24 = 8 + random.nextInt(10);
            task.hour = hour24 > 12 ? hour24 - 12 : (hour24 == 0 ? 12 : hour24);
            task.minute = random.nextBoolean() ? 0 : 30;
            task.amPm = hour24 >= 12 ? "PM" : "AM";
            
            task.urgency = priorities[random.nextInt(priorities.length)];
            task.isAlarmOn = true;
            task.isComplete = false;
            
            regularTasks.add(task);
        }

        // Generate 3 upcoming focus sessions
        for (int i = 0; i < 3; i++) {
            Task task = new Task();
            task.id = -(i + 100); // Negative ID for placeholder
            task.name = focusNames[i % focusNames.length];
            task.taskType = "focus";
            
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, 1 + random.nextInt(5)); // 1-5 days in future
            task.date = sdf.format(cal.getTime());
            
            int startHour24 = 9 + random.nextInt(8);
            task.hour = startHour24 > 12 ? startHour24 - 12 : (startHour24 == 0 ? 12 : startHour24);
            task.minute = 0;
            task.amPm = startHour24 >= 12 ? "PM" : "AM";
            
            int durationHours = 1 + random.nextInt(2);
            int endHour24 = startHour24 + durationHours;
            task.endHour = endHour24 > 12 ? endHour24 - 12 : (endHour24 == 0 ? 12 : endHour24);
            task.endMinute = 0;
            task.endAmPm = endHour24 >= 12 ? "PM" : "AM";
            
            task.urgency = priorities[1 + random.nextInt(3)]; // Low, Medium, or High
            task.isAlarmOn = true;
            task.isComplete = false;
            
            focusSessions.add(task);
        }
    }

    private void collectUpcomingItems(ArrayList<Task> tasks, String todayDate, 
                                       ArrayList<Task> focusSessions, ArrayList<Task> regularTasks) {
        if (tasks == null) return;

        for (Task task : tasks) {
            if (task == null || task.date == null) continue;
            // Only include tasks scheduled for FUTURE dates (not today)
            if (task.date.compareTo(todayDate) > 0 && !task.isComplete) {
                if (task.isFocusTask()) {
                    focusSessions.add(task);
                } else {
                    regularTasks.add(task);
                }
            }
        }
    }

    private void sortTasksByDateTime(ArrayList<Task> tasks) {
        tasks.sort((t1, t2) -> {
            if (t1.date != null && t2.date != null) {
                int dateCompare = t1.date.compareTo(t2.date);
                if (dateCompare != 0) return dateCompare;
                int t1Minutes = convertTo24Hour(t1.hour, t1.amPm) * 60 + t1.minute;
                int t2Minutes = convertTo24Hour(t2.hour, t2.amPm) * 60 + t2.minute;
                return Integer.compare(t1Minutes, t2Minutes);
            }
            return 0;
        });
    }

    private Map<String, List<Task>> groupByDate(ArrayList<Task> tasks) {
        Map<String, List<Task>> grouped = new LinkedHashMap<>();
        for (Task task : tasks) {
            if (task.date != null) {
                if (!grouped.containsKey(task.date)) {
                    grouped.put(task.date, new ArrayList<>());
                }
                grouped.get(task.date).add(task);
            }
        }
        return grouped;
    }

    private void displayGroupedItems(LinearLayout container, Map<String, List<Task>> groupedTasks, boolean isFocusSession) {
        if (container == null || getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Map.Entry<String, List<Task>> entry : groupedTasks.entrySet()) {
            String dateStr = entry.getKey();
            List<Task> tasks = entry.getValue();

            // Add date header
            View dateHeader = createDateHeader(inflater, dateStr, tasks.size(), isFocusSession);
            container.addView(dateHeader);

            // Add task items
            for (Task task : tasks) {
                View taskView = createTaskItemView(inflater, task, isFocusSession, container);
                container.addView(taskView);
            }
        }
    }

    private View createDateHeader(LayoutInflater inflater, String dateStr, int itemCount, boolean isFocusSession) {
        View headerView = inflater.inflate(R.layout.item_date_header, null, false);

        TextView dateDay = headerView.findViewById(R.id.dateDay);
        TextView dateMonth = headerView.findViewById(R.id.dateMonth);
        TextView dateFull = headerView.findViewById(R.id.dateFull);
        TextView dateRelative = headerView.findViewById(R.id.dateRelative);
        MaterialCardView iconContainer = headerView.findViewById(R.id.dateIconContainer);

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                // Set day number
                if (dateDay != null) {
                    dateDay.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
                }

                // Set month abbreviation
                if (dateMonth != null) {
                    SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
                    dateMonth.setText(monthFormat.format(date).toUpperCase());
                }

                // Set full date
                if (dateFull != null) {
                    SimpleDateFormat fullFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
                    dateFull.setText(fullFormat.format(date));
                }

                // Set relative date (Tomorrow, In 2 days, etc.)
                if (dateRelative != null) {
                    String relative = getRelativeDateString(date);
                    dateRelative.setText(relative);
                    dateRelative.setTextColor(isFocusSession ? 
                        getResources().getColor(R.color.primary, null) : 
                        getResources().getColor(R.color.success, null));
                }

                // Set icon container color based on type
                if (iconContainer != null) {
                    if (isFocusSession) {
                        iconContainer.setCardBackgroundColor(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.blue_light));
                    } else {
                        iconContainer.setCardBackgroundColor(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chip_priority_low));
                    }
                }
            }
        } catch (ParseException e) {
            if (dateFull != null) dateFull.setText(dateStr);
        }

        return headerView;
    }

    private String getRelativeDateString(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);

        // Reset time components for accurate day comparison
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        long diffDays = (target.getTimeInMillis() - today.getTimeInMillis()) / (1000 * 60 * 60 * 24);

        if (diffDays == 1) {
            return "Tomorrow";
        } else if (diffDays == 2) {
            return "In 2 days";
        } else if (diffDays <= 7) {
            return "In " + diffDays + " days";
        } else if (diffDays <= 14) {
            return "Next week";
        } else {
            return "In " + (diffDays / 7) + " weeks";
        }
    }

    private View createTaskItemView(LayoutInflater inflater, Task task, boolean isFocusSession, ViewGroup parent) {
        int layoutId = isFocusSession ? R.layout.item_upcoming_focus_session : R.layout.item_upcoming_task;
        View taskView = inflater.inflate(layoutId, parent, false);

        TextView taskNameTextView = taskView.findViewById(R.id.taskName);
        TextView taskTimeTextView = taskView.findViewById(R.id.taskTime);
        com.google.android.material.materialswitch.MaterialSwitch taskSwitch = taskView.findViewById(R.id.taskSwitch);
        View taskContent = taskView.findViewById(R.id.taskContent);

        // Get task icon for notification preview
        View taskIconContainer = null;
        if (taskView instanceof ViewGroup) {
            // Find the MaterialCardView containing the icon (usually first child)
            ViewGroup rootLayout = null;
            if (taskView instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) taskView;
                if (card.getChildCount() > 0 && card.getChildAt(0) instanceof ViewGroup) {
                    rootLayout = (ViewGroup) card.getChildAt(0);
                }
            }
            if (rootLayout != null) {
                for (int i = 0; i < rootLayout.getChildCount(); i++) {
                    View child = rootLayout.getChildAt(i);
                    if (child instanceof MaterialCardView && child.getId() != taskView.getId()) {
                        // This is likely the icon container
                        taskIconContainer = child;
                        break;
                    }
                }
            }
        }

        // Add click listener on icon container for notification preview
        if (taskIconContainer != null) {
            final View iconToClick = taskIconContainer;
            iconToClick.setClickable(true);
            iconToClick.setOnClickListener(v -> {
                showNotificationPreviewPopup(v, task, isFocusSession);
            });
        }

        // Set task name
        if (taskNameTextView != null) {
            taskNameTextView.setText(task.name);
        }

        // Set time
        if (taskTimeTextView != null) {
            if (isFocusSession) {
                String timeRange = String.format(Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                        task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                taskTimeTextView.setText(timeRange);
            } else {
                String time = String.format(Locale.getDefault(), "%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM");
                taskTimeTextView.setText(time);
            }
        }

        // Set priority border color
        if (taskView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView cardView = 
                (com.google.android.material.card.MaterialCardView) taskView;
            String priority = task.urgency != null ? task.urgency : "None";
            int borderColor;
            switch (priority) {
                case "High":
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_high);
                    break;
                case "Medium":
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_medium);
                    break;
                case "Low":
                    borderColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.priority_border_low);
                    break;
                default:
                    borderColor = isFocusSession ? 
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary) :
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success);
                    break;
            }
            // Set stroke width and color to make border visible
            int strokeWidth = (int) (3 * getResources().getDisplayMetrics().density); // 3dp
            cardView.setStrokeWidth(strokeWidth);
            cardView.setStrokeColor(borderColor);
        }

        // Alarm switch
        if (taskSwitch != null) {
            taskSwitch.setChecked(task.isAlarmOn);
            taskSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.isAlarmOn = isChecked;
                taskRepository.updateTask(task);
                if (isChecked) {
                    if (isFocusSession) {
                        AlarmHelper.scheduleFocusTaskAlarms(getContext(), task);
                    } else {
                        AlarmHelper.scheduleTaskAlarm(getContext(), task);
                    }
                    Toast.makeText(getContext(), "🔔 Alerts enabled", Toast.LENGTH_SHORT).show();
                } else {
                    if (isFocusSession) {
                        AlarmHelper.cancelFocusTaskAlarms(getContext(), task);
                    } else {
                        AlarmHelper.cancelTaskAlarm(getContext(), task);
                    }
                    Toast.makeText(getContext(), "🔕 Alerts disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Long-press for quick info popup (3D Touch style)
        if (taskContent != null) {
            taskContent.setOnLongClickListener(v -> {
                // Apply 3D touch scale effect
                View parentCard = taskView;
                parentCard.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        parentCard.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                        
                        // Haptic feedback
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        
                        // Show popup
                        showQuickInfoPopup(v, task, isFocusSession);
                    })
                    .start();
                return true;
            });
        }

        // Click to edit
        if (taskContent != null) {
            taskContent.setOnClickListener(v -> {
                if (isFocusSession) {
                    Intent intent = new Intent(getContext(), EditFocusTaskActivity.class);
                    intent.putExtra("task_id", task.id);
                    startActivity(intent);
                } else {
                    if (getActivity() != null) {
                        AddReminderBottomSheet bottomSheet = AddReminderBottomSheet.newInstance(task);
                        bottomSheet.setOnTaskSavedListener(() -> {
                            // Repository already updated - just refresh UI
                            refreshTasks();
                        });
                        bottomSheet.show(getActivity().getSupportFragmentManager(), "AddReminderBottomSheet");
                    }
                }
            });
        }

        // Delete button
        com.google.android.material.button.MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                String typeName = isFocusSession ? "Focus Session" : "Task";
                String title = "Delete " + typeName + "?";
                String message = "This action cannot be undone. {item} will be permanently removed.";
                
                ModernDialogHelper.showDestructiveDialog(
                    getContext(),
                    title,
                    message,
                    task.name,
                    R.drawable.ic_delete,
                    () -> {
                        // Animate slide-to-right deletion
                        animateTaskDeletion(taskView, () -> {
                            // Cancel alarms before deleting
                            if (isFocusSession) {
                                AlarmHelper.cancelFocusTaskAlarms(getContext(), task);
                            } else {
                                AlarmHelper.cancelTaskAlarm(getContext(), task);
                            }
                            taskRepository.deleteTask(task);
                            // Repository already updated - just refresh UI
                            refreshTasks();
                            Toast.makeText(getContext(), typeName + " deleted", Toast.LENGTH_SHORT).show();
                        });
                    },
                    null
                );
            });
        }

        return taskView;
    }
    
    private void animateTaskDeletion(View taskView, Runnable onComplete) {
        taskView.animate()
            .translationX(taskView.getWidth())
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction(onComplete)
            .start();
    }

    private void showQuickInfoPopup(View anchorView, Task task, boolean isFocusSession) {
        if (getContext() == null) return;

        // Create popup window
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(getContext());
        
        // Inflate the popup layout
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View popupView = inflater.inflate(R.layout.popup_quick_info, null);
        popupWindow.setContentView(popupView);
        
        // Configure popup window
        popupWindow.setWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(24f);
        
        // Animate popup
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        // Populate popup content
        TextView titleText = popupView.findViewById(R.id.quickInfoTitle);
        TextView typeText = popupView.findViewById(R.id.quickInfoType);
        TextView dateText = popupView.findViewById(R.id.quickInfoDate);
        TextView timeText = popupView.findViewById(R.id.quickInfoTime);
        TextView durationText = popupView.findViewById(R.id.quickInfoDuration);
        TextView alarmText = popupView.findViewById(R.id.quickInfoAlarm);
        View durationRow = popupView.findViewById(R.id.durationRow);
        android.widget.ImageView typeIcon = popupView.findViewById(R.id.typeIcon);
        android.widget.ImageView alarmIcon = popupView.findViewById(R.id.alarmIcon);
        MaterialCardView iconContainer = popupView.findViewById(R.id.typeIconContainer);

        // Set title
        if (titleText != null) {
            titleText.setText(task.name);
        }

        // Set type
        if (typeText != null) {
            typeText.setText(isFocusSession ? "Focus Session" : "Task");
            typeText.setTextColor(isFocusSession ? 
                getResources().getColor(R.color.primary, null) : 
                getResources().getColor(R.color.success, null));
        }

        // Set icon and color
        if (typeIcon != null && iconContainer != null) {
            if (isFocusSession) {
                typeIcon.setImageResource(R.drawable.ic_focus);
                iconContainer.setCardBackgroundColor(getResources().getColor(R.color.primary, null));
            } else {
                typeIcon.setImageResource(R.drawable.ic_reminder);
                iconContainer.setCardBackgroundColor(getResources().getColor(R.color.success, null));
            }
        }

        // Set date
        if (dateText != null && task.date != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(task.date);
                if (date != null) {
                    dateText.setText(outputFormat.format(date));
                }
            } catch (ParseException e) {
                dateText.setText(task.date);
            }
        }

        // Set time
        if (timeText != null) {
            if (isFocusSession) {
                String timeRange = String.format(Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                        task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                timeText.setText(timeRange);
            } else {
                String time = String.format(Locale.getDefault(), "%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM");
                timeText.setText(time);
            }
        }

        // Set duration for focus sessions
        if (isFocusSession && durationRow != null && durationText != null) {
            durationRow.setVisibility(View.VISIBLE);
            int startMinutes = convertTo24Hour(task.hour, task.amPm) * 60 + task.minute;
            int endMinutes = convertTo24Hour(task.endHour, task.endAmPm) * 60 + task.endMinute;
            int durationMins = endMinutes - startMinutes;
            if (durationMins < 0) durationMins += 24 * 60; // Handle overnight sessions
            
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

        // Set alarm status
        if (alarmText != null && alarmIcon != null) {
            if (task.isAlarmOn) {
                alarmText.setText("Alerts enabled");
                alarmIcon.setImageResource(R.drawable.ic_reminder);
            } else {
                alarmText.setText("Alerts disabled");
                alarmIcon.setImageResource(R.drawable.ic_reminder);
                alarmIcon.setAlpha(0.5f);
            }
        }

        // Show popup centered on screen
        popupWindow.showAtLocation(anchorView, android.view.Gravity.CENTER, 0, 0);
        
        // Add haptic feedback
        anchorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

        // Tap popup to edit
        popupView.setOnClickListener(v -> {
            popupWindow.dismiss();
            if (isFocusSession) {
                Intent intent = new Intent(getContext(), EditFocusTaskActivity.class);
                intent.putExtra("task_id", task.id);
                startActivity(intent);
            } else {
                if (getActivity() != null) {
                    AddReminderBottomSheet bottomSheet = AddReminderBottomSheet.newInstance(task);
                    bottomSheet.setOnTaskSavedListener(() -> {
                        // Repository already updated - just refresh UI
                        refreshTasks();
                    });
                    bottomSheet.show(getActivity().getSupportFragmentManager(), "AddReminderBottomSheet");
                }
            }
        });
    }

    /**
     * Shows a notification preview popup - a squircle popup showing how the notification
     * will appear when the task is due.
     */
    private void showNotificationPreviewPopup(View anchorView, Task task, boolean isFocusSession) {
        if (getContext() == null) return;

        // Create dialog for notification preview
        android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_notification_preview);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            params.gravity = android.view.Gravity.CENTER;
            dialog.getWindow().setAttributes(params);
        }

        // Populate dialog with task info
        android.widget.TextView titleText = dialog.findViewById(R.id.notificationTitle);
        android.widget.TextView typeText = dialog.findViewById(R.id.notificationType);
        android.widget.TextView timeText = dialog.findViewById(R.id.notificationTime);
        android.widget.TextView messageText = dialog.findViewById(R.id.notificationMessage);
        android.widget.ImageView iconView = dialog.findViewById(R.id.notificationIcon);
        MaterialCardView iconContainer = dialog.findViewById(R.id.notificationIconContainer);
        com.google.android.material.button.MaterialButton dismissButton = dialog.findViewById(R.id.dismissButton);
        com.google.android.material.button.MaterialButton actionButton = dialog.findViewById(R.id.actionButton);

        if (titleText != null) titleText.setText(task.name);
        
        if (typeText != null) {
            typeText.setText(isFocusSession ? "Focus Session" : "Task Reminder");
            typeText.setTextColor(isFocusSession ? 
                getResources().getColor(R.color.primary, null) : 
                getResources().getColor(R.color.success, null));
        }
        
        if (timeText != null) {
            if (isFocusSession) {
                timeText.setText(String.format(Locale.getDefault(), 
                    "%d:%02d %s → %d:%02d %s", 
                    task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                    task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM"));
            } else {
                timeText.setText(String.format(Locale.getDefault(), 
                    "%d:%02d %s", task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
            }
        }
        
        if (messageText != null) {
            String message = task.noteContent != null && !task.noteContent.isEmpty() 
                ? task.noteContent 
                : (isFocusSession ? "Your focus session is about to start. Time to eliminate distractions!" 
                    : "It's time for your scheduled task. Stay productive!");
            messageText.setText(message);
        }

        if (iconView != null && iconContainer != null) {
            if (isFocusSession) {
                iconView.setImageResource(R.drawable.ic_focus);
                iconContainer.setCardBackgroundColor(getResources().getColor(R.color.primary, null));
            } else {
                iconView.setImageResource(R.drawable.ic_reminder);
                iconContainer.setCardBackgroundColor(getResources().getColor(R.color.success, null));
            }
        }

        if (dismissButton != null) {
            dismissButton.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (actionButton != null) {
            actionButton.setText(isFocusSession ? "Start Session" : "Got It");
            actionButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), 
                    isFocusSession ? "Focus session notification preview!" : "Task reminder notification preview!", 
                    Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        // Haptic feedback
        anchorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        
        // Add tap-to-edit functionality on the entire dialog
        View rootView = dialog.findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setOnClickListener(v -> {
                dialog.dismiss();
                openTaskForEditing(task);
            });
        }
        
        dialog.show();
    }
    
    private void openTaskForEditing(Task task) {
        if (task.isFocusTask()) {
            // Open focus task editor
            Intent intent = new Intent(getContext(), EditFocusTaskActivity.class);
            intent.putExtra("task_id", task.id);
            startActivity(intent);
        } else {
            // Open reminder editor using bottom sheet
            if (getActivity() != null) {
                AddReminderBottomSheet bottomSheet = AddReminderBottomSheet.newInstance(task);
                bottomSheet.setOnTaskSavedListener(() -> {
                    // Repository already updated - just refresh UI
                    refreshTasks();
                });
                bottomSheet.show(getActivity().getSupportFragmentManager(), "AddReminderBottomSheet");
            }
        }
    }

    private int convertTo24Hour(int hour, String amPm) {
        if (amPm != null && amPm.equals("AM")) {
            return hour == 12 ? 0 : hour;
        } else {
            return hour == 12 ? 12 : hour + 12;
        }
    }
}
