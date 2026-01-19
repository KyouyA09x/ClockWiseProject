package com.example.mainactivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TasksContainerFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private View phoneTasksLayout;
    private View tabletTasksLayout;
    private WindowSizeHelper.WindowSizeClass currentWindowSize;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks_container, container, false);

        // Track current window size
        if (getActivity() != null) {
            currentWindowSize = WindowSizeHelper.getWidthSizeClass(getActivity());
        }

        initViews(view);
        setupAdaptiveLayout();
        setupBottomPadding(view);

        return view;
    }

    private void initViews(View view) {
        viewPager = view.findViewById(R.id.tasksViewPager);
        tabLayout = view.findViewById(R.id.tasksTabLayout);
        phoneTasksLayout = view.findViewById(R.id.phoneTasksLayout);
        tabletTasksLayout = view.findViewById(R.id.tabletTasksLayout);
    }

    private void setupAdaptiveLayout() {
        if (getActivity() == null) return;

        // Use WindowSizeHelper to detect if we should use the side-by-side layout
        if (WindowSizeHelper.isLargeScreen(getActivity())) {
            // TABLET/FOLDABLE: Show side-by-side panes
            if (phoneTasksLayout != null) phoneTasksLayout.setVisibility(View.GONE);
            if (tabletTasksLayout != null) {
                tabletTasksLayout.setVisibility(View.VISIBLE);
                
                // Embed fragments directly into the side-by-side frames
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.leftTasksPane, new CurrentTasksFragment())
                        .replace(R.id.rightTasksPane, new UpcomingTasksFragment())
                        .commit();
            }
        } else {
            // PHONE: Standard tabbed view
            if (phoneTasksLayout != null) phoneTasksLayout.setVisibility(View.VISIBLE);
            if (tabletTasksLayout != null) tabletTasksLayout.setVisibility(View.GONE);
            setupViewPager();
        }
    }

    private void setupViewPager() {
        if (viewPager == null) return;
        
        TasksViewPagerAdapter adapter = new TasksViewPagerAdapter(requireActivity());
        viewPager.setAdapter(adapter);

        if (tabLayout != null) {
            new TabLayoutMediator(tabLayout, viewPager,
                    (tab, position) -> {
                        switch (position) {
                            case 0:
                                tab.setText("📋 Current Tasks");
                                break;
                            case 1:
                                tab.setText("📅 Upcoming");
                                break;
                        }
                    }
            ).attach();
        }
    }

    private void setupBottomPadding(View view) {
        // Apply padding to avoid content being hidden by bottom navigation and FAB
        View container = view.findViewById(R.id.tasksContainerLinear);
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

                // Get bottom navigation height
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

                // Set padding on ViewPager and tablet layout to account for bottom nav + system bars
                int fabSpace = (int) (96 * density);
                int totalBottomPadding = bottomNavHeight + systemBars.bottom + fabSpace;

                if (viewPager != null) {
                    viewPager.setPadding(0, 0, 0, totalBottomPadding);
                    viewPager.setClipToPadding(false);
                }
                
                if (tabletTasksLayout != null) {
                    tabletTasksLayout.setPadding(
                        tabletTasksLayout.getPaddingLeft(),
                        tabletTasksLayout.getPaddingTop(),
                        tabletTasksLayout.getPaddingRight(),
                        totalBottomPadding
                    );
                }

                return insets;
            });

            androidx.core.view.ViewCompat.requestApplyInsets(view);
        });
    }

    public void refreshTasks() {
        // Only refresh visible child fragments for instant performance
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (!fragment.isVisible()) continue; // Skip invisible fragments

            if (fragment instanceof CurrentTasksFragment) {
                ((CurrentTasksFragment) fragment).refreshTasks();
            } else if (fragment instanceof UpcomingTasksFragment) {
                ((UpcomingTasksFragment) fragment).refreshTasks();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (getActivity() == null) return;

        // Check if window size class changed (e.g., folding/unfolding device)
        WindowSizeHelper.WindowSizeClass newWindowSize = WindowSizeHelper.getWidthSizeClass(getActivity());

        if (newWindowSize != currentWindowSize) {
            currentWindowSize = newWindowSize;

            // Re-setup the adaptive layout for the new screen size
            setupAdaptiveLayout();
        }
    }
}
