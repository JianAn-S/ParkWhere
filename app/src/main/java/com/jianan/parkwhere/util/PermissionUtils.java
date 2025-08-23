package com.jianan.parkwhere.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

/**
 * Utility class for handling permission-related operations in the application
 */
public class PermissionUtils {
    /**
     * Check if the application has either fine or coarse location permissions granted
     *
     * @param context the application or activity context
     * @return true if location permission is granted, false otherwise
     */
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Open the application settings screen for this application which allows the user to manually grant permissions
     *
     * @param context the application or activity context
     */
    public static void openParkWhereAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
