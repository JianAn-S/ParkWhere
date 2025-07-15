package com.jianan.parkwhere.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity class representing a car park record in the local Room database.
 */
@Entity(tableName = "car_park")
public class CarPark {
    @PrimaryKey
    @NonNull
    private String carParkNumber;
    private String address;
    private double latitude, longitude;
    private String carParkType, parkingSystemType, shortTermParking, freeParking, nightParking;
    private int carParkDecks;
    private double gantryHeight;
    private String carParkBasement;

    /**
     * Returns the unique identifier for this car park.
     *
     * @return the car park number
     */
    @NonNull
    public String getCarParkNumber() {
        return carParkNumber;
    }

    /**
     * Returns the address of this car park.
     *
     * @return the address string
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the latitude of the car park location.
     *
     * @return latitude in decimal degrees
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Returns the longitude of the car park location.
     *
     * @return longitude in decimal degrees
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Returns the type of car park (basement, multi-storey, surface, mechanised,
     * covered, mechanised and surface or surface/multi-storey).
     *
     * @return car park type
     */
    public String getCarParkType() {
        return carParkType;
    }

    /**
     * Returns the parking system type (electronic parking or coupon parking).
     *
     * @return parking system type
     */
    public String getParkingSystemType() {
        return parkingSystemType;
    }

    /**
     * Returns the short-term parking type for this car park.
     * The returned value indicates when short-term (casual) parking is allowed
     * (whole day, 7AM - 10.30PM, 7AM - 7PM or no short term parking)
     *
     * @return allowed short-term parking period.
     */
    public String getShortTermParking() {
        return shortTermParking;
    }

    /**
     * Returns the period during which free parking is available.
     *
     * @return free parking availability period
     */
    public String getFreeParking() {
        return freeParking;
    }

    /**
     * Returns night parking availability.
     *
     * @return night parking type
     */
    public String getNightParking() {
        return nightParking;
    }

    /**
     * Returns the number of levels for this car park.
     *
     * @return number of decks
     */
    public int getCarParkDecks() {
        return carParkDecks;
    }

    /**
     * Returns the gantry height for this car park.
     *
     * @return gantry height in meters
     */
    public double getGantryHeight() {
        return gantryHeight;
    }

    /**
     * Returns whether a basement is available for this car park.
     * "Y" indicates the presence of a basement while "N" indicates that there
     * is no basement.
     *
     * @return "Y" or "N" indicating basement availability
     */
    public String getCarParkBasement() {
        return carParkBasement;
    }

    /**
     * Sets the unique identifier for this car park.
     *
     * @param carParkNumber the unique car park identifier
     */
    public void setCarParkNumber(@NonNull String carParkNumber) {
        this.carParkNumber = carParkNumber;
    }

    /**
     * Sets the address of this car park.
     *
     * @param address the address string
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Sets the latitude of the car park location.
     *
     * @param latitude latitude in decimal degrees
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Sets the longitude of the car park location.
     * @param longitude longitude in decimal degrees
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Sets the type of car park (basement, multi-storey, surface, mechanised,
     * covered, mechanised and surface or surface/multi-storey).
     *
     * @param carParkType type of car park
     */
    public void setCarParkType(String carParkType) {
        this.carParkType = carParkType;
    }

    /**
     * Sets the parking system type (electronic parking or coupon parking).
     *
     * @param parkingSystemType  parking system type
     */
    public void setParkingSystemType(String parkingSystemType) {
        this.parkingSystemType = parkingSystemType;
    }

    /**
     * Sets the short-term parking type for this car park.
     * The returned value indicates when short-term (casual) parking is allowed
     * (whole day, 7AM - 10.30PM, 7AM - 7PM or no short term parking)
     *
     * @param shortTermParking allowed short-term parking period.
     */
    public void setShortTermParking(String shortTermParking) {
        this.shortTermParking = shortTermParking;
    }

    /**
     * Sets the period during which free parking is available.
     *
     * @param freeParking free parking availability period
     */
    public void setFreeParking(String freeParking) {
        this.freeParking = freeParking;
    }

    /**
     * Sets night parking availability.
     *
     * @param nightParking  night parking type
     */
    public void setNightParking(String nightParking) {
        this.nightParking = nightParking;
    }

    /**
     * Sets the number of levels for this car park.
     *
     * @param carParkDecks  number of decks
     */
    public void setCarParkDecks(int carParkDecks) {
        this.carParkDecks = carParkDecks;
    }

    /**
     * Sets the gantry height for this car park.
     *
     * @param gantryHeight gantry height in meters
     */
    public void setGantryHeight(double gantryHeight) {
        this.gantryHeight = gantryHeight;
    }

    /**
     * Sets whether a basement is available for this car park.
     * "Y" indicates the presence of a basement while "N" indicates that there
     * is no basement.
     *
     * @param carParkBasement  "Y" or "N" indicating basement availability
     */
    public void setCarParkBasement(String carParkBasement) {
        this.carParkBasement = carParkBasement;
    }
}
