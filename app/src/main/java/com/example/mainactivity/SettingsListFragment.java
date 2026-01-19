package com.example.mainactivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_list, container, false);
        
        setupItem(view, R.id.btnNotifications, "Notifications");
        setupItem(view, R.id.btnAppearance, "Appearance");
        setupItem(view, R.id.btnDataManagement, "Developer Tools");
        // About removed - accessible from side panel only

        return view;
    }

    private void setupItem(View root, int id, String category) {
        View item = root.findViewById(id);
        if (item != null) {
            item.setOnClickListener(v -> {
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).openDetailPane(SettingsDetailFragment.newInstance(category));
                }
            });
        }
    }
}
