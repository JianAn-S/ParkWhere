package com.jianan.parkwhere.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CarParkDao {
    /**
     * Retrieves the car park entry that matches the given car park number.
     *
     * @param carParkNumber The unique ID of the car park.
     * @return The matching CarPark entity, or null if not found.
     */
    @Query("SELECT * FROM car_park WHERE carParkNumber = :carParkNumber LIMIT 1")
    CarPark getCarParkByNumber(String carParkNumber);

    /**
     * Retrieves car parks within the specified bounding box coordinates.
     * Used as the first filter for nearby car park searches.
     *
     * @param minLat Minimum latitude (Y-axis) of the bounding box
     * @param maxLat Maximum latitude (Y-axis) of the bounding box
     * @param minLon Minimum longitude (X-axis) of the bounding box
     * @param maxLon Maximum longitude (X-axis) of the bounding box
     * @return List of car parks within the bounding box
     */
    @Query("SELECT * FROM car_park WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon")
    List<CarPark> getCarParksInBoundingBox(double minLat, double maxLat, double minLon, double maxLon);

    @Query ("SELECT * FROM car_park")
    List<CarPark> getAllCarParks();
}
