package com.jianan.parkwhere.ui.splash;

import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.jianan.parkwhere.R;
import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.local.CarParkDao;
import com.jianan.parkwhere.data.local.CarParkDatabase;
import com.jianan.parkwhere.data.remote.CarParkApiClient;
import com.jianan.parkwhere.data.remote.CarParkApiService;
import com.jianan.parkwhere.data.repository.CarParkRepository;
import com.jianan.parkwhere.model.CarParkApiData;
import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private CarParkRepository carParkRepo;
    // private final MutableLiveData<CarPark> carParkLive = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        carParkRepo = new CarParkRepository(getApplicationContext());

        // carParkApiLookup is not the LiveData itself but the current contents of that LiveData which is Map<String, CarParkApiData>
        carParkRepo.getCarParkApiLookupLive().observe(this, carParkApiLookup -> {

            // When the observer is first attached, LiveData immediately emits its current value.
            // As carParkApiLookupLive starts out as an empty Map, the first emitted value is empty.
            // Therefore, isEmpty() filters out this initial “empty” state so only real API results are processed.
            if (carParkApiLookup.isEmpty()) {
                return; // Exit the observer callback
            }

            CarParkApiData carParkApiData = carParkRepo.getCarParkDataForId("BE28"); // carParkApiData will hold the information from the API

            if (carParkApiData != null && !carParkApiData.getCarParkInfo().isEmpty()) {
                String lots = carParkApiData.getCarParkInfo().get(0).getLotsAvailable();
                String lotType = carParkApiData.getCarParkInfo().get(0).getLotType();
                String totalLots = carParkApiData.getCarParkInfo().get(0).getTotalLots();
                String updateTime = carParkApiData.getUpdateDateTime();

                Log.d(TAG, "Data from API, Lots: " + lots + " lot type: " + lotType + " total lots: " + totalLots + " update time:" + updateTime);
            } else {
                Log.d(TAG, "API data not found.");
            }
        });

        carParkRepo.fetchApi(); // This is suppose to be called every x minute but for now, I will just manually call it for this quick check

        carParkRepo.fetchCarParkByNumber("BE28", carPark -> {
           if (carPark != null) {
               Log.d(TAG, "Data from DB, Address: " + carPark.getAddress()
                       + ", Lat: " + carPark.getLatitude()
                       + ", Lng: " + carPark.getLongitude()
                       + ", Type: " + carPark.getCarParkType()
                       + ", System: " + carPark.getParkingSystemType()
                       + ", ShortTerm: " + carPark.getShortTermParking()
                       + ", Free: " + carPark.getFreeParking()
                       + ", Night: " + carPark.getNightParking()
                       + ", Decks: " + carPark.getCarParkDecks()
                       + ", Gantry: " + carPark.getGantryHeight()
                       + ", Basement: " + carPark.getCarParkBasement());
           } else {
               Log.d(TAG, "Car Park not found in DB?!");
           }
        });
    }
}
