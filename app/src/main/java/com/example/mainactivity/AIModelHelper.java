package com.example.mainactivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AI Model Helper - Provides intelligent ML-based predictions for tasks.
 * 
 * This class implements a hybrid approach:
 * 1. Rule-based keyword matching for basic predictions
 * 2. Pattern learning from user behavior stored locally
 * 3. TensorFlow Lite model integration ready (for future enhancement)
 * 
 * All processing is done on-device for privacy.
 */
public class AIModelHelper {
    
    private static final String TAG = "AIModelHelper";
    private static final String PREFS_NAME = "ai_model_prefs";
    private static final String PATTERN_DATA_KEY = "user_patterns";
    private static final String TASK_HISTORY_KEY = "task_history";
    
    private static AIModelHelper instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    
    // AI Configuration loaded from assets
    private Map<String, Object> aiConfig;
    private Map<String, List<String>> priorityKeywords;
    private Map<String, List<String>> categoryKeywords;
    private Map<String, List<String>> timeKeywords;
    
    // User pattern data
    private UserPatterns userPatterns;
    
    // Confidence thresholds
    private static final float HIGH_CONFIDENCE = 0.85f;
    private static final float MEDIUM_CONFIDENCE = 0.65f;
    private static final float LOW_CONFIDENCE = 0.45f;
    
    private AIModelHelper(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        
        loadAIConfig();
        loadUserPatterns();
    }
    
    public static synchronized AIModelHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AIModelHelper(context);
        }
        return instance;
    }
    
    /**
     * Load AI configuration from assets
     */
    @SuppressWarnings("unchecked")
    private void loadAIConfig() {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("ai_config.json"))
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            aiConfig = gson.fromJson(sb.toString(), type);
            
            // Extract keyword maps
            priorityKeywords = (Map<String, List<String>>) aiConfig.get("priority_keywords");
            categoryKeywords = (Map<String, List<String>>) aiConfig.get("category_keywords");
            timeKeywords = (Map<String, List<String>>) aiConfig.get("time_keywords");
            
            Log.d(TAG, "AI config loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load AI config", e);
            initializeDefaultConfig();
        }
    }
    
    /**
     * Initialize default configuration if loading fails
     */
    private void initializeDefaultConfig() {
        priorityKeywords = new HashMap<>();
        priorityKeywords.put("high", new ArrayList<>());
        priorityKeywords.put("medium", new ArrayList<>());
        priorityKeywords.put("low", new ArrayList<>());
        
        categoryKeywords = new HashMap<>();
        timeKeywords = new HashMap<>();
    }
    
    /**
     * Load user patterns from SharedPreferences
     */
    private void loadUserPatterns() {
        String json = prefs.getString(PATTERN_DATA_KEY, null);
        if (json != null) {
            try {
                userPatterns = gson.fromJson(json, UserPatterns.class);
            } catch (Exception e) {
                userPatterns = new UserPatterns();
            }
        } else {
            userPatterns = new UserPatterns();
        }
    }
    
    /**
     * Save user patterns to SharedPreferences
     */
    private void saveUserPatterns() {
        String json = gson.toJson(userPatterns);
        prefs.edit().putString(PATTERN_DATA_KEY, json).apply();
    }
    
    // ==================== PREDICTION METHODS ====================
    
    /**
     * Calculate Levenshtein distance for fuzzy matching
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j] + 1, 
                           Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost));
            }
        }
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Check for fuzzy match with threshold
     */
    private boolean fuzzyMatch(String word, String keyword, float threshold) {
        if (word.equals(keyword)) return true;
        if (word.contains(keyword) || keyword.contains(word)) return true;
        
        int distance = levenshteinDistance(word, keyword);
        float similarity = 1.0f - (float) distance / Math.max(word.length(), keyword.length());
        return similarity >= threshold;
    }
    
    /**
     * Generate n-grams from text
     */
    private List<String> generateNgrams(String text, int n) {
        List<String> ngrams = new ArrayList<>();
        String[] words = text.toLowerCase().split("\\s+");
        
        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) sb.append(" ");
                sb.append(words[i + j]);
            }
            ngrams.add(sb.toString());
        }
        return ngrams;
    }
    
    /**
     * Enhanced keyword scoring with n-gram and fuzzy matching
     */
    private float scoreKeywords(String text, List<String> keywords, boolean useFuzzy) {
        if (keywords == null || keywords.isEmpty()) return 0f;
        
        String lowerText = text.toLowerCase();
        String[] words = lowerText.split("\\s+");
        List<String> bigrams = generateNgrams(text, 2);
        
        float score = 0f;
        
        for (String keyword : keywords) {
            // Exact word match (highest score)
            for (String word : words) {
                if (word.equals(keyword)) {
                    score += 3.0f;
                }
            }
            
            // Phrase/substring match
            if (lowerText.contains(keyword)) {
                score += 2.0f;
            }
            
            // Bigram match for multi-word keywords
            for (String bigram : bigrams) {
                if (bigram.contains(keyword) || keyword.contains(bigram)) {
                    score += 1.5f;
                }
            }
            
            // Fuzzy matching for typos
            if (useFuzzy) {
                for (String word : words) {
                    if (word.length() >= 4 && keyword.length() >= 4 && fuzzyMatch(word, keyword, 0.75f)) {
                        score += 1.0f;
                    }
                }
            }
        }
        
        return score;
    }
    
    /**
     * Predict priority with enhanced confidence score using advanced matching
     */
    public AIPrediction predictPriority(String taskName) {
        if (taskName == null || taskName.isEmpty()) {
            return new AIPrediction("None", LOW_CONFIDENCE, "No task name provided");
        }
        
        // Enhanced scoring with n-gram and fuzzy matching
        float highScore = scoreKeywords(taskName, priorityKeywords.get("high"), true);
        float mediumScore = scoreKeywords(taskName, priorityKeywords.get("medium"), true);
        float lowScore = scoreKeywords(taskName, priorityKeywords.get("low"), true);
        
        // Context-aware boosting based on keywords
        String lowerName = taskName.toLowerCase();
        
        // ===== HIGH PRIORITY BOOSTING =====
        // Urgency indicators
        if (lowerName.contains("deadline") || lowerName.contains("due") || 
            lowerName.contains("asap") || lowerName.contains("urgent") ||
            lowerName.contains("important") || lowerName.contains("critical") ||
            lowerName.contains("emergency") || lowerName.contains("immediately") ||
            lowerName.contains("right now") || lowerName.contains("today") ||
            lowerName.contains("tonight") || lowerName.contains("this morning") ||
            lowerName.contains("this afternoon") || lowerName.contains("this evening")) {
            highScore += 5.0f;
        }
        
        // Time-sensitive phrases
        if (lowerName.contains("before") || lowerName.contains("by ") ||
            lowerName.contains("no later than") || lowerName.contains("must be done") ||
            lowerName.contains("cannot miss") || lowerName.contains("can't miss") ||
            lowerName.contains("don't forget") || lowerName.contains("dont forget") ||
            lowerName.contains("reminder") || lowerName.contains("alarm")) {
            highScore += 4.0f;
        }
        
        // Professional/Critical events
        if (lowerName.contains("meeting") || lowerName.contains("interview") ||
            lowerName.contains("presentation") || lowerName.contains("pitch") ||
            lowerName.contains("demo") || lowerName.contains("client") ||
            lowerName.contains("boss") || lowerName.contains("investor") ||
            lowerName.contains("board") || lowerName.contains("stakeholder")) {
            highScore += 3.5f;
        }
        
        // Medical/Legal/Travel
        if (lowerName.contains("doctor") || lowerName.contains("hospital") ||
            lowerName.contains("surgery") || lowerName.contains("court") ||
            lowerName.contains("flight") || lowerName.contains("airport") ||
            lowerName.contains("visa") || lowerName.contains("passport") ||
            lowerName.contains("appointment") || lowerName.contains("checkup")) {
            highScore += 4.0f;
        }
        
        // Deadline-related abbreviations
        if (lowerName.contains("eod") || lowerName.contains("cob") ||
            lowerName.contains("eow") || lowerName.contains("end of day") ||
            lowerName.contains("end of week") || lowerName.contains("close of business")) {
            highScore += 4.5f;
        }
        
        // ===== MEDIUM PRIORITY BOOSTING =====
        // Action words
        if (lowerName.contains("review") || lowerName.contains("prepare") ||
            lowerName.contains("complete") || lowerName.contains("finish") ||
            lowerName.contains("submit") || lowerName.contains("send") ||
            lowerName.contains("call") || lowerName.contains("email") ||
            lowerName.contains("respond") || lowerName.contains("reply")) {
            mediumScore += 2.5f;
        }
        
        // Planning/Organization
        if (lowerName.contains("plan") || lowerName.contains("organize") ||
            lowerName.contains("schedule") || lowerName.contains("book") ||
            lowerName.contains("arrange") || lowerName.contains("setup") ||
            lowerName.contains("set up") || lowerName.contains("confirm")) {
            mediumScore += 2.0f;
        }
        
        // Shopping/Errands
        if (lowerName.contains("buy") || lowerName.contains("shop") ||
            lowerName.contains("groceries") || lowerName.contains("pick up") ||
            lowerName.contains("pickup") || lowerName.contains("return") ||
            lowerName.contains("exchange") || lowerName.contains("order")) {
            mediumScore += 2.0f;
        }
        
        // ===== LOW PRIORITY BOOSTING =====
        // Flexible/Optional
        if (lowerName.contains("maybe") || lowerName.contains("optional") ||
            lowerName.contains("sometime") || lowerName.contains("when possible") ||
            lowerName.contains("if time") || lowerName.contains("whenever") ||
            lowerName.contains("eventually") || lowerName.contains("someday") ||
            lowerName.contains("no rush") || lowerName.contains("low priority")) {
            lowScore += 4.0f;
        }
        
        // Leisure/Relaxation
        if (lowerName.contains("relax") || lowerName.contains("chill") ||
            lowerName.contains("hobby") || lowerName.contains("fun") ||
            lowerName.contains("game") || lowerName.contains("movie") ||
            lowerName.contains("show") || lowerName.contains("youtube") ||
            lowerName.contains("netflix") || lowerName.contains("spotify")) {
            lowScore += 3.0f;
        }
        
        // Long-term goals
        if (lowerName.contains("bucket list") || lowerName.contains("dream") ||
            lowerName.contains("goal") || lowerName.contains("aspiration") ||
            lowerName.contains("long term") || lowerName.contains("future")) {
            lowScore += 2.5f;
        }
        
        // Apply user pattern boost
        String userPreferredPriority = userPatterns.getMostUsedPriority();
        if ("High".equals(userPreferredPriority)) highScore += 1.0f;
        else if ("Medium".equals(userPreferredPriority)) mediumScore += 1.0f;
        else if ("Low".equals(userPreferredPriority)) lowScore += 1.0f;
        
        // Determine result
        float maxScore = Math.max(highScore, Math.max(mediumScore, lowScore));
        String priority;
        float confidence;
        String reason;
        
        if (maxScore == 0) {
            priority = "None";
            confidence = LOW_CONFIDENCE;
            reason = "No priority indicators found";
        } else if (highScore >= maxScore) {
            priority = "High";
            confidence = Math.min(HIGH_CONFIDENCE, 0.5f + (highScore * 0.05f));
            reason = "Contains urgent/important keywords";
        } else if (mediumScore >= maxScore) {
            priority = "Medium";
            confidence = Math.min(MEDIUM_CONFIDENCE, 0.4f + (mediumScore * 0.05f));
            reason = "Contains action-oriented keywords";
        } else {
            priority = "Low";
            confidence = Math.min(MEDIUM_CONFIDENCE, 0.4f + (lowScore * 0.05f));
            reason = "Contains flexible/optional keywords";
        }
        
        return new AIPrediction(priority, confidence, reason);
    }
    
    /**
     * Predict category with enhanced confidence score using advanced matching
     */
    public AIPrediction predictCategory(String taskName) {
        if (taskName == null || taskName.isEmpty()) {
            return new AIPrediction("General", LOW_CONFIDENCE, "No task name provided");
        }
        
        // Enhanced scoring with n-gram and fuzzy matching
        Map<String, Float> categoryScores = new HashMap<>();
        for (String category : categoryKeywords.keySet()) {
            float score = scoreKeywords(taskName, categoryKeywords.get(category), true);
            categoryScores.put(category, score);
        }
        
        // Context-aware category detection
        String lowerName = taskName.toLowerCase();
        
        // ===== WORK DETECTION =====
        // Meetings & Communication
        if (lowerName.contains("meeting") || lowerName.contains("email") || 
            lowerName.contains("project") || lowerName.contains("report") ||
            lowerName.contains("presentation") || lowerName.contains("client") ||
            lowerName.contains("office") || lowerName.contains("work") ||
            lowerName.contains("slack") || lowerName.contains("teams") ||
            lowerName.contains("zoom") || lowerName.contains("call") ||
            lowerName.contains("conference") || lowerName.contains("standup") ||
            lowerName.contains("scrum") || lowerName.contains("sprint") ||
            lowerName.contains("deadline") || lowerName.contains("deliverable")) {
            categoryScores.put("Work", categoryScores.getOrDefault("Work", 0f) + 5.0f);
        }
        
        // Professional roles/terms
        if (lowerName.contains("boss") || lowerName.contains("manager") ||
            lowerName.contains("colleague") || lowerName.contains("coworker") ||
            lowerName.contains("team") || lowerName.contains("department") ||
            lowerName.contains("hr") || lowerName.contains(" it ") ||
            lowerName.contains("ceo") || lowerName.contains("cto") ||
            lowerName.contains("director") || lowerName.contains("executive")) {
            categoryScores.put("Work", categoryScores.getOrDefault("Work", 0f) + 4.0f);
        }
        
        // Tech/Development specific
        if (lowerName.contains("code") || lowerName.contains("debug") ||
            lowerName.contains("deploy") || lowerName.contains("git") ||
            lowerName.contains("pull request") || lowerName.contains(" pr ") ||
            lowerName.contains("bug") || lowerName.contains("feature") ||
            lowerName.contains("api") || lowerName.contains("database")) {
            categoryScores.put("Work", categoryScores.getOrDefault("Work", 0f) + 4.5f);
        }
        
        // ===== HEALTH DETECTION =====
        // Exercise & Fitness
        if (lowerName.contains("gym") || lowerName.contains("workout") || 
            lowerName.contains("exercise") || lowerName.contains("doctor") ||
            lowerName.contains("medicine") || lowerName.contains("health") ||
            lowerName.contains("fitness") || lowerName.contains("run") ||
            lowerName.contains("jog") || lowerName.contains("swim") ||
            lowerName.contains("yoga") || lowerName.contains("pilates") ||
            lowerName.contains("cardio") || lowerName.contains("weights") ||
            lowerName.contains("lift") || lowerName.contains("stretch")) {
            categoryScores.put("Health", categoryScores.getOrDefault("Health", 0f) + 5.0f);
        }
        
        // Medical
        if (lowerName.contains("dentist") || lowerName.contains("hospital") ||
            lowerName.contains("clinic") || lowerName.contains("therapy") ||
            lowerName.contains("therapist") || lowerName.contains("prescription") ||
            lowerName.contains("pharmacy") || lowerName.contains("checkup") ||
            lowerName.contains("appointment") || lowerName.contains("vaccine") ||
            lowerName.contains("blood test") || lowerName.contains("lab")) {
            categoryScores.put("Health", categoryScores.getOrDefault("Health", 0f) + 5.0f);
        }
        
        // Nutrition & Wellness
        if (lowerName.contains("diet") || lowerName.contains("nutrition") ||
            lowerName.contains("calories") || lowerName.contains("protein") ||
            lowerName.contains("vitamin") || lowerName.contains("supplement") ||
            lowerName.contains("meal prep") || lowerName.contains("healthy") ||
            lowerName.contains("meditation") || lowerName.contains("mindfulness")) {
            categoryScores.put("Health", categoryScores.getOrDefault("Health", 0f) + 4.0f);
        }
        
        // ===== LEARNING DETECTION =====
        // School & Education
        if (lowerName.contains("study") || lowerName.contains("learn") || 
            lowerName.contains("course") || lowerName.contains("class") ||
            lowerName.contains("homework") || lowerName.contains("exam") ||
            lowerName.contains("school") || lowerName.contains("university") ||
            lowerName.contains("college") || lowerName.contains("lecture") ||
            lowerName.contains("professor") || lowerName.contains("teacher") ||
            lowerName.contains("assignment") || lowerName.contains("essay") ||
            lowerName.contains("thesis") || lowerName.contains("research")) {
            categoryScores.put("Learning", categoryScores.getOrDefault("Learning", 0f) + 5.0f);
        }
        
        // Online Learning
        if (lowerName.contains("tutorial") || lowerName.contains("udemy") ||
            lowerName.contains("coursera") || lowerName.contains("khan academy") ||
            lowerName.contains("skillshare") || lowerName.contains("bootcamp") ||
            lowerName.contains("certification") || lowerName.contains("webinar")) {
            categoryScores.put("Learning", categoryScores.getOrDefault("Learning", 0f) + 4.5f);
        }
        
        // ===== PERSONAL DETECTION =====
        // Family & Friends
        if (lowerName.contains("family") || lowerName.contains("home") || 
            lowerName.contains("personal") || lowerName.contains("birthday") ||
            lowerName.contains("friend") || lowerName.contains("party") ||
            lowerName.contains("mom") || lowerName.contains("dad") ||
            lowerName.contains("parent") || lowerName.contains("sibling") ||
            lowerName.contains("spouse") || lowerName.contains("husband") ||
            lowerName.contains("wife") || lowerName.contains("kid") ||
            lowerName.contains("children") || lowerName.contains("anniversary")) {
            categoryScores.put("Personal", categoryScores.getOrDefault("Personal", 0f) + 5.0f);
        }
        
        // Home & Chores
        if (lowerName.contains("clean") || lowerName.contains("laundry") ||
            lowerName.contains("dishes") || lowerName.contains("vacuum") ||
            lowerName.contains("grocery") || lowerName.contains("cook") ||
            lowerName.contains("garden") || lowerName.contains("lawn") ||
            lowerName.contains("repair") || lowerName.contains("fix") ||
            lowerName.contains("organize") || lowerName.contains("declutter")) {
            categoryScores.put("Personal", categoryScores.getOrDefault("Personal", 0f) + 4.0f);
        }
        
        // ===== FINANCE DETECTION =====
        // Banking & Payments
        if (lowerName.contains("pay") || lowerName.contains("bill") || 
            lowerName.contains("bank") || lowerName.contains("money") ||
            lowerName.contains("budget") || lowerName.contains("invoice") ||
            lowerName.contains("credit") || lowerName.contains("debit") ||
            lowerName.contains("transfer") || lowerName.contains("transaction") ||
            lowerName.contains("payment") || lowerName.contains("balance")) {
            categoryScores.put("Finance", categoryScores.getOrDefault("Finance", 0f) + 5.0f);
        }
        
        // Investment & Taxes
        if (lowerName.contains("invest") || lowerName.contains("stock") ||
            lowerName.contains("401k") || lowerName.contains("retirement") ||
            lowerName.contains("tax") || lowerName.contains("irs") ||
            lowerName.contains("accountant") || lowerName.contains("cpa") ||
            lowerName.contains("insurance") || lowerName.contains("mortgage") ||
            lowerName.contains("rent") || lowerName.contains("loan")) {
            categoryScores.put("Finance", categoryScores.getOrDefault("Finance", 0f) + 5.0f);
        }
        
        // Find best category
        String bestCategory = "General";
        float bestScore = 0f;
        for (Map.Entry<String, Float> entry : categoryScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        
        float confidence = bestScore > 0 ? Math.min(HIGH_CONFIDENCE, 0.4f + (bestScore * 0.08f)) : LOW_CONFIDENCE;
        String reason = bestScore > 0 ? "Matched " + bestCategory.toLowerCase() + " keywords" : "No specific category detected";
        
        return new AIPrediction(bestCategory, confidence, reason);
    }
    
    /**
     * Predict optimal time of day with enhanced detection
     */
    public AIPrediction predictTimeOfDay(String taskName) {
        if (taskName == null || taskName.isEmpty()) {
            return new AIPrediction(getCurrentTimeOfDay(), MEDIUM_CONFIDENCE, "Based on current time");
        }
        
        String lowerName = taskName.toLowerCase();
        
        // Enhanced time detection with more keywords
        if (lowerName.contains("morning") || lowerName.contains("breakfast") ||
            lowerName.contains("wake") || lowerName.contains("early") ||
            lowerName.contains("am routine") || lowerName.contains("sunrise")) {
            return new AIPrediction("morning", HIGH_CONFIDENCE, "Task contains morning keywords");
        }
        
        if (lowerName.contains("afternoon") || lowerName.contains("lunch") ||
            lowerName.contains("midday") || lowerName.contains("noon")) {
            return new AIPrediction("afternoon", HIGH_CONFIDENCE, "Task contains afternoon keywords");
        }
        
        if (lowerName.contains("evening") || lowerName.contains("night") ||
            lowerName.contains("dinner") || lowerName.contains("bedtime") ||
            lowerName.contains("pm routine") || lowerName.contains("sunset")) {
            return new AIPrediction("night", HIGH_CONFIDENCE, "Task contains evening keywords");
        }
        
        // Check time keywords from config
        for (Map.Entry<String, List<String>> entry : timeKeywords.entrySet()) {
            if (entry.getValue() != null) {
                for (String keyword : entry.getValue()) {
                    if (lowerName.contains(keyword)) {
                        return new AIPrediction(entry.getKey(), HIGH_CONFIDENCE, 
                            "Task contains '" + keyword + "' suggesting " + entry.getKey());
                    }
                }
            }
        }
        
        // Check category-based time suggestions
        AIPrediction categoryPrediction = predictCategory(taskName);
        String suggestedTime;
        String reason;
        
        switch (categoryPrediction.getValue()) {
            case "Health":
                suggestedTime = "morning";
                reason = "Health tasks are often best in the morning";
                break;
            case "Work":
                suggestedTime = "morning";
                reason = "Work tasks benefit from peak morning productivity";
                break;
            case "Personal":
                suggestedTime = "evening";
                reason = "Personal tasks often fit better in the evening";
                break;
            case "Learning":
                suggestedTime = "afternoon";
                reason = "Learning is effective in the afternoon";
                break;
            default:
                suggestedTime = userPatterns.getMostProductiveTime();
                reason = "Based on your productivity patterns";
        }
        
        return new AIPrediction(suggestedTime, MEDIUM_CONFIDENCE, reason);
    }
    
    /**
     * Predict focus session duration
     */
    public AIPrediction predictFocusDuration(String taskName, int hourOfDay) {
        int baseDuration;
        String reason;
        
        // Time-based adjustment
        if (hourOfDay >= 6 && hourOfDay < 12) {
            baseDuration = 90;
            reason = "Morning peak productivity - longer sessions recommended";
        } else if (hourOfDay >= 12 && hourOfDay < 14) {
            baseDuration = 45;
            reason = "Post-lunch dip - shorter sessions work better";
        } else if (hourOfDay >= 14 && hourOfDay < 17) {
            baseDuration = 60;
            reason = "Afternoon focus - medium sessions ideal";
        } else if (hourOfDay >= 17 && hourOfDay < 20) {
            baseDuration = 45;
            reason = "Evening wind-down - moderate sessions";
        } else {
            baseDuration = 30;
            reason = "Late hours - keep sessions short";
        }
        
        // Task complexity adjustment
        if (taskName != null) {
            int wordCount = taskName.split("\\s+").length;
            if (wordCount > 5) {
                baseDuration += 15;
                reason += "; complex task detected";
            }
            
            // Check for deep work keywords
            String lower = taskName.toLowerCase();
            if (lower.contains("deep") || lower.contains("focus") || lower.contains("develop") || 
                lower.contains("write") || lower.contains("design") || lower.contains("code")) {
                baseDuration += 30;
                reason = "Deep work task - extended focus time recommended";
            }
        }
        
        // User pattern adjustment
        int userPreferredDuration = userPatterns.getAverageFocusDuration();
        if (userPreferredDuration > 0) {
            baseDuration = (baseDuration + userPreferredDuration) / 2;
        }
        
        // Clamp to valid range
        baseDuration = Math.max(15, Math.min(180, baseDuration));
        
        return new AIPrediction(String.valueOf(baseDuration), MEDIUM_CONFIDENCE, reason);
    }
    
    /**
     * Get comprehensive AI suggestions for a task
     */
    public TaskSuggestions getTaskSuggestions(String taskName) {
        TaskSuggestions suggestions = new TaskSuggestions();
        suggestions.taskName = taskName;
        suggestions.priority = predictPriority(taskName);
        suggestions.category = predictCategory(taskName);
        suggestions.timeOfDay = predictTimeOfDay(taskName);
        suggestions.focusDuration = predictFocusDuration(taskName, Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        return suggestions;
    }
    
    /**
     * Get personalized productivity insight
     */
    public String getProductivityInsight() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        
        // Check user patterns
        int tasksCompletedToday = userPatterns.getTasksCompletedToday();
        int avgDailyTasks = userPatterns.getAverageDailyCompletions();
        String mostProductiveTime = userPatterns.getMostProductiveTime();
        
        List<String> insights = new ArrayList<>();
        
        // Time-based insights
        if (hour >= 6 && hour < 9) {
            insights.add("🌅 Early bird advantage! Your brain is fresh - perfect for challenging tasks.");
        } else if (hour >= 9 && hour < 12) {
            insights.add("☀️ Peak productivity zone! Tackle your most important work now.");
        } else if (hour >= 12 && hour < 14) {
            insights.add("🍽️ Post-lunch dip is natural. Try lighter tasks or take a brief walk.");
        } else if (hour >= 14 && hour < 17) {
            insights.add("📈 Afternoon rebound! Great time for collaborative work and meetings.");
        } else if (hour >= 17 && hour < 20) {
            insights.add("🌆 Evening wind-down. Review your day and plan tomorrow.");
        } else {
            insights.add("🌙 Rest is essential for productivity. Consider wrapping up.");
        }
        
        // Pattern-based insights
        if (tasksCompletedToday > avgDailyTasks) {
            insights.add("🎯 You're on fire! " + tasksCompletedToday + " tasks done - above your average!");
        } else if (tasksCompletedToday == 0 && hour > 12) {
            insights.add("💪 No tasks completed yet. Start with something small to build momentum!");
        }
        
        // Day-specific insights
        if (dayOfWeek == Calendar.MONDAY) {
            insights.add("📋 Monday planning: Set your top 3 priorities for the week.");
        } else if (dayOfWeek == Calendar.FRIDAY) {
            insights.add("🎉 Friday finish: Wrap up loose ends before the weekend!");
        }
        
        // Productivity time insight
        if (mostProductiveTime != null && !mostProductiveTime.isEmpty()) {
            String currentTimeOfDay = getCurrentTimeOfDay();
            if (currentTimeOfDay.equals(mostProductiveTime)) {
                insights.add("⚡ This is your most productive time! Make it count.");
            }
        }
        
        // Return a random insight to keep it fresh
        if (insights.isEmpty()) {
            return "🧠 AI Assistant ready. Create a task to see smart suggestions!";
        }
        return insights.get(new Random().nextInt(insights.size()));
    }
    
    // ==================== LEARNING/TRACKING METHODS ====================
    
    /**
     * Record task creation for pattern learning
     */
    public void recordTaskCreation(String taskName, String priority, String category, String timeOfDay) {
        userPatterns.recordTaskCreation(priority, category, timeOfDay);
        saveUserPatterns();
    }
    
    /**
     * Record task completion for pattern learning
     */
    public void recordTaskCompletion(int hour, int dayOfWeek) {
        userPatterns.recordCompletion(hour, dayOfWeek);
        saveUserPatterns();
    }
    
    /**
     * Record focus session completion
     */
    public void recordFocusSessionCompletion(int durationMinutes) {
        userPatterns.recordFocusSession(durationMinutes);
        saveUserPatterns();
    }
    
    /**
     * Clear all AI learning data
     */
    public void clearAllData() {
        userPatterns = new UserPatterns();
        saveUserPatterns();
        prefs.edit().remove(TASK_HISTORY_KEY).apply();
        Log.d(TAG, "AI data cleared");
    }
    
    // ==================== UTILITY METHODS ====================
    
    private String getCurrentTimeOfDay() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "morning";
        if (hour >= 12 && hour < 18) return "afternoon";
        if (hour >= 18 && hour < 21) return "evening";
        return "night";
    }
    
    /**
     * Check if AI features are enabled (master toggle)
     */
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("smart_suggestions_enabled", true);
    }
    
    /**
     * Check if priority prediction is enabled
     */
    public static boolean isPriorityEnabled(Context context) {
        if (!isEnabled(context)) return false;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("ai_priority_enabled", true);
    }
    
    /**
     * Check if category detection is enabled
     */
    public static boolean isCategoryEnabled(Context context) {
        if (!isEnabled(context)) return false;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("ai_category_enabled", true);
    }
    
    /**
     * Check if time suggestions are enabled
     */
    public static boolean isTimeEnabled(Context context) {
        if (!isEnabled(context)) return false;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("ai_time_enabled", true);
    }
    
    /**
     * Check if duration prediction is enabled
     */
    public static boolean isDurationEnabled(Context context) {
        if (!isEnabled(context)) return false;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("ai_duration_enabled", true);
    }
    
    /**
     * Check if productivity insights are enabled
     */
    public static boolean isInsightsEnabled(Context context) {
        if (!isEnabled(context)) return false;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("ai_insights_enabled", true);
    }
    
    // ==================== NATURAL LANGUAGE PARSING ====================
    
    /**
     * Parse natural language input to extract task details.
     * Example: "I need to wake up before 10:00AM for my meeting"
     * Returns: NLParseResult with extracted time, task name, priority, subtasks
     */
    public NLParseResult parseNaturalLanguageTask(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new NLParseResult();
        }
        
        NLParseResult result = new NLParseResult();
        result.originalInput = input;
        
        // Extract date from input FIRST (for proper context)
        DateExtractionResult dateResult = extractDateFromText(input);
        result.hasDate = dateResult.found;
        result.dateContext = dateResult.context;
        result.dayOffset = dateResult.dayOffset;
        result.extractedDate = dateResult.extractedDate;
        
        // Extract time from input
        TimeExtractionResult timeResult = extractTimeFromText(input);
        result.extractedHour = timeResult.hour;
        result.extractedMinute = timeResult.minute;
        result.extractedAmPm = timeResult.amPm;
        result.hasTime = timeResult.found;
        result.timeContext = timeResult.context; // "before", "at", "by", etc.
        
        // INTELLIGENT FALLBACK: If time is specified but no date, and time is in past, assume tomorrow
        if (result.hasTime && !result.hasDate) {
            java.util.Calendar now = java.util.Calendar.getInstance();
            java.util.Calendar taskTime = java.util.Calendar.getInstance();
            
            int hour24 = result.extractedHour;
            if (result.extractedAmPm.equals("PM") && result.extractedHour != 12) {
                hour24 = result.extractedHour + 12;
            } else if (result.extractedAmPm.equals("AM") && result.extractedHour == 12) {
                hour24 = 0;
            }
            taskTime.set(java.util.Calendar.HOUR_OF_DAY, hour24);
            taskTime.set(java.util.Calendar.MINUTE, result.extractedMinute);
            taskTime.set(java.util.Calendar.SECOND, 0);
            
            // If time is in the past for today, assume tomorrow
            if (taskTime.before(now)) {
                result.dayOffset = 1;
                result.hasDate = true;
                result.dateContext = "Tomorrow";
                
                // Update extractedDate to tomorrow
                java.util.Calendar tomorrow = java.util.Calendar.getInstance();
                tomorrow.add(java.util.Calendar.DAY_OF_YEAR, 1);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                result.extractedDate = sdf.format(tomorrow.getTime());
            }
        }
        
        // Extract task name (remove time and date references)
        result.extractedTaskName = extractTaskName(input, timeResult);
        
        // Get AI predictions for priority and category
        TaskSuggestions suggestions = getTaskSuggestions(result.extractedTaskName);
        result.predictedPriority = suggestions.priority;
        result.predictedCategory = suggestions.category;
        result.predictedTimeOfDay = suggestions.timeOfDay;
        
        // Generate contextual subtasks
        result.suggestedSubtasks = generateSubtasks(result.extractedTaskName, 
            result.predictedCategory != null ? result.predictedCategory.getValue() : "General");
        
        // Calculate overall confidence
        result.overallConfidence = calculateOverallConfidence(result);
        
        return result;
    }
    
    /**
     * Extract date from natural language text.
     * Handles: "tomorrow", "next Monday", "this weekend", "next week", "in X days"
     */
    private DateExtractionResult extractDateFromText(String text) {
        DateExtractionResult result = new DateExtractionResult();
        String lowerText = text.toLowerCase();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        
        // Pattern 1: "tomorrow"
        if (lowerText.contains("tomorrow")) {
            result.found = true;
            result.context = "Tomorrow";
            result.dayOffset = 1;
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            result.extractedDate = sdf.format(cal.getTime());
            return result;
        }
        
        // Pattern 2: "today" (explicit)
        if (lowerText.contains("today")) {
            result.found = true;
            result.context = "Today";
            result.dayOffset = 0;
            result.extractedDate = sdf.format(cal.getTime());
            return result;
        }
        
        // Pattern 3: "next week"
        if (lowerText.contains("next week")) {
            result.found = true;
            result.context = "Next week";
            result.dayOffset = 7;
            cal.add(java.util.Calendar.DAY_OF_YEAR, 7);
            result.extractedDate = sdf.format(cal.getTime());
            return result;
        }
        
        // Pattern 4: "this weekend" / "weekend"
        if (lowerText.contains("weekend") || lowerText.contains("this weekend")) {
            result.found = true;
            result.context = "This weekend";
            // Find next Saturday
            int daysUntilSaturday = (java.util.Calendar.SATURDAY - cal.get(java.util.Calendar.DAY_OF_WEEK) + 7) % 7;
            if (daysUntilSaturday == 0) daysUntilSaturday = 7; // If today is Saturday, next Saturday
            result.dayOffset = daysUntilSaturday;
            cal.add(java.util.Calendar.DAY_OF_YEAR, daysUntilSaturday);
            result.extractedDate = sdf.format(cal.getTime());
            return result;
        }
        
        // Pattern 5: "next Monday/Tuesday/Wednesday/Thursday/Friday/Saturday/Sunday"
        String[] dayNames = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
        for (int i = 0; i < dayNames.length; i++) {
            if (lowerText.contains("next " + dayNames[i])) {
                result.found = true;
                result.context = "Next " + dayNames[i].substring(0, 1).toUpperCase() + dayNames[i].substring(1);
                int targetDay = i + 1; // Calendar days are 1-indexed (SUNDAY=1)
                int currentDay = cal.get(java.util.Calendar.DAY_OF_WEEK);
                int daysUntil = (targetDay - currentDay + 7) % 7;
                if (daysUntil == 0) daysUntil = 7; // "next X" means at least 1 week
                result.dayOffset = daysUntil;
                cal.add(java.util.Calendar.DAY_OF_YEAR, daysUntil);
                result.extractedDate = sdf.format(cal.getTime());
                return result;
            }
        }
        
        // Pattern 6: "on Monday/Tuesday/etc" (this week or next)
        for (int i = 0; i < dayNames.length; i++) {
            java.util.regex.Pattern dayPattern = java.util.regex.Pattern.compile(
                "\\b(on\\s+)?" + dayNames[i] + "\\b",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            if (dayPattern.matcher(text).find()) {
                result.found = true;
                int targetDay = i + 1;
                int currentDay = cal.get(java.util.Calendar.DAY_OF_WEEK);
                int daysUntil = (targetDay - currentDay + 7) % 7;
                if (daysUntil == 0) {
                    result.context = "Today";
                } else {
                    result.context = dayNames[i].substring(0, 1).toUpperCase() + dayNames[i].substring(1);
                }
                result.dayOffset = daysUntil;
                cal.add(java.util.Calendar.DAY_OF_YEAR, daysUntil);
                result.extractedDate = sdf.format(cal.getTime());
                return result;
            }
        }
        
        // Pattern 7: "in X days"
        java.util.regex.Pattern inDaysPattern = java.util.regex.Pattern.compile(
            "in\\s*(\\d+)\\s*days?",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = inDaysPattern.matcher(text);
        if (matcher.find()) {
            result.found = true;
            int days = Integer.parseInt(matcher.group(1));
            result.dayOffset = days;
            result.context = "In " + days + " day" + (days > 1 ? "s" : "");
            cal.add(java.util.Calendar.DAY_OF_YEAR, days);
            result.extractedDate = sdf.format(cal.getTime());
            return result;
        }
        
        // Pattern 8: Specific date formats "January 25", "Jan 25", "1/25", "25th"
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile(
            "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s*(\\d{1,2})(?:st|nd|rd|th)?",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        matcher = datePattern.matcher(text);
        if (matcher.find()) {
            result.found = true;
            String monthStr = matcher.group(1).toLowerCase();
            int day = Integer.parseInt(matcher.group(2));
            
            String[] months = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
            int month = 0;
            for (int i = 0; i < months.length; i++) {
                if (monthStr.startsWith(months[i])) {
                    month = i;
                    break;
                }
            }
            
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, day);
            
            // If the date is in the past, assume next year
            if (cal.before(java.util.Calendar.getInstance())) {
                cal.add(java.util.Calendar.YEAR, 1);
            }
            
            result.extractedDate = sdf.format(cal.getTime());
            result.context = matcher.group(1).substring(0, 1).toUpperCase() + 
                            matcher.group(1).substring(1).toLowerCase() + " " + day;
            
            java.util.Calendar today = java.util.Calendar.getInstance();
            long diffMillis = cal.getTimeInMillis() - today.getTimeInMillis();
            result.dayOffset = (int) (diffMillis / (24 * 60 * 60 * 1000));
            return result;
        }
        
        return result;
    }
    
    /**
     * Date extraction result helper class
     */
    private static class DateExtractionResult {
        boolean found = false;
        String context = ""; // "Tomorrow", "Next Monday", etc.
        int dayOffset = 0;
        String extractedDate = ""; // Format: yyyy-MM-dd
    }
    
    /**
     * Extract time from natural language text
     */
    private TimeExtractionResult extractTimeFromText(String text) {
        TimeExtractionResult result = new TimeExtractionResult();
        String lowerText = text.toLowerCase();
        
        // Pattern 1: "before/at/by X:XX AM/PM" or "before/at/by X AM/PM"
        java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile(
            "(before|at|by|around|after|until)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = timePattern.matcher(text);
        
        if (matcher.find()) {
            result.found = true;
            result.context = matcher.group(1).toLowerCase();
            result.hour = Integer.parseInt(matcher.group(2));
            result.minute = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            String amPmMatch = matcher.group(4);
            if (amPmMatch != null) {
                result.amPm = amPmMatch.toLowerCase().startsWith("p") ? "PM" : "AM";
            } else {
                // Infer AM/PM based on context
                result.amPm = result.hour >= 7 && result.hour <= 11 ? "AM" : "PM";
            }
            
            // Adjust for "before" context - set alarm earlier
            if ("before".equals(result.context)) {
                result.minute = Math.max(0, result.minute - 15);
                if (result.minute < 0) {
                    result.minute = 45;
                    result.hour = result.hour > 1 ? result.hour - 1 : 12;
                }
            }
            return result;
        }
        
        // Pattern 2: Relative times like "in X hours/minutes"
        java.util.regex.Pattern relativePattern = java.util.regex.Pattern.compile(
            "in\\s*(\\d+)\\s*(hour|hours|minute|minutes|min|mins)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        matcher = relativePattern.matcher(text);
        
        if (matcher.find()) {
            result.found = true;
            result.context = "in";
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            if (unit.startsWith("hour")) {
                cal.add(java.util.Calendar.HOUR, amount);
            } else {
                cal.add(java.util.Calendar.MINUTE, amount);
            }
            
            result.hour = cal.get(java.util.Calendar.HOUR);
            if (result.hour == 0) result.hour = 12;
            result.minute = cal.get(java.util.Calendar.MINUTE);
            result.amPm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM ? "AM" : "PM";
            return result;
        }
        
        // Pattern 3: Keywords like "morning", "noon", "evening", "tonight"
        if (lowerText.contains("morning") || lowerText.contains("sunrise")) {
            result.found = true;
            result.context = "time_of_day";
            result.hour = 8;
            result.minute = 0;
            result.amPm = "AM";
        } else if (lowerText.contains("noon") || lowerText.contains("midday")) {
            result.found = true;
            result.context = "time_of_day";
            result.hour = 12;
            result.minute = 0;
            result.amPm = "PM";
        } else if (lowerText.contains("afternoon")) {
            result.found = true;
            result.context = "time_of_day";
            result.hour = 2;
            result.minute = 0;
            result.amPm = "PM";
        } else if (lowerText.contains("evening") || lowerText.contains("tonight")) {
            result.found = true;
            result.context = "time_of_day";
            result.hour = 7;
            result.minute = 0;
            result.amPm = "PM";
        } else if (lowerText.contains("night") || lowerText.contains("bedtime")) {
            result.found = true;
            result.context = "time_of_day";
            result.hour = 9;
            result.minute = 0;
            result.amPm = "PM";
        }
        
        return result;
    }
    
    /**
     * Extract clean task name from input by removing time and date references
     */
    private String extractTaskName(String input, TimeExtractionResult timeResult) {
        String taskName = input;
        
        // Remove common time patterns
        taskName = taskName.replaceAll("(?i)(before|at|by|around|after|until)\\s*\\d{1,2}(:\\d{2})?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?", "");
        taskName = taskName.replaceAll("(?i)in\\s*\\d+\\s*(hour|hours|minute|minutes|min|mins)", "");
        taskName = taskName.replaceAll("(?i)(this\\s+)?(morning|afternoon|evening|tonight|noon|midday)", "");
        
        // Remove date patterns
        taskName = taskName.replaceAll("(?i)\\b(tomorrow|today|tonight)\\b", "");
        taskName = taskName.replaceAll("(?i)\\b(next\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", "");
        taskName = taskName.replaceAll("(?i)\\b(this\\s+)?weekend\\b", "");
        taskName = taskName.replaceAll("(?i)\\bnext\\s+week\\b", "");
        taskName = taskName.replaceAll("(?i)\\bin\\s*\\d+\\s*days?\\b", "");
        taskName = taskName.replaceAll("(?i)\\b(on\\s+)?(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s*\\d{1,2}(?:st|nd|rd|th)?\\b", "");
        
        // Remove common filler phrases
        taskName = taskName.replaceAll("(?i)^(i need to|i have to|i want to|i should|remind me to|don't forget to|remember to|make sure to|gotta|gonna|need to|have to|want to|wake me up|wake me)\\s*", "");
        taskName = taskName.replaceAll("(?i)\\s+(for my|for the|for a)\\s*$", "");
        
        // Clean up extra spaces and punctuation
        taskName = taskName.replaceAll("\\s+", " ").trim();
        taskName = taskName.replaceAll("^[\\s,.-]+|[\\s,.-]+$", "");
        
        // Capitalize first letter
        if (!taskName.isEmpty()) {
            taskName = taskName.substring(0, 1).toUpperCase() + taskName.substring(1);
        }
        
        return taskName.isEmpty() ? "Task" : taskName;
    }
    
    /**
     * Generate contextual subtasks based on task name and category
     */
    public List<String> generateSubtasks(String taskName, String category) {
        List<String> subtasks = new ArrayList<>();
        String lowerName = taskName.toLowerCase();
        
        // Meeting-related subtasks
        if (lowerName.contains("meeting") || lowerName.contains("call") || lowerName.contains("conference")) {
            subtasks.add("📋 Prepare agenda");
            subtasks.add("📝 Review previous notes");
            subtasks.add("🔗 Check meeting link/location");
            subtasks.add("📊 Gather relevant documents");
            if (lowerName.contains("presentation")) {
                subtasks.add("💻 Test presentation slides");
                subtasks.add("🎤 Practice key points");
            }
        }
        // Wake up / morning routine
        else if (lowerName.contains("wake") || lowerName.contains("get up") || lowerName.contains("morning")) {
            subtasks.add("⏰ Set backup alarm");
            subtasks.add("☕ Prepare coffee/breakfast");
            subtasks.add("🚿 Shower and get ready");
            subtasks.add("👔 Choose outfit");
            subtasks.add("📱 Check calendar for the day");
        }
        // Workout / Health
        else if (lowerName.contains("gym") || lowerName.contains("workout") || lowerName.contains("exercise")) {
            subtasks.add("🎽 Pack gym clothes");
            subtasks.add("💧 Fill water bottle");
            subtasks.add("🎵 Prepare workout playlist");
            subtasks.add("🥗 Plan post-workout meal");
        }
        // Study / Learning
        else if (lowerName.contains("study") || lowerName.contains("exam") || lowerName.contains("homework") || lowerName.contains("learn")) {
            subtasks.add("📚 Gather study materials");
            subtasks.add("📵 Turn off distractions");
            subtasks.add("📝 Review notes");
            subtasks.add("⏱️ Set study timer (Pomodoro)");
            subtasks.add("☕ Prepare snacks and drinks");
        }
        // Shopping
        else if (lowerName.contains("shop") || lowerName.contains("buy") || lowerName.contains("groceries")) {
            subtasks.add("📝 Make shopping list");
            subtasks.add("💳 Check wallet/payment method");
            subtasks.add("🛒 Bring reusable bags");
            subtasks.add("📱 Check for coupons/deals");
        }
        // Travel / Trip
        else if (lowerName.contains("travel") || lowerName.contains("trip") || lowerName.contains("flight") || lowerName.contains("vacation")) {
            subtasks.add("🎫 Confirm booking details");
            subtasks.add("🧳 Pack essentials");
            subtasks.add("📄 Check documents (ID, tickets)");
            subtasks.add("🔌 Pack chargers");
            subtasks.add("🏠 Arrange home security");
        }
        // Doctor / Medical
        else if (lowerName.contains("doctor") || lowerName.contains("appointment") || lowerName.contains("medical") || lowerName.contains("dentist")) {
            subtasks.add("📋 Bring insurance card");
            subtasks.add("💊 List current medications");
            subtasks.add("📝 Write down symptoms/questions");
            subtasks.add("🚗 Plan transportation");
        }
        // Work project / deadline
        else if (lowerName.contains("project") || lowerName.contains("deadline") || lowerName.contains("submit") || lowerName.contains("report")) {
            subtasks.add("📊 Review requirements");
            subtasks.add("✅ Create task checklist");
            subtasks.add("📧 Notify stakeholders");
            subtasks.add("💾 Backup your work");
            subtasks.add("👀 Proofread before submitting");
        }
        // Default subtasks based on category
        else {
            switch (category) {
                case "Work":
                    subtasks.add("📧 Check related emails");
                    subtasks.add("📝 Take notes");
                    subtasks.add("✅ Update task status");
                    break;
                case "Personal":
                    subtasks.add("📱 Set reminder");
                    subtasks.add("📍 Confirm location/details");
                    break;
                case "Health":
                    subtasks.add("💧 Stay hydrated");
                    subtasks.add("🧘 Prepare mentally");
                    break;
                case "Finance":
                    subtasks.add("📊 Review account balance");
                    subtasks.add("🧾 Keep receipts");
                    break;
                case "Learning":
                    subtasks.add("📖 Review material");
                    subtasks.add("📝 Take notes");
                    break;
                default:
                    subtasks.add("📝 Prepare in advance");
                    subtasks.add("⏰ Set reminder");
            }
        }
        
        return subtasks;
    }
    
    /**
     * Calculate overall confidence for the parsed result
     */
    private float calculateOverallConfidence(NLParseResult result) {
        float totalConfidence = 0f;
        int count = 0;
        
        if (result.hasTime) {
            totalConfidence += 0.9f; // High confidence for time extraction
            count++;
        }
        
        if (result.predictedPriority != null) {
            totalConfidence += result.predictedPriority.getConfidence();
            count++;
        }
        
        if (result.predictedCategory != null) {
            totalConfidence += result.predictedCategory.getConfidence();
            count++;
        }
        
        if (!result.extractedTaskName.isEmpty()) {
            totalConfidence += 0.8f;
            count++;
        }
        
        return count > 0 ? totalConfidence / count : 0.5f;
    }
    
    // ==================== NL PARSE RESULT CLASS ====================
    
    /**
     * Result of natural language parsing
     */
    public static class NLParseResult {
        public String originalInput = "";
        public String extractedTaskName = "";
        public int extractedHour = 9;
        public int extractedMinute = 0;
        public String extractedAmPm = "AM";
        public boolean hasTime = false;
        public String timeContext = ""; // "before", "at", "by", "in", etc.
        
        // Date extraction fields
        public String extractedDate = ""; // Format: "yyyy-MM-dd"
        public boolean hasDate = false;
        public String dateContext = ""; // "tomorrow", "next Monday", etc.
        public int dayOffset = 0; // Days from today (0 = today, 1 = tomorrow, etc.)
        
        public AIPrediction predictedPriority;
        public AIPrediction predictedCategory;
        public AIPrediction predictedTimeOfDay;
        public List<String> suggestedSubtasks = new ArrayList<>();
        public float overallConfidence = 0f;
        
        public String getFormattedTime() {
            return String.format("%d:%02d %s", extractedHour, extractedMinute, extractedAmPm);
        }
        
        public String getFormattedDateTime() {
            if (hasDate && !dateContext.isEmpty()) {
                return dateContext + " at " + getFormattedTime();
            }
            return getFormattedTime();
        }
        
        public String getConfidenceEmoji() {
            if (overallConfidence >= 0.8f) return "🎯";
            if (overallConfidence >= 0.6f) return "💡";
            return "🤔";
        }
        
        public String getConfidenceText() {
            if (overallConfidence >= 0.8f) return "High confidence";
            if (overallConfidence >= 0.6f) return "Good confidence";
            return "Best guess";
        }
        
        /**
         * Get the Calendar object representing the extracted date and time.
         * Handles relative dates like "tomorrow" intelligently.
         */
        public java.util.Calendar getExtractedCalendar() {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            
            // Apply day offset
            if (dayOffset > 0) {
                cal.add(java.util.Calendar.DAY_OF_YEAR, dayOffset);
            } else if (hasDate && !extractedDate.isEmpty()) {
                // Parse specific date
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    java.util.Date date = sdf.parse(extractedDate);
                    if (date != null) {
                        cal.setTime(date);
                    }
                } catch (Exception e) {
                    // Use today if parsing fails
                }
            }
            
            // Set the time
            int hour24 = extractedHour;
            if (extractedAmPm.equals("PM") && extractedHour != 12) {
                hour24 = extractedHour + 12;
            } else if (extractedAmPm.equals("AM") && extractedHour == 12) {
                hour24 = 0;
            }
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour24);
            cal.set(java.util.Calendar.MINUTE, extractedMinute);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            
            return cal;
        }
        
        /**
         * Check if the extracted date/time is in the past.
         */
        public boolean isInPast() {
            return getExtractedCalendar().before(java.util.Calendar.getInstance());
        }
    }
    
    /**
     * Time extraction result helper class
     */
    private static class TimeExtractionResult {
        boolean found = false;
        int hour = 9;
        int minute = 0;
        String amPm = "AM";
        String context = "";
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Represents an AI prediction with confidence
     */
    public static class AIPrediction {
        private final String value;
        private final float confidence;
        private final String reason;
        
        public AIPrediction(String value, float confidence, String reason) {
            this.value = value;
            this.confidence = confidence;
            this.reason = reason;
        }
        
        public String getValue() { return value; }
        public float getConfidence() { return confidence; }
        public String getReason() { return reason; }
        public String getConfidenceLevel() {
            if (confidence >= HIGH_CONFIDENCE) return "High";
            if (confidence >= MEDIUM_CONFIDENCE) return "Medium";
            return "Low";
        }
        public String getEmoji() {
            if (confidence >= HIGH_CONFIDENCE) return "🎯";
            if (confidence >= MEDIUM_CONFIDENCE) return "💡";
            return "🤔";
        }
    }
    
    /**
     * Comprehensive task suggestions
     */
    public static class TaskSuggestions {
        public String taskName;
        public AIPrediction priority;
        public AIPrediction category;
        public AIPrediction timeOfDay;
        public AIPrediction focusDuration;
    }
    
    /**
     * User pattern storage
     */
    private static class UserPatterns {
        Map<String, Integer> priorityUsage = new HashMap<>();
        Map<String, Integer> categoryUsage = new HashMap<>();
        Map<String, Integer> timeOfDayUsage = new HashMap<>();
        Map<Integer, Integer> hourlyCompletions = new HashMap<>();
        Map<Integer, Integer> dailyCompletions = new HashMap<>();
        List<Integer> focusDurations = new ArrayList<>();
        int totalTasksCreated = 0;
        int totalTasksCompleted = 0;
        long lastCompletionDate = 0;
        int tasksCompletedToday = 0;
        
        void recordTaskCreation(String priority, String category, String timeOfDay) {
            totalTasksCreated++;
            if (priority != null) {
                priorityUsage.put(priority, priorityUsage.getOrDefault(priority, 0) + 1);
            }
            if (category != null) {
                categoryUsage.put(category, categoryUsage.getOrDefault(category, 0) + 1);
            }
            if (timeOfDay != null) {
                timeOfDayUsage.put(timeOfDay, timeOfDayUsage.getOrDefault(timeOfDay, 0) + 1);
            }
        }
        
        void recordCompletion(int hour, int dayOfWeek) {
            totalTasksCompleted++;
            hourlyCompletions.put(hour, hourlyCompletions.getOrDefault(hour, 0) + 1);
            dailyCompletions.put(dayOfWeek, dailyCompletions.getOrDefault(dayOfWeek, 0) + 1);
            
            // Track today's completions
            long today = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
            if (today != lastCompletionDate) {
                tasksCompletedToday = 1;
                lastCompletionDate = today;
            } else {
                tasksCompletedToday++;
            }
        }
        
        void recordFocusSession(int duration) {
            if (focusDurations.size() > 100) {
                focusDurations.remove(0); // Keep last 100
            }
            focusDurations.add(duration);
        }
        
        String getMostUsedPriority() {
            return getMaxKey(priorityUsage);
        }
        
        String getMostProductiveTime() {
            int bestHour = -1;
            int maxCompletions = 0;
            for (Map.Entry<Integer, Integer> entry : hourlyCompletions.entrySet()) {
                if (entry.getValue() > maxCompletions) {
                    maxCompletions = entry.getValue();
                    bestHour = entry.getKey();
                }
            }
            if (bestHour < 0) return "morning";
            if (bestHour >= 5 && bestHour < 12) return "morning";
            if (bestHour >= 12 && bestHour < 18) return "afternoon";
            if (bestHour >= 18 && bestHour < 21) return "evening";
            return "night";
        }
        
        int getAverageFocusDuration() {
            if (focusDurations.isEmpty()) return 0;
            int sum = 0;
            for (int d : focusDurations) sum += d;
            return sum / focusDurations.size();
        }
        
        int getTasksCompletedToday() {
            long today = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
            if (today != lastCompletionDate) return 0;
            return tasksCompletedToday;
        }
        
        int getAverageDailyCompletions() {
            if (dailyCompletions.isEmpty()) return 3; // Default
            int sum = 0;
            for (int count : dailyCompletions.values()) sum += count;
            return sum / dailyCompletions.size();
        }
        
        private String getMaxKey(Map<String, Integer> map) {
            String maxKey = null;
            int maxValue = 0;
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (entry.getValue() > maxValue) {
                    maxValue = entry.getValue();
                    maxKey = entry.getKey();
                }
            }
            return maxKey;
        }
    }
}
