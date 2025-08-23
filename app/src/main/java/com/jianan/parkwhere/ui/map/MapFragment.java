package com.jianan.parkwhere.ui.map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import com.jianan.parkwhere.R;
import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.data.model.CarParkInfo;
import com.jianan.parkwhere.databinding.CustomLocationDialogBinding;
import com.jianan.parkwhere.databinding.FragmentMapBinding;
import com.jianan.parkwhere.util.ApiScheduler;
import com.jianan.parkwhere.util.PermissionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment that displays car parks on a Google Map and handles map interactions
 *
 * Uses MapViewModel to provide LiveData and to persist map state across configuration changes
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {
    // private static final String TAG = "MapFragment";
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final LatLng DEFAULT_LOCATION = new LatLng(1.36053, 103.98945); // When permission is not granted, show Singapore's Changi Airport
    private static final float DEFAULT_ZOOM = 15f;
    private FragmentMapBinding binding;
    private MapViewModel mapViewModel;
    private boolean isObserversSetup = false;

    // Google Map
    private GoogleMap map;
    private AutocompleteSupportFragment searchBarFragment;
    private Circle circle;
    private final List<Marker> carParkMarkers = new ArrayList<>();

    // Camera control
    private Location lastKnownLocation = null;

    // Creating a method for location permission launcher
    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean hasLocationPermission = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) || permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

        // Update mapViewModel's Livedata for MapFragment UI
        mapViewModel.setLocationPermissionLiveDataStatus(hasLocationPermission);

        if (hasLocationPermission) {
            startLocationService();

            // If the user grant location permission via location dialog, set the flag to animate the camera to their current position
            mapViewModel.setNeedInitialLocationMove(true);

            if (map != null) {
                enableUserLocationOnMap();
            }
        }
    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // For data binding, DataBindingUtil is used instead of FragmentMapBinding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map , container, false);

        // Lifecycle owner is set for LiveData in XML
        binding.setLifecycleOwner(this);

        // Initialise Places client
        PlacesClient placesClient = Places.createClient(requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@Nullable View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialise ViewModel using AndroidViewModel (application context is required by repository instance)
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        // Bind MapViewModel to XML
        binding.setViewModel(mapViewModel);

        // Obtain the Google map fragment in order to manipulate the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configure Places search bar
        searchBarFragment = (AutocompleteSupportFragment) getChildFragmentManager().findFragmentById(R.id.search_bar);
        searchBarFragment.setPlaceFields(Arrays.asList(Place.Field.LOCATION, Place.Field.DISPLAY_NAME));
        searchBarFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng searchedLocation = place.getLocation();

                if (searchedLocation != null) {
                    // searchedLocation here is just a label, there is no purpose for it
                    Location location = new Location("searchedLocation");
                    location.setLatitude(searchedLocation.latitude);
                    location.setLongitude(searchedLocation.longitude);
                    mapViewModel.setSearchedLocation(location);

                    // Show searched location on the search bar
                    String displayText = place.getDisplayName() != null ? place.getDisplayName() : String.format("%.6f, %.6f", searchedLocation.latitude, searchedLocation.longitude);
                    searchBarFragment.setText(displayText);
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                // Log.d(TAG, "Autocomplete Error: " + status);
            }
        });

        searchBarFragment.getView().post(() -> setupSearchBarTextWatcher());

        // Bind the button to the on click listener
        binding.buttonRecenterLocation.setOnClickListener(v -> handleLocationButtonClick());
        binding.buttonSearchRadius.setOnClickListener(v -> showRadiusBottomSheet());

        // Setup observers
        setupObservers();

        // If first launch, call requestLocationPermission
        // If location permission is already given (whereby the application is not running for the first time), start location update
        if (!mapViewModel.getFirstLaunch() && mapViewModel.hasLocationPermission()) {
            startLocationService();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Hide the default re-location button
        map.getUiSettings().setMyLocationButtonEnabled(false);

        setupMapListeners();
        setupMapObservers();

        // Retrieve the saved camera position (if any) from the view model
        // As the view model survives configuration changes (theme change) such as onDestroy(), this can be used to preserve the current camera state
        MapViewModel.CameraPosition savedPosition = mapViewModel.getSavedCameraPosition();

        Log.d("CameraTrigger", "mapViewModel.hasLocationPermission is " + mapViewModel.hasLocationPermission());
        Log.d("CameraTrigger", "mapViewModel.etLocationLiveData().getValue() is " + mapViewModel.getLocationLiveData().getValue());

        // If location permission was granted when away from Map Fragment, animate the camera to their current position
        if (mapViewModel.getNeedInitialLocationMove()) {
            enableUserLocationOnMap();
        }

        else if (savedPosition != null) {
            enableUserLocationOnMap();
            LatLng savedLatLng = new LatLng(savedPosition.getLatitude(), savedPosition.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(savedLatLng, savedPosition.getZoom()));
            Log.d("Cameratrigger", "Restored saved camera position: " + savedPosition);

            // Clear the saved position after restoring it from view model
            mapViewModel.clearSavedCameraPosition();
        }
        // Ensure that location permission is given AND location service is enabled on the phone
        else if (mapViewModel.hasLocationPermission() && mapViewModel.isLocationServicesEnabled()) {
            enableUserLocationOnMap();

        // Show default location
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
            // Log.d(TAG, "Moved to default location");

            if (mapViewModel.getFirstLaunch()) {
                locationPermissionLauncher.launch(LOCATION_PERMISSIONS);
                mapViewModel.setFirstLaunch();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean hasLocationPermission = mapViewModel.hasLocationPermission();
        boolean isLocationUpdateCurrentlyActive = mapViewModel.isLocationServiceActive();

        // Read previous LiveData value
        Boolean prevHasLocationPermission  = null;
        LiveData<Boolean> prevLiveData = mapViewModel.hasLocationPermissionLiveData();
        // If previous LiveData is not null, obtain its value else set it to false
        if (prevLiveData != null) {
            prevHasLocationPermission = prevLiveData.getValue();
        }
        if (prevHasLocationPermission == null) {
            prevHasLocationPermission = false;
        }

        // Update mapViewModel's Livedata for MapFragment UI
        mapViewModel.setLocationPermissionLiveDataStatus(hasLocationPermission);

        // If location permission is given manually
        if (!prevHasLocationPermission && hasLocationPermission) {
            // Added
            // User manually grant location permission via settings, set the flag to animate the camera to their current position
            enableUserLocationOnMap();
            mapViewModel.setNeedInitialLocationMove(true);
        }

        if (hasLocationPermission && !isLocationUpdateCurrentlyActive) {
            // Log.d(TAG, "onResume: Location permission given - starting updates");
            startLocationService();
        } else if (hasLocationPermission && isLocationUpdateCurrentlyActive) {
            // Log.d(TAG, "onResume: Has Location permission and already observing updates");
        } else if (!hasLocationPermission && isLocationUpdateCurrentlyActive) {
            // Log.d(TAG, "onResume: Location permission revoked - stopping location updates");
            mapViewModel.stopLocationService();
        } else {
            // Log.d(TAG, "onResume: No permission and not active");
        }

        // Ensure the API information is recent when returning to fragment
        ApiScheduler.getInstance().fetchIfStale();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Log.d(TAG, "onDestroyView is being called");

        if (map != null) {
            com.google.android.gms.maps.model.CameraPosition currentPosition= map.getCameraPosition();
            mapViewModel.saveCameraPosition(currentPosition.target.latitude, currentPosition.target.longitude, currentPosition.zoom);

            Log.d("CameraTrigger", "MapFragment values " +
                    currentPosition.target.latitude + ", " +
                    currentPosition.target.longitude + ", zoom=" +
                    currentPosition.zoom);
        }

        mapViewModel.stopLocationService();

        isObserversSetup = false;
        lastKnownLocation = null;

        if (circle != null) {
            circle.remove();
            circle = null;
        }

        for (Marker marker : carParkMarkers) {
            marker.remove();
        }
        carParkMarkers.clear();

        map =null;
        binding = null;
    }

    // -------------------------
    // Setup
    // -------------------------

    /**
     * Setup observers for location and snackbar messages
     */
    private void setupObservers() {
        if (isObserversSetup) {
            return;
        }

        isObserversSetup = true;

        // IMPORTANT: Added an initial null check to prevent the application from crashing when location is not set on the emulator
        LiveData<Location> locationLiveData = mapViewModel.getLocationLiveData();

        if (locationLiveData != null) {
            locationLiveData.observe(getViewLifecycleOwner(), location -> {
                if (location != null) {
                    // Log.d(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());
                } else {
                    // Log.d(TAG, "Location is NULL!");
                }
            });
        } else {
            // Log.d(TAG, "Location is null. Check to see if location is set on emulator");
        }

        mapViewModel.getSnackbarMessageLiveData().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                showCustomSnackbar(message);
            }
        });
    }

    /**
     * Configure map click and marker listeners
     */
    private void setupMapListeners() {
        if (map == null) {
            return;
        }
        map.setOnMarkerClickListener(marker ->  {
            Object tag = marker.getTag();
            if (tag instanceof CarPark) {
                CarPark carPark = (CarPark) tag;

                // Show bottom sheet dialog with car park details
                showCarParkBottomSheet(carPark);
                return true; // Indicate that the marker click was handled
            }
            return false; // Indicate that the marker click was not handled
        });
    }

    /**
     * Setup observers that directly update the map such as active location's nearby car parks and radius circle
     */
    private void setupMapObservers() {
        LiveData<Location> activeLocation = mapViewModel.getActiveLocationLiveData();
        if (activeLocation != null) {
            activeLocation.observe(getViewLifecycleOwner(), location-> {

                // Will be use to check if there is a saved camera position when returning to fragment. If it does not exist, the camera will move to the user location by default
                MapViewModel.CameraPosition savedPosition = mapViewModel.getSavedCameraPosition();

                if (location != null && map != null) {
                    updateMapLocation(location);
                    // The observer cannot be directly used to determine when to animate the camera as it always emits when returning back to the fragment
                    // If location permission has been given, animate the camera movement to their current position
                    if (mapViewModel.getNeedInitialLocationMove()) {
                        animateCameraToLocation(location);
                        mapViewModel.setNeedInitialLocationMove(false);
                        Log.d("CameraTrigger", "Camera animated due to location permission granted");
                    }

                    // If active location has changed, animate the camera movement
                    else if (shouldAnimateToLocation(location)) {
                        animateCameraToLocation(location);
                        Log.d("CameraTrigger", "Camera animated due to location change");
                    }

                    // Will be use to check if there is a saved camera position when returning to fragment
                    // If it does not exist, the camera will move to the user location by default
                    else if (shouldMoveToLocation(location) && (savedPosition == null)) {
                        moveCameraToLocation(location);
                        Log.d("CameraTrigger", "Camera moved due to location change");
                    }

                    // Update last known location for future comparison
                    lastKnownLocation = new Location(location);
                }
            });
        }

        mapViewModel.getNearbyCarParksLiveData().observe(getViewLifecycleOwner(), nearbyCarParkList -> {
            if (map != null) {
                updateCarParkMarkers(nearbyCarParkList);
            }
        });

        mapViewModel.getRadiusLiveData().observe(getViewLifecycleOwner(), radius -> {
           if (radius != null && circle != null) {
               circle.setRadius(radius);
           }
        });
    }

    /**
     * Configure a text watcher on the Places autocomplete search input to clear searched location when emptied
     */
    private void setupSearchBarTextWatcher() {
        View searchView = searchBarFragment.getView();
        if (searchView != null) {
            EditText searchEditText = searchView.findViewById(com.google.android.libraries.places.R.id.places_autocomplete_search_input);

            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (editable.toString().trim().isEmpty()) {
                        // Log.d(TAG, "Search bar cleared! Returning to user location");
                        mapViewModel.clearSearchedLocation();
                    }
                }
            });
        }
    }

    // -------------------------
    // Location Control
    // -------------------------

    /**
     * Handle recenter location button click and recenter map if active location is available
     */
    private void handleLocationButtonClick() {
        // Check that location permission is given and that location is not null
        if (mapViewModel.hasLocationPermission() && mapViewModel.isLocationServicesEnabled()) {

            // Clear searched location in the view model and the search bar
            mapViewModel.clearSearchedLocation();
            if (searchBarFragment != null) {
                searchBarFragment.setText("");
            }

            Location currentLocation = mapViewModel.getLocationLiveData().getValue();
            if (currentLocation != null && map != null) {
                LatLng userLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM));
            }
        } else {
            showSettingsDialog();
        }
    }

    /**
     * Start location updates via the ViewModel
     */
    private void startLocationService() {
        if (mapViewModel.startLocationService()) {
            // Log.d(TAG, "Location service started successfully");
        } else {
            // Log.d(TAG, "Location is null. Check to see if location is set on emulator");
        }
    }

    /**
     * Enable the user location blue dot on the Google map with permission handling
     */
    private void enableUserLocationOnMap() {
        if (mapViewModel.hasLocationPermission() && map != null) {
            try {
                map.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                // Log.e(TAG, "Location permission e");
            }
        }
    }

    /**
     * Show settings dialog that guides user to application settings to enable location permission
     */
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

        // Set up button on click listener
        dialogBinding.buttonLocationAccept.setOnClickListener(v -> {
            PermissionUtils.openParkWhereAppSettings(requireActivity());
            dialog.dismiss();
        });

        dialogBinding.buttonLocationDecline.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // -------------------------
    // Map Update Logic
    // -------------------------

    /**
     * Update or create the radius circle centered on the user location
     *
     * @param location user location to center the circle on
     */
    private void updateMapLocation(Location location) {
        if (map == null) {
            return;
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        LatLng userLatLng = new LatLng(lat, lon);

        // Add alpha 0xFF for stroke (opaque), and 0x33 for fill
        int stroke = 0xFF1976D2; // 0xFF40E0D0
        int fill   = 0x4D00BFFF;

        if (circle == null) {
            circle = map.addCircle(new CircleOptions()
                    .center(userLatLng)
                    .radius(mapViewModel.getRadiusValue())
                    .strokeColor(stroke)
                    .fillColor(fill));
        } else {
            circle.setCenter(userLatLng);
        }
    }

    /**
     * Update markers on the map for the given list of car parks
     *
     * @param carParkList list of nearby car parks to display
     */
    private void updateCarParkMarkers(List<CarPark> carParkList) {
        if (map == null) return;

        // Clear old markers
        for (Marker marker : carParkMarkers) {
            marker.remove();
        }
        carParkMarkers.clear();

        if (carParkList == null || carParkList.isEmpty()) {
            return;
        }

        // Add new markers
        for (CarPark carPark : carParkList) {
            LatLng carParkLatLng = new LatLng(carPark.getLatitude(), carPark.getLongitude());

            // Get the icon with tinting based on car park availability
            BitmapDescriptor markerIcon = getMarkerIconWithTint(carPark);

            Marker marker = map.addMarker(new MarkerOptions()
                    .position(carParkLatLng)
                    .title(carPark.getCarParkNumber())
                    .icon(markerIcon));

            if (marker != null) {
                marker.setTag(carPark);
                carParkMarkers.add(marker);
            }
        }
        // Log.d(TAG, "Added " + carParkMarkers.size() + " car park markers");
    }

    /**
     * Determine whether to animate the camera by comparing the previous active location with the new active location
     *
     * @param newLocation new active location
     * @return true when animation should be performed, otherwise false
     */
    private boolean shouldAnimateToLocation(Location newLocation) {
        if (lastKnownLocation == null) {
            Log.d("CameraTrigger", "shouldAnimateToLocation's last known location is null");
            return false;
        }

        float distance = lastKnownLocation.distanceTo(newLocation);

        boolean shouldAnimate = distance > 10.0f;

        Log.d("CameraTrigger","New location is: " + newLocation + " Last Location is: " + lastKnownLocation + " Distance is: " + distance);
        Log.d("CameraTrigger", "shouldAnimate is " + shouldAnimate);
        return shouldAnimate;
    }

    /**
     * Determines whether to move the camera by comparing the previous active location with the new active location
     *
     * @param newLocation new active location
     * @return true when camera should move false otherwise
     */
    private boolean shouldMoveToLocation(Location newLocation) {
        if (lastKnownLocation == null) {
            Log.d("CameraTrigger", "shouldMoveToLocation's last known location is null");
            return true;
        }

        float distance = lastKnownLocation.distanceTo(newLocation);

        boolean shouldMove = distance > 10.0f;

        Log.d("CameraTrigger","New location is: " + newLocation + " Last Location is: " + lastKnownLocation + " Distance is: " + distance);
        Log.d("CameraTrigger", "shouldAnimate is " + shouldMove);
        return shouldMove;
    }

    /**
     * Animate camera smoothly to the provided location
     *
     * @param location target location to animate to
     */
    private void animateCameraToLocation(Location location) {
        if (map == null || location == null) {
            return;
        }

        LatLng activeLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(activeLatLng, DEFAULT_ZOOM));
    }

    /**
     * Move camera immediately to the provided location
     *
     * @param location target location to move to
     */
    private void moveCameraToLocation(Location location) {
        if (map == null || location == null) {
            return;
        }

        LatLng activeLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(activeLatLng, DEFAULT_ZOOM));
    }

    // -------------------------
    // Car Park Bottom Sheet and UI helpers
    // -------------------------

    /**
     * Show bottom sheet with car park details when a marker is clicked
     *
     * @param carPark car park to show details for
     */
    private void showCarParkBottomSheet(CarPark carPark) {
        // Create bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        // Inflate the custom layout
        View bottomSheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_car_park, null);

        // Set the view to the dialog
        bottomSheetDialog.setContentView(bottomSheetView);

        // Apply rounded background to the bottom sheet container only when the dialog is inflated
        bottomSheetDialog.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.dialog_background);
            }
        });

        // Find views
        TextView textCarParkAddress = bottomSheetView.findViewById(R.id.text_car_park_address);
        TextView textCarParkNumber = bottomSheetView.findViewById(R.id.text_car_park_number);
        TextView textDistance = bottomSheetView.findViewById(R.id.text_distance);
        TextView textCarAvailability = bottomSheetView.findViewById(R.id.text_car_availability);
        TextView textMotorcycleAvailability = bottomSheetView.findViewById(R.id.text_motorcycle_availability);
        TextView textLastUpdated = bottomSheetView.findViewById(R.id.text_last_updated);
        TextView textNoInformation = bottomSheetView.findViewById(R.id.text_no_information);
        LinearLayout carAvailabilityContainer = bottomSheetView.findViewById(R.id.car_availability_container);
        LinearLayout motorcycleAvailabilityContainer = bottomSheetView.findViewById(R.id.motorcycle_availability_container);
        LinearLayout noInformationContainer = bottomSheetView.findViewById(R.id.no_information_container);
        ImageView iconBookmark = bottomSheetView.findViewById(R.id.icon_bookmark);

        // Get car park details from ViewModel
        MapViewModel.CarParkDetailsData details = mapViewModel.getCarParkDetails(carPark.getCarParkNumber());

        // Set basic car park information
        textCarParkAddress.setText(carPark.getAddress());
        textCarParkNumber.setText("Car Park " + carPark.getCarParkNumber());

        // Calculate and display distance
        // Location userLocation = mapViewModel.getLocationLiveData().getValue();
        if (details != null && details.getFormattedDistance() != null) {
            textDistance.setText(details.getFormattedDistance());
            textDistance.setVisibility(View.VISIBLE);
        } else {
            textDistance.setVisibility(View.GONE);
        }

        // Obtain vehicle type filter
        boolean isCarLotIncluded = mapViewModel.getCurrentVehicleType() == mapViewModel.VEHICLE_BOTH || mapViewModel.getCurrentVehicleType() == mapViewModel.VEHICLE_CAR;
        boolean isMotorcycleLotIncluded = mapViewModel.getCurrentVehicleType() == mapViewModel.VEHICLE_BOTH || mapViewModel.getCurrentVehicleType() == mapViewModel.VEHICLE_MOTORCYCLE;

        // Get API data for availability information
        if (details != null && details.getApiData() != null && !details.getApiData().getCarParkInfo().isEmpty()) {
            CarParkApiData apiData = details.getApiData();

            boolean hasCarLots = false;
            boolean hasMotorcycleLots = false;

            // Process each lot type
            for (CarParkInfo carParkInfo : apiData.getCarParkInfo()) {
                String lotType = carParkInfo.getLotType();
                String available = carParkInfo.getLotsAvailable();
                String total = carParkInfo.getTotalLots();

                if ("C".equals(lotType)) { // Car lots
                    textCarAvailability.setText(available + "/" + total);
                    hasCarLots = true;
                } else if ("M".equals(lotType) || "Y".equals(lotType)) { // Motorcycle lots
                    textMotorcycleAvailability.setText(available + "/" + total);
                    hasMotorcycleLots = true;
                }
            }

            // Show/hide availability containers based on vehicle type filter and available lot types
            carAvailabilityContainer.setVisibility((hasCarLots && isCarLotIncluded) ? View.VISIBLE : View.GONE);
            motorcycleAvailabilityContainer.setVisibility((hasMotorcycleLots && isMotorcycleLotIncluded) ? View.VISIBLE : View.GONE);

            // Check to see if no information container should be visible
            boolean showNoInfoContainer = false;
            String noInfoMessage = "";

            if (mapViewModel.getCurrentVehicleType() == mapViewModel.VEHICLE_CAR && !hasCarLots){
                showNoInfoContainer = true;
                noInfoMessage = "No Car Park Lots Found";
            } else if (mapViewModel.getCurrentVehicleType() == mapViewModel.VEHICLE_MOTORCYCLE && !hasMotorcycleLots) {
                showNoInfoContainer = true;
                noInfoMessage = "No Motorcycle Lots Found";
            }

            if (showNoInfoContainer) {
                textNoInformation.setText(noInfoMessage);
                noInformationContainer.setVisibility(View.VISIBLE);
                textLastUpdated.setVisibility(View.GONE);
            } else {
                noInformationContainer.setVisibility(View.GONE);
            }

            // Format and set last updated time
            if (apiData.getUpdateDateTime() != null) {
                String updateTime = mapViewModel.formatUpdateTime(apiData.getUpdateDateTime());
                textLastUpdated.setText(updateTime);
                textLastUpdated.setVisibility(View.VISIBLE);
            } else {
                textLastUpdated.setVisibility(View.GONE);
            }
        }
        // If no API data is available
        else {
            carAvailabilityContainer.setVisibility(View.GONE);
            motorcycleAvailabilityContainer.setVisibility(View.GONE);
            textLastUpdated.setVisibility(View.GONE);

            textNoInformation.setText("No Data Available");
            noInformationContainer.setVisibility(View.VISIBLE);
        }

        // Set bookmark icon state based on current bookmark status
        boolean isBookmarked = mapViewModel.isBookmarked(carPark.getCarParkNumber());
        updateBookmarkIcon(iconBookmark, isBookmarked);

        iconBookmark.setOnClickListener(v -> {
            mapViewModel.toggleBookmark(carPark.getCarParkNumber());

            // Update the icon's image
            boolean newBookmarkStatus = mapViewModel.isBookmarked(carPark.getCarParkNumber());
            updateBookmarkIcon(iconBookmark, newBookmarkStatus);
        });

        // Show the dialog
        bottomSheetDialog.show();
    }

    /**
     * Update bookmark icon drawable according to bookmark state
     *
     * @param iconBookmark ImageView that shows bookmark icon
     * @param isBookmarked current bookmark state
     */
    private void updateBookmarkIcon (ImageView iconBookmark, boolean isBookmarked) {
        if (isBookmarked) {
            iconBookmark.setImageResource(R.drawable.ic_baseline_bookmark_black_24dp);
        } else {
            iconBookmark.setImageResource(R.drawable.ic_outline_bookmark_border_black_24dp);
        }
    }

    /**
     * Displays a custom Snackbar message anchored above the bottom navigation view
     *
     * @param message The message to display
     */
    private void showCustomSnackbar(String message) {
        if (binding == null || binding.getRoot() == null) {
            return;
        }

        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT);

        // Anchor the Snackbar above the bottom navigation bar (to prevent blocking)
        View bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
        snackbar.setAnchorView(bottomNavigationView);

        // Center the text
        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        // Set the duration of the Snackbar, 2s for now
        snackbar.setDuration(2000);
        snackbar.show();
    }

    // -------------------------
    // Radius Bottom Sheet
    // -------------------------

    /**
     * Show a bottom sheet to adjust search radius and persist information via ViewModel
     */
    private void showRadiusBottomSheet() {
        // Check if there is an active location
        Location activeLocation = mapViewModel.getActiveLocationLiveData().getValue();
        if (activeLocation == null) {
            showCustomSnackbar("To adjust search radius, please turn on your location services or search for a specific location");
            return;
        }

        // Create bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        // Inflate the custom layout
        View bottomSheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_radius, null);

        // Set the view to the dialog
        bottomSheetDialog.setContentView(bottomSheetView);

        // Apply rounded background to the bottom sheet container only when the dialog is inflated
        bottomSheetDialog.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.dialog_background);
            }
        });

        // Find views
        Slider radiusSlider = bottomSheetDialog.findViewById(R.id.slider_car_park_radius);

        // Set the initial slider value from view model
        float currentRadius = mapViewModel.getRadiusValue();
        radiusSlider.setValue(currentRadius);

        // Save the radius value when the user moves the slider
        radiusSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                mapViewModel.setRadiusValue(value);
            }
        });

        // Show the dialog
        bottomSheetDialog.show();
    }

    // -------------------------
    // Custom Tinted Markers (according to availability)
    // -------------------------

    /**
     * Display marker icon based on parking availability
     * @param carPark The car park to get availability data for
     * @return BitmapDescriptor with appropriate color tint
     */
    private BitmapDescriptor getMarkerIconWithTint(CarPark carPark) {
        // Get availability percentage
        float availabilityPercentage = getAvailabilityPercentage(carPark);

        if (availabilityPercentage < 0) {
            return createUntintedIcon(R.drawable.ic_outline_parking_black_24dp);
        }

        // Determine drawable resource based on availability
        int drawableRes = getDrawableForAvailability(availabilityPercentage);

        // Use the icon
        return createUntintedIcon(drawableRes);
    }

    /**
     * Calculates the availability percentage for a car park
     * @param carPark The car park to calculate availability for
     * @return Percentage of available spots (0.0 to 1.0) or -1 if no data available
     */
    private float getAvailabilityPercentage(CarPark carPark) {
        MapViewModel.CarParkDetailsData details = mapViewModel.getCarParkDetails(carPark.getCarParkNumber());

        if (details == null || details.getApiData() == null || details.getApiData().getCarParkInfo().isEmpty()) {
            return -1; // No data available
        }

        CarParkApiData apiData = details.getApiData();
        int totalAvailable = 0;
        int totalSpots = 0;

        // Get current vehicle type filter
        int vehicleType = mapViewModel.getCurrentVehicleType();

        // Process each lot type
        for (CarParkInfo carParkInfo : apiData.getCarParkInfo()) {
            String lotType = carParkInfo.getLotType();

            // Only count lots that match the current filter
            boolean shouldCount = false;
            if ("C".equals(lotType) && (vehicleType == mapViewModel.VEHICLE_BOTH || vehicleType == mapViewModel.VEHICLE_CAR)) {
                shouldCount = true;
            } else if (("M".equals(lotType) || "Y".equals(lotType)) &&
                    (vehicleType == mapViewModel.VEHICLE_BOTH || vehicleType == mapViewModel.VEHICLE_MOTORCYCLE)) {
                shouldCount = true;
            }

            if (shouldCount) {
                try {
                    int available = Integer.parseInt(carParkInfo.getLotsAvailable());
                    int total = Integer.parseInt(carParkInfo.getTotalLots());
                    totalAvailable += available;
                    totalSpots += total;
                } catch (NumberFormatException e) {
                    // Log.w(TAG, "Invalid number format for car park " + carPark.getCarParkNumber());
                }
            }
        }

        if (totalSpots == 0) {
            return -1; // No relevant spots found
        }

        return (float) totalAvailable / totalSpots;
    }

    /**
     * Returns appropriate drawable based on availability percentage
     * @param availabilityPercentage Percentage of available spots (0.0 to 1.0) or -1 for no data
     * @return Drawable resource id
     */
    private int getDrawableForAvailability(float availabilityPercentage) {
        // 30% or more available lots
        if (availabilityPercentage >= 0.3f) {
            return R.drawable.ic_outline_parking_green_24dp;
        }
        // 10-29% available lots
        else if (availabilityPercentage >= 0.1f) {
            return R.drawable.ic_outline_parking_orange_24dp;
        }
        // Less than 10% available lots
        else {
            return R.drawable.ic_outline_parking_red_24dp;
        }
    }

    /**
     * Creates an untinted bitmap descriptor from a vector drawable
     * @param drawableRes The drawable resource ID
     * @return BitmapDescriptor with original colors
     */
    private BitmapDescriptor createUntintedIcon(int drawableRes) {
        Drawable vectorDrawable = ContextCompat.getDrawable(requireContext(), drawableRes);
        if (vectorDrawable == null) {
            return BitmapDescriptorFactory.defaultMarker(); // Fallback
        }

        // Convert to bitmap without tinting
        Bitmap bitmap = Bitmap.createBitmap(
                vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}