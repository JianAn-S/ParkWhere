package com.jianan.parkwhere.ui.list;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
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
import com.jianan.parkwhere.databinding.FragmentListBinding;
import com.jianan.parkwhere.ui.CustomFragment;
import com.jianan.parkwhere.util.ApiScheduler;
import com.jianan.parkwhere.util.CarParkAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment that shows nearby car parks in a list
 *
 * Uses ListViewModel to provide LiveData sources and handles search and radius UI
 */
public class ListFragment extends CustomFragment implements CarParkAdapter.OnCarParkClickListener {
    // private static final String TAG = "ListFragment";
    private FragmentListBinding binding;
    private ListViewModel listViewModel;
    private CarParkAdapter carParkAdapter;
    private AutocompleteSupportFragment searchBarFragment;
    private boolean isObserversSetup = false;

    @Override
    public View inflateFragmentLayout(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentListBinding.inflate(inflater, container, false);

        // Initialise Places client
        PlacesClient placesClient = Places.createClient(requireContext());

        return binding.getRoot();
    }

    // Sets the title to be displayed in the custom toolbar for the List fragment
    @Override
    protected String getActionBarTitle() {
        return "Nearby";
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialise ViewModel using AndroidViewModel (application context is required by repository instance)
        listViewModel = new ViewModelProvider(requireActivity()).get(ListViewModel.class);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup observers
        setupObservers();

        // Test Car Park API & DB implementation
        // carParkApiAndDbTest();

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
                    listViewModel.setSearchedLocation(location);

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

        binding.buttonSearchRadius.setOnClickListener(v -> showRadiusBottomSheet());

        // If location permission is already given, start location update
        if (listViewModel.hasLocationPermission()) {
            startLocationService();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean hasLocationPermission = listViewModel.hasLocationPermission();
        boolean isLocationUpdateCurrentlyActive = listViewModel.isLocationServiceActive();

        // Update listViewModel's Livedata for listFragment UI
        listViewModel.setLocationPermissionLiveDataStatus(hasLocationPermission);

        if (hasLocationPermission && !isLocationUpdateCurrentlyActive) {
            startLocationService();
        } else if (hasLocationPermission && isLocationUpdateCurrentlyActive) {
            // Log.d(TAG, "onResume: Has Location permission and already observing updates");
        } else if (!hasLocationPermission && isLocationUpdateCurrentlyActive) {
            // Log.d(TAG, "onResume: Location permission revoked - stopping location updates");
            stopLocationUpdates();
        } else {
            // Log.d(TAG, "onResume: No permission and not active");
        }

        // Ensure the API information is recent when returning to fragment
        ApiScheduler.getInstance().fetchIfStale();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopLocationUpdates();
        isObserversSetup = false;
        binding = null;
    }

    // -------------------------
    // Setup
    // -------------------------

    /**
     * Sets up the RecyclerView with a CarParkAdapter that displays car parks with distance info
     */
    private void setupRecyclerView() {
        // Create an adapter that displays each car park's distance from the user
        carParkAdapter = new CarParkAdapter(true);
        carParkAdapter.setOnCarParkClickListener(this);

        binding.listRecyclerView.setAdapter(carParkAdapter);
        binding.listRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    /**
     * Sets up LiveData observers for API data and car park lists
     */
    private void setupObservers() {
        if (isObserversSetup) {
            return;
        }

        isObserversSetup = true;

        // Observe location permission status
        listViewModel.hasLocationPermissionLiveData().observe(getViewLifecycleOwner(), hasPermission -> {
            updateUIState(hasPermission, listViewModel.getNearbyCarParksLiveData().getValue());
        });

        // Observe location changes
        // IMPORTANT: Added an initial null check to prevent the application from crashing when location is not set on the emulator
        LiveData<Location> locationLiveData = listViewModel.getLocationLiveData();
        if (locationLiveData != null) {
            locationLiveData.observe(getViewLifecycleOwner(), location -> {
                if (location != null) {
                    carParkAdapter.updateUserLocation(location);
                } else {
                    carParkAdapter.updateUserLocation(null);
                }
            });
        } else {
            // Log.d(TAG, "Location is null. Check to see if location is set on emulator");
        }

        // Observe nearby car parks
        listViewModel.getNearbyCarParksLiveData().observe(getViewLifecycleOwner(), nearbyCarParkList -> {

            Boolean hasPermission = listViewModel.hasLocationPermissionLiveData().getValue();
            updateUIState(hasPermission, nearbyCarParkList);

            if (nearbyCarParkList != null && !nearbyCarParkList.isEmpty()){
                carParkAdapter.submitList(nearbyCarParkList);
            } else {
                carParkAdapter.submitList(new ArrayList<>());
            }
        });

        // Observe API data changes
        listViewModel.getCarParkApiLookupLive().observe(getViewLifecycleOwner(), carParkApiLookup -> {

            // When the observer is first attached, LiveData immediately emits its current value.
            // As carParkApiLookupLive starts out as an empty Map, the first emitted value is empty.
            // Therefore, isEmpty() filters out this initial “empty” state so only real API results are processed.
            if (carParkApiLookup.isEmpty()) {
                return; // Exit the observer callback
            }

            // Update adapter with new API data
            carParkAdapter.updateApiData(carParkApiLookup);

            // Get nearby car parks and fetch additional details from the API
            List<CarPark> currentNearbyCarParks = listViewModel.getNearbyCarParksLiveData().getValue();

            if (currentNearbyCarParks != null && !currentNearbyCarParks.isEmpty()) {
                for (CarPark carPark : currentNearbyCarParks) {
                    String carParkId = carPark.getCarParkNumber();

                    // carParkApiData will hold the information from the API
                    CarParkApiData carParkApiData = listViewModel.getCarParkDataForId(carParkId);

                    if (carParkApiData != null && !carParkApiData.getCarParkInfo().isEmpty()) {
                        String lots = carParkApiData.getCarParkInfo().get(0).getLotsAvailable();
                        String lotType = carParkApiData.getCarParkInfo().get(0).getLotType();
                        String totalLots = carParkApiData.getCarParkInfo().get(0).getTotalLots();
                        String updateTime = carParkApiData.getUpdateDateTime();

                        // Log.d(TAG, "Data from API, Lots: " + lots + " Lot type: " + lotType + " total lots: " + totalLots + " update time:" + updateTime);
                    } else {
                        // Log.d(TAG, "No API data available for Car Park ID: " + carParkId);
                    }
                }
            } else {
                // Log.d(TAG, "No nearby car parks detected, nothing to query from the API.");
            }
        });

        // Observe bookmark changes
        listViewModel.getBookmarkLiveData().observe(getViewLifecycleOwner(), bookmarkEvent -> {
            if (bookmarkEvent != null) {
                // Update the specific item in the adapter
                carParkAdapter.updateBookmarkStatus(bookmarkEvent.getCarParkNumber());

                // Show message via snackbar
                String message = bookmarkEvent.isBookmarked() ? "Added " + bookmarkEvent.getCarParkNumber() + " to Bookmark" : "Removed " + bookmarkEvent.getCarParkNumber() + " from Bookmark";
                showCustomSnackbar(message);
            }
        });

        // Observe vehicle type filter
        listViewModel.getVehicleTypeLiveData().observe(getViewLifecycleOwner(), vehicleType -> {
            if (carParkAdapter != null) {
                carParkAdapter.updateVehicleType(vehicleType);
            }
        });

        // Observe if a location is being searched
        listViewModel.getSearchedLocationLiveData().observe(getViewLifecycleOwner(), searchedLocation -> {
            Boolean hasPermission = listViewModel.hasLocationPermissionLiveData().getValue();
            List<CarPark> currentNearbyCarParks = listViewModel.getNearbyCarParksLiveData().getValue();
            updateUIState(hasPermission, currentNearbyCarParks);
        });
    }

    // -------------------------
    // Search
    // -------------------------
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
                        listViewModel.clearSearchedLocation();
                    }
                }
            });
        }
    }

    private boolean isCurrentlySearching() {
        return listViewModel.isSearching();
    }

    // -------------------------
    // Location Control
    // -------------------------

    /**
     * Start location updates via the ViewModel
     */
    private void startLocationService() {
        if (listViewModel.startLocationService()) {
            // Log.d(TAG, "Location service started successfully");

        } else {
            // Log.d(TAG, "Location is null. Check to see if location is set on emulator");
        }
    }

    /**
     * Stop location updates via the ViewModel
     */
    private void stopLocationUpdates() {
        listViewModel.stopLocationService();
    }

    // -------------------------
    // Bookmark
    // -------------------------

    /**
     * Handle bookmark click from the adapter
     *
     * @param carPark the car park that was bookmarked
     */
    @Override
    public void onBookmarkClick(CarPark carPark) {
        listViewModel.toggleBookmark(carPark.getCarParkNumber());
    }

    /**
     * Check if a car park is currently bookmarked
     *
     * @param carParkNumber car park identifier
     * @return true if bookmarked false otherwise
     */
    @Override
    public boolean isCarParkBookmarked(String carParkNumber) {
        return listViewModel.isBookmarked(carParkNumber);
    }

    // @Override
    // public void onCarParkClick(CarPark carPark) {
    // Might add a Bottom Sheet Dialog in the future to show more car park info.
    // }

    // -------------------------
    // UI helpers
    // -------------------------

    /**
     * Updates the UI state depending on whether nearby car parks are available
     */
    private void updateUIState (Boolean hasLocationPermission, List<CarPark> nearbyCarParks) {
        // Safety check
        if (binding == null) {
            return;
        }

        // Default: hide all state layouts
        binding.layoutNoLocation.setVisibility(View.GONE);
        binding.layoutNoNearby.setVisibility(View.GONE);
        binding.listRecyclerView.setVisibility(View.GONE);

        // Check if user is currently searching
        boolean isSearching = isCurrentlySearching();

        // Show search results regardless of location permission
        if (isSearching) {
            // If there are car parks nearby show the recycler view
            if (nearbyCarParks != null && !nearbyCarParks.isEmpty()) {
                binding.listRecyclerView.setVisibility(View.VISIBLE);
            }
            // Else show no nearby car parks state
            else {
                binding.layoutNoNearby.setVisibility(View.VISIBLE);
            }
        }

        // No location permission, show location permission required state
        else if (hasLocationPermission == null || !hasLocationPermission || !listViewModel.isLocationServicesEnabled()) {
            binding.layoutNoLocation.setVisibility(View.VISIBLE);
        }

        // Has permission but no nearby car parks, show no nearby car parks state
        else if (nearbyCarParks == null || nearbyCarParks.isEmpty()) {
            binding.layoutNoNearby.setVisibility(View.VISIBLE);
        }

        // Has permission and has nearby car parks, show normal RecyclerView
        else {
            binding.listRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Displays a custom Snackbar message anchored above the bottom navigation view
     *
     * @param message The message to display
     */
    private void showCustomSnackbar(String message) {
        if (binding == null && binding.getRoot() == null) {
            return;
        }

        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT);

        // Anchor the Snackbar above the bottom navigation bar (to prevent blocking)
        View bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation_view);
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
    private void showRadiusBottomSheet() {
        // Check if there is an active location
        Location activeLocation = listViewModel.getActiveLocationLiveData().getValue();
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
        float currentRadius = listViewModel.getRadiusValue();
        radiusSlider.setValue(currentRadius);

        // Save the radius value when the user moves the slider
        radiusSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                listViewModel.setRadiusValue(value);
            }
        });

        // Show the dialog
        bottomSheetDialog.show();
    }
}