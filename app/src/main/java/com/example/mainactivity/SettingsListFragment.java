package com.example.mainactivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

public class SettingsListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialCardView notificationsCard = view.findViewById(R.id.notificationsCard);
        MaterialCardView appearanceCard = view.findViewById(R.id.appearanceCard);
        MaterialCardView developerCard = view.findViewById(R.id.developerCard);

        if (notificationsCard != null) {
            notificationsCard.setOnClickListener(v -> {
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).openDetailPane(SettingsDetailFragment.newInstance("Notifications"));
                }
            });
        }

        if (appearanceCard != null) {
            appearanceCard.setOnClickListener(v -> {
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).openDetailPane(SettingsDetailFragment.newInstance("Appearance"));
                }
            });
        }

        if (developerCard != null) {
            developerCard.setOnClickListener(v -> {
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).openDetailPane(SettingsDetailFragment.newInstance("Developer"));
                }
            });
        }
    }
}
