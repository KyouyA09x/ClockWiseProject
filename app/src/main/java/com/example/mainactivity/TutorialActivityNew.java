package com.example.mainactivity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;

import java.util.Locale;

/**
 * Interactive tutorial overlaying the actual app UI with Material Design 3 styling
 * Uses REAL MainActivity UI with sample data
 */
public class TutorialActivityNew extends BaseThemedActivity {

    private int currentStep = 0;
    private TutorialSpotlightView spotlightView;
    private MaterialCardView tutorialCard;
    private FrameLayout tutorialCardContainer;
    private TextView tutorialTitle;
    private TextView tutorialDescription;
    private MaterialButton nextButton;
    private MaterialButton skipButton;
    private LinearProgressIndicator progressIndicator;
    private TextView stepCounter;
    
    // TABLET SUPPORT: Visual area for side-by-side layout
    private ImageView tutorialImage;
    private boolean isTabletLayout = false;

    // References to actual app UI elements
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private NavigationView navigationView;
    private MaterialCardView progressTracker;
    private LinearLayout morningTasksContainer;
    private LinearLayout afternoonTasksContainer;
    private LinearLayout nightTasksContainer;
    private TextView morningTasksHeader;
    private TextView afternoonTasksHeader;
    private TextView nightTasksHeader;
    private View sampleTaskView;
    private NestedScrollView scrollView;
    private TabLayout tabLayout;
    private LinearLayout upcomingTasksSection;
    private LinearLayout upcomingTasksContainer;
    private MaterialCardView tasksContainerCard;
    private FrameLayout fragmentContainer;
    private View currentFragmentView;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AnimatorSet currentAnimation;
    private android.widget.PopupWindow quickInfoPopup;

    // Track current window size to detect changes
    private WindowSizeHelper.WindowSizeClass currentWindowSize;

    // Tutorial step definitions
    private final TutorialStep[] steps = {
            new TutorialStep(
                    "Welcome to ClockWise! 🎯",
                    "Your personal productivity companion. Let's take a quick tour of all the main features!",
                    StepType.INTRO,
                    CardPosition.BOTTOM_CENTER
            ),
            new TutorialStep(
                    "Hamburger Menu 🍔",
                    "Tap here to access History, Calendar, Trash Bin, Settings, and this Tutorial.",
                    StepType.HAMBURGER_MENU,
                    CardPosition.MIDDLE_BOTTOM
            ),
            new TutorialStep(
                    "Calendar Button 📅",
                    "Quick access to your calendar view. See all your tasks organized by date.",
                    StepType.CALENDAR_BUTTON,
                    CardPosition.TOP_CENTER
            ),
            new TutorialStep(
                    "Actions Button ➕",
                    "The center button! Tap here to create Quick Tasks, Tasks, Focus Sessions, Convert Notes, and more!",
                    StepType.BOTTOM_NAV_ADD,
                    CardPosition.TOP_CENTER
            ),
            new TutorialStep(
                    "Tasks Tab 📋",
                    "View all your current tasks organized by time of day. This is your main productivity hub.",
                    StepType.BOTTOM_NAV_TASKS,
                    CardPosition.TOP_CENTER
            ),
            new TutorialStep(
                    "Notepad Tab 📝",
                    "Quick notes and reminders. Switch to Notepad mode for capturing ideas on the go.",
                    StepType.BOTTOM_NAV_NOTEPAD,
                    CardPosition.TOP_CENTER
            ),
            new TutorialStep(
                    "Task Organization 🕐",
                    "Tasks are organized by time: Morning (6 AM-12 PM), Afternoon (12 PM-6 PM), and Night (6 PM-6 AM).",
                    StepType.TASK_SECTIONS,
                    CardPosition.BOTTOM_CENTER
            ),
            new TutorialStep(
                    "Task Cards 📝",
                    "Each task shows its name, time, and a completion switch. Tap the switch to mark tasks as done!",
                    StepType.TASK_CARD,
                    CardPosition.BOTTOM_CENTER
            ),
            new TutorialStep(
                    "Long Press for Details ✨",
                    "Long press any task to see detailed information including date, priority, and alarm settings.",
                    StepType.LONG_PRESS_DEMO,
                    CardPosition.BOTTOM_CENTER
            ),
            new TutorialStep(
                    "You're Ready! ✨",
                    "That's it! You're all set to boost your productivity with ClockWise. Let's get started!",
                    StepType.FINISH,
                    CardPosition.BOTTOM_CENTER
            )
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_new);

        // Track initial window size
        currentWindowSize = WindowSizeHelper.getWidthSizeClass(this);
        boolean isLargeScreen = WindowSizeHelper.isLargeScreen(this);

        // TABLET SUPPORT: Load TasksContainerFragment for proper tablet layout
        if (isLargeScreen && savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new TasksContainerFragment())
                    .commit();
        } else {
            // PHONE: Show inline scroll view with sample tasks
            View scrollView = findViewById(R.id.tutorialScrollView);
            if (scrollView != null) {
                scrollView.setVisibility(View.VISIBLE);
            }
        }

        initializeViews();
        setupButtons();
        setupAppUI();
        
        // Handle back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishTutorial();
            }
        });
        
        // Start tutorial with entrance animation
        handler.postDelayed(this::playEntranceAnimation, 300);
    }

    private void initializeViews() {
        spotlightView = findViewById(R.id.spotlightView);
        tutorialCardContainer = findViewById(R.id.tutorialCardContainer);

        boolean isLargeScreen = WindowSizeHelper.isLargeScreen(this);

        // TABLET SUPPORT: Inflate appropriate tutorial card layout based on screen size
        if (isLargeScreen) {
            // Inflate tablet layout (side-by-side: 35% visual, 65% content)
            tutorialCardContainer.removeAllViews();
            LayoutInflater.from(this).inflate(R.layout.tutorial_card, tutorialCardContainer, true);
            isTabletLayout = true;
        }

        // Now find views from the inflated card (either phone or tablet layout)
        tutorialCard = findViewById(R.id.tutorialCard);
        tutorialTitle = findViewById(R.id.tutorialTitle);
        tutorialDescription = findViewById(R.id.tutorialDescription);
        nextButton = findViewById(R.id.nextButton);
        skipButton = findViewById(R.id.skipButton);
        progressIndicator = findViewById(R.id.progressIndicator);
        stepCounter = findViewById(R.id.stepCounter);
        
        // Check if using tablet layout with visual area
        tutorialImage = findViewById(R.id.tutorialImage);
        if (tutorialImage != null) {
            isTabletLayout = true;
        }

        // Get references to actual app UI
        drawerLayout = findViewById(R.id.drawerLayout);
        toolbar = findViewById(R.id.topBar);
        bottomNav = findViewById(R.id.bottomNavigation);
        navigationView = findViewById(R.id.navigationView);
        scrollView = findViewById(R.id.tutorialScrollView);
        fragmentContainer = findViewById(R.id.fragmentContainer);

        // Configure progress indicator
        progressIndicator.setMax(steps.length);
        progressIndicator.setProgress(1, false);
        stepCounter.setText(String.format(Locale.getDefault(), "1 / %d", steps.length));

        // TABLET: Wait for fragment to inflate, then find views
        // PHONE: Populate inline content immediately
        if (isTabletLayout) {
            // Give fragment time to inflate
            handler.postDelayed(() -> {
                // Try to find fragment views (they may be in different panes)
                // For tutorial, we just need the UI to look right, not be functional
            }, 100);
        }

        // Always setup inline content for tutorial demonstration
        tabLayout = findViewById(R.id.tasksTabLayout);
        progressTracker = findViewById(R.id.progressTracker);
        morningTasksContainer = findViewById(R.id.morningTasksContainer);
        afternoonTasksContainer = findViewById(R.id.afternoonTasksContainer);
        nightTasksContainer = findViewById(R.id.nightTasksContainer);
        morningTasksHeader = findViewById(R.id.morningTasksHeader);
        afternoonTasksHeader = findViewById(R.id.afternoonTasksHeader);
        nightTasksHeader = findViewById(R.id.nightTasksHeader);
        upcomingTasksSection = findViewById(R.id.upcomingTasksSection);
        upcomingTasksContainer = findViewById(R.id.upcomingTasksContainer);
        tasksContainerCard = findViewById(R.id.tasksContainerCard);

        // Setup tabs
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText("Current Tasks"));
            tabLayout.addTab(tabLayout.newTab().setText("Upcoming"));
        }

        // Always populate with sample tasks for tutorial
        populateSampleTasks();
        populateUpcomingTasks();
    }

    private void setupButtons() {
        nextButton.setOnClickListener(v -> {
            animateButtonClick(v);
            nextStep();
        });
        
        skipButton.setOnClickListener(v -> {
            animateButtonClick(v);
            finishTutorial();
        });
    }

    private void setupAppUI() {
        // Disable interactions with app UI during tutorial
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        if (bottomNav != null) bottomNav.setEnabled(false);
        
        // Make sure all UI is visible for demonstration
        if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
    }
    
    private void populateSampleTasks() {
        // Create sample tasks to show real UI with data
        if (morningTasksContainer != null) {
            // Use FIRST morning task for long press demo - it's at the TOP and most visible!
            View morningTask1 = createSampleTask("Morning Workout", "6:30 AM", R.drawable.ic_alarm, false);
            morningTasksContainer.addView(morningTask1);
            // Save reference to demonstrate long press - FIRST task at TOP!
            if (sampleTaskView == null) {
                sampleTaskView = morningTask1;
            }
            morningTasksContainer.addView(createSampleTask("Team Meeting", "9:00 AM", R.drawable.ic_focus, true));
            morningTasksContainer.addView(createSampleTask("Review Project Docs", "10:30 AM", R.drawable.ic_notepad, false));
        }
        
        if (afternoonTasksContainer != null) {
            afternoonTasksContainer.addView(createSampleTask("Lunch Break", "12:30 PM", R.drawable.ic_clock, true));
            afternoonTasksContainer.addView(createSampleTask("Client Call", "2:00 PM", R.drawable.ic_reminder, false));
            afternoonTasksContainer.addView(createSampleTask("Finish Presentation", "4:00 PM", R.drawable.ic_focus_task, false));
        }
        
        if (nightTasksContainer != null) {
            nightTasksContainer.addView(createSampleTask("Dinner with Family", "7:00 PM", R.drawable.ic_calendar, false));
            nightTasksContainer.addView(createSampleTask("Read Book", "9:00 PM", R.drawable.ic_moon, false));
        }
        
        // Update progress tracker
        if (progressTracker != null) {
            TextView taskCountText = progressTracker.findViewById(R.id.taskCountText);
            TextView completionText = progressTracker.findViewById(R.id.completionText);
            LinearProgressIndicator progressBar = progressTracker.findViewById(R.id.progressBar);
            
            if (taskCountText != null) taskCountText.setText("2 of 8 tasks completed");
            if (completionText != null) completionText.setText("25% Complete");
            if (progressBar != null) {
                progressBar.setMax(8);
                progressBar.setProgress(2, false);
            }
        }
    }
    
    private View createSampleTask(String name, String time, int iconRes, boolean completed) {
        // Inflate with parent context to get proper layout params from task_item.xml
        LinearLayout parent = morningTasksContainer != null ? morningTasksContainer : 
                              afternoonTasksContainer != null ? afternoonTasksContainer : 
                              nightTasksContainer;
        
        View taskView = LayoutInflater.from(this).inflate(R.layout.task_item, parent, false);
        
        TextView taskName = taskView.findViewById(R.id.taskName);
        TextView taskTime = taskView.findViewById(R.id.taskTime);
        ImageView taskIcon = taskView.findViewById(R.id.taskTypeIcon);
        MaterialSwitch taskSwitch = taskView.findViewById(R.id.taskSwitch);
        MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        
        if (taskName != null) taskName.setText(name);
        if (taskTime != null) taskTime.setText(time);
        if (taskIcon != null) taskIcon.setImageResource(iconRes);
        if (taskSwitch != null) {
            taskSwitch.setChecked(completed);
            taskSwitch.setEnabled(false); // Disable during tutorial
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(false); // Will enable for demo
        }
        
        return taskView;
    }
    
    private void populateUpcomingTasks() {
        if (upcomingTasksContainer != null) {
            upcomingTasksContainer.addView(createUpcomingTask("Doctor Appointment", "Tomorrow, 10:00 AM", R.drawable.ic_calendar));
            upcomingTasksContainer.addView(createUpcomingTask("Project Deadline", "Dec 21, 5:00 PM", R.drawable.ic_focus_task));
            upcomingTasksContainer.addView(createUpcomingTask("Team Presentation", "Dec 22, 2:00 PM", R.drawable.ic_focus));
        }
    }
    
    private View createUpcomingTask(String name, String dateTime, int iconRes) {
        LinearLayout parent = upcomingTasksContainer;
        View taskView = LayoutInflater.from(this).inflate(R.layout.task_item, parent, false);
        
        TextView taskName = taskView.findViewById(R.id.taskName);
        TextView taskTime = taskView.findViewById(R.id.taskTime);
        ImageView taskIcon = taskView.findViewById(R.id.taskTypeIcon);
        MaterialSwitch taskSwitch = taskView.findViewById(R.id.taskSwitch);
        MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        
        if (taskName != null) taskName.setText(name);
        if (taskTime != null) taskTime.setText(dateTime);
        if (taskIcon != null) taskIcon.setImageResource(iconRes);
        if (taskSwitch != null) {
            taskSwitch.setChecked(false);
            taskSwitch.setEnabled(false);
        }
        if (deleteButton != null) deleteButton.setEnabled(false);
        
        return taskView;
    }
    
    private void switchToCurrentTasks() {
        if (tasksContainerCard != null) tasksContainerCard.setVisibility(View.VISIBLE);
        if (progressTracker != null) progressTracker.setVisibility(View.VISIBLE);
        if (upcomingTasksSection != null) upcomingTasksSection.setVisibility(View.GONE);
        
        if (tabLayout != null && tabLayout.getTabCount() > 0) {
            TabLayout.Tab tab = tabLayout.getTabAt(0);
            if (tab != null) tab.select();
        }
    }
    
    private void switchToUpcomingTasks() {
        if (tasksContainerCard != null) tasksContainerCard.setVisibility(View.GONE);
        if (progressTracker != null) progressTracker.setVisibility(View.GONE);
        if (upcomingTasksSection != null) upcomingTasksSection.setVisibility(View.VISIBLE);
        
        if (tabLayout != null && tabLayout.getTabCount() > 1) {
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            if (tab != null) tab.select();
        }
        
        if (scrollView != null) scrollView.smoothScrollTo(0, 0);
    }

    private void switchToNotepadView() {
        // Hide the current tasks scroll view
        if (scrollView != null) {
            scrollView.setVisibility(View.GONE);
        }

        // Show notepad-style view in fragment container
        if (fragmentContainer != null) {
            // Create a simple notepad demo view
            View notepadDemoView = createNotepadDemoView();
            fragmentContainer.removeAllViews();
            fragmentContainer.addView(notepadDemoView);
            currentFragmentView = notepadDemoView;
        }
    }

    private void switchBackToTasksView() {
        // Show the tasks scroll view again
        if (scrollView != null) {
            scrollView.setVisibility(View.VISIBLE);
        }

        // Clear fragment container
        if (fragmentContainer != null && currentFragmentView != null) {
            fragmentContainer.removeView(currentFragmentView);
            currentFragmentView = null;
        }
    }

    private View createNotepadDemoView() {
        // Create a simple notepad interface for tutorial demonstration
        LinearLayout notepadLayout = new LinearLayout(this);
        notepadLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        notepadLayout.setOrientation(LinearLayout.VERTICAL);
        notepadLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        notepadLayout.setBackgroundColor(getColor(android.R.color.transparent));

        // Add sample notes
        for (int i = 0; i < 3; i++) {
            MaterialCardView noteCard = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.bottomMargin = dpToPx(12);
            noteCard.setLayoutParams(cardParams);
            noteCard.setCardBackgroundColor(getResources().getColor(android.R.color.white, null));
            noteCard.setRadius(dpToPx(12));
            noteCard.setCardElevation(dpToPx(2));

            LinearLayout noteContent = new LinearLayout(this);
            noteContent.setOrientation(LinearLayout.VERTICAL);
            noteContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

            TextView noteTitle = new TextView(this);
            noteTitle.setTextSize(16);
            noteTitle.setTypeface(null, Typeface.BOLD);
            noteTitle.setTextColor(getResources().getColor(android.R.color.black, null));

            TextView noteBody = new TextView(this);
            noteBody.setTextSize(14);
            noteBody.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
            noteBody.setPadding(0, dpToPx(8), 0, 0);

            switch (i) {
                case 0:
                    noteTitle.setText("📋 Meeting Notes");
                    noteBody.setText("Discuss project timeline and deliverables with the team");
                    break;
                case 1:
                    noteTitle.setText("💡 Ideas");
                    noteBody.setText("New feature: Add voice notes for quick capture");
                    break;
                case 2:
                    noteTitle.setText("✅ Shopping List");
                    noteBody.setText("Milk, eggs, bread, coffee");
                    break;
            }

            noteContent.addView(noteTitle);
            noteContent.addView(noteBody);
            noteCard.addView(noteContent);
            notepadLayout.addView(noteCard);
        }

        return notepadLayout;
    }

    private void demonstrateTaskOrganization() {
        // This method demonstrates the task organization by highlighting each time section
        // sequentially to show how tasks are grouped by time of day

        // First, scroll to show morning section
        if (morningTasksHeader != null) {
            scrollToView(morningTasksHeader);
        }

        // Highlight all three sections together to show the organization
        handler.postDelayed(() -> {
            // Find the parent container that holds all sections
            if (tasksContainerCard != null) {
                // Highlight the entire sections container with all sample tasks
                spotlightView.highlightView(tasksContainerCard);
                animateViewPulse(tasksContainerCard);

                // Sequentially highlight each section with its sample tasks
                highlightSectionSequentially();
            } else if (morningTasksContainer != null && morningTasksContainer.getParent() != null) {
                View sectionsParent = (View) morningTasksContainer.getParent();
                spotlightView.highlightView(sectionsParent);
                animateViewPulse(sectionsParent);
                highlightSectionSequentially();
            }
        }, 400);
    }

    private void highlightSectionSequentially() {
        // Highlight Morning section with its sample tasks
        if (morningTasksHeader != null && morningTasksContainer != null && morningTasksContainer.getChildCount() > 0) {
            handler.postDelayed(() -> {
                scrollToView(morningTasksHeader);
                handler.postDelayed(() -> {
                    // Pulse morning header
                    animateHeaderPulse(morningTasksHeader);

                    // Highlight each morning task
                    highlightTasksInContainer(morningTasksContainer, 0, 500);
                }, 200);
            }, 800);
        }

        // Then highlight Afternoon section with its sample tasks
        if (afternoonTasksHeader != null && afternoonTasksContainer != null && afternoonTasksContainer.getChildCount() > 0) {
            handler.postDelayed(() -> {
                scrollToView(afternoonTasksHeader);
                handler.postDelayed(() -> {
                    animateHeaderPulse(afternoonTasksHeader);

                    // Highlight each afternoon task
                    highlightTasksInContainer(afternoonTasksContainer, 0, 500);
                }, 200);
            }, 2000);
        }

        // Finally highlight Night section with its sample tasks
        if (nightTasksHeader != null && nightTasksContainer != null && nightTasksContainer.getChildCount() > 0) {
            handler.postDelayed(() -> {
                scrollToView(nightTasksHeader);
                handler.postDelayed(() -> {
                    animateHeaderPulse(nightTasksHeader);

                    // Highlight each night task
                    highlightTasksInContainer(nightTasksContainer, 0, 500);
                }, 200);
            }, 3200);
        }
    }

    private void highlightTasksInContainer(LinearLayout container, int startIndex, long delayBetween) {
        // Sequentially highlight each task card in the container
        if (container == null || container.getChildCount() == 0) return;

        for (int i = startIndex; i < container.getChildCount(); i++) {
            final int index = i;
            final View taskView = container.getChildAt(i);

            handler.postDelayed(() -> {
                if (taskView != null) {
                    // Quick pulse animation for each task
                    animateTaskCardPulse(taskView);
                }
            }, delayBetween * (i - startIndex));
        }
    }

    private void animateTaskCardPulse(View taskView) {
        // Create a quick pulse animation for individual task cards
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(taskView, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(taskView, "scaleY", 1f, 1.05f, 1f);

        scaleX.setDuration(400);
        scaleY.setDuration(400);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(scaleX, scaleY);
        pulseSet.start();
    }

    private void animateHeaderPulse(View header) {
        // Create a quick pulse animation for section headers
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(header, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(header, "scaleY", 1f, 1.1f, 1f);

        scaleX.setDuration(600);
        scaleY.setDuration(600);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(scaleX, scaleY);
        pulseSet.start();
    }

    private void demonstrateTaskCards() {
        // Sequentially highlight different task cards to show their features

        // Highlight second morning task (Team Meeting - completed task)
        if (morningTasksContainer != null && morningTasksContainer.getChildCount() > 1) {
            handler.postDelayed(() -> {
                View secondTask = morningTasksContainer.getChildAt(1);
                if (secondTask != null) {
                    scrollToView(secondTask);
                    handler.postDelayed(() -> {
                        animateTaskCardPulse(secondTask);
                    }, 200);
                }
            }, 1200);
        }

        // Highlight first afternoon task (Lunch Break - another completed task)
        if (afternoonTasksContainer != null && afternoonTasksContainer.getChildCount() > 0) {
            handler.postDelayed(() -> {
                View afternoonTask = afternoonTasksContainer.getChildAt(0);
                if (afternoonTask != null) {
                    scrollToView(afternoonTask);
                    handler.postDelayed(() -> {
                        animateTaskCardPulse(afternoonTask);
                    }, 200);
                }
            }, 2400);
        }
    }

    private void playEntranceAnimation() {
        // Initial state
        tutorialCard.setAlpha(0f);
        tutorialCard.setTranslationY(300);
        spotlightView.setAlpha(0f);
        
        // Animate entrance
        spotlightView.animate()
                .alpha(1f)
                .setDuration(400)
                .start();
        
        tutorialCard.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .withEndAction(() -> handler.postDelayed(() -> showStep(0), 200))
                .start();
    }

    private void nextStep() {
        if (currentStep < steps.length - 1) {
            currentStep++;
            showStep(currentStep);
            updateProgress();
        } else {
            finishTutorial();
        }
    }

    private void updateProgress() {
        progressIndicator.setProgress(currentStep + 1, true);
        stepCounter.setText(String.format(Locale.getDefault(), 
            "%d / %d", currentStep + 1, steps.length));
    }

    private void showStep(int step) {
        if (step < 0 || step >= steps.length) return;
        
        TutorialStep tutorialStep = steps[step];
        
        // Cancel any ongoing animations
        cancelCurrentAnimation();
        
        // Clear any previous long press listeners
        if (sampleTaskView != null) {
            sampleTaskView.setOnLongClickListener(null);
        }
        
        // Update text with fade animation
        fadeTextUpdate(tutorialTitle, tutorialStep.title);
        fadeTextUpdate(tutorialDescription, tutorialStep.description);
        
        // TABLET SUPPORT: Update visual area if in tablet layout
        if (isTabletLayout && tutorialImage != null) {
            updateTutorialVisual(tutorialStep.type);
        }

        // Update button
        if (step == steps.length - 1) {
            nextButton.setText(R.string.start_using_clockwise);
            skipButton.setVisibility(View.GONE);
        } else {
            nextButton.setText(R.string.next);
            skipButton.setVisibility(View.VISIBLE);
        }
        
        // Reposition tutorial card based on step
        repositionTutorialCard(tutorialStep.cardPosition);
        
        // Highlight appropriate UI element
        handler.postDelayed(() -> highlightUIElement(tutorialStep.type), 350);
    }
    
    private void repositionTutorialCard(CardPosition position) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) tutorialCard.getLayoutParams();
        
        switch (position) {
            case TOP_CENTER:
                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                params.topMargin = dpToPx(100);
                params.bottomMargin = 0;
                params.leftMargin = dpToPx(20);
                params.rightMargin = dpToPx(20);
                break;
            case MIDDLE_CENTER:
                params.gravity = Gravity.CENTER;
                params.topMargin = 0;
                params.bottomMargin = 0;
                params.leftMargin = dpToPx(20);
                params.rightMargin = dpToPx(20);
                break;
            case MIDDLE_BOTTOM:
                params.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
                params.topMargin = dpToPx(150);
                params.bottomMargin = 0;
                params.leftMargin = dpToPx(20);
                params.rightMargin = dpToPx(20);
                break;
            case BOTTOM_CENTER:
            default:
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.topMargin = 0;
                params.bottomMargin = dpToPx(24);
                params.leftMargin = dpToPx(20);
                params.rightMargin = dpToPx(20);
                break;
        }
        
        tutorialCard.setLayoutParams(params);
        tutorialCard.requestLayout();
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    
    private void scrollToView(View view) {
        if (scrollView == null || view == null) return;
        
        // Get the view's position within the scroll view content
        int[] viewLocation = new int[2];
        view.getLocationInWindow(viewLocation);
        
        int[] scrollLocation = new int[2];
        scrollView.getLocationInWindow(scrollLocation);
        
        // Calculate current scroll position
        int currentScrollY = scrollView.getScrollY();
        
        // Calculate where the view is relative to scroll content
        int viewTopInScroll = viewLocation[1] - scrollLocation[1] + currentScrollY;
        
        // Scroll so the view appears near the TOP of the visible area (below toolbar/tabs)
        // Add some padding so it's not right at the edge
        int targetScroll = Math.max(0, viewTopInScroll - dpToPx(120));
        
        // Smooth scroll
        scrollView.smoothScrollTo(0, targetScroll);
    }

    private void highlightUIElement(StepType type) {
        switch (type) {
            case INTRO:
                spotlightView.clearHighlight();
                animateTutorialCardPulse();
                break;
                
            case HAMBURGER_MENU:
                if (toolbar != null) {
                    int[] location = new int[2];
                    toolbar.getLocationOnScreen(location);
                    // Highlight the hamburger menu area (left side of toolbar)
                    spotlightView.highlightRect(
                        location[0],
                        location[1],
                        location[0] + dpToPx(56),
                        location[1] + toolbar.getHeight()
                    );
                    // Briefly open and close drawer to show it
                    animateDrawerPreview();
                }
                break;
                
            case CALENDAR_BUTTON:
                // Highlight calendar button in top right
                if (toolbar != null) {
                    int[] location = new int[2];
                    toolbar.getLocationOnScreen(location);
                    int toolbarWidth = toolbar.getWidth();
                    // Highlight the right side of toolbar where calendar icon is
                    spotlightView.highlightRect(
                        location[0] + toolbarWidth - dpToPx(56),
                        location[1],
                        location[0] + toolbarWidth,
                        location[1] + toolbar.getHeight()
                    );
                    animateViewPulse(toolbar);
                }
                break;
                
            case BOTTOM_NAV_ADD:
                // Highlight the center Actions button in bottom nav
                if (bottomNav != null) {
                    int[] location = new int[2];
                    bottomNav.getLocationOnScreen(location);
                    int navWidth = bottomNav.getWidth();
                    int buttonWidth = navWidth / 3; // Three buttons
                    // Highlight center button
                    spotlightView.highlightRect(
                        location[0] + buttonWidth,
                        location[1],
                        location[0] + buttonWidth * 2,
                        location[1] + bottomNav.getHeight()
                    );
                    animateViewPulse(bottomNav);
                }
                break;
                
            case BOTTOM_NAV_TASKS:
                // Highlight the Tasks button (left) in bottom nav
                if (bottomNav != null) {
                    int[] location = new int[2];
                    bottomNav.getLocationOnScreen(location);
                    int navWidth = bottomNav.getWidth();
                    int buttonWidth = navWidth / 3;
                    // Highlight left button
                    spotlightView.highlightRect(
                        location[0],
                        location[1],
                        location[0] + buttonWidth,
                        location[1] + bottomNav.getHeight()
                    );
                    animateViewPulse(bottomNav);
                }
                break;

            case BOTTOM_NAV_NOTEPAD:
                // Switch to notepad view to show the interface
                switchToNotepadView();

                // Highlight the Notepad button (right) in bottom nav
                handler.postDelayed(() -> {
                    if (bottomNav != null) {
                        int[] location = new int[2];
                        bottomNav.getLocationOnScreen(location);
                        int navWidth = bottomNav.getWidth();
                        int buttonWidth = navWidth / 3;
                        // Highlight right button
                        spotlightView.highlightRect(
                            location[0] + buttonWidth * 2,
                            location[1],
                            location[0] + navWidth,
                            location[1] + bottomNav.getHeight()
                        );
                        animateViewPulse(bottomNav);
                    }
                }, 400);
                break;
            
            case TASK_SECTIONS:
                // Switch back to tasks view if we were on notepad
                switchBackToTasksView();

                // Make sure scroll view is visible and scroll to top
                handler.postDelayed(() -> {
                    if (scrollView != null) {
                        scrollView.setVisibility(View.VISIBLE);
                        scrollView.smoothScrollTo(0, 0);
                    }

                    // Demonstrate task organization by highlighting sections with sample tasks
                    demonstrateTaskOrganization();
                }, 300);
                break;
            
            case TASK_CARD:
                // Make sure we're on tasks view
                if (scrollView != null) {
                    scrollView.setVisibility(View.VISIBLE);
                }

                // Scroll to top to show morning tasks
                if (scrollView != null) {
                    scrollView.smoothScrollTo(0, 0);
                }

                // Find and highlight sample task cards
                handler.postDelayed(() -> {
                    // Highlight the first morning task (Morning Workout)
                    if (morningTasksContainer != null && morningTasksContainer.getChildCount() > 0) {
                        sampleTaskView = morningTasksContainer.getChildAt(0);

                        if (sampleTaskView != null) {
                            scrollToView(sampleTaskView);
                            handler.postDelayed(() -> {
                                // Highlight the task card with spotlight
                                spotlightView.highlightView(sampleTaskView);
                                animateViewPulse(sampleTaskView);

                                // Demonstrate multiple task cards by highlighting them sequentially
                                demonstrateTaskCards();
                            }, 300);
                        }
                    } else if (afternoonTasksContainer != null && afternoonTasksContainer.getChildCount() > 0) {
                        sampleTaskView = afternoonTasksContainer.getChildAt(0);
                        scrollToView(sampleTaskView);
                        handler.postDelayed(() -> {
                            spotlightView.highlightView(sampleTaskView);
                            animateViewPulse(sampleTaskView);
                        }, 300);
                    } else if (nightTasksContainer != null && nightTasksContainer.getChildCount() > 0) {
                        sampleTaskView = nightTasksContainer.getChildAt(0);
                        scrollToView(sampleTaskView);
                        handler.postDelayed(() -> {
                            spotlightView.highlightView(sampleTaskView);
                            animateViewPulse(sampleTaskView);
                        }, 300);
                    } else {
                        spotlightView.clearHighlight();
                    }
                }, 200);
                break;
            
            case LONG_PRESS_DEMO:
                // Show long press demo on task card
                if (sampleTaskView == null) {
                    // Try to find a task view
                    if (morningTasksContainer != null && morningTasksContainer.getChildCount() > 0) {
                        sampleTaskView = morningTasksContainer.getChildAt(0);
                    } else if (afternoonTasksContainer != null && afternoonTasksContainer.getChildCount() > 0) {
                        sampleTaskView = afternoonTasksContainer.getChildAt(0);
                    }
                }

                if (sampleTaskView != null) {
                    scrollToView(sampleTaskView);
                    handler.postDelayed(() -> {
                        spotlightView.highlightView(sampleTaskView);
                        animateViewPulse(sampleTaskView);

                        // Show visual hint for long press
                        animateLongPressHint(sampleTaskView);
                    }, 300);
                } else {
                    spotlightView.clearHighlight();
                }
                break;
            
            case FINISH:
                // Switch back to tasks view
                switchBackToTasksView();
                spotlightView.clearHighlight();
                animateTutorialCardPulse();
                break;

            default:
                spotlightView.clearHighlight();
                break;
        }
    }

    private void animateTutorialCardPulse() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tutorialCard, "scaleX", 1f, 1.02f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tutorialCard, "scaleY", 1f, 1.02f, 1f);
        
        scaleX.setDuration(2000);
        scaleY.setDuration(2000);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        currentAnimation = new AnimatorSet();
        currentAnimation.playTogether(scaleX, scaleY);
        currentAnimation.start();
    }

    private void animateDrawerPreview() {
        if (drawerLayout == null || navigationView == null) return;
        
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        
        handler.postDelayed(() -> {
            drawerLayout.openDrawer(GravityCompat.START);
            handler.postDelayed(() -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }, 2000);
        }, 500);
    }

    private void animateFABBounce(View fab) {
        fab.setScaleX(1f);
        fab.setScaleY(1f);
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 1.15f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(fab, "rotation", 0f, 360f);
        
        scaleX.setDuration(1500);
        scaleY.setDuration(1500);
        rotation.setDuration(3000);
        
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        currentAnimation = new AnimatorSet();
        currentAnimation.playTogether(scaleX, scaleY, rotation);
        currentAnimation.start();
    }

    private void animateViewPulse(View view) {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.02f, 1f);
        pulse.setDuration(2000);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        
        currentAnimation = new AnimatorSet();
        currentAnimation.play(pulse);
        currentAnimation.start();
    }

    private void animateCelebration() {
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(tutorialCard, "scaleX", 1f, 1.1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(tutorialCard, "scaleY", 1f, 1.1f);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(tutorialCard, "scaleX", 1.1f, 1f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(tutorialCard, "scaleY", 1.1f, 1f);
        
        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.setDuration(300);
        scaleUp.playTogether(scaleUpX, scaleUpY);
        
        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.setDuration(300);
        scaleDown.playTogether(scaleDownX, scaleDownY);
        
        currentAnimation = new AnimatorSet();
        currentAnimation.playSequentially(scaleUp, scaleDown);
        currentAnimation.start();
    }
    
    private void show3DTouchPopup(View taskView) {
        // Apply 3D touch scale effect (like real app)
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
                taskView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                
                // Show quick info popup
                showQuickInfoPopup(taskView);
            })
            .start();
    }
    
    private void showQuickInfoPopup(View anchorView) {
        // Create popup window - same as real app
        quickInfoPopup = new android.widget.PopupWindow(this);
        
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_quick_info, null);
        quickInfoPopup.setContentView(popupView);
        
        // Configure popup window
        quickInfoPopup.setWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        quickInfoPopup.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        quickInfoPopup.setFocusable(true);
        quickInfoPopup.setOutsideTouchable(true);
        quickInfoPopup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        quickInfoPopup.setElevation(24f);
        quickInfoPopup.setAnimationStyle(android.R.style.Animation_Dialog);

        // Populate popup with sample task data
        TextView titleText = popupView.findViewById(R.id.quickInfoTitle);
        TextView typeText = popupView.findViewById(R.id.quickInfoType);
        TextView dateText = popupView.findViewById(R.id.quickInfoDate);
        TextView timeText = popupView.findViewById(R.id.quickInfoTime);
        View durationRow = popupView.findViewById(R.id.durationRow);
        ImageView typeIcon = popupView.findViewById(R.id.typeIcon);
        MaterialCardView iconContainer = popupView.findViewById(R.id.typeIconContainer);

        if (titleText != null) titleText.setText("Morning Workout");
        if (typeText != null) {
            typeText.setText("Task");
            typeText.setTextColor(getResources().getColor(R.color.success, null));
        }
        if (dateText != null) dateText.setText("Thursday, December 19, 2024");
        if (timeText != null) timeText.setText("6:30 AM");
        if (durationRow != null) durationRow.setVisibility(View.GONE);
        if (typeIcon != null) typeIcon.setImageResource(R.drawable.ic_reminder);
        if (iconContainer != null) {
            iconContainer.setCardBackgroundColor(getResources().getColor(R.color.success, null));
        }

        // Get position and show popup above the task
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();
        int popupWidth = popupView.getMeasuredWidth();
        
        int xPos = location[0] + (anchorView.getWidth() - popupWidth) / 2;
        int yPos = location[1] - popupHeight - dpToPx(16);
        
        // Ensure popup doesn't go off-screen
        if (yPos < dpToPx(100)) {
            yPos = location[1] + anchorView.getHeight() + dpToPx(16);
        }
        
        quickInfoPopup.showAtLocation(anchorView, Gravity.NO_GRAVITY, xPos, yPos);
        
        // Highlight the popup after it appears
        handler.postDelayed(() -> {
            spotlightView.highlightView(popupView);
            animateViewPulse(popupView);
        }, 300);
        
        // Auto-close popup after 4 seconds and advance
        handler.postDelayed(() -> {
            if (quickInfoPopup != null && quickInfoPopup.isShowing()) {
                quickInfoPopup.dismiss();
            }
            // Auto-advance to next step
            if (currentStep == 7) {
                nextStep();
            }
        }, 4000);
    }
    
    private void animateLongPressHint(View view) {
        // Create a ripple/pulse effect to indicate long press is needed
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 1f, 1.05f, 1f);

        scaleX.setDuration(2000);
        scaleY.setDuration(2000);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.start();

        // Store animation to cancel later
        currentAnimation = animatorSet;
    }

    private void animateDeleteButtonPulse(View deleteButton) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(deleteButton, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(deleteButton, "scaleY", 1f, 1.15f, 1f);
        
        scaleX.setDuration(1200);
        scaleY.setDuration(1200);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        
        currentAnimation = new AnimatorSet();
        currentAnimation.playTogether(scaleX, scaleY);
        currentAnimation.start();
    }
    
    private void animateTaskDeletion(View taskView) {
        // Cancel current animation
        cancelCurrentAnimation();
        
        // Animate task sliding out and fading
        taskView.animate()
            .translationX(taskView.getWidth())
            .alpha(0f)
            .setDuration(400)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                // Remove the task from its parent
                if (taskView.getParent() instanceof LinearLayout) {
                    ((LinearLayout) taskView.getParent()).removeView(taskView);
                }
                
                // Clear spotlight since task is gone
                spotlightView.clearHighlight();
                
                // Update progress text (fake update)
                if (progressTracker != null) {
                    TextView taskCountText = progressTracker.findViewById(R.id.taskCountText);
                    if (taskCountText != null) taskCountText.setText("2 of 7 tasks completed");
                }
                
                // Show a toast
                android.widget.Toast.makeText(this, "Task deleted!", android.widget.Toast.LENGTH_SHORT).show();
                
                // Auto-advance after deletion
                handler.postDelayed(this::nextStep, 1000);
            })
            .start();
    }
    
    // Keep old methods for compatibility but they now redirect
    private void animateLongPressDemo(View taskView) {
        show3DTouchPopup(taskView);
    }
    
    private void animateDeleteDemo(View taskView) {
        MaterialButton deleteButton = taskView.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            animateDeleteButtonPulse(deleteButton);
        }
    }

    private void animateButtonClick(View button) {
        button.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(100)
                .withEndAction(() -> button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }

    private void fadeTextUpdate(TextView textView, String newText) {
        textView.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }

    private void cancelCurrentAnimation() {
        if (currentAnimation != null && currentAnimation.isRunning()) {
            currentAnimation.cancel();
        }
        if (quickInfoPopup != null && quickInfoPopup.isShowing()) {
            quickInfoPopup.dismiss();
        }
        handler.removeCallbacksAndMessages(null);
    }

    private void finishTutorial() {
        cancelCurrentAnimation();
        spotlightView.clearHighlight();
        
        // Exit animation
        tutorialCard.animate()
                .alpha(0f)
                .translationY(300)
                .setDuration(400)
                .start();
        
        spotlightView.animate()
                .alpha(0f)
                .setDuration(500)
                .setStartDelay(100)
                .withEndAction(() -> {
                    // Return to main activity
                    Intent intent = new Intent(TutorialActivityNew.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                })
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCurrentAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update current window size when resuming (backup check)
        currentWindowSize = WindowSizeHelper.getWidthSizeClass(this);
    }

    // Data classes
    private static class TutorialStep {
        String title;
        String description;
        StepType type;
        CardPosition cardPosition;

        TutorialStep(String title, String description, StepType type, CardPosition cardPosition) {
            this.title = title;
            this.description = description;
            this.type = type;
            this.cardPosition = cardPosition;
        }
    }

    /**
     * TABLET SUPPORT: Update visual area with appropriate icons/images for each tutorial step
     * This provides visual context on the left side of the tutorial card on tablets
     */
    private void updateTutorialVisual(StepType stepType) {
        if (tutorialImage == null) return;

        int imageResource;

        switch (stepType) {
            case INTRO:
                imageResource = R.drawable.app_icon;
                break;
            case HAMBURGER_MENU:
                imageResource = R.drawable.ic_menu_hamburger;
                break;
            case CALENDAR_BUTTON:
                imageResource = R.drawable.ic_calendar;
                break;
            case BOTTOM_NAV_ADD:
                imageResource = R.drawable.ic_add_fab;
                break;
            case BOTTOM_NAV_TASKS:
            case TASK_SECTIONS:
            case TASK_CARD:
                imageResource = R.drawable.ic_reminder;
                break;
            case BOTTOM_NAV_NOTEPAD:
                imageResource = R.drawable.ic_notepad;
                break;
            case LONG_PRESS_DEMO:
                imageResource = R.drawable.ic_focus_task;
                break;
            case FINISH:
                imageResource = R.drawable.app_icon;
                break;
            // Legacy types
            case FAB_CENTER:
            case FAB_QUICK:
                imageResource = R.drawable.ic_add_fab;
                break;
            case FLOATING_BUTTON:
                imageResource = R.drawable.ic_flash;
                break;
            case PROGRESS_TRACKER:
                imageResource = R.drawable.ic_check;
                break;
            case DELETE_DEMO:
                imageResource = R.drawable.ic_delete;
                break;
            case BOTTOM_NAV:
            case UPCOMING_TAB:
            case UPCOMING_TASKS:
                imageResource = R.drawable.ic_reminder;
                break;
            default:
                imageResource = R.drawable.app_icon;
                break;
        }

        // Animate visual change with fade
        tutorialImage.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    tutorialImage.setImageResource(imageResource);
                    tutorialImage.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                })
                .start();
    }

    private enum StepType {
        INTRO,
        HAMBURGER_MENU,
        CALENDAR_BUTTON,
        BOTTOM_NAV_ADD,
        BOTTOM_NAV_TASKS,
        BOTTOM_NAV_NOTEPAD,
        TASK_SECTIONS,
        TASK_CARD,
        LONG_PRESS_DEMO,
        FINISH,
        // Legacy types for visual updates
        FAB_CENTER,
        FAB_QUICK,
        FLOATING_BUTTON,
        PROGRESS_TRACKER,
        SMART_VIEW_TOGGLE,
        RIGHT_NOW_SECTION,
        MISSED_SECTION,
        LATER_TODAY_SECTION,
        TASK_BADGES,
        DELETE_DEMO,
        BOTTOM_NAV,
        UPCOMING_TAB,
        UPCOMING_TASKS
    }
    
    private enum CardPosition {
        TOP_CENTER,
        MIDDLE_CENTER,
        MIDDLE_BOTTOM,
        BOTTOM_CENTER
    }
}
