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
        boolean value = sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
        Log.d("SharedPreferences", "KEY_DATABASE_INITIALISED value is : " +  value);
        return value;

        // Once proven to work, delete the above and uncomment below
        //return sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
    }

    public void setDatabaseInitialised(boolean isInitialised) {
        sharedPreferences.edit().putBoolean(KEY_DATABASE_INITIALISED, isInitialised).apply();

        // Check the value to confirm, to delete later
        boolean savedValue = sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
        Log.d("SharedPreferences", "KEY_DATABASE_INITIALISED is set to: " + savedValue);
    }

    public boolean isFirstLaunch() {
        boolean value = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
        Log.d("SharedPreferences", "KEY_FIRST_LAUNCH value is : " + value);
        return value;

        // Once proven to work, delete the above and uncomment below
        //return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean isFirstLaunch) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch).apply();

        // Check the value to confirm, to delete later
        boolean savedValue = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
        Log.d("SharedPreferences", "KEY_FIRST_LAUNCH is set to: " + savedValue);
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
}
