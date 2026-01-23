package com.example.mainactivity;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Smart Task Helper - Provides AI-like intelligent suggestions for tasks.
 * Uses rule-based heuristics to analyze task names and suggest:
 * - Priority levels
 * - Optimal times
 * - Task categories
 * - Focus session durations
 * 
 * Also tracks user patterns to provide personalized productivity insights.
 */
public class SmartTaskHelper {
    
    private static SmartTaskHelper instance;
    private SharedPreferences prefs;
    
    // Keywords for priority detection
    private static final Set<String> HIGH_PRIORITY_KEYWORDS = new HashSet<>(Arrays.asList(
        "urgent", "asap", "critical", "deadline", "important", "meeting", "presentation",
        "interview", "exam", "test", "review", "submit", "due", "emergency", "boss",
        "client", "doctor", "appointment", "flight", "pickup"
    ));
    
    private static final Set<String> MEDIUM_PRIORITY_KEYWORDS = new HashSet<>(Arrays.asList(
        "call", "email", "follow", "update", "prepare", "plan", "schedule", "arrange",
        "buy", "shop", "pay", "bill", "report", "send", "complete", "finish"
    ));
    
    private static final Set<String> LOW_PRIORITY_KEYWORDS = new HashSet<>(Arrays.asList(
        "maybe", "someday", "optional", "consider", "think", "read", "watch", "learn",
        "practice", "hobby", "relax", "gym", "workout", "exercise", "meditation"
    ));
    
    // Keywords for category detection
    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = new HashMap<>();
    static {
        CATEGORY_KEYWORDS.put("Work", new HashSet<>(Arrays.asList(
            "meeting", "call", "email", "report", "presentation", "client", "project",
            "deadline", "review", "team", "office", "work", "boss", "colleague"
        )));
        CATEGORY_KEYWORDS.put("Personal", new HashSet<>(Arrays.asList(
            "family", "friend", "birthday", "party", "dinner", "lunch", "movie",
            "travel", "vacation", "holiday", "home", "clean", "organize"
        )));
        CATEGORY_KEYWORDS.put("Health", new HashSet<>(Arrays.asList(
            "gym", "workout", "exercise", "run", "jog", "yoga", "meditation",
            "doctor", "dentist", "medicine", "health", "sleep", "diet", "walk"
        )));
        CATEGORY_KEYWORDS.put("Finance", new HashSet<>(Arrays.asList(
            "pay", "bill", "bank", "money", "budget", "invest", "tax", "insurance",
            "rent", "mortgage", "salary", "expense"
        )));
        CATEGORY_KEYWORDS.put("Learning", new HashSet<>(Arrays.asList(
            "study", "learn", "read", "course", "class", "tutorial", "practice",
            "exam", "test", "homework", "assignment", "research"
        )));
    }
    
    // Time keywords
    private static final Set<String> MORNING_KEYWORDS = new HashSet<>(Arrays.asList(
        "breakfast", "morning", "early", "wake", "sunrise", "coffee"
    ));
    
    private static final Set<String> AFTERNOON_KEYWORDS = new HashSet<>(Arrays.asList(
        "lunch", "afternoon", "midday", "noon"
    ));
    
    private static final Set<String> EVENING_KEYWORDS = new HashSet<>(Arrays.asList(
        "dinner", "evening", "night", "late", "sunset", "sleep", "bed"
    ));
    
    private SmartTaskHelper(Context context) {
        prefs = context.getSharedPreferences("smart_task_prefs", Context.MODE_PRIVATE);
    }
    
    public static synchronized SmartTaskHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SmartTaskHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Suggest a priority level based on task name analysis.
     * @return "High", "Medium", "Low", or "None"
     */
    public String suggestPriority(String taskName) {
        if (taskName == null || taskName.isEmpty()) return "None";
        
        String lowerName = taskName.toLowerCase();
        String[] words = lowerName.split("\\s+");
        
        // Check for high priority keywords
        for (String word : words) {
            if (HIGH_PRIORITY_KEYWORDS.contains(word)) {
                return "High";
            }
        }
        
        // Check for medium priority keywords
        for (String word : words) {
            if (MEDIUM_PRIORITY_KEYWORDS.contains(word)) {
                return "Medium";
            }
        }
        
        // Check for low priority keywords
        for (String word : words) {
            if (LOW_PRIORITY_KEYWORDS.contains(word)) {
                return "Low";
            }
        }
        
        // Default based on task name length (longer = potentially more complex)
        if (words.length > 5) {
            return "Medium";
        }
        
        return "None";
    }
    
    /**
     * Suggest a category based on task name analysis.
     * @return Category name or "General"
     */
    public String suggestCategory(String taskName) {
        if (taskName == null || taskName.isEmpty()) return "General";
        
        String lowerName = taskName.toLowerCase();
        String[] words = lowerName.split("\\s+");
        
        // Count matches for each category
        Map<String, Integer> categoryScores = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String word : words) {
                if (entry.getValue().contains(word)) {
                    score++;
                }
            }
            if (score > 0) {
                categoryScores.put(entry.getKey(), score);
            }
        }
        
        // Return category with highest score
        String bestCategory = "General";
        int bestScore = 0;
        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        
        return bestCategory;
    }
    
    /**
     * Suggest optimal time of day based on task name and user patterns.
     * @return "morning", "afternoon", or "evening"
     */
    public String suggestTimeOfDay(String taskName) {
        if (taskName == null || taskName.isEmpty()) {
            return getCurrentTimeOfDay();
        }
        
        String lowerName = taskName.toLowerCase();
        
        // Check for time-specific keywords
        for (String keyword : MORNING_KEYWORDS) {
            if (lowerName.contains(keyword)) return "morning";
        }
        for (String keyword : AFTERNOON_KEYWORDS) {
            if (lowerName.contains(keyword)) return "afternoon";
        }
        for (String keyword : EVENING_KEYWORDS) {
            if (lowerName.contains(keyword)) return "evening";
        }
        
        // Check category-based time suggestions
        String category = suggestCategory(taskName);
        switch (category) {
            case "Health":
                // Exercise is often best in morning
                if (lowerName.contains("gym") || lowerName.contains("workout") || 
                    lowerName.contains("exercise") || lowerName.contains("run")) {
                    return "morning";
                }
                break;
            case "Work":
                // Work tasks often best in morning when fresh
                return "morning";
            case "Personal":
                // Personal tasks often in evening
                return "evening";
            case "Learning":
                // Learning can be afternoon
                return "afternoon";
        }
        
        // Default to user's most productive time
        return getMostProductiveTimeOfDay();
    }
    
    /**
     * Suggest focus session duration based on time of day and task type.
     * @return Duration in minutes
     */
    public int suggestFocusDuration(int hourOfDay, String taskName) {
        // Base duration on time of day
        int baseDuration;
        if (hourOfDay >= 6 && hourOfDay < 12) {
            // Morning: longer focus sessions (peak productivity)
            baseDuration = 90;
        } else if (hourOfDay >= 12 && hourOfDay < 17) {
            // Afternoon: medium sessions (post-lunch dip)
            baseDuration = 60;
        } else if (hourOfDay >= 17 && hourOfDay < 21) {
            // Evening: shorter sessions
            baseDuration = 45;
        } else {
            // Late night: short sessions
            baseDuration = 30;
        }
        
        // Adjust based on task complexity (name length as proxy)
        if (taskName != null && taskName.split("\\s+").length > 4) {
            baseDuration += 15; // Complex tasks need more time
        }
        
        return baseDuration;
    }
    
    /**
     * Get a productivity insight based on user patterns.
     * @return Human-readable insight string
     */
    public String getProductivityInsight() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        
        // Time-based insights
        if (hour >= 6 && hour < 9) {
            return "🌅 Early bird advantage! Morning hours are great for deep work.";
        } else if (hour >= 9 && hour < 12) {
            return "☀️ Peak productivity time! Tackle your most important tasks now.";
        } else if (hour >= 12 && hour < 14) {
            return "🍽️ Lunch time dip is normal. Light tasks work best now.";
        } else if (hour >= 14 && hour < 17) {
            return "📈 Afternoon focus zone. Great time for collaborative work.";
        } else if (hour >= 17 && hour < 20) {
            return "🌆 Evening wind-down. Review tomorrow's priorities.";
        } else {
            return "🌙 Rest is productive too! Consider planning for tomorrow.";
        }
    }
    
    /**
     * Get emoji for priority level
     */
    public String getPriorityEmoji(String priority) {
        switch (priority) {
            case "High": return "🔴";
            case "Medium": return "🟡";
            case "Low": return "🟢";
            default: return "⚪";
        }
    }
    
    /**
     * Get emoji for category
     */
    public String getCategoryEmoji(String category) {
        switch (category) {
            case "Work": return "💼";
            case "Personal": return "🏠";
            case "Health": return "💪";
            case "Finance": return "💰";
            case "Learning": return "📚";
            default: return "📋";
        }
    }
    
    // Helper methods
    
    private String getCurrentTimeOfDay() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "morning";
        if (hour >= 12 && hour < 18) return "afternoon";
        return "evening";
    }
    
    private String getMostProductiveTimeOfDay() {
        // Could be enhanced with actual user pattern tracking
        // For now, default to morning
        return "morning";
    }
    
    /**
     * Record a task completion for pattern tracking
     */
    public void recordCompletion(int hour, int dayOfWeek) {
        String key = "completions_hour_" + hour;
        int count = prefs.getInt(key, 0);
        prefs.edit().putInt(key, count + 1).apply();
        
        String dayKey = "completions_day_" + dayOfWeek;
        int dayCount = prefs.getInt(dayKey, 0);
        prefs.edit().putInt(dayKey, dayCount + 1).apply();
    }
    
    /**
     * Check if smart suggestions are enabled
     */
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("smart_suggestions_enabled", true);
    }
}
