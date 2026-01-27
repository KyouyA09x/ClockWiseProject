package com.example.mainactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CurrentTasksFragment extends Fragment {

    private View progressTracker;
    private View emptyStateCard;
    private View tasksContainerCard;
    private LinearLayout focusTasksSection;
    private LinearLayout focusTasksContainer;
    
    // Time-aware sections
    private LinearLayout rightNowSection;
    private LinearLayout rightNowContainer;
    private TextView rightNowSubtitle;
    private LinearLayout missedTasksSection;
    private LinearLayout missedTasksContainer;
    private LinearLayout laterTodaySection;
    private LinearLayout laterTodayContainer;
    
    // Smart View toggle
    private com.google.android.material.materialswitch.MaterialSwitch smartViewToggle;
    private boolean isSmartViewEnabled = true;
    private static final String PREFS_NAME = "clockwise_prefs";
    private static final String PREF_SMART_VIEW = "smart_view_enabled";
    
    private LinearLayout morningTasksContainer;
    private LinearLayout afternoonTasksContainer;
    private LinearLayout nightTasksContainer;
    private TextView morningTasksHeader;
    private TextView afternoonTasksHeader;
    private TextView nightTasksHeader;
    private TextView taskCountText;
    private LinearProgressIndicator progressBar;
    private TextView completionText;
    private com.google.android.material.button.MaterialButton priorityFilterButton;
    private com.google.android.material.button.MaterialButton searchButton;

    // Smart insight views
    private View smartInsightCard;
    private TextView smartInsightText;
    
    // Filtered out tasks views
    private com.google.android.material.card.MaterialCardView filteredOutCard;
    private View filteredOutHeader;
    private ImageView filteredOutExpandIcon;
    private TextView filteredOutTitle;
    private TextView filteredOutCount;
    private LinearLayout filteredOutContent;
    private LinearLayout filteredMorningContainer;
    private LinearLayout filteredAfternoonContainer;
    private LinearLayout filteredNightContainer;
    private TextView filteredMorningHeader;
    private TextView filteredAfternoonHeader;
    private TextView filteredNightHeader;
    private boolean isFilteredSectionExpanded = false;

    private TaskRepository taskRepository;
    private String currentPriorityFilter = "All"; // Track current filter: "All", "Low", "Medium", "High"

    // Broadcast receiver to refresh tasks when added from floating button
    private final BroadcastReceiver taskRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.mainactivity.REFRESH_TASKS".equals(intent.getAction())) {
                refreshTasks();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_tasks, container, false);

        initViews(view);
        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(requireContext());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register broadcast receiver for task refresh
        IntentFilter filter = new IntentFilter("com.example.mainactivity.REFRESH_TASKS");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(taskRefreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(taskRefreshReceiver, filter);
        }
        refreshTasks();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        try {
            requireContext().unregisterReceiver(taskRefreshReceiver);
        } catch (Exception ignored) {}
    }

    private void initViews(View view) {
        progressTracker = view.findViewById(R.id.progressTracker);
        emptyStateCard = view.findViewById(R.id.emptyStateCard);
        tasksContainerCard = view.findViewById(R.id.tasksContainerCard);
        focusTasksSection = view.findViewById(R.id.focusTasksSection);
        focusTasksContainer = view.findViewById(R.id.focusTasksContainer);
        
        // Initialize time-aware sections
        rightNowSection = view.findViewById(R.id.rightNowSection);
        rightNowContainer = view.findViewById(R.id.rightNowContainer);
        rightNowSubtitle = view.findViewById(R.id.rightNowSubtitle);
        missedTasksSection = view.findViewById(R.id.missedTasksSection);
        missedTasksContainer = view.findViewById(R.id.missedTasksContainer);
        laterTodaySection = view.findViewById(R.id.laterTodaySection);
        laterTodayContainer = view.findViewById(R.id.laterTodayContainer);
        
        // Initialize Smart View toggle
        smartViewToggle = view.findViewById(R.id.smartViewToggle);
        loadSmartViewPreference();
        if (smartViewToggle != null) {
            smartViewToggle.setChecked(isSmartViewEnabled);
            smartViewToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isSmartViewEnabled = isChecked;
                saveSmartViewPreference(isChecked);
                
                // AI Sparkle animation effect
                playAISparkleAnimation(buttonView, isChecked);
                
                refreshTasksWithAnimation(); // Use animated refresh for smooth transition
            });
        }
        
        morningTasksContainer = view.findViewById(R.id.morningTasksContainer);
        afternoonTasksContainer = view.findViewById(R.id.afternoonTasksContainer);
        nightTasksContainer = view.findViewById(R.id.nightTasksContainer);
        morningTasksHeader = view.findViewById(R.id.morningTasksHeader);
        afternoonTasksHeader = view.findViewById(R.id.afternoonTasksHeader);
        nightTasksHeader = view.findViewById(R.id.nightTasksHeader);
        taskCountText = view.findViewById(R.id.taskCountText);
        progressBar = view.findViewById(R.id.progressBar);
        completionText = view.findViewById(R.id.completionText);
        priorityFilterButton = view.findViewById(R.id.priorityFilterButton);
        
        // Initialize smart insight views
        smartInsightCard = view.findViewById(R.id.smartInsightCard);
        smartInsightText = view.findViewById(R.id.smartInsightText);
        
        // Initialize filtered out tasks views
        filteredOutCard = view.findViewById(R.id.filteredOutCard);
        filteredOutHeader = view.findViewById(R.id.filteredOutHeader);
        filteredOutExpandIcon = view.findViewById(R.id.filteredOutExpandIcon);
        filteredOutTitle = view.findViewById(R.id.filteredOutTitle);
        filteredOutCount = view.findViewById(R.id.filteredOutCount);
        filteredOutContent = view.findViewById(R.id.filteredOutContent);
        filteredMorningContainer = view.findViewById(R.id.filteredMorningContainer);
        filteredAfternoonContainer = view.findViewById(R.id.filteredAfternoonContainer);
        filteredNightContainer = view.findViewById(R.id.filteredNightContainer);
        filteredMorningHeader = view.findViewById(R.id.filteredMorningHeader);
        filteredAfternoonHeader = view.findViewById(R.id.filteredAfternoonHeader);
        filteredNightHeader = view.findViewById(R.id.filteredNightHeader);
        
        // Setup filtered section expand/collapse
        if (filteredOutHeader != null) {
            filteredOutHeader.setOnClickListener(v -> toggleFilteredSection());
        }

        if (progressTracker != null) {
            progressTracker.setOnClickListener(v -> showCompletedTasksDialog());
        }

        // Setup priority filter button
        if (priorityFilterButton != null) {
            updateFilterButtonText();
            priorityFilterButton.setOnClickListener(v -> showPriorityFilterDialog());
        }

        // Setup search button
        searchButton = view.findViewById(R.id.searchButton);
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showSearchDialog();
                }
            });
        }

        // Setup dynamic padding for content to avoid being hidden by FABs
        setupDynamicPadding(view);
    }
    
    /**
     * Toggle the filtered out section expand/collapse state with animation.
     */
    private void toggleFilteredSection() {
        isFilteredSectionExpanded = !isFilteredSectionExpanded;
        
        if (filteredOutContent != null) {
            if (isFilteredSectionExpanded) {
                filteredOutContent.setVisibility(View.VISIBLE);
                filteredOutContent.setAlpha(0f);
                filteredOutContent.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
            } else {
                filteredOutContent.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> filteredOutContent.setVisibility(View.GONE))
                    .start();
            }
        }
        
        // Rotate the expand icon
        if (filteredOutExpandIcon != null) {
            filteredOutExpandIcon.animate()
                .rotation(isFilteredSectionExpanded ? 180f : 0f)
                .setDuration(200)
                .start();
        }
    }

    private void setupDynamicPadding(View view) {
        LinearLayout container = view.findViewById(R.id.currentTasksContainer);
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

                // Calculate total reserved space at bottom
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

                // Reserve space for: bottom nav + system nav + FABs (2 stacked) + margins
                // Quick Task FAB:  56dp (height)
                // Spacing:         8dp
                // Main FAB:        56dp (height)
                // Bottom margin:   16dp
                int fabsReservedSpace = (int) ((56 + 8 + 56 + 16) * density); // Two regular FABs + spacing + margin
                int totalBottomPadding = bottomNavHeight + systemBars.bottom + fabsReservedSpace;

                // Set dynamic padding
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

    /**
     * Refresh tasks with smooth fade animation when switching between views.
     * Fades out current content, refreshes data, then fades in new content.
     */
    private void refreshTasksWithAnimation() {
        if (getView() == null) {
            refreshTasks();
            return;
        }
        
        // Collect all visible containers to animate
        java.util.List<View> containersToAnimate = new java.util.ArrayList<>();
        
        // Smart view containers
        if (rightNowSection != null && rightNowSection.getVisibility() == View.VISIBLE) {
            containersToAnimate.add(rightNowSection);
        }
        if (missedTasksSection != null && missedTasksSection.getVisibility() == View.VISIBLE) {
            containersToAnimate.add(missedTasksSection);
        }
        if (laterTodaySection != null && laterTodaySection.getVisibility() == View.VISIBLE) {
            containersToAnimate.add(laterTodaySection);
        }
        
        // Traditional view containers
        if (focusTasksSection != null && focusTasksSection.getVisibility() == View.VISIBLE) {
            containersToAnimate.add(focusTasksSection);
        }
        if (tasksContainerCard != null && tasksContainerCard.getVisibility() == View.VISIBLE) {
            containersToAnimate.add(tasksContainerCard);
        }
        
        // Animation duration
        final long FADE_DURATION = 200L;
        
        if (containersToAnimate.isEmpty()) {
            // No visible containers, just refresh with fade in
            refreshTasks();
            fadeInNewContainers(FADE_DURATION);
            return;
        }
        
        // Fade out all visible containers
        final int[] completedAnimations = {0};
        final int totalAnimations = containersToAnimate.size();
        
        for (View container : containersToAnimate) {
            container.animate()
                .alpha(0f)
                .setDuration(FADE_DURATION)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    completedAnimations[0]++;
                    if (completedAnimations[0] >= totalAnimations) {
                        // All fade-out animations complete, now refresh and fade in
                        refreshTasks();
                        fadeInNewContainers(FADE_DURATION);
                    }
                })
                .start();
        }
    }
    
    /**
     * Fade in all newly visible containers after refresh.
     */
    private void fadeInNewContainers(long duration) {
        java.util.List<View> containersToFadeIn = new java.util.ArrayList<>();
        
        // Check which containers are now visible and need fade-in
        if (rightNowSection != null && rightNowSection.getVisibility() == View.VISIBLE) {
            rightNowSection.setAlpha(0f);
            containersToFadeIn.add(rightNowSection);
        }
        if (missedTasksSection != null && missedTasksSection.getVisibility() == View.VISIBLE) {
            missedTasksSection.setAlpha(0f);
            containersToFadeIn.add(missedTasksSection);
        }
        if (laterTodaySection != null && laterTodaySection.getVisibility() == View.VISIBLE) {
            laterTodaySection.setAlpha(0f);
            containersToFadeIn.add(laterTodaySection);
        }
        if (focusTasksSection != null && focusTasksSection.getVisibility() == View.VISIBLE) {
            focusTasksSection.setAlpha(0f);
            containersToFadeIn.add(focusTasksSection);
        }
        if (tasksContainerCard != null && tasksContainerCard.getVisibility() == View.VISIBLE) {
            tasksContainerCard.setAlpha(0f);
            containersToFadeIn.add(tasksContainerCard);
        }
        
        // Stagger the fade-in animations for a smoother effect
        long staggerDelay = 50L;
        for (int i = 0; i < containersToFadeIn.size(); i++) {
            View container = containersToFadeIn.get(i);
            container.animate()
                .alpha(1f)
                .setDuration(duration)
                .setStartDelay(i * staggerDelay)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        }
    }
    
    /**
     * Play AI sparkle animation when Smart View toggle is switched.
     * Creates a visual "AI magic" effect with multiple sparkles bursting from center.
     */
    private void playAISparkleAnimation(View toggleView, boolean isEnabled) {
        if (toggleView == null || getContext() == null || getActivity() == null) return;
        
        try {
            // Haptic feedback
            toggleView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            
            // Use Activity's DecorView as overlay container
            android.widget.FrameLayout rootView = (android.widget.FrameLayout) 
                getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
            if (rootView == null) {
                animateTogglePulse(toggleView, isEnabled);
                return;
            }
            
            // Get screen dimensions for center positioning
            int screenWidth = rootView.getWidth();
            int screenHeight = rootView.getHeight();
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 3; // Upper third for visibility
            
            // Create multiple sparkle particles for burst effect
            int particleCount = 5;
            int mainSparkleSize = (int) (80 * getResources().getDisplayMetrics().density);
            int smallSparkleSize = (int) (40 * getResources().getDisplayMetrics().density);
            
            // Main center sparkle (large)
            android.widget.ImageView mainSparkle = createSparkleView(mainSparkleSize, isEnabled);
            android.widget.FrameLayout.LayoutParams mainParams = new android.widget.FrameLayout.LayoutParams(
                mainSparkleSize, mainSparkleSize
            );
            mainParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            mainParams.leftMargin = centerX - (mainSparkleSize / 2);
            mainParams.topMargin = centerY - (mainSparkleSize / 2);
            mainSparkle.setLayoutParams(mainParams);
            rootView.addView(mainSparkle);
            
            // Animate main sparkle - big burst
            mainSparkle.animate()
                .alpha(1f)
                .scaleX(2f)
                .scaleY(2f)
                .rotation(isEnabled ? 360f : -360f)
                .setDuration(500)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                .withEndAction(() -> {
                    mainSparkle.animate()
                        .alpha(0f)
                        .scaleX(3f)
                        .scaleY(3f)
                        .setDuration(400)
                        .withEndAction(() -> safeRemoveView(rootView, mainSparkle))
                        .start();
                })
                .start();
            
            // Create surrounding particle sparkles
            float[] angles = {0f, 72f, 144f, 216f, 288f}; // 5 sparkles around
            int radius = (int) (120 * getResources().getDisplayMetrics().density);
            
            for (int i = 0; i < particleCount; i++) {
                final android.widget.ImageView particle = createSparkleView(smallSparkleSize, isEnabled);
                android.widget.FrameLayout.LayoutParams particleParams = new android.widget.FrameLayout.LayoutParams(
                    smallSparkleSize, smallSparkleSize
                );
                particleParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                particleParams.leftMargin = centerX - (smallSparkleSize / 2);
                particleParams.topMargin = centerY - (smallSparkleSize / 2);
                particle.setLayoutParams(particleParams);
                particle.setAlpha(0f);
                particle.setScaleX(0.2f);
                particle.setScaleY(0.2f);
                rootView.addView(particle);
                
                // Calculate end position for radial burst
                double angleRad = Math.toRadians(angles[i]);
                float endX = (float) (Math.cos(angleRad) * radius);
                float endY = (float) (Math.sin(angleRad) * radius);
                
                // Stagger the animation start
                final int delay = i * 60;
                particle.postDelayed(() -> {
                    particle.animate()
                        .alpha(1f)
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .translationX(endX)
                        .translationY(endY)
                        .rotation(isEnabled ? 180f : -180f)
                        .setDuration(400)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(() -> {
                            particle.animate()
                                .alpha(0f)
                                .scaleX(0.5f)
                                .scaleY(0.5f)
                                .setDuration(300)
                                .withEndAction(() -> safeRemoveView(rootView, particle))
                                .start();
                        })
                        .start();
                }, delay);
            }
            
            // Animate the toggle itself
            animateTogglePulse(toggleView, isEnabled);
            
        } catch (Exception e) {
            android.util.Log.e("CurrentTasksFragment", "Sparkle animation error", e);
        }
        
        // Show AI toast message
        String message = isEnabled ? "🤖 Smart View activated!" : "📋 Classic View";
        android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Create a sparkle ImageView with proper styling.
     */
    private android.widget.ImageView createSparkleView(int size, boolean isEnabled) {
        android.widget.ImageView sparkle = new android.widget.ImageView(getContext());
        sparkle.setImageResource(R.drawable.ic_ai_sparkle_base);
        sparkle.setColorFilter(isEnabled ? 
            androidx.core.content.ContextCompat.getColor(getContext(), R.color.purple_primary) : 
            androidx.core.content.ContextCompat.getColor(getContext(), R.color.text_secondary));
        sparkle.setAlpha(0f);
        sparkle.setScaleX(0.3f);
        sparkle.setScaleY(0.3f);
        return sparkle;
    }
    
    /**
     * Safely remove a view from parent, handling exceptions.
     */
    private void safeRemoveView(android.view.ViewGroup parent, View child) {
        try {
            if (parent != null && child != null) {
                parent.removeView(child);
            }
        } catch (Exception e) {
            // Ignore
        }
    }
    
    /**
     * Animate the toggle button with a subtle pulse effect.
     */
    private void animateTogglePulse(View toggleView, boolean isEnabled) {
        if (toggleView == null) return;
        
        toggleView.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .setInterpolator(new android.view.animation.OvershootInterpolator())
            .withEndAction(() -> {
                toggleView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
            })
            .start();
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

        // Clear all containers
        if (focusTasksContainer != null) focusTasksContainer.removeAllViews();
        if (rightNowContainer != null) rightNowContainer.removeAllViews();
        if (missedTasksContainer != null) missedTasksContainer.removeAllViews();
        if (laterTodayContainer != null) laterTodayContainer.removeAllViews();
        if (morningTasksContainer != null) morningTasksContainer.removeAllViews();
        if (afternoonTasksContainer != null) afternoonTasksContainer.removeAllViews();
        if (nightTasksContainer != null) nightTasksContainer.removeAllViews();

        // Get today's date once
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new java.util.Date());
        
        // Determine if we're actively filtering by priority
        final boolean isFiltering = !"All".equals(currentPriorityFilter);
        
        // Clear filtered containers
        if (filteredMorningContainer != null) filteredMorningContainer.removeAllViews();
        if (filteredAfternoonContainer != null) filteredAfternoonContainer.removeAllViews();
        if (filteredNightContainer != null) filteredNightContainer.removeAllViews();
        
        // Get current time period
        String currentPeriod = getCurrentTimePeriod();
        
        // Collect ALL tasks for today (both focus and regular)
        ArrayList<Task> allTodayTasks = new ArrayList<>();
        collectAllTasksForToday(morningTasks, todayDate, allTodayTasks);
        collectAllTasksForToday(afternoonTasks, todayDate, allTodayTasks);
        collectAllTasksForToday(nightTasks, todayDate, allTodayTasks);
        
        // Categorize tasks into: current period, missed (past), and later (future)
        ArrayList<Task> currentPeriodTasks = new ArrayList<>();
        ArrayList<Task> missedTasks = new ArrayList<>();
        ArrayList<Task> laterTasks = new ArrayList<>();
        
        categorizeTasksByTimePeriod(allTodayTasks, currentPeriod, currentPeriodTasks, missedTasks, laterTasks);
        
        // Sort each list by start time
        sortTasksByStartTime(currentPeriodTasks);
        sortTasksByStartTime(missedTasks);
        sortTasksByStartTime(laterTasks);
        
        // Populate time-aware sections when smart view is ON and not filtering
        if (isSmartViewEnabled && !isFiltering) {
            // Populate "Right Now" section - current period tasks (both focus + regular)
            if (rightNowContainer != null) {
                for (Task task : currentPeriodTasks) {
                    if (task.isComplete) continue; // Skip completed tasks
                    View taskView = createTaskView(task, task.isFocusTask(), rightNowContainer);
                    rightNowContainer.addView(taskView);
                }
            }
            
            // Update subtitle with current period name
            if (rightNowSubtitle != null) {
                String periodName = getPeriodDisplayName(currentPeriod);
                rightNowSubtitle.setText(periodName + " tasks • Focus sessions & reminders");
            }
            
            // Populate "Missed" section - past period incomplete tasks
            if (missedTasksContainer != null) {
                for (Task task : missedTasks) {
                    if (task.isComplete) continue; // Skip completed tasks
                    View taskView = createTaskViewWithMissedBadge(task, task.isFocusTask(), missedTasksContainer);
                    missedTasksContainer.addView(taskView);
                }
            }
            
            // Populate "Later Today" section - future period tasks
            if (laterTodayContainer != null) {
                for (Task task : laterTasks) {
                    if (task.isComplete) continue; // Skip completed tasks
                    View taskView = createTaskView(task, task.isFocusTask(), laterTodayContainer);
                    laterTodayContainer.addView(taskView);
                }
            }
            
            // Update smart insight
            updateSmartInsight();
        }
        
        // Process regular task lists when:
        // 1. Smart view is OFF (traditional view)
        // 2. Priority filtering is active
        boolean useTraditionalView = !isSmartViewEnabled || isFiltering;
        
        // In traditional view, also populate focus tasks container
        if (useTraditionalView && focusTasksContainer != null) {
            ArrayList<Task> todayFocusTasks = new ArrayList<>();
            collectFocusTasksForToday(morningTasks, todayDate, todayFocusTasks);
            collectFocusTasksForToday(afternoonTasks, todayDate, todayFocusTasks);
            collectFocusTasksForToday(nightTasks, todayDate, todayFocusTasks);
            
            for (Task focusTask : todayFocusTasks) {
                if (focusTask.isComplete) continue;
                View taskView = createTaskView(focusTask, true, focusTasksContainer);
                focusTasksContainer.addView(taskView);
            }
        }
        
        int filteredCount = 0;
        filteredCount += processTaskListWithFilter(morningTasks, todayDate, morningTasksContainer, 
            filteredMorningContainer, isFiltering, useTraditionalView);
        filteredCount += processTaskListWithFilter(afternoonTasks, todayDate, afternoonTasksContainer, 
            filteredAfternoonContainer, isFiltering, useTraditionalView);
        filteredCount += processTaskListWithFilter(nightTasks, todayDate, nightTasksContainer, 
            filteredNightContainer, isFiltering, useTraditionalView);
        final int finalFilteredCount = filteredCount;

        // Count tasks efficiently
        int totalTasks = 0;
        int completedTasks = 0;
        boolean hasCurrentPeriodTasks = false;
        boolean hasMissedTasks = false;
        boolean hasLaterTasks = false;
        
        for (Task task : currentPeriodTasks) {
            if (!task.isComplete) hasCurrentPeriodTasks = true;
        }
        for (Task task : missedTasks) {
            if (!task.isComplete) hasMissedTasks = true;
        }
        for (Task task : laterTasks) {
            if (!task.isComplete) hasLaterTasks = true;
        }
        
        boolean hasReminderTasksForToday = false;

        // Combined counting loop - process all task lists together
        @SuppressWarnings("unchecked")
        ArrayList<Task>[] allTaskLists = new ArrayList[]{morningTasks, afternoonTasks, nightTasks};

        for (ArrayList<Task> taskList : allTaskLists) {
            for (Task task : taskList) {
                if (task != null && task.date != null && task.date.equals(todayDate)) {
                    // Apply priority filter to count as well
                    if (!shouldShowTask(task)) continue;
                    
                    totalTasks++;
                    if (task.isComplete) {
                        completedTasks++;
                    } else {
                        hasReminderTasksForToday = true;
                    }
                }
            }
        }

        // Update UI with calculated values
        if (taskCountText != null) {
            taskCountText.setText(String.format(Locale.getDefault(), "Task %d/%d", completedTasks, totalTasks));
        }
        if (progressBar != null && totalTasks > 0) {
            int progress = (completedTasks * 100) / totalTasks;
            progressBar.setProgress(progress);
            if (completionText != null) {
                completionText.setText(String.format(Locale.getDefault(), "%d%% completed", progress));
            }
        } else if (completionText != null) {
            completionText.setText("0% completed");
        }

        // Show/hide time-aware sections based on smart view toggle
        boolean showSmartView = isSmartViewEnabled && !isFiltering;
        
        if (rightNowSection != null) {
            rightNowSection.setVisibility(hasCurrentPeriodTasks && showSmartView ? View.VISIBLE : View.GONE);
        }
        
        if (missedTasksSection != null) {
            missedTasksSection.setVisibility(hasMissedTasks && showSmartView ? View.VISIBLE : View.GONE);
        }
        
        if (laterTodaySection != null) {
            laterTodaySection.setVisibility(hasLaterTasks && showSmartView ? View.VISIBLE : View.GONE);
        }

        // Show focus sessions section in traditional view if there are focus tasks
        boolean hasFocusTasks = focusTasksContainer != null && focusTasksContainer.getChildCount() > 0;
        if (focusTasksSection != null) {
            // Show focus section in traditional view (smart view OFF), hide in smart view
            focusTasksSection.setVisibility(!showSmartView && hasFocusTasks ? View.VISIBLE : View.GONE);
        }

        // Handle task containers visibility
        boolean hasTimeAwareTasks = (hasCurrentPeriodTasks || hasMissedTasks || hasLaterTasks) && showSmartView;
        
        if (hasTimeAwareTasks) {
            // Hide regular task containers when time-aware mode is active
            if (tasksContainerCard != null) tasksContainerCard.setVisibility(View.GONE);
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.GONE);
        } else if (!isSmartViewEnabled && hasReminderTasksForToday) {
            // Smart view OFF - show traditional morning/afternoon/night containers
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.GONE);
            if (tasksContainerCard != null) tasksContainerCard.setVisibility(View.VISIBLE);
        } else if (!hasReminderTasksForToday && !isFiltering) {
            // No tasks at all
            if (tasksContainerCard != null) tasksContainerCard.setVisibility(View.GONE);
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
        } else if (isFiltering) {
            // Filtering mode - show regular containers
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.GONE);
            if (tasksContainerCard != null) tasksContainerCard.setVisibility(hasReminderTasksForToday ? View.VISIBLE : View.GONE);
        }

        if (morningTasksHeader != null && morningTasksContainer != null) {
            morningTasksHeader.setVisibility(morningTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
        if (afternoonTasksHeader != null && afternoonTasksContainer != null) {
            afternoonTasksHeader.setVisibility(afternoonTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
        if (nightTasksHeader != null && nightTasksContainer != null) {
            nightTasksHeader.setVisibility(nightTasksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
        
        // Handle filtered out section - visible when filtering and has deprioritized tasks
        if (filteredOutCard != null) {
            if (isFiltering && finalFilteredCount > 0) {
                filteredOutCard.setVisibility(View.VISIBLE);
                if (filteredOutCount != null) {
                    filteredOutCount.setText(String.valueOf(finalFilteredCount));
                }
                if (filteredOutTitle != null) {
                    filteredOutTitle.setText("Other Tasks (not " + currentPriorityFilter + " priority)");
                }
                
                // Update filtered section headers visibility
                if (filteredMorningHeader != null && filteredMorningContainer != null) {
                    filteredMorningHeader.setVisibility(filteredMorningContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
                }
                if (filteredAfternoonHeader != null && filteredAfternoonContainer != null) {
                    filteredAfternoonHeader.setVisibility(filteredAfternoonContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
                }
                if (filteredNightHeader != null && filteredNightContainer != null) {
                    filteredNightHeader.setVisibility(filteredNightContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE);
                }
            } else {
                filteredOutCard.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Collect all tasks (both focus and regular) for today.
     */
    private void collectAllTasksForToday(ArrayList<Task> tasks, String todayDate, ArrayList<Task> result) {
        if (tasks == null) return;
        for (Task task : tasks) {
            if (task != null && task.date != null && task.date.equals(todayDate)) {
                result.add(task);
            }
        }
    }
    
    /**
     * Categorize tasks into current period, missed (past), and later (future).
     */
    private void categorizeTasksByTimePeriod(ArrayList<Task> allTasks, String currentPeriod,
                                             ArrayList<Task> currentPeriodTasks,
                                             ArrayList<Task> missedTasks,
                                             ArrayList<Task> laterTasks) {
        for (Task task : allTasks) {
            String taskPeriod = getTaskTimePeriod(task);
            
            if (taskPeriod.equals(currentPeriod)) {
                // Task is in current time period
                currentPeriodTasks.add(task);
            } else if (isTaskPeriodPast(taskPeriod, currentPeriod)) {
                // Task is from a past time period - missed
                missedTasks.add(task);
            } else {
                // Task is for a future time period - later
                laterTasks.add(task);
            }
        }
    }
    
    /**
     * Check if a task's time period is in the past relative to current period.
     * Time flow: morning -> afternoon -> night
     */
    private boolean isTaskPeriodPast(String taskPeriod, String currentPeriod) {
        // Define period order: morning (0) -> afternoon (1) -> night (2)
        int taskOrder = getPeriodOrder(taskPeriod);
        int currentOrder = getPeriodOrder(currentPeriod);
        
        return taskOrder < currentOrder;
    }
    
    /**
     * Get numeric order for time period comparison.
     */
    private int getPeriodOrder(String period) {
        switch (period) {
            case "morning": return 0;
            case "afternoon": return 1;
            case "night": return 2;
            default: return 1; // Default to afternoon
        }
    }
    
    /**
     * Get display name for a time period.
     */
    private String getPeriodDisplayName(String period) {
        switch (period) {
            case "morning": return "🌅 Morning";
            case "afternoon": return "🌤️ Afternoon";
            case "night": return "🌙 Evening";
            default: return "📋 Current";
        }
    }
    
    /**
     * Get category text with emoji prefix.
     */
    private String getCategoryWithEmoji(String category) {
        if (category == null) return "";
        switch (category) {
            case "Work": return "💼 Work";
            case "Personal": return "🏠 Personal";
            case "Health": return "💪 Health";
            case "Learning": return "📚 Learning";
            case "Finance": return "💰 Finance";
            case "Social": return "👥 Social";
            case "School": return "🎓 School";
            case "Shopping": return "🛒 Shopping";
            case "Travel": return "✈️ Travel";
            case "Entertainment": return "🎬 Entertainment";
            default: return "📋 " + category;
        }
    }
    
    /**
     * Get priority text with emoji prefix.
     */
    private String getPriorityWithEmoji(String priority) {
        if (priority == null) return "⚪ None";
        switch (priority) {
            case "High": return "🔴 High Priority";
            case "Medium": return "🟡 Medium Priority";
            case "Low": return "🟢 Low Priority";
            default: return "⚪ No Priority";
        }
    }
    
    /**
     * Generate AI insight text for a task.
     */
    private String generateTaskInsight(Task task, AIModelHelper aiHelper) {
        if (task == null || task.name == null) return null;
        
        StringBuilder insight = new StringBuilder();
        
        // Get AI suggestions
        AIModelHelper.TaskSuggestions suggestions = aiHelper.getTaskSuggestions(task.name);
        
        if (suggestions != null) {
            // Add priority insight
            if (suggestions.priority != null) {
                String reason = suggestions.priority.getReason();
                if (reason != null && !reason.isEmpty()) {
                    insight.append(reason);
                }
            }
            
            // Add time insight for non-focus tasks
            if (!task.isFocusTask() && suggestions.timeOfDay != null) {
                String currentPeriod = getCurrentTimePeriod();
                String suggestedTime = suggestions.timeOfDay.getValue();
                if (suggestedTime != null && !suggestedTime.equals(currentPeriod)) {
                    if (insight.length() > 0) insight.append(" ");
                    insight.append("Best done in the ").append(suggestedTime).append(".");
                }
            }
            
            // Add duration insight for focus tasks
            if (task.isFocusTask() && suggestions.focusDuration != null) {
                if (insight.length() > 0) insight.append(" ");
                insight.append(suggestions.focusDuration.getReason());
            }
        }
        
        // Add contextual insight based on task characteristics
        if (insight.length() == 0) {
            if (task.isFocusTask()) {
                insight.append("Deep focus sessions help you achieve flow state for complex tasks.");
            } else if ("High".equals(task.urgency)) {
                insight.append("High priority tasks benefit from immediate attention.");
            } else {
                insight.append("Breaking tasks into smaller steps increases completion rate.");
            }
        }
        
        return insight.toString();
    }
    
    /**
     * Sort tasks by their start time.
     */
    private void sortTasksByStartTime(ArrayList<Task> tasks) {
        java.util.Collections.sort(tasks, (t1, t2) -> {
            int time1 = convertTo24Hour(t1.hour, t1.amPm) * 60 + t1.minute;
            int time2 = convertTo24Hour(t2.hour, t2.amPm) * 60 + t2.minute;
            return Integer.compare(time1, time2);
        });
    }
    
    /**
     * Create a task view with a "Missed" badge indicator.
     */
    private View createTaskViewWithMissedBadge(Task task, boolean isFocusTaskView, ViewGroup parent) {
        View taskView = createTaskView(task, isFocusTaskView, parent);
        
        // Add visual indicator for missed status
        try {
            // Add a red/warning tint to the card to indicate missed status
            if (taskView instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView card = 
                    (com.google.android.material.card.MaterialCardView) taskView;
                card.setStrokeColor(getResources().getColor(R.color.error, null));
                card.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            }
            
            // Also update the task name to show missed indicator
            TextView taskNameView = taskView.findViewById(R.id.taskName);
            if (taskNameView != null && task.name != null) {
                taskNameView.setText("⚠️ " + task.name);
            }
        } catch (Exception e) {
            // Silently handle - the view will still work without the badge
        }
        
        return taskView;
    }

    /**
     * Old processTaskList method without filtering support.
     * @deprecated Use processTaskListWithFilter for filtering support.
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    private void processTaskList(ArrayList<Task> tasks, String todayDate, LinearLayout container, LinearLayout focusContainer) {
        if (tasks == null || container == null) return;

        for (Task task : tasks) {
            if (task == null || task.date == null) continue;
            if (!task.date.equals(todayDate) || task.isComplete) continue;

            if (task.isFocusTask() && focusContainer != null) {
                View taskView = createTaskView(task, true, focusContainer);
                focusContainer.addView(taskView);
            } else {
                View taskView = createTaskView(task, false, container);
                container.addView(taskView);
            }
        }
    }

    private View createTaskView(Task task, boolean isFocusTaskView, ViewGroup parent) {
        if (task == null || getContext() == null) return new View(getContext());

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View taskView;

        if (isFocusTaskView) {
            taskView = inflater.inflate(R.layout.focus_task_item, parent, false);
        } else {
            taskView = inflater.inflate(R.layout.task_item, parent, false);
        }

        // Setup task view with click listeners
        setupTaskView(taskView, task);

        return taskView;
    }

    private void setupTaskView(View taskView, Task task) {
        TextView taskNameTextView = taskView.findViewById(R.id.taskName);
        TextView taskTimeTextView = taskView.findViewById(R.id.taskTime);
        TextView categoryChip = taskView.findViewById(R.id.categoryChip);
        SwitchCompat taskSwitch = taskView.findViewById(R.id.taskSwitch);
        View taskContent = taskView.findViewById(R.id.taskContent);
        
        // Get task type icon for notification preview
        View taskTypeIcon = taskView.findViewById(R.id.taskTypeIcon);
        // For focus_task_item, the icon might have a different parent
        View iconContainer = taskView.findViewWithTag("iconContainer");
        if (iconContainer == null) {
            // Try finding the first MaterialCardView child that contains the icon
            android.view.ViewGroup parent = (android.view.ViewGroup) taskView;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof com.google.android.material.card.MaterialCardView) {
                    iconContainer = child;
                    break;
                }
            }
        }

        if (taskNameTextView != null) {
            taskNameTextView.setText(task.name);
        }

        if (taskTimeTextView != null) {
            if (task.isFocusTask()) {
                String timeRange = String.format(Locale.getDefault(), "%d:%02d %s → %d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM",
                        task.endHour, task.endMinute, task.endAmPm != null ? task.endAmPm : "AM");
                taskTimeTextView.setText(timeRange);
            } else {
                taskTimeTextView.setText(String.format(Locale.getDefault(), "%d:%02d %s",
                        task.hour, task.minute, task.amPm != null ? task.amPm : "AM"));
            }
        }
        
        // Display category chip if task has a category
        if (categoryChip != null) {
            if (task.category != null && !task.category.isEmpty()) {
                categoryChip.setVisibility(View.VISIBLE);
                categoryChip.setText(getCategoryWithEmoji(task.category));
            } else {
                // Try to detect category using AI if not set
                android.content.Context ctx = getContext();
                if (ctx != null && task.name != null && !task.name.isEmpty() && AIModelHelper.isEnabled(ctx)) {
                    AIModelHelper aiHelper = AIModelHelper.getInstance(ctx);
                    AIModelHelper.AIPrediction categoryPrediction = aiHelper.predictCategory(task.name);
                    if (categoryPrediction != null && categoryPrediction.getValue() != null) {
                        categoryChip.setVisibility(View.VISIBLE);
                        categoryChip.setText(getCategoryWithEmoji(categoryPrediction.getValue()));
                        // Also save the detected category
                        task.category = categoryPrediction.getValue();
                        if (taskRepository != null) {
                            taskRepository.updateTask(task);
                        }
                    } else {
                        categoryChip.setVisibility(View.GONE);
                    }
                } else {
                    categoryChip.setVisibility(View.GONE);
                }
            }
        }
        
        // Add click listener on task icon to show notification preview
        View clickableIcon = taskTypeIcon != null ? taskTypeIcon : iconContainer;
        if (clickableIcon != null) {
            clickableIcon.setClickable(true);
            clickableIcon.setOnClickListener(v -> {
                // Show notification preview popup
                showNotificationPreviewPopup(v, task, task.isFocusTask());
            });
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
                    borderColor = task.isFocusTask() ? 
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary) :
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success);
                    break;
            }
            // Set stroke width and color to make border visible
            int strokeWidth = (int) (3 * getResources().getDisplayMetrics().density); // 3dp
            cardView.setStrokeWidth(strokeWidth);
            cardView.setStrokeColor(borderColor);
        }

        if (taskSwitch != null) {
            taskSwitch.setChecked(task.isAlarmOn);
            taskSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.isAlarmOn = isChecked;
                taskRepository.updateTask(task);
                if (isChecked) {
                    if (task.isFocusTask()) {
                        AlarmHelper.scheduleFocusTaskAlarms(getContext(), task);
                    } else {
                        AlarmHelper.scheduleTaskAlarm(getContext(), task);
                    }
                    Toast.makeText(getContext(), "🔔 Alerts enabled", Toast.LENGTH_SHORT).show();
                } else {
                    if (task.isFocusTask()) {
                        AlarmHelper.cancelFocusTaskAlarms(getContext(), task);
                    } else {
                        AlarmHelper.cancelTaskAlarm(getContext(), task);
                    }
                    Toast.makeText(getContext(), "🔕 Alerts disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Make entire task clickable to edit
        if (taskContent != null) {
            taskContent.setOnClickListener(v -> openTaskForEditing(task));
            
            // Long press with 3D touch effect
            taskContent.setOnLongClickListener(v -> {
                // Apply 3D touch scale effect
                taskView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        taskView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                        
                        // Haptic feedback
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        
                        // Show quick info popup - same as UpcomingTasksFragment
                        showQuickInfoPopup(v, task, task.isFocusTask());
                    })
                    .start();
                return true;
            });
        }

        // Delete button
        com.google.android.material.button.MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                String title = task.isFocusTask() ? "Move to Trash?" : "Move to Trash?";
                String message = "{item} will be moved to the trash bin. You can restore it later.";
                
                ModernDialogHelper.showDestructiveDialog(
                    getContext(),
                    title,
                    message,
                    task.name,
                    R.drawable.ic_delete,
                    () -> {
                        // Animate slide-to-right deletion
                        animateTaskDeletion(taskView, () -> {
                            if (task.isFocusTask()) {
                                AlarmHelper.cancelFocusTaskAlarms(getContext(), task);
                            } else {
                                AlarmHelper.cancelTaskAlarm(getContext(), task);
                            }
                            taskRepository.deleteTask(task);
                            // Repository already updated - just refresh UI
                            refreshTasks();
                            Toast.makeText(getContext(), (task.isFocusTask() ? "Focus session" : "Task") + " moved to trash", Toast.LENGTH_SHORT).show();
                        });
                    },
                    null
                );
            });
        }
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
        ImageView typeIcon = popupView.findViewById(R.id.typeIcon);
        ImageView alarmIcon = popupView.findViewById(R.id.alarmIcon);
        com.google.android.material.card.MaterialCardView iconContainer = popupView.findViewById(R.id.typeIconContainer);

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
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
                java.util.Date date = inputFormat.parse(task.date);
                if (date != null) {
                    dateText.setText(outputFormat.format(date));
                }
            } catch (java.text.ParseException e) {
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

        // Set alarm status
        if (alarmText != null && alarmIcon != null) {
            if (task.isAlarmOn) {
                alarmText.setText("Alerts enabled");
                alarmIcon.setImageResource(R.drawable.ic_reminder);
            } else {
                alarmText.setText("Alerts disabled");
                alarmIcon.setImageResource(R.drawable.ic_reminder);
            }
        }
        
        // Set category (NEW)
        View categoryRow = popupView.findViewById(R.id.categoryRow);
        TextView categoryText = popupView.findViewById(R.id.quickInfoCategory);
        if (categoryRow != null && categoryText != null) {
            String category = task.category;
            // If no category set, try AI detection
            android.content.Context ctx = getContext();
            if ((category == null || category.isEmpty()) && task.name != null && ctx != null && AIModelHelper.isEnabled(ctx)) {
                AIModelHelper aiHelper = AIModelHelper.getInstance(ctx);
                AIModelHelper.AIPrediction categoryPrediction = aiHelper.predictCategory(task.name);
                if (categoryPrediction != null) {
                    category = categoryPrediction.getValue();
                }
            }
            
            if (category != null && !category.isEmpty()) {
                categoryRow.setVisibility(View.VISIBLE);
                categoryText.setText(getCategoryWithEmoji(category));
            } else {
                categoryRow.setVisibility(View.GONE);
            }
        }
        
        // Set priority (NEW)
        View priorityRow = popupView.findViewById(R.id.priorityRow);
        TextView priorityText = popupView.findViewById(R.id.quickInfoPriority);
        if (priorityRow != null && priorityText != null) {
            if (task.urgency != null && !task.urgency.isEmpty() && !"None".equals(task.urgency)) {
                priorityRow.setVisibility(View.VISIBLE);
                priorityText.setText(getPriorityWithEmoji(task.urgency));
            } else {
                priorityRow.setVisibility(View.GONE);
            }
        }
        
        // Set AI Insight (NEW)
        View aiInsightCard = popupView.findViewById(R.id.aiInsightCard);
        TextView aiInsightText = popupView.findViewById(R.id.aiInsightText);
        android.content.Context insightCtx = getContext();
        if (aiInsightCard != null && aiInsightText != null && insightCtx != null && AIModelHelper.isEnabled(insightCtx)) {
            AIModelHelper aiHelper = AIModelHelper.getInstance(insightCtx);
            String insight = generateTaskInsight(task, aiHelper);
            if (insight != null && !insight.isEmpty()) {
                aiInsightCard.setVisibility(View.VISIBLE);
                aiInsightText.setText(insight);
            } else {
                aiInsightCard.setVisibility(View.GONE);
            }
        } else if (aiInsightCard != null) {
            aiInsightCard.setVisibility(View.GONE);
        }

        // Show popup at center of screen
        popupWindow.showAtLocation(anchorView, android.view.Gravity.CENTER, 0, 0);
        
        // Add haptic feedback
        anchorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

        // Tap popup to edit
        popupView.setOnClickListener(v -> {
            popupWindow.dismiss();
            openTaskForEditing(task);
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
        TextView titleText = dialog.findViewById(R.id.notificationTitle);
        TextView typeText = dialog.findViewById(R.id.notificationType);
        TextView timeText = dialog.findViewById(R.id.notificationTime);
        TextView messageText = dialog.findViewById(R.id.notificationMessage);
        ImageView iconView = dialog.findViewById(R.id.notificationIcon);
        com.google.android.material.card.MaterialCardView iconContainer = dialog.findViewById(R.id.notificationIconContainer);
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
            actionButton.setText(isFocusSession ? "Start Session" : "Mark Complete");
            actionButton.setOnClickListener(v -> {
                if (!isFocusSession) {
                    // Mark task as complete
                    task.isComplete = true;
                    taskRepository.updateTask(task);
                    refreshTasks();
                    Toast.makeText(getContext(), "Task marked complete!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Focus session started!", Toast.LENGTH_SHORT).show();
                }
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
    
    private int convertTo24Hour(int hour, String amPm) {
        if (amPm == null) amPm = "AM";
        if (hour == 12) {
            return amPm.equals("AM") ? 0 : 12;
        } else {
            return amPm.equals("PM") ? hour + 12 : hour;
        }
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

    private void showCompletedTasksDialog() {
        // Show completed tasks dialog
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).triggerShowCompletedTasksDialog();
        }
    }

    private void showPriorityFilterDialog() {
        String[] filterOptions = {"All", "Low Priority", "Medium Priority", "High Priority"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Filter by Priority");

        builder.setItems(filterOptions, (dialog, which) -> {
            switch (which) {
                case 0:
                    currentPriorityFilter = "All";
                    break;
                case 1:
                    currentPriorityFilter = "Low";
                    break;
                case 2:
                    currentPriorityFilter = "Medium";
                    break;
                case 3:
                    currentPriorityFilter = "High";
                    break;
            }
            updateFilterButtonText();
            refreshTasks();
            Toast.makeText(getContext(), "Filtered by: " + currentPriorityFilter, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateFilterButtonText() {
        if (priorityFilterButton != null) {
            if ("All".equals(currentPriorityFilter)) {
                priorityFilterButton.setText("Priority: All");
                priorityFilterButton.setIconResource(R.drawable.ic_filter);
            } else {
                priorityFilterButton.setText("Priority: " + currentPriorityFilter);
                priorityFilterButton.setIconResource(R.drawable.ic_filter);
            }
        }
    }
    
    /**
     * Load smart view preference from SharedPreferences
     */
    private void loadSmartViewPreference() {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isSmartViewEnabled = prefs.getBoolean(PREF_SMART_VIEW, true); // Default to ON
    }
    
    /**
     * Save smart view preference to SharedPreferences
     */
    private void saveSmartViewPreference(boolean enabled) {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_SMART_VIEW, enabled).apply();
    }
    
    /**
     * Create a time period header view (e.g., "☀️ Morning - Current", "🌙 Night - Later")
     */
    private View createTimePeriodHeader(String timePeriod, boolean isCurrentPeriod) {
        if (getContext() == null) return new View(getContext());
        
        TextView header = new TextView(getContext());
        
        String emoji;
        String label;
        switch (timePeriod) {
            case "morning":
                emoji = "☀️";
                label = "Morning";
                break;
            case "afternoon":
                emoji = "🌤️";
                label = "Afternoon";
                break;
            default:
                emoji = "🌙";
                label = "Night";
                break;
        }
        
        String text = emoji + " " + label;
        if (isCurrentPeriod) {
            text += " — Now";
        } else {
            text += " — Later";
        }
        
        header.setText(text);
        header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        header.setTextColor(isCurrentPeriod ? 
            androidx.core.content.ContextCompat.getColor(getContext(), R.color.primary) :
            androidx.core.content.ContextCompat.getColor(getContext(), R.color.text_secondary));
        
        if (isCurrentPeriod) {
            header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        }
        
        // Padding
        int paddingH = (int) (16 * getResources().getDisplayMetrics().density);
        int paddingTop = (int) (12 * getResources().getDisplayMetrics().density);
        int paddingBottom = (int) (4 * getResources().getDisplayMetrics().density);
        header.setPadding(paddingH, paddingTop, paddingH, paddingBottom);
        
        return header;
    }
    
    /**
     * Update the smart insight card with AI-powered productivity tip
     */
    private void updateSmartInsight() {
        if (getContext() == null) return;
        
        // Check if smart suggestions are enabled
        if (!AIModelHelper.isInsightsEnabled(getContext())) {
            if (smartInsightCard != null) {
                smartInsightCard.setVisibility(View.GONE);
            }
            return;
        }
        
        if (smartInsightCard != null) {
            smartInsightCard.setVisibility(View.VISIBLE);
        }
        
        if (smartInsightText != null) {
            // Use AIModelHelper for richer ML-backed insights
            AIModelHelper aiHelper = AIModelHelper.getInstance(getContext());
            String insight = aiHelper.getProductivityInsight();
            smartInsightText.setText(insight);
        }
    }

    private boolean shouldShowTask(Task task) {
        if ("All".equals(currentPriorityFilter)) {
            return true;
        }

        // Check if task priority matches current filter
        if (task.urgency == null || task.urgency.isEmpty()) {
            // If task has no priority, only show it when filter is "All" (already checked above)
            return false;
        }

        return task.urgency.equalsIgnoreCase(currentPriorityFilter);
    }
    
    /**
     * Collect all focus tasks for today from a task list.
     */
    private void collectFocusTasksForToday(ArrayList<Task> tasks, String todayDate, ArrayList<Task> focusTasks) {
        if (tasks == null) return;
        for (Task task : tasks) {
            if (task != null && task.isFocusTask() && task.date != null 
                    && task.date.equals(todayDate) && !task.isComplete) {
                focusTasks.add(task);
            }
        }
    }
    
    /**
     * Collect all regular (non-focus) tasks for today from a task list.
     */
    private void collectRegularTasksForToday(ArrayList<Task> tasks, String todayDate, ArrayList<Task> regularTasks) {
        if (tasks == null) return;
        for (Task task : tasks) {
            if (task != null && !task.isFocusTask() && task.date != null 
                    && task.date.equals(todayDate)) {
                regularTasks.add(task);
            }
        }
    }
    
    /**
     * Get the current time period based on system time.
     * @return "morning" (5 AM - 11:59 AM), "afternoon" (12 PM - 5:59 PM), or "night" (6 PM - 4:59 AM)
     */
    private String getCurrentTimePeriod() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) {
            return "morning";
        } else if (hour >= 12 && hour < 18) {
            return "afternoon";
        } else {
            return "night";
        }
    }
    
    /**
     * Get the time period for a task based on its start time.
     */
    private String getTaskTimePeriod(Task task) {
        int hour24 = convertTo24Hour(task.hour, task.amPm);
        
        if (hour24 >= 5 && hour24 < 12) {
            return "morning";
        } else if (hour24 >= 12 && hour24 < 18) {
            return "afternoon";
        } else {
            return "night";
        }
    }
    
    /**
     * Sort focus tasks by time awareness - tasks in the current time period come first,
     * then sorted by start time within each period.
     */
    private void sortFocusTasksByTimeAwareness(ArrayList<Task> focusTasks) {
        if (focusTasks == null || focusTasks.size() <= 1) return;
        
        final String currentPeriod = getCurrentTimePeriod();
        
        // Sort: current time period first, then by start time
        java.util.Collections.sort(focusTasks, (t1, t2) -> {
            String period1 = getTaskTimePeriod(t1);
            String period2 = getTaskTimePeriod(t2);
            
            boolean t1InCurrentPeriod = period1.equals(currentPeriod);
            boolean t2InCurrentPeriod = period2.equals(currentPeriod);
            
            // Tasks in current period come first
            if (t1InCurrentPeriod && !t2InCurrentPeriod) return -1;
            if (!t1InCurrentPeriod && t2InCurrentPeriod) return 1;
            
            // If both in same period status, sort by time period order (morning -> afternoon -> night)
            if (!t1InCurrentPeriod && !t2InCurrentPeriod) {
                int periodOrder1 = getTimePeriodOrder(period1, currentPeriod);
                int periodOrder2 = getTimePeriodOrder(period2, currentPeriod);
                if (periodOrder1 != periodOrder2) return periodOrder1 - periodOrder2;
            }
            
            // Within same period, sort by start time
            int time1 = convertTo24Hour(t1.hour, t1.amPm) * 60 + t1.minute;
            int time2 = convertTo24Hour(t2.hour, t2.amPm) * 60 + t2.minute;
            return time1 - time2;
        });
    }
    
    /**
     * Get the order of a time period relative to the current period.
     * Periods closer to current time come first.
     */
    private int getTimePeriodOrder(String period, String currentPeriod) {
        // Order based on what comes next after current period
        if (currentPeriod.equals("morning")) {
            if (period.equals("morning")) return 0;
            if (period.equals("afternoon")) return 1;
            return 2; // night
        } else if (currentPeriod.equals("afternoon")) {
            if (period.equals("afternoon")) return 0;
            if (period.equals("night")) return 1;
            return 2; // morning (next day conceptually)
        } else { // night
            if (period.equals("night")) return 0;
            if (period.equals("morning")) return 1;
            return 2; // afternoon
        }
    }
    
    /**
     * Process tasks with filtering support - adds tasks to appropriate containers.
     * Tasks that match the filter go to the main container, non-matching go to filtered container.
     * Focus tasks are skipped here - they're handled separately with time-aware sorting.
     * @param useTraditionalView If true, always populate containers (smart view OFF or filtering)
     * @return The number of filtered-out (non-matching) tasks added
     */
    private int processTaskListWithFilter(ArrayList<Task> tasks, String todayDate, 
            LinearLayout container, LinearLayout filteredContainer, boolean isFiltering, boolean useTraditionalView) {
        if (tasks == null || container == null) return 0;
        
        // Only process if we're using traditional view or filtering
        if (!useTraditionalView) return 0;
        
        int filteredOutCount = 0;

        for (Task task : tasks) {
            if (task == null || task.date == null) continue;
            if (!task.date.equals(todayDate) || task.isComplete) continue;
            
            // Skip focus tasks - they're handled separately with time-aware sorting
            if (task.isFocusTask()) continue;

            // Regular tasks - check if matches filter
            if (shouldShowTask(task)) {
                View taskView = createTaskView(task, false, container);
                container.addView(taskView);
            } else if (isFiltering && filteredContainer != null) {
                // Task doesn't match filter - add to filtered container (visible at bottom)
                View taskView = createTaskView(task, false, filteredContainer);
                taskView.setAlpha(0.7f); // Slightly dimmed
                filteredContainer.addView(taskView);
                filteredOutCount++;
            }
        }
        
        return filteredOutCount;
    }
}
