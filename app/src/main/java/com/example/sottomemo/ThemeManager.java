package com.example.sottomemo;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    public static final String LIGHT_MODE = "light";
    public static final String DARK_MODE = "dark";
    public static final String SYSTEM_DEFAULT = "system";

    public static void applyTheme(String themePreference) {
        switch (themePreference) {
            case LIGHT_MODE:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK_MODE:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}