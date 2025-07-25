package com.jianan.parkwhere.data.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

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
    private static final String TAG = "LocationRepository";
    private final Context appContext;
    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final MutableLiveData<Location> liveLocation = new MutableLiveData<>();
    private LocationCallback callback;
    private static volatile LocationRepository instance;

    private LocationRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public static LocationRepository getLocationRepo (Context context) {
        if (instance == null){
            synchronized (LocationRepository.class){
                if (instance == null) {
                    instance = new LocationRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @SuppressLint("MissingPermission")
    public LiveData<Location> getLocationUpdates(long intervalMs, float minDistanceM) {
        if (!PermissionUtils.hasLocationPermission(appContext)) {
            Log.d(TAG, "No location permission - posting null and returning");
            liveLocation.postValue(null);
            return liveLocation;
        }

        Log.d(TAG, "Permission granted - stopping previous update");
        stopUpdates();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateDistanceMeters(minDistanceM)
                .build();

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG, "onLocationResult callback triggered");
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Log.d(TAG, "Got location: " + locationResult.getLastLocation());
                    liveLocation.postValue(locationResult.getLastLocation());
                } else {
                    Log.d(TAG, "LocationResult has no location");
                }
            }
        };

        Log.d(TAG, "Reuqesting location updates from fusedLocationProviderClient");
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper());
        return liveLocation;
    }
    public void stopUpdates() {
        if (callback != null) {
            fusedLocationProviderClient.removeLocationUpdates(callback);
            callback = null;
        }
    }
}
