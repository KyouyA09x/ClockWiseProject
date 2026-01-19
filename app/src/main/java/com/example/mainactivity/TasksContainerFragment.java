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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks_container, container, false);

        initViews(view);
        setupViewPager();
        setupBottomPadding(view);

        return view;
    }

    private void initViews(View view) {
        viewPager = view.findViewById(R.id.tasksViewPager);
        tabLayout = view.findViewById(R.id.tasksTabLayout);
    }

    private void setupViewPager() {
        TasksViewPagerAdapter adapter = new TasksViewPagerAdapter(requireActivity());
        viewPager.setAdapter(adapter);

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

                // Set padding on ViewPager to account for bottom nav + system bars
                if (viewPager != null) {
                    // Add extra padding for FAB (80dp) + margins
                    int fabSpace = (int) (96 * density);
                    int totalBottomPadding = bottomNavHeight + systemBars.bottom + fabSpace;
                    viewPager.setPadding(0, 0, 0, totalBottomPadding);
                    viewPager.setClipToPadding(false);
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
}