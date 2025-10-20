package com.jianan.parkwhere.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.libraries.places.api.Places;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jianan.parkwhere.BuildConfig;
import com.jianan.parkwhere.R;
import com.jianan.parkwhere.databinding.ActivityBottomNavHostBinding;
import com.jianan.parkwhere.ui.map.MapViewModel;
import com.jianan.parkwhere.util.ApiScheduler;
import com.jianan.parkwhere.util.ThemeUtils;

/**
 * Activity hosting the bottom navigation and application content
 * Displays a splash screen until API data is loaded, a timeout occurs, or an error happens
 */
public class BottomNavHostActivity extends AppCompatActivity {
    // private static final String TAG = "BottomNavHostActivity";
    private static final long SPLASH_TIMEOUT_MS = 5000;
    private ActivityBottomNavHostBinding binding;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;

    // Boolean flags
    private boolean isDataLoaded = false;
    private boolean isTimeout = false;
    private boolean hasError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        // Keep splash screen visible till either one of the boolean flags becomes true
        splashScreen.setKeepOnScreenCondition(() -> !isDataLoaded && !isTimeout && !hasError);

        super.onCreate(savedInstanceState);

        // Setup timeout handler to allow splash screen to proceed to BottomNavHostActivity if it takes longer than 5s to receive data from the API endpoint
        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutRunnable = () -> {
            if (!isDataLoaded) {
                // Log.d(TAG, "Splash screen timeout reached, proceeding without API information");
                isTimeout = true;
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, SPLASH_TIMEOUT_MS);

        // Switch to the saved application theme after SplashScreen API finishes
        ThemeUtils.applyTheme(this);

        // On Android 15+ (API 35+), edge-to-edge is enabled automatically when targeting SDK 35 or higher
        // For earlier versions, this enables edge-to-edge compatibility on older devices
        EdgeToEdge.enable(this);

        // Initialise Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }

        binding = ActivityBottomNavHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Insets represents the area of the screen occupied by system UI
        // Setup window insets for edge-to-edge
        setupWindowInsets();

        // Setup MapViewModel and observe API data to check if data is received
        setupMapViewModelAndObserver();

        // Setup bottom navigation bar
        setupBottomNavigation();
    }

    /**
     * Sets up the MapViewModel and observes API data
     * Exits splash screen once valid API results are received
     */
    private void setupMapViewModelAndObserver() {
        MapViewModel mapViewModel;
        try {
            mapViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(MapViewModel.class);

            // Initialise API scheduler
            mapViewModel.initialiseApiScheduler();

        } catch (Exception e) {
            // Log.d(TAG, "Error creating MapViewModel");
            hasError = true;
            return;
        }

        // Observe the API Lookup LiveData
        mapViewModel.getCarParkApiLookupLive().observe(this, carParkApiLookup -> {
            // When the observer is first attached, LiveData immediately emits its current value.
            // As carParkApiLookupLive starts out as an empty Map, the first emitted value is empty.
            // Therefore, isEmpty() filters out this initial “empty” state so only real API results are processed.
            // Log.d(TAG, "API data observer triggered");

            if (carParkApiLookup != null && !carParkApiLookup.isEmpty()) {
                // Log.d(TAG, "API data loaded successfully, exiting out of splash screen");
                isDataLoaded = true;
            }
        });
    }

    /**
     * Sets up window insets so that system UI (status bar, navigation bar) does not overlap application's content
     */
    private void setupWindowInsets() {
        // Listener gets called when the status bar or bottom navigation bar is detected
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            // Obtain the insets for the system bars (status bar, bottom navigation bar, etc)
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Apply padding to the root container, bottom navigation bar is already handled hence 0
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            return insets;
        });
    }

    /**
     * Configures bottom navigation bar with navigation controller
     * Connects menu items to top-level destinations
     */
    private void setupBottomNavigation() {
        // Setup Bottom Navigation View
        BottomNavigationView navView = findViewById(R.id.bottom_navigation_view);

        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_map, R.id.navigation_list, R.id.navigation_bookmarks, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        // NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up handler to prevent memory leaks
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }

        binding = null;
        ApiScheduler.getInstance().cleanup();
    }
}
