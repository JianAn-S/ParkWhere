package com.jianan.parkwhere.ui.settings;

import androidx.lifecycle.ViewModelProvider;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jianan.parkwhere.R;
import com.jianan.parkwhere.databinding.FragmentSettingsBinding;
import com.jianan.parkwhere.ui.map.MapViewModel;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;
    private SettingsViewModel settingsViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialise ViewModel using AndroidViewModel (application context is required by repository instance)
        // The keyword this is used here instead of requireActivity() as the ViewModel only needs to exist for the duration of the fragment's lifecycle
        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        // Initialise the selected button in the button toggle group based on saved preferences
        setupInitialButtonToggleGroupStates();

        // Set up onclick listeners
        setupOnClickListeners();

        // Set device info
        setDeviceInfo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupInitialButtonToggleGroupStates () {
        int currentVehicleType = settingsViewModel.getCurrentVehicleType();
        int currentThemeMode = settingsViewModel.getCurrentThemeMode();

        switch (currentVehicleType) {
            case SettingsViewModel.VEHICLE_CAR:
                binding.vehicleToggleGroup.check(R.id.button_car);
                break;
            case SettingsViewModel.VEHICLE_MOTORCYCLE:
                binding.vehicleToggleGroup.check(R.id.button_motorcycle);
                break;
            default:
                binding.vehicleToggleGroup.check(R.id.button_both);
                break;
        }

        switch(currentThemeMode) {
            case SettingsViewModel.THEME_LIGHT:
                binding.appearanceToggleGroup.check(R.id.button_light);
                break;
            case SettingsViewModel.THEME_DARK:
                binding.appearanceToggleGroup.check(R.id.button_dark);
                break;
            default:
                binding.appearanceToggleGroup.check(R.id.button_system);
                break;
        }
    }

    private void setupOnClickListeners() {
        // Vehicle type button toggle group
        binding.vehicleToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int vehicleType;
                if (checkedId == R.id.button_car) {
                    vehicleType = SettingsViewModel.VEHICLE_CAR;
                } else if (checkedId == R.id.button_motorcycle) {
                    vehicleType = SettingsViewModel.VEHICLE_MOTORCYCLE;
                } else {
                    vehicleType = SettingsViewModel.VEHICLE_BOTH;
                }
                settingsViewModel.setVehicleType(vehicleType);
            }
        });

        // Appearance button toggle group
        binding.appearanceToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int themeMode;
                if (checkedId == R.id.button_light) {
                    themeMode = SettingsViewModel.THEME_LIGHT;
                } else if (checkedId == R.id.button_dark) {
                    themeMode = SettingsViewModel.THEME_DARK;
                } else {
                    themeMode = SettingsViewModel.THEME_SYSTEM;
                }
                settingsViewModel.setThemeMode(themeMode);
            }
        });
    }

    private void setDeviceInfo() {
        String deviceInfo = Build.MANUFACTURER + " " + Build.MODEL;
        String apiInfo = "(API: " + Build.VERSION.SDK_INT + ")";

        binding.textDeviceInfo.setText(deviceInfo);
        binding.textDeviceAPI.setText(apiInfo);
    }
}