package com.jianan.parkwhere.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SettingsManager {
    private static final String PREF_NAME = "park_where_preferences";
    private static final String KEY_DATABASE_INITIALISED = "database_initialised";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_LOCATION_PERMISSION = "location_permission";
    private static final String KEY_RADIUS = "radius_value";
    private static final String KEY_VEHICLE_TYPE = "vehicle_type";
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

    private SettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, context.MODE_PRIVATE);
    }

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

    public boolean isDatabaseInitialised() {
        // boolean value = sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
        // Log.d("SharedPreferences", "KEY_DATABASE_INITIALISED value is : " +  value);
        // return value;

        // Once proven to work, delete the above and uncomment below
        // Retrieve the initialisation status of the database, default to false if not set
        return sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
    }

    public void setDatabaseInitialised(boolean isInitialised) {
        // Set the initialisation status of the database
        sharedPreferences.edit().putBoolean(KEY_DATABASE_INITIALISED, isInitialised).apply();

        // Check the value to confirm, to delete later
        // boolean savedValue = sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
        // Log.d("SharedPreferences", "KEY_DATABASE_INITIALISED is set to: " + savedValue);
    }

    public boolean isFirstLaunch() {
        // boolean value = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
        // Log.d("SharedPreferences", "KEY_FIRST_LAUNCH value is : " + value);
        // return value;

        // Once proven to work, delete the above and uncomment below
        // Check to see if this is the first launch of the application, default to false if not set
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean isFirstLaunch) {
        // Set the first launch status of the application
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch).apply();

        // Check the value to confirm, to delete later
        // boolean savedValue = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
        // Log.d("SharedPreferences", "KEY_FIRST_LAUNCH is set to: " + savedValue);
    }

    // The getter for location permission
    public boolean isLocationPermissionAccepted() {
        boolean value = sharedPreferences.getBoolean(KEY_LOCATION_PERMISSION, false);
        Log.d("SharedPreferences", "KEY_LOCATION_PERMISSION value is : " + value);
        return value;

        // Once proven to work, delete the above and uncomment below
        //return sharedPreferences.getBoolean(KEY_LOCATION_PERMISSION_DENIED, false);
    }

    public void setLocationPermission(boolean isAccepted) {
        sharedPreferences.edit().putBoolean(KEY_LOCATION_PERMISSION, isAccepted).apply();

        // Check the value to confirm, to delete later
        boolean savedValue = sharedPreferences.getBoolean(KEY_LOCATION_PERMISSION, false);
        Log.d("SharedPreferences", "KEY_LOCATION_PERMISSION is set to: " + savedValue);
    }

    public float getRadiusValue() {
        float radius = sharedPreferences.getFloat(KEY_RADIUS, 1000.0f);
        Log.d("SharedPreferences", "KEY_RADIUS value is: " + radius);
        return radius;
    }

    public void setRadiusValue(float radius) {
        sharedPreferences.edit().putFloat(KEY_RADIUS, radius).apply();

        // Check the value to confirm, to delete later
        float savedValue = sharedPreferences.getFloat(KEY_RADIUS, 1000.0f);
        Log.d("SharedPreferences", "KEY_RADIUS value is set to: " + savedValue);
    }

    public int getVehicleType() {
        int vehicleType = sharedPreferences.getInt(KEY_VEHICLE_TYPE, VEHICLE_BOTH);
        Log.d("SharedPreferences", "KEY_VEHICLE_TYPE value is : " + vehicleType);
        return vehicleType;

        // Once proven to work, delete the above and uncomment below
        //return sharedPreferences.getBoolean(KEY_VEHICLE_TYPE, VEHICLE_BOTH);
    }

    public void setVehicleType(int vehicleType) {
        sharedPreferences.edit().putInt(KEY_VEHICLE_TYPE, vehicleType).apply();

        // Check the value to confirm, to delete later
        int savedValue = sharedPreferences.getInt(KEY_VEHICLE_TYPE, VEHICLE_BOTH);
        Log.d("SharedPreferences", "KEY_VEHICLE_TYPE value is set to: " + savedValue);
    }

    public int getThemeMode() {
        int themeMode = sharedPreferences.getInt(KEY_THEME_MODE, THEME_SYSTEM);
        Log.d("SharedPreferences", "KEY_THEME_MODE value is : " + themeMode);
        return themeMode;

        // Once proven to work, delete the above and uncomment below
        //return sharedPreferences.getBoolean(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public void setThemeMode(int themeMode) {
        sharedPreferences.edit().putInt(KEY_THEME_MODE, themeMode).apply();

        // Check the value to confirm, to delete later
        int savedValue = sharedPreferences.getInt(KEY_THEME_MODE, THEME_SYSTEM);
        Log.d("SharedPreferences", "KEY_THEME_MODE value is set to: " + savedValue);
    }
}
