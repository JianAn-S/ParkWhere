package com.jianan.parkwhere.ui.bookmarks;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.data.preferences.SettingsManager;
import com.jianan.parkwhere.data.repository.CarParkRepository;
import com.jianan.parkwhere.ui.list.ListViewModel;
import com.jianan.parkwhere.util.ApiScheduler;
import com.jianan.parkwhere.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BookmarksViewModel extends AndroidViewModel {
    private static final String TAG = "BookmarksViewModel";
    private final CarParkRepository carParkRepo;
    private final SettingsManager settingsManager;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final LiveData<Map<String, CarParkApiData>> carParkApiLookupLive;

    // LiveData for bookmarked car parks
    private final MutableLiveData<List<CarPark>> bookmarkedCarParksLiveData = new MutableLiveData<>();

    // Track bookmark changes
    private final SingleLiveEvent<BookmarkChangeEvent> bookmarkLiveData = new SingleLiveEvent<>();

    public BookmarksViewModel(@NonNull Application application) {
        super(application);

        carParkRepo = CarParkRepository.getCarParkRepo(application);
        settingsManager = SettingsManager.getSettingsManager(application);

        carParkApiLookupLive = carParkRepo.getCarParkApiLookupLive();

        // Load bookmarked car parks
        loadBookmarkedCarParks();

        // Initial API data fetch
        carParkRepo.fetchApi();
    }

    // -------------------------
    // Bookmark
    // -------------------------

    /**
     * Loads all bookmarked car parks from the database and updates the LiveData.
     * Car parks are sorted alphabetically by car park number.
     */
    private void loadBookmarkedCarParks() {
        Log.d(TAG, "Loading bookmarked car parks");

        executor.execute(() -> {
            Set<String> bookmarkedNumbers = settingsManager.getBookmarkedCarParkNumbers();
            Log.d(TAG, "Found " + bookmarkedNumbers.size() + " bookmarked car park numbers");

            if (bookmarkedNumbers.isEmpty()) {
                Log.d(TAG, "No bookmarked car parks, setting empty list");
                bookmarkedCarParksLiveData.postValue(new ArrayList<>());
                return;
            }

            List<CarPark> bookmarkedCarParks = new ArrayList<>();
            int totalToFetch = bookmarkedNumbers.size();
            AtomicInteger fetchedCount = new AtomicInteger(0);

            for (String carParkNumber : bookmarkedNumbers) {
                carParkRepo.getCarParkByNumber(carParkNumber, carPark -> {
                    if (carPark != null) {
                        synchronized (bookmarkedCarParks) {
                            bookmarkedCarParks.add(carPark);
                            Log.d(TAG, "Fetched bookmarked car park: " + carPark.getCarParkNumber());
                        }
                    } else {
                        Log.w(TAG, "Bookmarked car park not found in database: " + carParkNumber);
                    }

                    // Check if all car parks have been processed
                    if (fetchedCount.incrementAndGet() == totalToFetch) {
                        // Sort alphabetically by car park number
                        Collections.sort(bookmarkedCarParks, (cp1, cp2) ->
                                cp1.getCarParkNumber().compareTo(cp2.getCarParkNumber()));

                        Log.d(TAG, "All bookmarked car parks loaded and sorted. Total: " + bookmarkedCarParks.size());
                        bookmarkedCarParksLiveData.postValue(bookmarkedCarParks);
                    }
                });
            }
        });
    }

    /**
     * Refreshes the bookmarked car parks list.
     * Should be called when returning to the BookmarksFragment to ensure data is up to date.
     */
    public void refreshBookmarkedCarParks() {
        Log.d(TAG, "Refreshing bookmarked car parks");
        loadBookmarkedCarParks();
    }

    // -------------------------
    // LiveData Accessors
    // -------------------------

    /**
     * @return LiveData containing the list of bookmarked car parks, sorted alphabetically by car park number
     */
    public LiveData<List<CarPark>> getBookmarkedCarParksLiveData() {
        return bookmarkedCarParksLiveData;
    }

    /**
     * @return LiveData containing the API data lookup map (filtered to exclude empty states)
     */
    public LiveData<Map<String, CarParkApiData>> getCarParkApiLookupLive() {
        return carParkApiLookupLive;
    }

    /**
     * @return LiveData for bookmark change events
     */
    public LiveData<BookmarkChangeEvent> getBookmarkLiveData() {
        return bookmarkLiveData;
    }

    // -------------------------
    // API Methods
    // -------------------------

    /**
     * Gets the API data for a specific car park ID
     * @param carParkId the car park ID to look up
     * @return CarParkApiData or null if not found
     */
    public CarParkApiData getCarParkDataForId(String carParkId) {
        return carParkRepo.getCarParkDataForId(carParkId);
    }

    // -------------------------
    // Bookmark Methods
    // -------------------------

    /**
     * Checks if a car park is currently bookmarked
     * @param carParkNumber the car park number to check
     * @return true if bookmarked, false otherwise
     */
    public boolean isBookmarked(String carParkNumber) {
        return settingsManager.isBookmarked(carParkNumber);
    }

    /**
     * Toggles the bookmark status of a car park and triggers a refresh of the bookmarked list
     * @param carParkNumber the car park number to toggle
     */
    public void toggleBookmark(String carParkNumber) {
        Log.d(TAG, "Toggling bookmark for car park: " + carParkNumber);

        settingsManager.toggleBookmark(carParkNumber);
        boolean isNowBookmarked = settingsManager.isBookmarked(carParkNumber);

        // Create event with both carParkNumber and bookmark status
        BookmarkChangeEvent event = new BookmarkChangeEvent(carParkNumber, isNowBookmarked);
        bookmarkLiveData.setValue(event);

        // Refresh the bookmarked car parks list to reflect the change
        refreshBookmarkedCarParks();
    }

    /**
     * @return the total number of bookmarked car parks
     */
    public int getBookmarkCount() {
        return settingsManager.getBookmarkCount();
    }

    // -------------------------
    // Event Classes
    // -------------------------

    /**
     * Event class to contain bookmark change information
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
}