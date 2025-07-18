package com.jianan.parkwhere.ui.map;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.data.repository.CarParkRepository;

import java.util.Map;
import java.util.function.Consumer;

public class MapViewModel extends AndroidViewModel {
    private static final String TAG = "MapViewModel"; // For logging
    private final CarParkRepository carParkRepo;
    private final LiveData<Map<String, CarParkApiData>> carParkApiLookupLive;

    public MapViewModel(@NonNull Application application) {
        super(application);

        carParkRepo = CarParkRepository.getCarParkRepo(application);
        carParkApiLookupLive = carParkRepo.getCarParkApiLookupLive();

        carParkRepo.fetchApi(); // This is suppose to be called every x minute but for now, I will just manually call it for this quick check
    }

    // API
    public LiveData<Map<String, CarParkApiData>> getCarParkApiLookupLive() {
        return carParkApiLookupLive;
    }

    public CarParkApiData getCarParkDataForId (String carParkId) {
        return carParkRepo.getCarParkDataForId(carParkId);
    }

    // DB
    public void getCarParkByNumber(String carParkNumber, Consumer<CarPark> callback) {
        carParkRepo.getCarParkByNumber(carParkNumber, callback);
    }
}