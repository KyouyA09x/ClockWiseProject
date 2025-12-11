package com.example.mainactivity;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_THEME_COLOR = "theme_color";

    public static final int MODE_LIGHT = 0;
    public static final int MODE_DARK = 1;
    public static final int MODE_AUTO = 2;

    public static final String COLOR_DEFAULT = "default";
    public static final String COLOR_CYAN = "cyan";
    public static final String COLOR_GREEN = "green";
    public static final String COLOR_PURPLE = "purple";
    public static final String COLOR_ORANGE = "orange";

    public static void applyTheme(Context context) {
        int mode = getThemeMode(context);
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_AUTO:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static int getThemeResource(Context context) {
        String color = getThemeColor(context);
        switch (color) {
            case COLOR_CYAN:
                return R.style.Theme_MainActivity_Cyan;
            case COLOR_GREEN:
                return R.style.Theme_MainActivity_Green;
            case COLOR_PURPLE:
                return R.style.Theme_MainActivity_Purple;
            case COLOR_ORANGE:
                return R.style.Theme_MainActivity_Orange;
            case COLOR_DEFAULT:
            default:
                return R.style.Theme_MainActivity;
        }
    }

    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_AUTO);
    }

    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyTheme(context);
    }

    public static String getThemeColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME_COLOR, COLOR_DEFAULT);
    }

    public static void setThemeColor(Context context, String color) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME_COLOR, color).apply();
    }
}