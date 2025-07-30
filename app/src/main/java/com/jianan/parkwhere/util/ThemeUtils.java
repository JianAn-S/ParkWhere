package com.jianan.parkwhere.util;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

import com.jianan.parkwhere.data.preferences.SettingsManager;

public class ThemeUtils {
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
