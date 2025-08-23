package com.jianan.parkwhere.data.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
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

/**
 * Repository that provides location updates using the Google Fused Location Provider
 *
 * This class is a thread-safe, lazily-initialised singleton that wraps a {@link com.google.android.gms.location.FusedLocationProviderClient} and exposes location updates via {@link androidx.lifecycle.LiveData}{@code <Location>} (`getLocationLiveData()`)
 *
 * Important behavior notes:
 * - Call {@link #startLocationUpdates(long, float)} to begin receiving updates, the method will return {@code false} if the application does not have the required location permission
 * - Location updates are delivered to observers of the returned LiveData. Calling @link #getLocationLiveData()} does not start location updates by itself
 * - The repository uses the application context internally to avoid leaking activity contexts
 *
 * This class depends on:
 * - {@link PermissionUtils} for permission checks
 * - {@link com.google.android.gms.location.LocationServices} for the fused provider
 * - {@link androidx.lifecycle.LiveData} for observable location state
 */
public class LocationRepository {
    // private static final String TAG = "LocationRepository";
    private final Context appContext;
    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final MutableLiveData<Location> liveLocation = new MutableLiveData<>();
    private LocationCallback callback;
    private boolean isLocationServiceActive = false;
    private static volatile LocationRepository instance;

    private LocationRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Returns the singleton {@code LocationRepository} instance
     *
     * Uses a thread-safe lazy initialisation (double-checked locking). The provided {@code context}
     * will be converted to the application context internally
     *
     * @param context any valid {@link android.content.Context}; application context will be used
     * @return the singleton {@link LocationRepository} instance
     */
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

    /**
     * Starts periodic location updates using the fused location provider
     *
     * This method:
     *   - Verifies that the application has the required location permission via {@link PermissionUtils} and creates a {@link com.google.android.gms.location.LocationRequest} with the provided {@code intervalMs} and {@code minDistanceM}
     *   - Registers a {@link com.google.android.gms.location.LocationCallback} that posts updates into the repository's LiveData
     *
     * Notes:
     *   - If the application lacks permission, the method posts {@code null} to the {@code liveLocation} LiveData and returns {@code false}
     *   - If updates are already active, this method does nothing and returns {@code true}
     *   - Location callbacks are requested on the main looper and significant changes are filtered by {@link #shouldUpdateLocation(Location, Location)} before posting to LiveData
     *
     * @param intervalMs   desired update interval in milliseconds
     * @param minDistanceM minimum distance in metres before an update is considered significant
     * @return {@code true} if updates were started (or already active); {@code false} if permission is missing
     */
    @SuppressLint("MissingPermission")
    public boolean startLocationUpdates(long intervalMs, float minDistanceM) {
        if (!PermissionUtils.hasLocationPermission(appContext)) {
            // Log.d(TAG, "No location permission - cannot start updates");
            liveLocation.postValue(null);
            return false;
        }

        if (isLocationServiceActive) {
            // Log.d(TAG, "Location service already active");
            return true;
        }

        // Log.d(TAG, "Permission granted - starting location updates");
        stopUpdates();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateDistanceMeters(minDistanceM)
                .build();

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // Log.d(TAG, "onLocationResult callback triggered");
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Location newLocation = locationResult.getLastLocation();
                    Location currentLocation = liveLocation.getValue();

                    if (shouldUpdateLocation(currentLocation, newLocation)) {
                        liveLocation.setValue(newLocation);
                    }

                } else {
                    liveLocation.setValue(null);
                }
            }
        };

        // Log.d(TAG, "Requesting location updates from fusedLocationProviderClient");
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper());
        isLocationServiceActive = true;
        return true;
    }

    /**
     * Stops location updates if they are active and clears the internal callback.
     *
     * After calling this method, {@link #isLocationServiceActive()} will return {@code false}.
     */
    public void stopUpdates() {
        if (callback != null) {
            fusedLocationProviderClient.removeLocationUpdates(callback);
            callback = null;
        }
        isLocationServiceActive = false;
    }

    /**
     * Returns a {@link androidx.lifecycle.LiveData} that observers can use to receive the latest {@link android.location.Location} values posted by the repository
     *
     * Calling this method does not start the location service, use {@link #startLocationUpdates(long, float) instead to begin updates
     *
     * @return LiveData stream of {@link android.location.Location}, may contain {@code null} when location is unavailable
     */
    public LiveData<Location> getLocationLiveData() {
        return liveLocation;
    }

    /**
     * Returns information on whether location service is currently active
     *
     * * @return {@code true} if location updates are active, {@code false} otherwise
     */
    public boolean isLocationServiceActive() {
        return isLocationServiceActive;
    }


    /**
     * Determines whether the new location is sufficiently different from the current one to warrant updating
     *
     * Returns {@code true} if there is no current location (first update) or if the straight-line distance between {@code current} and {@code newLocation} exceeds 10 metres
     *
     * @param current     the previously recorded {@link android.location.Location} or {@code null} if none
     * @param newLocation the newly received {@link android.location.Location} to evaluate
     * @return {@code true} if the repository should replace {@code current} with {@code newLocation}, {@code false} otherwise
     */
    private boolean shouldUpdateLocation(Location current, Location newLocation){
        if (current == null) {
            return true;
        }

        float distance = current.distanceTo(newLocation);
        return distance > 10f;
    }

    /**
     * Checks whether device location providers (GPS or network) are enabled on the device
     *
     * This is a quick check of the system providers, it does not verify application permissions
     *
     * @return {@code true} if either GPS or Network location provider is enabled, {@code false} otherwise
     */
    public boolean isLocationServicesEnabled() {
        LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }
}
