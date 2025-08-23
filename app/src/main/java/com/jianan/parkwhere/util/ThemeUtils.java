package com.jianan.parkwhere.util;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.jianan.parkwhere.data.preferences.SettingsManager;
import com.jianan.parkwhere.R;

/**
 * Utility class for applying themes across the application
 */
public class ThemeUtils {
    /**
     * Apply the user-selected theme mode across the application
     *
     * @param context the application or activity context used to access settings
     */
    public static void applyTheme(Context context) {
        SettingsManager settingsManager = SettingsManager.getSettingsManager(context);
        int themeMode = settingsManager.getThemeMode();

        switch (themeMode) {
            case SettingsManager.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SettingsManager.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SettingsManager.THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
