package com.jianan.parkwhere.util;

import com.jianan.parkwhere.data.local.CarPark;

/**
 * Helper class to sort nearby car parks by distance from a reference point.
 */
public class CarParkDistance {
    private final CarPark carPark;
    private final double distanceMeters;

    public CarParkDistance(CarPark carPark, double distanceMeters) {
        this.carPark = carPark;
        this.distanceMeters = distanceMeters;
    }

    public CarPark getCarPark() {
        return carPark;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }
}
