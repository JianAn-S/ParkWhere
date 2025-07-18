package com.jianan.parkwhere.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;

import androidx.core.content.ContextCompat;

public class PermissionUtils {
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
