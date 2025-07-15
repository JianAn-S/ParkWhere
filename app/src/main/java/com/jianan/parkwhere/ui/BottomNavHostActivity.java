package com.jianan.parkwhere.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jianan.parkwhere.R;
import com.jianan.parkwhere.databinding.ActivityBottomNavHostBinding;

public class BottomNavHostActivity extends AppCompatActivity {

    private ActivityBottomNavHostBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_ParkWhere); // This line is required for layout inflater

        binding = ActivityBottomNavHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
}
