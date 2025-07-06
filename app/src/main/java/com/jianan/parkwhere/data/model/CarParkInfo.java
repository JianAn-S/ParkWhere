package com.jianan.parkwhere.model;

import com.google.gson.annotations.SerializedName;

/**
 * Contains detailed information about the lots available in a car park,
 * including total lots, lot type and current availability.
 */
public class CarParkInfo {
    @SerializedName("total_lots")
    private String totalLots;
    @SerializedName("lot_type")
    private String lotType;
    @SerializedName("lots_available")
    private String lotsAvailable;

    public String getTotalLots() {
        return totalLots;
    }

    public String getLotType() {
        return lotType;
    }

    public String getLotsAvailable() {
        return lotsAvailable;
    }
}
