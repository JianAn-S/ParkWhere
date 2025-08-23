package com.jianan.parkwhere.data.repository;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.local.CarParkDao;
import com.jianan.parkwhere.data.local.CarParkDatabase;
import com.jianan.parkwhere.data.preferences.SettingsManager;
import com.jianan.parkwhere.data.remote.CarParkApiClient;
import com.jianan.parkwhere.data.remote.CarParkApiService;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.data.model.CarParkApiItem;
import com.jianan.parkwhere.data.model.CarParkApiResponse;
import com.jianan.parkwhere.util.CarParkDistance;
import com.jianan.parkwhere.util.GeoUtils;

/**
 * Repository responsible for coordinating data operations related to {@link CarPark} entities
 *
 * This class mediates between the remote car park availability API (via {@link CarParkApiService})
 * and the local persistence layer (via {@link CarParkDao}). It provides:
 * - A lazily-initialised, thread-safe singleton instance.
 * - An in-memory LiveData lookup map of the latest API availability data keyed by car park number
 * - Asynchronous database access utilities executed on a single background thread
 * - Helper methods to query nearby car parks using a bounding-box + Haversine filtering strategy
 *
 * All long-running operations (database queries, heavy calculations, API fetch callbacks)
 * are executed off the main thread. The repository posts results to {@link androidx.lifecycle.LiveData}
 * so that UI components can observe updates safely
 *
 * @see CarParkApiService
 * @see CarParkDao
 */
public class CarParkRepository {
    // private static final String TAG = "CarParkRepository";
    private final CarParkApiService apiService;
    private final CarParkDao carParkDao;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Map<String, CarParkApiData>> carParkApiLookupLive = new MutableLiveData<>(new HashMap<>()); // In-memory lookup table where the key is the car park number (String) and the value is CarParkApiData
    private final SettingsManager settingsManager;
    private static volatile CarParkRepository instance;

    private CarParkRepository(Context context) {
        // Obtain the Retrofit service instance
        this.apiService = CarParkApiClient.getService();
        this.settingsManager = SettingsManager.getSettingsManager(context);

        // If this is the first application launch, create the database from asset
        boolean shouldCreateFromAsset = !settingsManager.isDatabaseInitialised();

        // Obtain database instance
        // Typically ApplicationContext (which ties to the app lifecycle) will be used instead of Context to avoid leaking references tied to short-lived components like activities
        // getDatabase will obtain the application context, hence it is fine to provide context here
        CarParkDatabase database = CarParkDatabase.getDatabase(context, shouldCreateFromAsset);
        this.carParkDao = database.carParkDao();

        // Mark database as initialised if it is created from asset
        if (shouldCreateFromAsset) {
            settingsManager.setDatabaseInitialised(true);
        }
    }

    /**
     * Returns the singleton {@code CarParkRepository} instance.
     *
     * Uses a thread-safe lazy initialisation (double-checked locking). The provided {@code context}
     * is converted to application context to avoid leaking activity contexts
     *
     * @param context any valid {@link android.content.Context}, application context will be used internally
     * @return the singleton {@link CarParkRepository} instance
     */
    public static CarParkRepository getCarParkRepo(Context context) {
        if (instance == null) {
            synchronized (CarParkRepository.class) {
                if (instance == null) {
                    instance = new CarParkRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // -------------------------
    // API Methods
    // -------------------------

    /**
     * Triggers an asynchronous fetch of the latest car park availability from the remote API
     * On a successful HTTP response, the {@link #carParkApiLookupLive} LiveData map will be
     * updated so that all observers receive the new data
     * If there is a failure or an empty result, an error will be logged
     */
    public void fetchApi() {
        apiService.fetchCarParkAvailability().enqueue(new Callback<CarParkApiResponse>() {
            @Override
            public void onResponse(Call<CarParkApiResponse> call, Response<CarParkApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CarParkApiResponse apiResponse = response.body();
                    List<CarParkApiItem> apiItems = apiResponse.getCarParkApiItem();

                    if (apiItems != null && !apiItems.isEmpty()) {
                        List<CarParkApiData> apiData = apiItems.get(0).getCarParkApiData();

                        // Updates the in-memory lookup map with the latest car park data from the API
                        Map<String, CarParkApiData> newLookup = new HashMap<>();
                        for (CarParkApiData data : apiData) {
                            newLookup.put(data.getCarParkNumber(), data);
                        }

                        // Inform observers that the value of carParkApiLookup has been updated
                        carParkApiLookupLive.postValue(newLookup);
                    }
                }
                // else {
                //   Log.d(TAG, "No response from API: " + response.code());
                // }
            }

            @Override
            public void onFailure(Call<CarParkApiResponse> call, Throwable t) {
                // Log.e(TAG, "API call failed", t);
            }
        });
    }

    /**
     * Returns a LiveData wrapping the current in‑memory map of car park availability with car park number being the unique key
     *
     * @return LiveData whose value is a Map<String, CarParkApiData> of the latest API data
     */
    public LiveData<Map<String, CarParkApiData>> getCarParkApiLookupLive() {
        return carParkApiLookupLive;
    }

    /**
     * Retrieves the latest API data for the specified car park ID from the LiveData map
     *
     * Uses {@code getValue()}, a method provided by LiveData, to access the current data map
     * Example:
     * CarParkApiData data = getCarParkDataForId("BE28");
     * String lots = data.getCarParkInfo().get(0).getLotsAvailable();
     *
     * @param   carParkId the car park number (e.g. "BE28")
     * @return  CarParkApiData for the specified ID, or null if not found
     */
    public CarParkApiData getCarParkDataForId (String carParkId) {
        Map<String, CarParkApiData> currentMap = carParkApiLookupLive.getValue(); // getValue() is a method generated by LiveData
        if (currentMap != null) {
            return currentMap.get(carParkId);
        }
        return null;
    }

    // -------------------------
    // DB Methods
    // -------------------------
    /**
     * Asynchronously fetches a {@link CarPark} entity by its car park number using a background thread
     *
     * Since this method does not run on the main thread, it does not return the result directly
     * Instead, it accepts a {@link Consumer} callback that will be invoked exactly once with the query result when available
     *
     * @param number    the car park number to query (e.g., "BE28")
     * @param callback  a {@link Consumer} that will be called with the resulting {@link CarPark}, or null if not found
     */
    public void getCarParkByNumber(String number, Consumer<CarPark> callback) {
        executor.execute(() -> {
            CarPark cp = carParkDao.getCarParkByNumber(number);
            callback.accept(cp);
        });
    }

    /**
     * Returns a {@link androidx.lifecycle.LiveData} containing a list of nearby {@link CarPark}
     * entities within the specified circular radius (in metres) from the provided {@link android.location.Location}
     *
     * Implementation notes:
     *   - A square bounding box is computed first using {@link com.google.maps.android.SphericalUtil} to perform a fast DB query
     *   - Results from the bounding box are filtered using the Haversine distance (via {@link GeoUtils}) to ensure the returned set lies within the exact circular radius
     *   - Returned list is sorted by ascending distance (closest first) and posted to the LiveData
     *
     * @param location     the central location to search from
     * @param radiusMeters the radius in metres to search within
     * @return LiveData whose value will be the sorted {@link java.util.List} of {@link CarPark} within the specified radius
     */
    public LiveData<List<CarPark>> getNearbyCarParks(Location location, float radiusMeters) {
        MutableLiveData<List<CarPark>> nearbyCarParkLiveData = new MutableLiveData<>();

        executor.execute(() -> {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Calculate bounding box using SphericalUtil
            LatLng north = SphericalUtil.computeOffset(latLng, radiusMeters, 0);
            LatLng south = SphericalUtil.computeOffset(latLng, radiusMeters, 180);
            LatLng east = SphericalUtil.computeOffset(latLng, radiusMeters, 90);
            LatLng west = SphericalUtil.computeOffset(latLng, radiusMeters, 270);

            // Latitude is Y-Axis, Longitude is X-Axis
            double maxLat = north.latitude;
            double minLat = south.latitude;
            double maxLon = east.longitude;
            double minLon = west.longitude;

            // Query car parks within square bounding box
            List<CarPark> boundingBoxResults = carParkDao.getCarParksInBoundingBox(minLat, maxLat, minLon, maxLon);

            // Apply Haversine formula and ensure it is within the circular radius
            List<CarParkDistance> carParkDistance = new ArrayList<>();

            for (CarPark carPark : boundingBoxResults) {
                double distance = GeoUtils.calculateHaversineDistance(
                        location.getLatitude(), location.getLongitude(),
                        carPark.getLatitude(), carPark.getLongitude()
                );

                // Only include car parks within the exact radius
                if (distance <= radiusMeters) {
                    carParkDistance.add(new CarParkDistance(carPark, distance));
                }
            }

            // Sort by distance (closest to furthest)
            carParkDistance.sort((a, b) -> Double.compare(a.getDistanceMeters(), b.getDistanceMeters()));

            // Extract CarPark entities from CarParkDistance wrapper
            List<CarPark> sortedResults = new ArrayList<>();
            for (CarParkDistance carPark : carParkDistance) {
                sortedResults.add(carPark.getCarPark());
            }

            nearbyCarParkLiveData.postValue(sortedResults);
        });
        return nearbyCarParkLiveData;
    }
}
