package com.example.mainactivity;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

public class SettingsActivity extends AppCompatActivity {

    private MaterialCardView lightModeButton;
    private MaterialCardView darkModeButton;
    private MaterialCardView autoModeButton;

    private RadioGroup themeColorGroup;
    private RadioButton radioDefault;
    private RadioButton radioCyan;
    private RadioButton radioGreen;
    private RadioButton radioPurple;
    private RadioButton radioOrange;
    
    private boolean isInitializing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        setTheme(ThemeHelper.getThemeResource(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        initViews();
        updateThemeModeUI();
        updateThemeColorUI();
        
        // Set up listeners AFTER setting initial UI state to prevent immediate recreation
        setupListeners();
        isInitializing = false;
    }

    private void initViews() {
        lightModeButton = findViewById(R.id.lightModeButton);
        darkModeButton = findViewById(R.id.darkModeButton);
        autoModeButton = findViewById(R.id.autoModeButton);

        themeColorGroup = findViewById(R.id.themeRadioGroup);
        radioDefault = findViewById(R.id.defaultThemeRadio);
        radioCyan = findViewById(R.id.cyanThemeRadio);
        radioGreen = findViewById(R.id.greenThemeRadio);
        radioPurple = findViewById(R.id.purpleThemeRadio);
        radioOrange = findViewById(R.id.orangeThemeRadio);
    }

    private void setupListeners() {
        if (lightModeButton != null) {
            lightModeButton.setOnClickListener(v -> {
                int currentMode = ThemeHelper.getThemeMode(this);
                if (currentMode != ThemeHelper.MODE_LIGHT) {
                    ThemeHelper.setThemeMode(this, ThemeHelper.MODE_LIGHT);
                    recreate();
                }
            });
        }

        if (darkModeButton != null) {
            darkModeButton.setOnClickListener(v -> {
                int currentMode = ThemeHelper.getThemeMode(this);
                if (currentMode != ThemeHelper.MODE_DARK) {
                    ThemeHelper.setThemeMode(this, ThemeHelper.MODE_DARK);
                    recreate();
                }
            });
        }

        if (autoModeButton != null) {
            autoModeButton.setOnClickListener(v -> {
                int currentMode = ThemeHelper.getThemeMode(this);
                if (currentMode != ThemeHelper.MODE_AUTO) {
                    ThemeHelper.setThemeMode(this, ThemeHelper.MODE_AUTO);
                    recreate();
                }
            });
        }

        if (themeColorGroup != null) {
            themeColorGroup.setOnCheckedChangeListener((group, checkedId) -> {
                // Don't recreate during initial setup
                if (isInitializing) return;
                
                String currentColor = ThemeHelper.getThemeColor(this);
                String newColor;
                
                if (checkedId == R.id.cyanThemeRadio) {
                    newColor = ThemeHelper.COLOR_CYAN;
                } else if (checkedId == R.id.greenThemeRadio) {
                    newColor = ThemeHelper.COLOR_GREEN;
                } else if (checkedId == R.id.purpleThemeRadio) {
                    newColor = ThemeHelper.COLOR_PURPLE;
                } else if (checkedId == R.id.orangeThemeRadio) {
                    newColor = ThemeHelper.COLOR_ORANGE;
                } else {
                    newColor = ThemeHelper.COLOR_DEFAULT;
                }
                
                // Only recreate if the color actually changed
                if (!currentColor.equals(newColor)) {
                    ThemeHelper.setThemeColor(this, newColor);
                    recreate();
                }
            });
        }
    }

    private void updateThemeModeUI() {
        int mode = ThemeHelper.getThemeMode(this);

        // Get theme colors
        int outlineColor = MaterialColors.getColor(this, android.R.attr.colorControlNormal, getColor(R.color.outline_light));
        int primaryColor = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, getColor(R.color.blue_primary));

        // Reset all stroke colors
        if (lightModeButton != null) lightModeButton.setStrokeColor(ColorStateList.valueOf(outlineColor));
        if (darkModeButton != null) darkModeButton.setStrokeColor(ColorStateList.valueOf(outlineColor));
        if (autoModeButton != null) autoModeButton.setStrokeColor(ColorStateList.valueOf(outlineColor));

        // Highlight selected with primary color stroke
        switch (mode) {
            case ThemeHelper.MODE_LIGHT:
                if (lightModeButton != null) lightModeButton.setStrokeColor(ColorStateList.valueOf(primaryColor));
                break;
            case ThemeHelper.MODE_DARK:
                if (darkModeButton != null) darkModeButton.setStrokeColor(ColorStateList.valueOf(primaryColor));
                break;
            case ThemeHelper.MODE_AUTO:
            default:
                if (autoModeButton != null) autoModeButton.setStrokeColor(ColorStateList.valueOf(primaryColor));
                break;
        }
    }

    private void updateThemeColorUI() {
        String color = ThemeHelper.getThemeColor(this);

        if (themeColorGroup == null) return;

        switch (color) {
            case ThemeHelper.COLOR_CYAN:
                if (radioCyan != null) radioCyan.setChecked(true);
                break;
            case ThemeHelper.COLOR_GREEN:
                if (radioGreen != null) radioGreen.setChecked(true);
                break;
            case ThemeHelper.COLOR_PURPLE:
                if (radioPurple != null) radioPurple.setChecked(true);
                break;
            case ThemeHelper.COLOR_ORANGE:
                if (radioOrange != null) radioOrange.setChecked(true);
                break;
            case ThemeHelper.COLOR_DEFAULT:
            default:
                if (radioDefault != null) radioDefault.setChecked(true);
                break;
        }
    }
}
