package com.example.mainactivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TasksContainerFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ExtendedFloatingActionButton fabAddTask;
    private ExtendedFloatingActionButton fabQuickTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks_container, container, false);

        initViews(view);
        setupViewPager();
        
        fabAddTask.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showTaskTypeChooser();
            }
        });

        fabQuickTask.setOnClickListener(v -> {
            showQuickTaskOptions();
        });

        return view;
    }

    private void initViews(View view) {
        viewPager = view.findViewById(R.id.tasksViewPager);
        tabLayout = view.findViewById(R.id.tasksTabLayout);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        fabQuickTask = view.findViewById(R.id.fabQuickTask);
    }

    private void showQuickTaskOptions() {
        if (getContext() == null) return;
        
        String[] options = {
            "➕ Create New Quick Task",
            "🔄 Convert Note to Task"
        };
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setTitle("⚡ Quick Task Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Create new quick task
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showQuickTaskBottomSheet();
                        }
                    } else if (which == 1) {
                        // Convert note to task
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showNotepadToConvertToTask();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    public void refreshTasks() {
        // Refresh fragments
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof CurrentTasksFragment) {
                ((CurrentTasksFragment) fragment).refreshTasks();
            } else if (fragment instanceof UpcomingTasksFragment) {
                ((UpcomingTasksFragment) fragment).refreshTasks();
            }
        }
    }
}
