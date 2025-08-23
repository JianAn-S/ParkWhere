package com.jianan.parkwhere.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Centralised manager for application settings stored in {@link android.content.SharedPreferences}.
 *
 * This class provides convenient accessors and mutators for various application preferences
 * (radius, vehicle type, bookmarks, theme, etc). It uses a thread-safe lazy singleton pattern
 * (double-checked locking) and caches bookmarked car park numbers in memory for quick access.
 **/
public class SettingsManager {
    private static final String PREF_NAME = "park_where_preferences";
    private static final String KEY_DATABASE_INITIALISED = "database_initialised";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_RADIUS = "radius_value";
    private static final String KEY_VEHICLE_TYPE = "vehicle_type";
    private static final String KEY_BOOKMARKS = "bookmarked_car_parks";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Vehicle Type Constants
    public static final int VEHICLE_BOTH = 0;
    public static final int VEHICLE_CAR = 1;
    public static final int VEHICLE_MOTORCYCLE = 2;

    // Theme Mode Constants
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private final SharedPreferences sharedPreferences;
    private static volatile SettingsManager instance;

    // In-memory cache for bookmarked car park numbers
    private Set<String> bookmarkedCarParkNumbers = null;

    // To observe vehicle type changes
    private final MutableLiveData <Integer> vehicleTypeLiveData = new MutableLiveData<>();

    private SettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, context.MODE_PRIVATE);
        vehicleTypeLiveData.setValue(getVehicleType());
    }

    /**
     * Returns the singleton {@code SettingsManager} instance.
     *
     * This method uses a thread-safe lazy initialisation (double-checked locking) and will
     * internally convert the provided {@code context} to the application context to avoid
     * leaking activity contexts.
     *
     * @param context any valid {@link android.content.Context}, the application context will be used
     * @return the singleton {@link SettingsManager} instance
     */
    public static SettingsManager getSettingsManager (Context context) {
        if (instance == null){
            synchronized (SettingsManager.class) {
                if (instance == null) {
                    instance = new SettingsManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // -------------------------
    // Database Methods
    // -------------------------

    /**
     * Checks whether the application's database has been initialised.
     *
     * @return {@code true} if the database has been initialise, {@code false} otherwise
     */
    public boolean isDatabaseInitialised() {
        return sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
    }

    /**
     * Sets the database initialisation flag in preferences.
     *
     * @param isInitialised {@code true} if the database is initialised, {@code false} otherwise
     */
    public void setDatabaseInitialised(boolean isInitialised) {
        sharedPreferences.edit().putBoolean(KEY_DATABASE_INITIALISED, isInitialised).apply();
    }

    // -------------------------
    // Location Methods
    // -------------------------

    /**
     * Returns whether the application is launched for the first time.
     *
     * @return {@code true} if this is the first launch (default), {@code false} otherwise
     */
    public boolean isFirstLaunch() {
        // Check to see if this is the first launch of the application, default to false if not set
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    /**
     * Sets the first-launch flag.
     *
     * @param isFirstLaunch {@code true} if this is the first launch, {@code false} otherwise
     */
    public void setFirstLaunch(boolean isFirstLaunch) {
        // Set the first launch status of the application
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch).apply();
    }

    // -------------------------
    // Radius Methods
    // -------------------------

    /**
     * Returns the configured search radius in metres.
     *
     * @return the radius value (default {@code 1000.0f} metres if not set)
     */
    public float getRadiusValue() {
        float radius = sharedPreferences.getFloat(KEY_RADIUS, 1000.0f);
        Log.d("SharedPreferences", "KEY_RADIUS value is: " + radius);
        return radius;
    }

    /**
     * Stores the search radius value.
     *
     * @param radius radius in metres to store
     */
    public void setRadiusValue(float radius) {
        sharedPreferences.edit().putFloat(KEY_RADIUS, radius).apply();

        // Check the value to confirm, to delete later
        float savedValue = sharedPreferences.getFloat(KEY_RADIUS, 1000.0f);
        Log.d("SharedPreferences", "KEY_RADIUS value is set to: " + savedValue);
    }

    // -------------------------
    // Vehicle Type Methods
    // -------------------------

    /**
     * Returns the currently selected vehicle type.
     *
     * @return one of {@link #VEHICLE_BOTH}, {@link #VEHICLE_CAR}, or {@link #VEHICLE_MOTORCYCLE}
     */
    public int getVehicleType() {
        return sharedPreferences.getInt(KEY_VEHICLE_TYPE, VEHICLE_BOTH);
    }

    /**
     * Updates the stored vehicle type and notifies observers if the value changed.
     *
     * @param vehicleType one of {@link #VEHICLE_BOTH}, {@link #VEHICLE_CAR}, or {@link #VEHICLE_MOTORCYCLE}
     */
    public void setVehicleType(int vehicleType) {
        int currentValue = getVehicleType();
        if (currentValue != vehicleType) {
            sharedPreferences.edit().putInt(KEY_VEHICLE_TYPE, vehicleType).apply();
            vehicleTypeLiveData.setValue(vehicleType);
        }
    }

    /**
     * Returns a {@link androidx.lifecycle.LiveData} stream that observers can use to react to
     * vehicle type changes.
     *
     * @return a LiveData of {@link Integer} representing the current vehicle type
     */
    public LiveData<Integer> getVehicleTypeLiveData() {
        return vehicleTypeLiveData;
    }

    // -------------------------
    // Bookmark Methods
    // -------------------------

    /**
     * Returns the set of bookmarked car park numbers.
     * This method initialises an in-memory cache on first call and returns a defensive copy
     * so callers cannot mutate the internal cache directly.
     *
     * @return a new {@link java.util.Set} containing the bookmarked car park numbers
     */
    public Set<String> getBookmarkedCarParkNumbers() {
        if (bookmarkedCarParkNumbers == null) {
            bookmarkedCarParkNumbers = new HashSet<>(sharedPreferences.getStringSet(KEY_BOOKMARKS, new HashSet<>()));
        }
        return new HashSet<>(bookmarkedCarParkNumbers);
    }

    /**
     * Checks whether a car park is currently bookmarked.
     *
     * @param carParkNumber the car park number to check
     * @return {@code true} if the car park is bookmarked; {@code false} otherwise
     */
    public boolean isBookmarked (String carParkNumber) {
        return getBookmarkedCarParkNumbers().contains(carParkNumber);
    }

    /**
     * Toggles the bookmark state for a given car park number.
     *
     * If the car park is already bookmarked it will be removed, otherwise it will be added.
     * The change is saved to both the in-memory cache and preferences.
     *
     * @param carParkNumber the car park number to toggle
     */
    public void toggleBookmark(String carParkNumber) {
        Set<String> bookmarks = new HashSet<>(getBookmarkedCarParkNumbers());
        if (bookmarks.contains(carParkNumber)) {
            bookmarks.remove(carParkNumber);
        } else {
            bookmarks.add(carParkNumber);
        }
        // Update in-memory cache and shared preferences
        bookmarkedCarParkNumbers = bookmarks;
        sharedPreferences.edit().putStringSet(KEY_BOOKMARKS, bookmarks).apply();
    }

    /**
     * Returns the number of bookmarked car parks.
     *
     * @return the count of bookmarks
     */
    public int getBookmarkCount() {
        return getBookmarkedCarParkNumbers().size();
    }

    // -------------------------
    // Theme Methods
    // -------------------------

    /**
     * Returns the configured theme mode.
     *
     * @return one of {@link #THEME_SYSTEM}, {@link #THEME_LIGHT}, or {@link #THEME_DARK}
     */
    public int getThemeMode() {
        return sharedPreferences.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    /**
     * Stores the selected theme mode.
     *
     * @param themeMode one of {@link #THEME_SYSTEM}, {@link #THEME_LIGHT}, or {@link #THEME_DARK}
     */
    public void setThemeMode(int themeMode) {
        sharedPreferences.edit().putInt(KEY_THEME_MODE, themeMode).apply();
    }
}
