package com.jianan.parkwhere.ui.map;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.data.preferences.SettingsManager;
import com.jianan.parkwhere.data.repository.CarParkRepository;
import com.jianan.parkwhere.data.repository.LocationRepository;
import com.jianan.parkwhere.util.PermissionUtils;

import java.util.Map;
import java.util.function.Consumer;

public class MapViewModel extends AndroidViewModel {
    private static final String TAG = "MapViewModel"; // For logging
    private final CarParkRepository carParkRepo;
    private final LocationRepository locationRepo;
    private final SettingsManager settingsManager;
    private final LiveData<Map<String, CarParkApiData>> carParkApiLookupLive;

    // Track location updates in ViewModel
    private boolean isLocationUpdateActive = false;
    private LiveData<Location> locationUpdateLiveData;

    // LiveData for location permission (data binding)
    private MutableLiveData<Boolean> hasLocationPermissionLiveData;

    public MapViewModel(@NonNull Application application) {
        super(application);

        carParkRepo = CarParkRepository.getCarParkRepo(application);
        locationRepo = LocationRepository.getLocationRepo(application);
        settingsManager = SettingsManager.getSettingsManager(application);

        carParkApiLookupLive = carParkRepo.getCarParkApiLookupLive();

        // Initialise permission LiveData
        hasLocationPermissionLiveData = new MutableLiveData<>();
        hasLocationPermissionLiveData.setValue(PermissionUtils.hasLocationPermission(application));

        carParkRepo.fetchApi(); // This is suppose to be called every x minute but for now, I will just manually call it for this quick check
    }

    // -------------------------
    // API Methods
    // -------------------------
    public LiveData<Map<String, CarParkApiData>> getCarParkApiLookupLive() {
        return carParkApiLookupLive;
    }

    public CarParkApiData getCarParkDataForId (String carParkId) {
        return carParkRepo.getCarParkDataForId(carParkId);
    }

    // -------------------------
    // DB Methods
    // -------------------------
    public void getCarParkByNumber(String carParkNumber, Consumer<CarPark> callback) {
        carParkRepo.getCarParkByNumber(carParkNumber, callback);
    }

    // -------------------------
    // Location Methods
    // -------------------------
    public boolean getFirstLaunch() {
        return settingsManager.isFirstLaunch();
    }

    public void setFirstLaunch() {
        settingsManager.setFirstLaunch(false);
    }

    public LiveData<Location> getLocationUpdates() {
        Log.d(TAG, "ViewModel getLocationUpdates() called");

        // Create LiveData once and reuse it
        if (locationUpdateLiveData == null) {
            try {
                locationUpdateLiveData = locationRepo.getLocationUpdates(5000L, 10.0f);

                if (locationUpdateLiveData == null) {
                    Log.e(TAG, "LocationRepository returned null LiveData - check to see if location is set on emulator");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while getting location updates: " + e.getMessage());
                locationUpdateLiveData = null;
            }
        }
        return locationUpdateLiveData;
    }

    public boolean startLocationUpdates() {
        if (isLocationUpdateActive) {
            Log.d(TAG, "Location update is already active");
            return false;
        }

        // Check if location is null before starting (for Emulator)
        LiveData<Location> locationLiveData = getLocationUpdates();
        if (locationLiveData == null) {
            Log.e(TAG, "Cannot start location updates - check to see if location is set on emulator");
            return false;
        }

        Log.d(TAG, "Starting location updates in ViewModel");
        isLocationUpdateActive = true;
        return true;
    }

    public void stopLocationUpdates() {
        if (!isLocationUpdateActive) {
            Log.d(TAG, "Location update already stopped");
            return;
        }

        Log.d(TAG, "Stopping location updates in ViewModel");
        locationRepo.stopUpdates();
        isLocationUpdateActive = false;
    }

    public boolean isLocationUpdateActive() {
        return isLocationUpdateActive;
    }

    public boolean hasLocationPermission() {
        return PermissionUtils.hasLocationPermission(getApplication());
    }

    // Location methods for data binding UI
    public LiveData<Boolean> hasLocationPermissionLiveData() {
        return hasLocationPermissionLiveData;
    }

    public void setLocationPermissionLiveDataStatus(boolean hasLocationPermission) {
        hasLocationPermissionLiveData.setValue(hasLocationPermission);
    }
}