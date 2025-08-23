package com.jianan.parkwhere.util;

import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.jianan.parkwhere.R;
import com.jianan.parkwhere.data.local.CarPark;
import com.jianan.parkwhere.data.model.CarParkApiData;
import com.jianan.parkwhere.data.model.CarParkInfo;
import com.jianan.parkwhere.data.preferences.SettingsManager;
import com.jianan.parkwhere.databinding.ItemCarParkBinding;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter for displaying a list of {@link CarPark} items in a RecyclerView
 *
 * Provides partial bind updates using payloads to minimise UI work on updates
 * - PAYLOAD_API_DATA indicates an API data only update
 * - PAYLOAD_LOCATION indicates a location only update
 * - PAYLOAD_BOOKMARK indicates a bookmark state update
 * - PAYLOAD_VEHICLE_TYPE indicates a vehicle type filter update
 *
 * The adapter supports optional distance display that is used by bookmarks and a listener interface for bookmark interactions
 */
public class CarParkAdapter extends ListAdapter<CarPark, CarParkAdapter.CarParkViewHolder>{

    // private static final String TAG = "CarParkAdapter";
    private Map<String, CarParkApiData> carParkApiData;
    private Location userLocation;
    private OnCarParkClickListener clickListener;
    private boolean showDistance = true; // Used to control distance visibility
    private int currentVehicleType = SettingsManager.VEHICLE_BOTH; // Default value

    private static final String PAYLOAD_API_DATA = "api_data";
    private static final String PAYLOAD_LOCATION = "location";
    private static final String PAYLOAD_BOOKMARK = "bookmark";
    private static final String PAYLOAD_VEHICLE_TYPE = "vehicle_type";

    /**
     * Listener interface to handle bookmark click events and bookmark state checks
     *
     * Implementations should handle bookmark toggling and return whether a car park is bookmarked
     */
    public interface OnCarParkClickListener {

        void onBookmarkClick(CarPark carPark);
        boolean isCarParkBookmarked(String carParkNumber);
    }

    /**
     * Construct a CarParkAdapter
     *
     * @param showDistance if true distance will be displayed for each item if a user location is available
     */
    public CarParkAdapter(boolean showDistance) {
        super(DIFF_CALLBACK);
        this.showDistance = showDistance;
    }

    private static final DiffUtil.ItemCallback<CarPark> DIFF_CALLBACK = new DiffUtil.ItemCallback<CarPark>() {
        @Override
        public boolean areItemsTheSame(@NonNull CarPark oldItem, @NonNull CarPark newItem) {
            return oldItem.getCarParkNumber().equals(newItem.getCarParkNumber());
        }

        @Override
        public boolean areContentsTheSame(@NonNull CarPark oldItem, @NonNull CarPark newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public CarParkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCarParkBinding binding = ItemCarParkBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CarParkViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CarParkViewHolder holder, int position) {
        CarPark carPark = getItem(position);
        holder.bind(carPark);
    }

    @Override
    public void onBindViewHolder(@NonNull CarParkViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }

        // Partial update based on payload
        CarPark carPark = getItem(position);
        for (Object payload : payloads) {
            if (PAYLOAD_LOCATION.equals(payload)) {
                holder.updateDistance(carPark);
            } else if (PAYLOAD_API_DATA.equals(payload)) {
                holder.updateApiData(carPark);
            } else if (PAYLOAD_BOOKMARK.equals(payload)) {
                holder.updateBookmarkIcon(carPark);
            } else if (PAYLOAD_VEHICLE_TYPE.equals(payload)) {
                holder.updateApiData(carPark);
            }
        }
    }

    /**
     * Update the adapter with a new API data map and notify only changed items
     *
     * @param apiData map of car park number to {@link CarParkApiData}
     */
    public void updateApiData(Map<String, CarParkApiData> apiData) {
        Map<String, CarParkApiData> oldApiData = this.carParkApiData;
        this.carParkApiData = apiData;

        // If this is the first time API data is received, update everything
        if (oldApiData == null) {
            // PAYLOAD_API_DATA is used to indicate that this is an API only update
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_API_DATA);
            return;
        }

        // Only update items whose API data actually changed
        for (int i = 0; i < getItemCount(); i++) {
            CarPark carPark = getItem(i);
            String carParkId = carPark.getCarParkNumber();

            CarParkApiData oldData = oldApiData.get(carParkId);
            CarParkApiData newData = apiData.get(carParkId);

            if (!isApiDataEqual(oldData, newData)) {
                notifyItemChanged(i, PAYLOAD_API_DATA);
            }
        }
    }

    /**
     * Update the stored user location and notify visible items of distance changes when appropriate
     *
     * @param location the new user {@link Location} or null to clear location
     */
    public void updateUserLocation(Location location) {
        boolean locationChanged = !isLocationEqual(this.userLocation, location);
        this.userLocation = location;

        if (locationChanged && getItemCount() > 0 && showDistance) {
            // PAYLOAD_LOCATION is used to indicate that this is a location only update
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_LOCATION);
        }
    }

    /**
     * Notify the adapter that a specific car park's bookmark state changed
     *
     * @param carParkNumber the car park number whose bookmark state changed
     */
    public void updateBookmarkStatus(String carParkNumber) {
        for (int i = 0; i < getItemCount(); i++) {
            CarPark carPark = getItem(i);
            if (carPark.getCarParkNumber().equals(carParkNumber)) {
                notifyItemChanged(i, PAYLOAD_BOOKMARK);
                break;
            }
        }
    }

    /**
     * Set the click listener for bookmark interactions
     *
     * @param listener implementation of {@link OnCarParkClickListener}
     */
    public void setOnCarParkClickListener(OnCarParkClickListener listener) {
        this.clickListener = listener;
    }

    class CarParkViewHolder extends RecyclerView.ViewHolder {
        private final ItemCarParkBinding binding;

        public CarParkViewHolder(@NonNull ItemCarParkBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.iconBookmark.setOnClickListener(v -> {
                if (clickListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    clickListener.onBookmarkClick(getItem(getBindingAdapterPosition()));
                }
            });
        }

        /**
         * Bind a {@link CarPark} to the item view and update distance API data and bookmark icon
         *
         * @param carPark the CarPark to bind
         */
        public void bind(CarPark carPark) {
            // Set basic car park information
            binding.textCarParkAddress.setText(carPark.getAddress());
            binding.textCarParkNumber.setText(String.format("Car Park %s", carPark.getCarParkNumber()));

            updateDistance(carPark); // Calculate and display distance
            updateApiData(carPark);
            updateBookmarkIcon(carPark);
        }

        /**
         * Update the distance text visibility and value for a bound car park
         *
         * @param carPark the CarPark to calculate distance to
         */
        private void updateDistance(CarPark carPark) {
            if (!showDistance) {
                binding.textDistance.setVisibility(View.GONE);
                return;
            }

            if (userLocation != null) {
                double distanceMeters = GeoUtils.calculateHaversineDistance(
                        userLocation.getLatitude(), userLocation.getLongitude(),
                        carPark.getLatitude(), carPark.getLongitude()
                );

                String distanceText;
                if (distanceMeters < 1000) {
                    distanceText = String.format(Locale.getDefault(), "%.0f m", distanceMeters);
                } else {
                    distanceText = String.format(Locale.getDefault(), "%.1f km", distanceMeters / 1000);
                }
                binding.textDistance.setText(distanceText);
                binding.textDistance.setVisibility(View.VISIBLE);
            } else {
                binding.textDistance.setVisibility(View.GONE);
            }
        }

        /**
         * Update availability UI and last updated time using API data
         *
         * @param carPark the CarPark associated with the api data
         */
        public void updateApiData(CarPark carPark) {
            CarParkApiData apiData = carParkApiData != null ?
                    carParkApiData.get(carPark.getCarParkNumber()) : null;

            handleAvailabilityDisplay(apiData);
            displayLastUpdatedTime(apiData);
        }

        /**
         * Update the bookmark icon based on listener bookmark state
         *
         * @param carPark the CarPark whose bookmark icon should be refreshed
         */
        public void updateBookmarkIcon(CarPark carPark) {
            boolean isBookmarked = false;
            if (clickListener != null) {
                isBookmarked = clickListener.isCarParkBookmarked(carPark.getCarParkNumber());

                if (isBookmarked) {
                    binding.iconBookmark.setImageResource(R.drawable.ic_baseline_bookmark_black_24dp);
                } else {
                    binding.iconBookmark.setImageResource(R.drawable.ic_outline_bookmark_border_black_24dp);
                }
            }
        }

        /**
         * Compute and bind availability information to the UI based on API data and vehicle filter
         *
         * @param apiData the {@link CarParkApiData} for this car park or null if not available
         */
        private void handleAvailabilityDisplay(CarParkApiData apiData) {
            int vehicleType = currentVehicleType;

            // If no API data is available, hide the appropriate UI and return
            if (apiData == null || apiData.getCarParkInfo() == null || apiData.getCarParkInfo().isEmpty()) {
                binding.carAvailabilityContainer.setVisibility(View.GONE);
                binding.motorcycleAvailabilityContainer.setVisibility(View.GONE);
                binding.textLastUpdated.setVisibility(View.GONE);

                binding.textNoInformation.setText("No Data Available");
                binding.noInformationContainer.setVisibility(View.VISIBLE);
                return;
            }

            // Default value
            String carAvailability = "-/-";
            String motorcycleAvailability = "-/-";
            boolean hasCarLots = false;
            boolean hasMotorcycleLots = false;

            // Obtain vehicle type filter
            boolean isCarLotIncluded = vehicleType == SettingsManager.VEHICLE_BOTH || vehicleType == SettingsManager.VEHICLE_CAR;
            boolean isMotorcycleLotIncluded = vehicleType == SettingsManager.VEHICLE_BOTH || vehicleType == SettingsManager.VEHICLE_MOTORCYCLE;

            // Obtain the type(s) of parking lot available in the car park
            for (CarParkInfo carParkInfo : apiData.getCarParkInfo()) {
                String lotType = carParkInfo.getLotType();
                String available = carParkInfo.getLotsAvailable();
                String total = carParkInfo.getTotalLots();
                String availabilityText = available + "/" + total;

                if ("C".equals(lotType)) { // Car parking lots
                    carAvailability = availabilityText;
                    hasCarLots = true;
                } else if ("M".equals(lotType) || "Y".equals(lotType)) { // Motorcycle parking lots (M or Y)
                    motorcycleAvailability = availabilityText;
                    hasMotorcycleLots = true;
                }
            }
            // Show/hide availability text based on vehicle type filter and available lot types
            if (hasCarLots && isCarLotIncluded) {
                binding.textCarAvailability.setText(carAvailability);
            }

            if (hasMotorcycleLots && isMotorcycleLotIncluded) {
                binding.textMotorcycleAvailability.setText(motorcycleAvailability);
            }

            // Show/hide availability containers based on vehicle type filter and available lot types
            binding.carAvailabilityContainer.setVisibility((hasCarLots && isCarLotIncluded) ? View.VISIBLE : View.GONE);
            binding.motorcycleAvailabilityContainer.setVisibility((hasMotorcycleLots && isMotorcycleLotIncluded) ? View.VISIBLE : View.GONE);

            // Check to see if no information container should be visible
            boolean showNoInfoContainer = false;
            String noInfoMessage = "";

            // Show or hide the UI depending on vehicle type filter and whether the car park has specific parking lot types
            if (currentVehicleType == SettingsManager.VEHICLE_CAR && !hasCarLots) {
                showNoInfoContainer = true;
                noInfoMessage = "No Car Park Lots Found";
            } else if (currentVehicleType == SettingsManager.VEHICLE_MOTORCYCLE && !hasMotorcycleLots) {
                showNoInfoContainer = true;
                noInfoMessage = "No Motorcycle Lots Found";
            }

            if (showNoInfoContainer) {
                binding.textNoInformation.setText(noInfoMessage);
                binding.noInformationContainer.setVisibility(View.VISIBLE);
                binding.textLastUpdated.setVisibility(View.GONE);
            } else {
                binding.noInformationContainer.setVisibility(View.GONE);
            }
        }

        /**
         * Display the formatted last updated time based on API timestamp
         *
         * @param apiData the {@link CarParkApiData} containing the update timestamp
         */
        private void displayLastUpdatedTime(CarParkApiData apiData) {
            if (apiData != null && apiData.getUpdateDateTime() != null) {
                String updateTime = formatUpdateTime(apiData.getUpdateDateTime());
                // Log.d(TAG, "Update Time is: " + apiData.getUpdateDateTime());
                binding.textLastUpdated.setText(updateTime);
                binding.textLastUpdated.setVisibility(View.VISIBLE);
            } else {
                binding.textLastUpdated.setVisibility(View.GONE);
            }
        }

        /**
         * Parse and format the API timestamp into a human readable relative time string
         *
         * @param timestamp ISO style timestamp in pattern yyyy-MM-dd'T'HH:mm:ss in Asia/Singapore timezone
         * @return a short relative time string such as Updated just now or Updated 5 minutes ago
         */
        private String formatUpdateTime(String timestamp) {
            try {
                // Parse the timestamp as local Singapore time
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                LocalDateTime localUpdateTime = LocalDateTime.parse(timestamp, formatter);

                // Attach GMT+8 zone (Asia/Singapore)
                ZonedDateTime updateTime = localUpdateTime.atZone(ZoneId.of("Asia/Singapore"));

                // Get current time in GMT+8
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Singapore"));

                Duration duration = Duration.between(updateTime, now);

                long minutes = duration.toMinutes();
                long hours = duration.toHours();
                long days = duration.toDays();

                // Return formatted "Updated x ago"
                if (minutes < 1) {
                    return "Updated just now";
                } else if (minutes < 60) {
                    return String.format("Updated %d minute%s ago", minutes, minutes > 1 ? "s" : "");
                } else if (hours < 24) {
                    return String.format("Updated %d hour%s ago", hours, hours > 1 ? "s" : "");
                } else {
                    return String.format("Updated %d day%s ago", days, days > 1 ? "s" : "");
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "Updated recently";
            }
        }
    }

    /**
     * Update the adapter's vehicle type filter and refresh visible items if the filter changed
     *
     * @param vehicleType one of SettingsManager.VEHICLE_BOTH SettingsManager.VEHICLE_CAR SettingsManager.VEHICLE_MOTORCYCLE
     */
    public void updateVehicleType(int vehicleType) {
        if (this.currentVehicleType != vehicleType) {
            this.currentVehicleType = vehicleType;
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_VEHICLE_TYPE);
        }
    }

    /**
     * Compare two CarParkApiData objects to determine if they represent the same availability state
     *
     * @param data1 first {@link CarParkApiData} or null
     * @param data2 second {@link CarParkApiData} or null
     * @return true if both are null or if update time and all CarParkInfo entries match false otherwise
     */
    private boolean isApiDataEqual(CarParkApiData data1, CarParkApiData data2) {
        if (data1 == null && data2 == null) {
            return true;
        }

        if (data1 == null || data2 == null) {
            return false;
        }

        // Compare update time and car park info
        if (!data1.getUpdateDateTime().equals(data2.getUpdateDateTime())) {
            return false;
        }

        List<CarParkInfo> info1 = data1.getCarParkInfo();
        List<CarParkInfo> info2 = data2.getCarParkInfo();

        if (info1.size() != info2.size()) {
            return false;
        }

        // Simple comparison
        for (int i = 0; i < info1.size(); i++) {
            CarParkInfo cp1 = info1.get(i);
            CarParkInfo cp2 = info2.get(i);
            if (!cp1.getLotType().equals(cp2.getLotType()) ||
                    !cp1.getLotsAvailable().equals(cp2.getLotsAvailable()) ||
                    !cp1.getTotalLots().equals(cp2.getTotalLots())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method to compare locations for near equality
     *
     * @param loc1 first location to compare
     * @param loc2 second location to compare
     * @return true if both locations are null or if the straight line distance between them is less than 1 meter false otherwise
     */
    private boolean isLocationEqual(Location loc1, Location loc2) {
        if (loc1 == null && loc2 == null) {
            return true;
        }
        if (loc1 == null || loc2 == null) {
            return false;
        }

        // Consider locations equal if they're very close (within 1 meter)
        double distance = GeoUtils.calculateHaversineDistance(
                loc1.getLatitude(), loc1.getLongitude(),
                loc2.getLatitude(), loc2.getLongitude());
        return distance < 1.0; // Less than 1 meter difference
    }
}
