package com.jianan.parkwhere.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jianan.parkwhere.R;
import com.jianan.parkwhere.databinding.ActivityBottomNavHostBinding;
import com.jianan.parkwhere.ui.map.MapViewModel;

public class BottomNavHostActivity extends AppCompatActivity {
    private static final String TAG = "BottomNavHostActivity";
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

        // Keep splash screen visible till either one of the boolen flags becomes true
        splashScreen.setKeepOnScreenCondition(() -> !isDataLoaded && !isTimeout && !hasError);

        super.onCreate(savedInstanceState);

        // Setup timeout handler to allow splash screen to proceed to BottomNavHostActivity if it takes longer than 10s to receive data from the API endpoint
        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutRunnable = () -> {
            if (!isDataLoaded) {
                Log.d(TAG, "Splash screen timeout reached, proceeding without API information");
                isTimeout = true;
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, SPLASH_TIMEOUT_MS);

        // Switch to the default application theme after SplashScreen API finishes
        setTheme(R.style.Theme_ParkWhere);

        binding = ActivityBottomNavHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup MapViewModel and observe API data
        setupMapViewModelAndObserver();

        // Setup bottom navigation bar
        setupBottomNavigation();
    }

    private void setupMapViewModelAndObserver() {
        MapViewModel mapViewModel;
        try {
            mapViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(MapViewModel.class);
        } catch (Exception e) {
            Log.d(TAG, "Error creating MapViewModel");
            hasError = true;
            return;
        }

        // Observe the API Lookup LiveData
        mapViewModel.getCarParkApiLookupLive().observe(this, carParkApiLookup -> {
            // When the observer is first attached, LiveData immediately emits its current value.
            // As carParkApiLookupLive starts out as an empty Map, the first emitted value is empty.
            // Therefore, isEmpty() filters out this initial “empty” state so only real API results are processed.
            Log.d(TAG, "API data observer triggered");

            if (carParkApiLookup != null && !carParkApiLookup.isEmpty()) {
                Log.d(TAG, "API data loaded successfully, exiting out of splash screen");
                isDataLoaded = true;
            }
        });
    }

    private void setupBottomNavigation() {
        // Setup Bottom Navigation View
        BottomNavigationView navView = findViewById(R.id.bottomNavigationView);

        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_map, R.id.navigation_list, R.id.navigation_bookmarks, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.navHostFragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
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
    }
}
