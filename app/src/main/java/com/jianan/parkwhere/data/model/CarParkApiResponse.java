package com.jianan.parkwhere.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The top-level response from the Car Park Availability API.
 * It contains a {@code CarParkApiItem} object.
 */
public class CarParkApiResponse {
    @SerializedName("items")
    private List<CarParkApiItem> carParkApiItem;

    public List<CarParkApiItem> getCarParkApiItem() {
        return carParkApiItem;
    }
}
