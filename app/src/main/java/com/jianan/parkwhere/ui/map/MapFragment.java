package com.jianan.parkwhere.ui.map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.location.Location;
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
import com.jianan.parkwhere.databinding.CustomLocationDialogBinding;
import com.jianan.parkwhere.databinding.FragmentMapBinding;
import com.jianan.parkwhere.util.PermissionUtils;

public class MapFragment extends Fragment {
    private static final String TAG = "MapFragment";
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FragmentMapBinding binding;
    private MapViewModel mapViewModel;

    // Creating a method for location permission launcher
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                boolean hasLocationPermission = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
                        || permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                // Update mapViewModel's Livedata for MapFragment UI
                mapViewModel.setLocationPermissionLiveDataStatus(hasLocationPermission);

                if (hasLocationPermission) {
                    startLocationUpdates();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // For data binding, DataBindingUtil is used instead of FragmentMapBinding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map , container, false);

        // Lifecycle owner is set for LiveData in XML
        binding.setLifecycleOwner(this);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@Nullable View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialise ViewModel using AndroidViewModel (application context is required by repository instance)
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        // Bind MapViewModel to XML
        binding.setViewModel(mapViewModel);

        // Bind the button to the onclick listener
        binding.buttonRecenterLocation.setOnClickListener(v -> handleLocationButtonClick());

        // Test Car Park API & DB implementation
        carParkApiAndDbTest();

        // If first launch, call requestLocationPermission
        // If location permission is already given (whereby the application is not running for the first time), start location update
        if (mapViewModel.getFirstLaunch()) {
            Log.d(TAG, "First launch - requesting location permission");
            requestLocationPermission();
            mapViewModel.setFirstLaunch();
        } else if (mapViewModel.hasLocationPermission()) {
            Log.d(TAG, "Permission already granted - starting location updates");
            startLocationUpdates();
        } else { // TESTING, to delete later
            Log.d(TAG, "No location permission available");
        }
    }

    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause is being called");
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume is being called");

        boolean hasLocationPermission = mapViewModel.hasLocationPermission();
        boolean isLocationUpdateCurrentlyActive = mapViewModel.isLocationUpdateActive();

        // Update mapViewModel's Livedata for MapFragment UI
        mapViewModel.setLocationPermissionLiveDataStatus(hasLocationPermission);

        if (hasLocationPermission && !isLocationUpdateCurrentlyActive) {
            Log.d(TAG, "onResume: Location permission given - starting updates");
            startLocationUpdates();
        } else if (hasLocationPermission && isLocationUpdateCurrentlyActive) {
            Log.d(TAG, "onResume: Has Location permission and already observing updates");
        } else if (!hasLocationPermission && isLocationUpdateCurrentlyActive) {
            Log.d(TAG, "onResume: Location permission revoked - stopping location updates");
            stopLocationUpdates();
        } else {
            Log.d(TAG, "onResume: No permission and not active");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.d(TAG, "onDestroyView is being called");

        stopLocationUpdates();
        binding = null;
    }

    // Testing purposes
    private void carParkApiAndDbTest() {
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

    // Location
    private void requestLocationPermission() {
        locationPermissionLauncher.launch(LOCATION_PERMISSIONS);
    }

    private void handleLocationButtonClick() {
        if (mapViewModel.hasLocationPermission()) {
            Log.d (TAG, "Returning back to user's location");
        } else {
            showSettingsDialog();
        }
    }

    private void showSettingsDialog() {
        // Create view binding for custom location dialog
        CustomLocationDialogBinding dialogBinding = CustomLocationDialogBinding.inflate(getLayoutInflater());

        // Create alert dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .setCancelable(true)
                .create();

        // Make custom location dialog transparent to show rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Set up button click listeners
        dialogBinding.buttonLocationAccept.setOnClickListener(v -> {
            PermissionUtils.openParkWhereAppSettings(requireActivity());
            dialog.dismiss();
        });

        dialogBinding.buttonLocationDecline.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void startLocationUpdates() {
        if (mapViewModel.startLocationUpdates()) {
            Log.d(TAG, "Setting up location observer");

            // IMPORTANT: Added an initial null check to prevent the application from crashing when location is not set on the emulator
            LiveData<Location> locationLiveData = mapViewModel.getLocationUpdates();

            if (locationLiveData != null) {
                locationLiveData.observe(getViewLifecycleOwner(), location -> {
                    Log.d(TAG, "Location observer triggered");
                    if (location != null) {
                        Log.d(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());
                    } else {
                        Log.d(TAG, "Location is NULL!");
                    }
                });
            } else {
                Log.d(TAG, "Location is null. Check to see if location is set on emulator");
                mapViewModel.stopLocationUpdates();
            }
        } else {
            Log.d(TAG, "Location update is active, observer is already set up");
        }
    }

    private void stopLocationUpdates() {
        Log.d(TAG, "Stopping location update");
        mapViewModel.stopLocationUpdates();
    }
}