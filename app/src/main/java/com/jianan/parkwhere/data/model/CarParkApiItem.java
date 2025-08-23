package com.jianan.parkwhere.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a single entry in the API response which includes the timestamp
 * when the data was retrieved. It contains a {@code CarParkApiData} object
 */
public class CarParkApiItem {
    private String timestamp;
    @SerializedName("carpark_data")
    private List<CarParkApiData> carParkApiData;

    public String getTimestamp() {
        return timestamp;
    }

    public List<CarParkApiData> getCarParkApiData() {
        return carParkApiData;
    }
}
