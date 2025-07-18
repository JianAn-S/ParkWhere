package com.jianan.parkwhere.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.local.CarParkDao;
import com.jianan.parkwhere.data.local.CarParkDatabase;
import com.jianan.parkwhere.data.remote.CarParkApiClient;
import com.jianan.parkwhere.data.remote.CarParkApiService;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.data.model.CarParkApiItem;
import com.jianan.parkwhere.data.model.CarParkApiResponse;

public class CarParkRepository {
    private final CarParkApiService apiService;
    private final CarParkDao carParkDao;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Map<String, CarParkApiData>> carParkApiLookupLive = new MutableLiveData<>(new HashMap<>()); // In-memory lookup table where the key is the car park number (String) and the value is CarParkApiData
    private static final String TAG = "CarParkRepository";
    private static volatile CarParkRepository instance;

    public CarParkRepository(Context context) {
        // Obtain the Retrofit service instance
        this.apiService = CarParkApiClient.getService();

        // Use ApplicationContext (which ties to the app lifecycle) instead of Context to avoid leaking references tied to short-lived components like activities
        // getDatabase will obtain the application context, hence it is fine to provide context here
        this.carParkDao = CarParkDatabase.getDatabase(context).carParkDao();
    }

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
     * Triggers an asynchronous fetch of the latest car park availability from the remote API.
     * <p> On a successful HTTP response, the {@link #carParkApiLookupLive} LiveData map will be
     * updated so that all observers receive the new data.
     * If there is a failure or an empty result, an error will be logged.
     * </p>
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
                } else {
                    Log.d(TAG, "No response from API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CarParkApiResponse> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    /**
     * Returns a LiveData wrapping the current in‑memory map of car park availability with car park
     * number being the unique key.
     *
     * @return LiveData whose value is a Map<String, CarParkApiData> of the latest API data
     */
    public LiveData<Map<String, CarParkApiData>> getCarParkApiLookupLive() {
        return carParkApiLookupLive;
    }

    /**
     * Retrieves the latest API data for the specified car park ID from the LiveData map.
     *
     * <p> Uses {@code getValue()}, a method provided by LiveData, to access the current data map.
     * Example:
     * CarParkApiData data = getCarParkDataForId("BE28");
     * String lots = data.getCarParkInfo().get(0).getLotsAvailable();
     * </p>
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
     * Asynchronously fetches a {@link CarPark} entity by its car park number using a background thread.
     * <p>
     * Since this method does not run on the main thread, it does not return the result directly.
     * Instead, it accepts a {@link Consumer} callback that will be invoked exactly once
     * with the query result when available.
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
}
