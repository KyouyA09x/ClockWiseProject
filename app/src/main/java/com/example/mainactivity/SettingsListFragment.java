package com.example.mainactivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

public class SettingsListFragment extends Fragment {
    
    private MaterialCardView notificationsCard;
    private MaterialCardView appearanceCard;
    private MaterialCardView developerCard;
    private MaterialCardView taskPreferencesCard;
    private MaterialCardView dataStorageCard;
    private MaterialCardView aboutHelpCard;
    private MaterialCardView selectedCard = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationsCard = view.findViewById(R.id.notificationsCard);
        appearanceCard = view.findViewById(R.id.appearanceCard);
        developerCard = view.findViewById(R.id.developerCard);
        taskPreferencesCard = view.findViewById(R.id.taskPreferencesCard);
        dataStorageCard = view.findViewById(R.id.dataStorageCard);
        aboutHelpCard = view.findViewById(R.id.aboutHelpCard);
        
        // Debug logging
        android.util.Log.d("SettingsListFragment", "Cards found - notifications:" + (notificationsCard != null) +
            ", appearance:" + (appearanceCard != null) + ", developer:" + (developerCard != null) +
            ", taskPref:" + (taskPreferencesCard != null) + ", dataStorage:" + (dataStorageCard != null) +
            ", aboutHelp:" + (aboutHelpCard != null));
        
        // Check if we're on a large screen
        boolean isLargeScreen = getActivity() != null && WindowSizeHelper.isLargeScreen(getActivity());
        android.util.Log.d("SettingsListFragment", "isLargeScreen=" + isLargeScreen);

        setupCardClickListener(notificationsCard, "Notifications");
        setupCardClickListener(appearanceCard, "Appearance");
        setupCardClickListener(developerCard, "Developer");
        setupCardClickListener(taskPreferencesCard, "Task Preferences");
        setupCardClickListener(dataStorageCard, "Data & Storage");
        setupCardClickListener(aboutHelpCard, "About & Help");
        
        // Default selection to Notifications
        setSelectedCard(notificationsCard);
    }
    
    private void setupCardClickListener(MaterialCardView card, String category) {
        if (card == null) {
            android.util.Log.w("SettingsListFragment", "Card is null for category: " + category);
            return;
        }
        
        card.setOnClickListener(v -> {
            android.util.Log.d("SettingsListFragment", "Card clicked: " + category);
            setSelectedCard(card);
            if (getActivity() instanceof SettingsActivity) {
                ((SettingsActivity) getActivity()).openDetailPane(
                    SettingsDetailFragment.newInstance(category)
                );
            } else {
                android.util.Log.e("SettingsListFragment", "Activity is not SettingsActivity!");
            }
        });
    }
    
    /**
     * Highlight the selected card and unhighlight others
     */
    private void setSelectedCard(MaterialCardView card) {
        if (getContext() == null) return;
        
        // Reset all cards to default state
        resetCardState(notificationsCard);
        resetCardState(appearanceCard);
        resetCardState(developerCard);
        resetCardState(taskPreferencesCard);
        resetCardState(dataStorageCard);
        resetCardState(aboutHelpCard);
        
        // Highlight selected card
        if (card != null) {
            card.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            card.setStrokeColor(ContextCompat.getColor(getContext(), R.color.primary));
            selectedCard = card;
        }
    }
    
    /**
     * Reset a card to its default state
     */
    private void resetCardState(MaterialCardView card) {
        if (card == null || getContext() == null) return;
        card.setStrokeWidth(0);
    }
}
