package com.jianan.parkwhere.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SettingsManager {
    private static final String PREF_NAME = "park_where_preferences";
    private static final String KEY_DATABASE_INITIALISED = "database_initialised";
    private static final String KEY_RADIUS = "radius_value";

    private  final SharedPreferences sharedPreferences;

    public SettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, context.MODE_PRIVATE);
    }

    public boolean isDatabaseInitialised() {
        boolean value = sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
        Log.d("SharedPreferences", "KEY_DATABASE_INITIALISED value is : " +  value);
        return value;

        // Once proven to work, delete the above and uncomment below
        //return sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
    }

    public void markDatabaseInitialised() {
        sharedPreferences.edit().putBoolean(KEY_DATABASE_INITIALISED, true).apply();

        // Check the value to confirm, to delete later
        boolean savedValue = sharedPreferences.getBoolean(KEY_DATABASE_INITIALISED, false);
        Log.d("SharedPreferences", "KEY_DATABASE_INITIALISED is set to: " + savedValue);
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
