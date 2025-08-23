package com.jianan.parkwhere.ui.list;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.data.preferences.SettingsManager;
import com.jianan.parkwhere.data.repository.CarParkRepository;
import com.jianan.parkwhere.data.repository.LocationRepository;
import com.jianan.parkwhere.util.ApiScheduler;
import com.jianan.parkwhere.util.NearbySearchParams;
import com.jianan.parkwhere.util.PermissionUtils;
import com.jianan.parkwhere.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ViewModel for the list fragment that provides nearby car parks and bookmark interactions
 *
 * Coordinates repositories (car park data, location and settings) and exposes LiveData for the UI
 */
public class ListViewModel extends AndroidViewModel {
    // private static final String TAG = "ListViewModel";
    private final CarParkRepository carParkRepo;
    private final LocationRepository locationRepo;
    private final SettingsManager settingsManager;
    private final LiveData<Map<String, CarParkApiData>> carParkApiLookupLive;

    // MutableLiveData for location permission (data binding)
    private final MutableLiveData<Boolean> hasLocationPermissionLiveData;

    // Track location updates in ViewModel
    private final LiveData<Location> locationLiveData;

    // MutableLiveData for searched location
    private final MutableLiveData<Location> searchedLocationLiveData = new MutableLiveData<>();

    // MediatorLiveData determines the location to query for nearby car parks
    private final MediatorLiveData<Location> activeLocationLiveData = new MediatorLiveData<>();

    // Track radius changes done by user
    private final MutableLiveData<Float> radiusLiveData = new MutableLiveData<>();

    private final MediatorLiveData<NearbySearchParams> nearbySearchParamsLiveData = new MediatorLiveData<>();

    private final MediatorLiveData<List<CarPark>> nearbyCarParksLiveData;

    // Used to pass information to nearbyCarParksLiveData be manually detaching and attaching
    private LiveData<List<CarPark>> currentNearbyCarParksSource = null;

    // Track bookmark changes
    private final SingleLiveEvent<BookmarkChangeEvent> bookmarkLiveData = new SingleLiveEvent<>();

    public ListViewModel(@NonNull Application application) {
        super(application);

        carParkRepo = CarParkRepository.getCarParkRepo(application);
        locationRepo = LocationRepository.getLocationRepo(application);
        settingsManager = SettingsManager.getSettingsManager(application);

        carParkApiLookupLive = carParkRepo.getCarParkApiLookupLive();

        // Initialise permission LiveData
        hasLocationPermissionLiveData = new MutableLiveData<>();
        hasLocationPermissionLiveData.setValue(PermissionUtils.hasLocationPermission(application));

        // Obtain the location LiveData once
        locationLiveData = locationRepo.getLocationLiveData();

        // Initialise radius from settings (default of 1000m)
        float savedRadius = settingsManager.getRadiusValue();
        radiusLiveData.setValue(savedRadius);

        // Initialise nearbyCarParks MediatorLiveData
        nearbyCarParksLiveData = new MediatorLiveData<>();

        // Initialise MediatorLiveData to combine location and radius sources for nearby car park search
        setupActiveLocationMediatorLiveData();
        setupNearbyCarParksWithManualSwitching();

        // Initial API data fetch
        carParkRepo.fetchApi();
    }

    // -------------------------
    // LiveData & Mediator Setup
    // -------------------------

    /**
     * Combine user location and searched location into a single active location source
     *
     * Search location takes precedence over user location
     */
    private void setupActiveLocationMediatorLiveData() {

        // Add the user's location as a source
        activeLocationLiveData.addSource(locationLiveData, userLocation -> {
            // Use user's current location if there is no search location
            Location searchedLocation = searchedLocationLiveData.getValue();

            if (searchedLocation == null) {
                activeLocationLiveData.setValue(userLocation);
            }
        });

        // Add the search location as a source
        activeLocationLiveData.addSource(searchedLocationLiveData, searchedLocation -> {
            if (searchedLocation != null) {
                activeLocationLiveData.setValue(searchedLocation);
            }
            // When search bar is cleared, use the user's location (only if location permission is granted)
            else {
                Location userLocation = locationLiveData.getValue();
                if (userLocation != null) {
                    activeLocationLiveData.setValue(userLocation);
                }
            }
        });
    }

    /**
     * Combine active location and radius and switch LiveData sources manually
     */
    private void setupNearbyCarParksWithManualSwitching() {

        nearbySearchParamsLiveData.addSource(activeLocationLiveData, location -> {
            Float radius = radiusLiveData.getValue();

            if (location != null && radius != null) {
                NearbySearchParams params = new NearbySearchParams(location, radius);
                nearbySearchParamsLiveData.setValue(params);
            } else {
                nearbySearchParamsLiveData.setValue(null);
            }
        });

        nearbySearchParamsLiveData.addSource(radiusLiveData, radius -> {
            Location location = activeLocationLiveData.getValue();

            if (location != null && radius != null) {
                NearbySearchParams params = new NearbySearchParams(location, radius);
                nearbySearchParamsLiveData.setValue(params);
            } else {
                nearbySearchParamsLiveData.setValue(null);
            }
        });

        // Set up nearbyCarParksLiveData to observe nearbySearchParamsLiveData
        nearbyCarParksLiveData.addSource(nearbySearchParamsLiveData, params -> {
            switchToNewNearbyCarParksSource(params);
        });
    }

    /**
     * Switch the LiveData source for nearby car parks based on parameters
     *
     * Detaches previous source and attaches new LiveData from repository
     */
    private void switchToNewNearbyCarParksSource(NearbySearchParams params) {

        // Remove the previous source if it exists
        if(currentNearbyCarParksSource != null) {
            nearbyCarParksLiveData.removeSource(currentNearbyCarParksSource);
        }

        if(params == null || params.getLocation() == null) {
            nearbyCarParksLiveData.setValue(new ArrayList<>());
            currentNearbyCarParksSource = null;
            return;
        }

        // Obtain new LiveData source from repository
        currentNearbyCarParksSource = carParkRepo.getNearbyCarParks(params.getLocation(), params.getRadiusMeters());

        // Add the new source
        nearbyCarParksLiveData.addSource(currentNearbyCarParksSource, carParks-> {
            nearbyCarParksLiveData.setValue(carParks);
        });
    }

    // -------------------------
    // LiveData Accessors
    // -------------------------

    /**
     * Expose the currently active location used
     *
     * @return LiveData of active {@link Location}
     */
    public LiveData<Location> getActiveLocationLiveData() {
        return activeLocationLiveData;
    }

    /**
     * Expose the list of nearby car parks determined by mediators
     *
     * @return LiveData list of {@link CarPark}
     */
    public LiveData<List<CarPark>> getNearbyCarParksLiveData() {
        return nearbyCarParksLiveData;
    }

    /**
     * Expose the latest API lookup map keyed by car park number
     *
     * @return LiveData map of car park id to {@link CarParkApiData}
     */
    public LiveData<Map<String, CarParkApiData>> getCarParkApiLookupLive() {
        return carParkApiLookupLive;
    }

    /**
     * LiveData for data binding to indicate whether the application has location permission
     *
     * @return LiveData of boolean permission state
     */
    public LiveData<Boolean> hasLocationPermissionLiveData() {
        return hasLocationPermissionLiveData;
    }

    /**
     * Expose bookmark change event LiveData for one time events
     *
     * @return LiveData of {@link BookmarkChangeEvent}
     */
    public LiveData<BookmarkChangeEvent> getBookmarkLiveData() {
        return bookmarkLiveData;
    }

    // -------------------------
    // API
    // -------------------------

    /**
     * Return API data for a car park id from the repository lookup map
     *
     * @param carParkId car park identifier
     * @return {@link CarParkApiData} or null if not available
     */
    public CarParkApiData getCarParkDataForId (String carParkId) {
        return carParkRepo.getCarParkDataForId(carParkId);
    }

    // -------------------------
    // DB
    // -------------------------
    public void getCarParkByNumber(String carParkNumber, Consumer<CarPark> callback) {
        carParkRepo.getCarParkByNumber(carParkNumber, callback);
    }

    // -------------------------
    // Location Permission
    // -------------------------

    /**
     * Check if the application currently has location permission according to PermissionUtils
     *
     * @return true if permission is granted false otherwise
     */
    public boolean hasLocationPermission() {
        return PermissionUtils.hasLocationPermission(getApplication());
    }

    /**
     * Update the LiveData used by data binding when the permission status changes
     *
     * @param hasLocationPermission the current permission state
     */
    public void setLocationPermissionLiveDataStatus(boolean hasLocationPermission) {
        hasLocationPermissionLiveData.setValue(hasLocationPermission);
    }

    /**
     * Check if device location providers are enabled
     *
     * @return true if GPS or network provider is enabled false otherwise
     */
    public boolean isLocationServicesEnabled() {
        return locationRepo.isLocationServicesEnabled();
    }

    // -------------------------
    // Location Settings
    // -------------------------

    /**
     * Return whether this is the first launch obtained from user preferences
     *
     * @return true if first launch, otherwise false
     */
    public boolean getFirstLaunch() {
        return settingsManager.isFirstLaunch();
    }

    /**
     * Mark first launch as completed
     */
    public void setFirstLaunch() {
        settingsManager.setFirstLaunch(false);
    }

    // -------------------------
    // Location Update
    // -------------------------

    /**
     * Expose raw location LiveData from the location repository
     *
     * @return LiveData of {@link Location}
     */
    public LiveData<Location> getLocationLiveData() {
        return locationLiveData;
    }

    /**
     * Request the location repository to start updates with default interval and distance
     *
     * @return true if updates started or already active, false if permission not given
     */
    public boolean startLocationService() {
        return locationRepo.startLocationUpdates(5000L, 10.0f);
    }

    /**
     * Stop location updates via location repository
     */
    public void stopLocationService() {
        locationRepo.stopUpdates();
    }

    /**
     * Query whether the location service is currently active
     *
     * @return true if active false otherwise
     */
    public boolean isLocationServiceActive() {
        return locationRepo.isLocationServiceActive();
    }

    // -------------------------
    // Vehicle Type Filter
    // -------------------------

    /**
     * LiveData stream of the currently selected vehicle type from settings manager
     *
     * @return LiveData of integer vehicle type
     */
    public LiveData<Integer> getVehicleTypeLiveData() {
        return settingsManager.getVehicleTypeLiveData();
    }

    // -------------------------
    // Radius
    // -------------------------

    /**
     * Return the current radius value for search parameter
     *
     * @return radius in metres or null if not set
     */
    public float getRadiusValue() {
        return radiusLiveData.getValue();
    }

    /**
     * Set the radius used for nearby searches and persist to user preferences
     *
     * @param radius radius in metres
     */
    public void setRadiusValue(float radius) {
        radiusLiveData.setValue(radius);
        settingsManager.setRadiusValue(radius);
    }

    // -------------------------
    // Search Location
    // -------------------------

    /**
     * Return the LiveData representing the searched location
     *
     * @return LiveData of searched {@link Location}
     */
    public LiveData<Location> getSearchedLocationLiveData() {
        return searchedLocationLiveData;
    }

    /**
     * Return whether there is an active search location
     *
     * @return true if searching, otherwise false
     */
    public boolean isSearching() {
        Location searchedLocation = searchedLocationLiveData.getValue();
        return searchedLocation != null;
    }

    /**
     * Set the search location used to override user location
     *
     * @param location the searched {@link Location}
     */
    public void setSearchedLocation(Location location) {
        searchedLocationLiveData.setValue(location);
    }

    /**
     * Clear the active searched location so user location becomes active
     */
    public void clearSearchedLocation() {
        searchedLocationLiveData.setValue(null);
    }

    // -------------------------
    // Bookmark
    // -------------------------

    /**
     * Event payload for bookmark changes
     */
    public static class BookmarkChangeEvent {
        private final String carParkNumber;
        private final boolean isBookmarked;

        public BookmarkChangeEvent(String carParkNumber, boolean isBookmarked) {
            this.carParkNumber = carParkNumber;
            this.isBookmarked = isBookmarked;
        }

        public String getCarParkNumber() { return carParkNumber; }
        public boolean isBookmarked() { return isBookmarked; }
    }

    /**
     * Check whether a car park is bookmarked according to settings
     *
     * @param carParkNumber car park id
     * @return true if bookmarked false otherwise
     */
    public boolean isBookmarked(String carParkNumber) {
        return settingsManager.isBookmarked(carParkNumber);
    }

    /**
     * Toggle bookmark state for a car park and emit a bookmark event
     *
     * @param carParkNumber car park id to toggle
     */
    public void toggleBookmark(String carParkNumber) {
        settingsManager.toggleBookmark(carParkNumber);
        boolean isNowBookmarked = settingsManager.isBookmarked(carParkNumber);

        // Create event with both carParkNumber and bookmark status
        BookmarkChangeEvent event = new BookmarkChangeEvent(carParkNumber, isNowBookmarked);
        bookmarkLiveData.setValue(event);
    }
}