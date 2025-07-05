package com.jianan.parkwhere.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    @NonNull
    public String getCarParkNumber() {
        return carParkNumber;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCarParkType() {
        return carParkType;
    }

    public String getParkingSystemType() {
        return parkingSystemType;
    }

    public String getShortTermParking() {
        return shortTermParking;
    }

    public String getFreeParking() {
        return freeParking;
    }

    public String getNightParking() {
        return nightParking;
    }

    public int getCarParkDecks() {
        return carParkDecks;
    }

    public double getGantryHeight() {
        return gantryHeight;
    }

    public String getCarParkBasement() {
        return carParkBasement;
    }

    public void setCarParkNumber(@NonNull String carParkNumber) {
        this.carParkNumber = carParkNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setCarParkType(String carParkType) {
        this.carParkType = carParkType;
    }

    public void setParkingSystemType(String parkingSystemType) {
        this.parkingSystemType = parkingSystemType;
    }

    public void setShortTermParking(String shortTermParking) {
        this.shortTermParking = shortTermParking;
    }

    public void setFreeParking(String freeParking) {
        this.freeParking = freeParking;
    }

    public void setNightParking(String nightParking) {
        this.nightParking = nightParking;
    }

    public void setCarParkDecks(int carParkDecks) {
        this.carParkDecks = carParkDecks;
    }

    public void setGantryHeight(double gantryHeight) {
        this.gantryHeight = gantryHeight;
    }

    public void setCarParkBasement(String carParkBasement) {
        this.carParkBasement = carParkBasement;
    }
}
