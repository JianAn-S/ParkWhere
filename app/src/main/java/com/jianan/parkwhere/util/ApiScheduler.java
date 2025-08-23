package com.jianan.parkwhere.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jianan.parkwhere.data.repository.CarParkRepository;

/**
 * Scheduler responsible for periodically triggering API fetches via {@link CarParkRepository}
 *
 * This class implements a simple singleton scheduler that posts Runnables to a {@link android.os.Handler}
 * bound to the main looper
 *
 * It supports
 * - Periodic fetching at a fixed interval while a UI fragment requires live API data
 * - On demand fetches guarded by a minimum stale threshold to avoid redundant network calls
 * - Safe start stop lifecycle handling and a cleanup method to release resources
 *
 * Timing constants modifiable at compile time
 * - {@link #PERIODIC_FETCH_INTERVAL} how often periodic fetches fire in milliseconds
 * - {@link #MIN_FETCH_INTERVAL} minimum age in milliseconds before an immediate fetch is allowed
 *
 * Threading and lifecycle notes
 * - All scheduling and callback execution uses the main thread's {@link android.os.Looper}
 * - {@link #initialise(CarParkRepository)} must be called before any fetch operations
 * - Call {@link #cleanup()} to remove pending callbacks and release references
 *
 * @see CarParkRepository
 */
public class ApiScheduler {
    // private static final String TAG = "ApiScheduler";

    // Singleton instance
    private static ApiScheduler instance;

    // Time intervals for fetching API data:
    // PERIODIC_FETCH_INTERVAL: Fetch every 2 minutes while on a fragment that needs API data.
    // MIN_FETCH_INTERVAL: Minimum 5 minute gap between fetches when returning to a API dependent fragment
    // For example, when returning back from Settings. If the last update was more than 5 minutes, fetch again.
    private static final long PERIODIC_FETCH_INTERVAL = 2 * 60 * 1000; // 2 minutes
    private static final long MIN_FETCH_INTERVAL = 60 * 1000; // 1 minute

    // Handler and repository
    private Handler mainHandler;
    private CarParkRepository repository;
    private Runnable fetchRunnable;

    // State tracking
    private boolean isSchedulingActive = false;
    private long lastFetchTime = 0;

    private ApiScheduler() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Return the singleton ApiScheduler instance
     *
     * @return the singleton ApiScheduler instance
     */
    public static synchronized ApiScheduler getInstance() {
        if (instance == null) {
            instance = new ApiScheduler();
        }
        return instance;
    }

    /**
     * Initialize the scheduler with reference to repository
     */
    public void initialise(CarParkRepository repository) {
        this.repository = repository;
        // Log.d(TAG, "ApiScheduler initialized with repository");
    }

    /**
     * Start periodic API fetching every 2 minutes
     * Safe to call multiple times, it will not create duplicate schedulers
     */
    public void startPeriodicFetch() {
        if (repository == null) {
            // Log.e(TAG, "Cannot start periodic fetch - repository not initialised");
            return;
        }

        if (isSchedulingActive) {
            // Log.d(TAG, "Periodic fetching already active, ignoring start request");
            return;
        }

        // Log.d(TAG, "Starting periodic API fetch every " + (PERIODIC_FETCH_INTERVAL / (1000 * 60) ) + " minutes");
        isSchedulingActive = true;

        // Create the fetch runnable
        fetchRunnable = new Runnable() {
            @Override
            public void run() {
                if (isSchedulingActive) {
                    // Log.d(TAG, "Periodic fetch triggered");
                    performFetch();
                    // Schedule next fetch
                    mainHandler.postDelayed(this, PERIODIC_FETCH_INTERVAL);
                }
            }
        };

        // Start the periodic fetching
        mainHandler.post(fetchRunnable);
    }

    /**
     * Stop periodic API fetching
     * Called when the application is being destroyed
     */
    public void stopPeriodicFetch() {
        if (!isSchedulingActive) {
            //Log.d(TAG, "Periodic fetching not active, ignoring stop request");
            return;
        }

        // Log.d(TAG, "Stopping periodic API fetch");
        isSchedulingActive = false;

        if (fetchRunnable != null) {
            mainHandler.removeCallbacks(fetchRunnable);
            fetchRunnable = null;
        }
    }

    /**
     * Fetch API data immediately if it's stale (older than MIN_FETCH_INTERVAL)
     * This prevents redundant API calls when users switch fragments frequently
     */
    public void fetchIfStale() {
        if (repository == null) {
            //Log.e(TAG, "Cannot fetch - repository not initialised");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastFetch = currentTime - lastFetchTime;

        if (timeSinceLastFetch > MIN_FETCH_INTERVAL) {
            // Log.d(TAG, "Data is stale (" + (timeSinceLastFetch / (1000)) + "s old), fetching fresh data");
            performFetch();
        } else {
            // Log.d(TAG, "Data is fresh (" + (timeSinceLastFetch / (1000 * 60)) + "m old), skipping fetch");
        }
    }

    /**
     * Perform the actual API fetch and update the last fetch timestamp
     *
     * This delegates to {@link CarParkRepository#fetchApi()} to perform the network request
     */
    private void performFetch() {
        // Log.d(TAG, "Performing API fetch");
        lastFetchTime = System.currentTimeMillis();
        repository.fetchApi();
    }

    /**
     * Clean up scheduler resources and release references
     *
     * After calling this method the scheduler will not be usable until reinitialized
     */
    public void cleanup() {
        // Log.d(TAG, "Cleaning up ApiScheduler");
        stopPeriodicFetch();

        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }

        repository = null;
        instance = null;
    }
}
