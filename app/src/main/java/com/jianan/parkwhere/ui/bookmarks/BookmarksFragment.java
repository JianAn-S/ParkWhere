package com.jianan.parkwhere.ui.bookmarks;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.jianan.parkwhere.R;
import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.databinding.FragmentBookmarksBinding;
import com.jianan.parkwhere.ui.CustomFragment;
import com.jianan.parkwhere.util.ApiScheduler;
import com.jianan.parkwhere.util.CarParkAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays the list of bookmarked car parks
 *
 * Observes bookmarked car park data from {@link BookmarksViewModel} and updates its RecyclerView accordingly
 * Implements {@link CarParkAdapter.OnCarParkClickListener} to handle bookmark interactions on individual car park items
 */
public class BookmarksFragment extends CustomFragment implements CarParkAdapter.OnCarParkClickListener{

    // private static final String TAG = "BookmarksFragment";
    private FragmentBookmarksBinding binding;
    private BookmarksViewModel bookmarksViewModel;
    private CarParkAdapter carParkAdapter;
    private boolean isObserversSetup = false;

    @Override
    public View inflateFragmentLayout(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookmarksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    // Sets the title to be displayed in the custom toolbar for the Bookmarks fragment
    @Override
    protected String getActionBarTitle() {
        return "Bookmarks";
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialise ViewModel using AndroidViewModel (application context is required by repository instance)
        bookmarksViewModel = new ViewModelProvider(requireActivity()).get(BookmarksViewModel.class);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup observers
        setupObservers();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Log.d(TAG, "onResume is being called");

        // Refresh the list of bookmarked car parks to ensure it is updated
        bookmarksViewModel.refreshBookmarkedCarParks();

        // Ensure the API information is recent when returning to fragment
        ApiScheduler.getInstance().fetchIfStale();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Log.d(TAG, "onDestroyView is being called");

        isObserversSetup = false;
        binding = null;
    }

    // -------------------------
    // Setup
    // -------------------------
    /**
     * Configures the RecyclerView for displaying bookmarked car parks
     *
     * Creates the adapter and sets its click listener and a LinearLayoutManager
     */
    private void setupRecyclerView() {
        // Create an adapter that hides each car park's distance from the user
        carParkAdapter = new CarParkAdapter(false);
        carParkAdapter.setOnCarParkClickListener(this);

        binding.bookmarksRecyclerView.setAdapter(carParkAdapter);
        binding.bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    /**
     * Attaches LiveData observers for API data bookmarked car parks and bookmark state changes
     *
     * Ensures observers are set up only once per fragment lifecycle
     */
    private void setupObservers() {
        if (isObserversSetup) {
            // Log.d(TAG, "Observers already set up, skipping");
            return;
        }

        isObserversSetup = true;
        // Log.d(TAG, "Setting up location observer");

        // Observe API data changes
        bookmarksViewModel.getCarParkApiLookupLive().observe(getViewLifecycleOwner(), carParkApiLookup -> {

            // When the observer is first attached, LiveData immediately emits its current value.
            // As carParkApiLookupLive starts out as an empty Map, the first emitted value is empty.
            // Therefore, isEmpty() filters out this initial “empty” state so only real API results are processed.
            if (carParkApiLookup.isEmpty()) {
                return; // Exit the observer callback
            }

            // Update adapter with new API data
            carParkAdapter.updateApiData(carParkApiLookup);

            // Get nearby car parks and fetch additional details from the API
            List<CarPark> currentNearbyCarParks = bookmarksViewModel.getBookmarkedCarParksLiveData().getValue();

            if (currentNearbyCarParks != null && !currentNearbyCarParks.isEmpty()) {
                for (CarPark carPark : currentNearbyCarParks) {
                    String carParkId = carPark.getCarParkNumber();

                    // carParkApiData will hold the information from the API
                    CarParkApiData carParkApiData = bookmarksViewModel.getCarParkDataForId(carParkId);

                    if (carParkApiData != null && !carParkApiData.getCarParkInfo().isEmpty()) {
                        String lots = carParkApiData.getCarParkInfo().get(0).getLotsAvailable();
                        String lotType = carParkApiData.getCarParkInfo().get(0).getLotType();
                        String totalLots = carParkApiData.getCarParkInfo().get(0).getTotalLots();
                        String updateTime = carParkApiData.getUpdateDateTime();
                    }
                    //else {
                        //Log.d(TAG, "No API data available for Car Park ID: " + carParkId);
                    //}
                }
            }
            // else {
                // Log.d(TAG, "No nearby car parks detected, nothing to query from the API.");
            // }
        });

        // Observe bookmarked car parks
        bookmarksViewModel.getBookmarkedCarParksLiveData().observe(getViewLifecycleOwner(), bookmarkedCarParks -> {
            updateUIState(bookmarkedCarParks);

            if (bookmarkedCarParks != null && !bookmarkedCarParks.isEmpty()) {
                // Log.d(TAG, "Found " + bookmarkedCarParks.size() + " bookmarked car parks");
                carParkAdapter.submitList(bookmarkedCarParks);

                // Log basic information for each bookmarked car park
                for (CarPark carPark : bookmarkedCarParks) {
                    // Log.d(TAG, "Bookmarked Car Park - ID: " + carPark.getCarParkNumber() + ", Address: " + carPark.getAddress() + ", Lat: " + carPark.getLatitude() + ", Lng: " + carPark.getLongitude());
                }
            } else {
                // Log.d(TAG, "No bookmarked car parks found or list is null");
                carParkAdapter.submitList(new ArrayList<>());
            }
        });

        // Observe bookmark changes
        bookmarksViewModel.getBookmarkLiveData().observe(getViewLifecycleOwner(), bookmarkEvent -> {
            if (bookmarkEvent != null) {
                // Log.d(TAG, "Bookmark changed for car park: " + bookmarkEvent.getCarParkNumber() + ", isBookmarked: " + bookmarkEvent.isBookmarked());

                // Update the specific item in the adapter
                carParkAdapter.updateBookmarkStatus(bookmarkEvent.getCarParkNumber());

                // Show message via snackbar
                String message = bookmarkEvent.isBookmarked() ?
                        "Added " + bookmarkEvent.getCarParkNumber() + " to Bookmark" :
                        "Removed " + bookmarkEvent.getCarParkNumber() + " from Bookmark";
                showCustomSnackbar(message);
            }
        });
    }

    // -------------------------
    // Adapter callbacks
    // -------------------------

    /**
     * Handles bookmark click events from the RecyclerView
     *
     * @param carPark The car park whose bookmark icon was clicked
     */
    @Override
    public void onBookmarkClick(CarPark carPark) {
        bookmarksViewModel.toggleBookmark(carPark.getCarParkNumber());
    }

    /**
     * Checks whether a given car park is currently bookmarked
     *
     * @param carParkNumber The ID of the car park
     * @return true if bookmarked otherwise false
     */
    @Override
    public boolean isCarParkBookmarked(String carParkNumber) {
        return bookmarksViewModel.isBookmarked(carParkNumber);
    }

    // -------------------------
    // UI helpers
    // -------------------------
    /**
     * Updates the UI state based on whether there are any bookmarked car parks
     *
     * @param bookmarkedCarParks The list of currently bookmarked car parks
     */
    private void updateUIState(List<CarPark> bookmarkedCarParks) {
        // Safety check
        if (binding == null) {
            return;
        }

        // Default: hide all state layouts
        binding.layoutNoBookmarks.setVisibility(View.GONE);
        binding.bookmarksRecyclerView.setVisibility(View.GONE);

        if (bookmarkedCarParks == null || bookmarkedCarParks.isEmpty()) {
            // Show empty state when there is no bookmarks
            binding.layoutNoBookmarks.setVisibility(View.VISIBLE);
        } else {
            // Show normal RecyclerView if there are bookmarks
            binding.bookmarksRecyclerView.setVisibility(View.VISIBLE);
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
}