package com.jianan.parkwhere.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a list of individual car park data from the API response
 *
 * It consists the car park information, number and last update time for the specific car park
 */
public class CarParkApiData {
    @SerializedName("carpark_number")
    private String carParkNumber;
    @SerializedName("update_datetime")
    private String updateDateTime;
    @SerializedName("carpark_info")
    private List<CarParkInfo> carParkInfo;

    public String getCarParkNumber() {
        return carParkNumber;
    }

    public String getUpdateDateTime() {
        return updateDateTime;
    }

    public List<CarParkInfo> getCarParkInfo() {
        return carParkInfo;
    }
}
