package com.example.mainactivity;

/**
 * Enum representing settings categories for the Settings screen.
 * Used for iPadOS-style master-detail layout on large screens.
 */
public enum SettingsCategory {
    NOTIFICATIONS("Notifications", "Alerts & floating button", R.drawable.ic_reminder),
    APPEARANCE("Appearance", "Theme & colors", R.drawable.ic_sun),
    TASK_PREFERENCES("Task Preferences", "Defaults & smart suggestions", R.drawable.ic_task),
    DATA_STORAGE("Data & Storage", "Backup & clear data", R.drawable.ic_storage),
    ABOUT_HELP("About & Help", "App info & support", R.drawable.ic_info),
    DEVELOPER("Developer Options", "Testing & debug tools", R.drawable.ic_delete);

    private final String title;
    private final String description;
    private final int iconResId;

    SettingsCategory(String title, String description, int iconResId) {
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getIconResId() {
        return iconResId;
    }

    /**
     * Get category from string identifier
     */
    public static SettingsCategory fromString(String str) {
        if (str == null) return NOTIFICATIONS;
        switch (str) {
            case "Notifications":
                return NOTIFICATIONS;
            case "Appearance":
                return APPEARANCE;
            case "Task Preferences":
                return TASK_PREFERENCES;
            case "Data & Storage":
            case "Data":
                return DATA_STORAGE;
            case "About & Help":
            case "About":
                return ABOUT_HELP;
            case "Developer":
            case "Developer Options":
            case "Developer Tools":
                return DEVELOPER;
            default:
                return NOTIFICATIONS;
        }
    }
}
