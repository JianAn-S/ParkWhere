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
}
