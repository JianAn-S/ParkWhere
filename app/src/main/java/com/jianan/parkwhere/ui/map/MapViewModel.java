package com.jianan.parkwhere.ui.map;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.data.preferences.SettingsManager;
import com.jianan.parkwhere.data.repository.CarParkRepository;
import com.jianan.parkwhere.data.repository.LocationRepository;
import com.jianan.parkwhere.ui.list.ListViewModel;
import com.jianan.parkwhere.util.ApiScheduler;
import com.jianan.parkwhere.util.GeoUtils;
import com.jianan.parkwhere.util.NearbySearchParams;
import com.jianan.parkwhere.util.PermissionUtils;
import com.jianan.parkwhere.util.SingleLiveEvent;

import org.apache.commons.lang3.mutable.Mutable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ViewModel for the map fragment that handles map state distances and UI events
 *
 * Coordinates repositories and exposes LiveData for the UI
 */
public class MapViewModel extends AndroidViewModel {
    // private static final String TAG = "MapViewModel";

    // Repositories
    private final CarParkRepository carParkRepo;
    private final LocationRepository locationRepo;
    private final SettingsManager settingsManager;

    // API data
    private final LiveData<Map<String, CarParkApiData>> carParkApiLookupLive;

    // Location permission (data binding)
    private final MutableLiveData<Boolean> hasLocationPermissionLiveData;
    private MediatorLiveData<Boolean> locationButtonLiveData = null;

    // Location Data
    private final LiveData<Location> locationLiveData;
    private final MutableLiveData<Location> searchedLocationLiveData = new MutableLiveData<>();
    private final MediatorLiveData<Location> activeLocationLiveData = new MediatorLiveData<>();

    // Search parameters
    private final MutableLiveData<Float> radiusLiveData = new MutableLiveData<>();
    private final MediatorLiveData<NearbySearchParams> nearbySearchParamsLiveData = new MediatorLiveData<>();
    private final MediatorLiveData<List<CarPark>> nearbyCarParksLiveData;
    private LiveData<List<CarPark>> currentNearbyCarParksSource = null; // Used to pass information to nearbyCarParksLiveData be manually detaching and attaching

    // UI Events
    private final SingleLiveEvent<BookmarkChangeEvent> bookmarkLiveData = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> snackbarMessageLiveData = new SingleLiveEvent<>();

    // Distance Calculation
    private final MediatorLiveData<Map<String, Double>> carParkDistances = new MediatorLiveData<>();

    // Camera Position
    private CameraPosition savedCameraPosition = null;
    private boolean needInitialLocationMove = false;

    // Vehicle Type Constants
    public static final int VEHICLE_BOTH = SettingsManager.VEHICLE_BOTH;
    public static final int VEHICLE_CAR = SettingsManager.VEHICLE_CAR;
    public static final int VEHICLE_MOTORCYCLE = SettingsManager.VEHICLE_MOTORCYCLE;

    public MapViewModel(@NonNull Application application) {
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

        // Setup mediator LiveData
        // Initialise MediatorLiveData to combine location and radius sources for nearby car park search
        setupActiveLocationMediatorLiveData();

        setupNearbyCarParksWithManualSwitching();

        setupCarParkDistances();

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
     * Setup mediators to compute distances between active location and nearby car parks
     */
    private void setupCarParkDistances() {
        carParkDistances.addSource(activeLocationLiveData, location -> updateDistances());
        carParkDistances.addSource(nearbyCarParksLiveData, carParks -> updateDistances());
    }


    /**
     * Updates the distances of the car parks when active location or nearby car parks change
     */
    private void updateDistances() {
        Location location = activeLocationLiveData.getValue();
        List<CarPark> carParks = nearbyCarParksLiveData.getValue();

        if (location == null || carParks == null) {
            carParkDistances.setValue(new HashMap<>());
            return;
        }

        Map<String, Double> distances = new HashMap<>();
        for (CarPark carPark : carParks) {
            double distance = GeoUtils.calculateHaversineDistance(
                    location.getLatitude(), location.getLongitude(),
                    carPark.getLatitude(), carPark.getLongitude()
            );
            distances.put(carPark.getCarParkNumber(), distance);
        }
        carParkDistances.setValue(distances);
    }

    /**
     * Switch the LiveData source for nearby car parks based on parameters
     *
     * Detaches previous source and attaches new LiveData from repository
     */
    private void switchToNewNearbyCarParksSource(NearbySearchParams params) {

        // Remove the previous source if it exists
        if (currentNearbyCarParksSource != null) {
            nearbyCarParksLiveData.removeSource(currentNearbyCarParksSource);
        }

        if (params == null || params.getLocation() == null) {
            nearbyCarParksLiveData.setValue(new ArrayList<>());
            currentNearbyCarParksSource = null;
            return;
        }
        // Obtain new LiveData source from repository
        currentNearbyCarParksSource = carParkRepo.getNearbyCarParks(params.getLocation(), params.getRadiusMeters());

        // Add the new source
        nearbyCarParksLiveData.addSource(currentNearbyCarParksSource, carParks -> {
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
     * LiveData to indicate whether the location action button should be enabled on the UI
     *
     * @return LiveData of boolean representing button availability
     */
    public LiveData<Boolean> locationButtonLiveData() {
        if (locationButtonLiveData == null) {
            locationButtonLiveData = new MediatorLiveData<>();

            // React to permission changes
            locationButtonLiveData.addSource(hasLocationPermissionLiveData, hasPermission -> {
                boolean hasLocationServices = locationRepo.isLocationServicesEnabled();
                boolean isAvailable = (hasPermission != null && hasPermission) && hasLocationServices;

                locationButtonLiveData.setValue(isAvailable);
            });
        }
        return locationButtonLiveData;
    }

    /**
     * Expose radius LiveData used by the UI
     *
     * @return LiveData of Float radius in metres
     */
    public LiveData<Float> getRadiusLiveData() {
        return radiusLiveData;
    }

    // -------------------------
    // UI Event
    // -------------------------
    public LiveData<String> getSnackbarMessageLiveData() {
        return snackbarMessageLiveData;
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
    public CarParkApiData getCarParkDataForId(String carParkId) {
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
    // Location Settings Methods
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
    // API Scheduler
    // -------------------------

    /**
     * Initialise and start the global ApiScheduler for periodic fetching
     */
    public void initialiseApiScheduler() {
        // Initialise API scheduler with reference to CarParkRepository
        ApiScheduler.getInstance().initialise(carParkRepo);

        // Start API scheduler for the entire application
        ApiScheduler.getInstance().startPeriodicFetch();
    }

    // -------------------------
    // Vehicle Type Filter
    // -------------------------

    /**
     * Return current vehicle type from user preferences
     *
     * @return integer vehicle type constant
     */
    public int getCurrentVehicleType() {
        return settingsManager.getVehicleType();
    }

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

        public String getCarParkNumber() {
            return carParkNumber;
        }

        public boolean isBookmarked() {
            return isBookmarked;
        }
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

        BookmarkChangeEvent event = new BookmarkChangeEvent(carParkNumber, isNowBookmarked);
        bookmarkLiveData.setValue(event);

        // Show snackbar message
        String message = isNowBookmarked ? "Added " + carParkNumber + " to Bookmark" : "Removed " + carParkNumber + " from Bookmark";
        snackbarMessageLiveData.setValue(message);
    }

    // -------------------------
    // Camera Position
    // -------------------------

    /**
     * Simple container for saving camera position state for map restoration
     */
    public static class CameraPosition {
        private final double latitude;
        private final double longitude;
        private final float zoom;

        public CameraPosition(double latitude, double longitude, float zoom) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.zoom = zoom;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public float getZoom() {
           return zoom;
        }

        @Override
        public String toString() {
            return "CameraPosition{lat=" + latitude + ", lng=" + longitude + ", zoom=" + zoom + "}";
        }
    }

    /**
     * Save a camera position for later restoration
     *
     * @param latitude latitude of camera
     * @param longitude longitude of camera
     * @param zoom zoom level
     */
    public void saveCameraPosition(double latitude, double longitude, float zoom) {
        savedCameraPosition = new CameraPosition(latitude, longitude, zoom);
        Log.d("CameraTrigger", "Camera position saved: " + savedCameraPosition);
    }

    /**
     * Return saved camera position or null if not set
     *
     * @return saved {@link CameraPosition} or null
     */
    public CameraPosition getSavedCameraPosition() {
        return savedCameraPosition;
    }

    /**
     * Clear any saved camera position so map will not restore
     */
    public void clearSavedCameraPosition() {
        Log.d("CameraTrigger", "Camera position cleared");
        savedCameraPosition = null;
    }

    /**
     * Return whether initial location move is required on the map
     *
     * @return true if initial move is required false otherwise
     */
    public boolean getNeedInitialLocationMove() {
        return needInitialLocationMove;
    }

    /**
     * Set whether an initial camera move should occur when map loads
     *
     * @param needMove true to request initial move false otherwise
     */
    public void setNeedInitialLocationMove(boolean needMove) {
        this.needInitialLocationMove = needMove;
    }

    // -------------------------
    // Car Park
    // -------------------------

    /**
     * Container for combined car park details including distance and API data
     */
    public static class CarParkDetailsData {
        private final CarPark carPark;
        private final Double distance;
        private final CarParkApiData apiData;

        public CarParkDetailsData(CarPark carPark, Double distance, CarParkApiData apiData) {
            this.carPark = carPark;
            this.distance = distance;
            this.apiData = apiData;
        }

        // Getters
        public CarPark getCarPark() {
            return carPark;
        }

        public Double getDistance() {
            return distance;
        }

        public CarParkApiData getApiData() {
            return apiData;
        }

        public String getFormattedDistance() {
            if (distance == null) return null;

            if (distance < 1000) {
                return String.format("%.0f m", distance);
            } else {
                return String.format("%.1f km", distance / 1000);
            }
        }
    }

    /**
     * Find car park by number in the current nearby list and assemble details
     *
     * @param carParkNumber car park id to search for
     * @return {@link CarParkDetailsData} or null if not found
     */
    public CarParkDetailsData getCarParkDetails(String carParkNumber) {
        // Find the car park
        CarPark carPark = null;
        List<CarPark> nearbyCarParks = nearbyCarParksLiveData.getValue();
        if (nearbyCarParks != null) {
            for (CarPark cp : nearbyCarParks) {
                if (cp.getCarParkNumber().equals(carParkNumber)) {
                    carPark = cp;
                    break;
                }
            }
        }

        if (carPark == null) return null;

        // Get distance
        Map<String, Double> distances = carParkDistances.getValue();
        Double distance = distances != null ? distances.get(carParkNumber) : null;

        // Get API data
        CarParkApiData apiData = getCarParkDataForId(carParkNumber);

        return new CarParkDetailsData(carPark, distance, apiData);
    }

    // -------------------------
    // Time Formatting
    // -------------------------

    /**
     * Format API timestamp string into a short human friendly relative time string
     *
     * @param timestamp ISO style timestamp in pattern yyyy-MM-dd'T'HH:mm:ss in Asia/Singapore timezone
     * @return human readable relative update string
     */
    public String formatUpdateTime(String timestamp) {
        try {
            // Parse the timestamp as local Singapore time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime localUpdateTime = LocalDateTime.parse(timestamp, formatter);

            // Attach GMT+8 zone (Asia/Singapore)
            ZonedDateTime updateTime = localUpdateTime.atZone(ZoneId.of("Asia/Singapore"));

            // Get current time in GMT+8
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Singapore"));

            Duration duration = Duration.between(updateTime, now);

            long minutes = duration.toMinutes();
            long hours = duration.toHours();
            long days = duration.toDays();

            // Return formatted "Updated x ago"
            if (minutes < 1) {
                return "Updated just now";
            } else if (minutes < 60) {
                return String.format("Updated %d minute%s ago", minutes, minutes > 1 ? "s" : "");
            } else if (hours < 24) {
                return String.format("Updated %d hour%s ago", hours, hours > 1 ? "s" : "");
            } else {
                return String.format("Updated %d day%s ago", days, days > 1 ? "s" : "");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Updated recently";
        }
    }
}