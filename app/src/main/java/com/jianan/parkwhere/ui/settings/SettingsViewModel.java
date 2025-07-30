package com.jianan.parkwhere.ui.settings;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jianan.parkwhere.data.preferences.SettingsManager;
import com.jianan.parkwhere.util.ThemeUtils;

public class SettingsViewModel extends AndroidViewModel {
    private static final String TAG = "SettingsViewModel"; // For logging
    private final SettingsManager settingsManager;

    // Expose SettingsManager constants for UI
    public static final int VEHICLE_BOTH = SettingsManager.VEHICLE_BOTH;
    public static final int VEHICLE_CAR = SettingsManager.VEHICLE_CAR;
    public static final int VEHICLE_MOTORCYCLE = SettingsManager.VEHICLE_MOTORCYCLE;

    public static final int THEME_SYSTEM = SettingsManager.THEME_SYSTEM;
    public static final int THEME_LIGHT = SettingsManager.THEME_LIGHT;
    public static final int THEME_DARK = SettingsManager.THEME_DARK;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        settingsManager = SettingsManager.getSettingsManager(application);
    }

    public int getCurrentVehicleType() {
        return settingsManager.getVehicleType();
    }

    public void setVehicleType(int vehicleType) {
        settingsManager.setVehicleType(vehicleType);
    }

    public int getCurrentThemeMode() {
        return settingsManager.getThemeMode();
    }

    public void setThemeMode(int themeMode) {
        settingsManager.setThemeMode(themeMode);

        // Apply the new theme
        ThemeUtils.applyTheme(getApplication());
    }
}