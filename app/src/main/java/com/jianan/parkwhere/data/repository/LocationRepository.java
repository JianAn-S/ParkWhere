package com.jianan.parkwhere.data.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.jianan.parkwhere.util.PermissionUtils;

public class LocationRepository {
    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final MutableLiveData<Location> liveLocation = new MutableLiveData<>();
    private LocationCallback callback;
    private final Context appContext;

    public LocationRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public LiveData<Location> getLocationUpdates(long intervalMs, float minDistanceM) {
        if (!PermissionUtils.hasLocationPermission(appContext)) {
            liveLocation.postValue(null);
            return liveLocation;
        }
        stopUpdates();
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
                .setMinUpdateDistanceMeters(minDistanceM)
                .build();

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    liveLocation.postValue(locationResult.getLastLocation());
                }
            }
        };
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper());
        return liveLocation;
    }
    private void stopUpdates() {
        if (callback != null) {
            fusedLocationProviderClient.removeLocationUpdates(callback);
            callback = null;
        }
    }
}
