package com.jianan.parkwhere.ui.map;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jianan.parkwhere.R;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.databinding.FragmentMapBinding;

public class MapFragment extends Fragment {
    private static final String TAG = "MapFragment";
    private FragmentMapBinding binding;
    private MapViewModel mapViewModel;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@Nullable View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialise ViewModel using AndroidViewModel (application context is required by repository instance)
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        mapViewModel.getCarParkApiLookupLive().observe(getViewLifecycleOwner(), carParkApiLookup -> {
            Log.d(TAG, "Fragment API data observer triggered");

            // When the observer is first attached, LiveData immediately emits its current value.
            // As carParkApiLookupLive starts out as an empty Map, the first emitted value is empty.
            // Therefore, isEmpty() filters out this initial “empty” state so only real API results are processed.
            if (carParkApiLookup.isEmpty()) {
                return; // Exit the observer callback
            }

            CarParkApiData carParkApiData = mapViewModel.getCarParkDataForId("BE28"); // carParkApiData will hold the information from the API

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

        mapViewModel.getCarParkByNumber("BE28", carPark -> {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}