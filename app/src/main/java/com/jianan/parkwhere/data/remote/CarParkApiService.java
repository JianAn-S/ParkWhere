package com.jianan.parkwhere.data.remote;

import com.jianan.parkwhere.model.CarParkApiResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface CarParkApiService {
    // Creates a GET request to Car Park API endpoint, this method is named getRawCarParkAvailability()
    @GET("transport/carpark-availability")
    Call<CarParkApiResponse> fetchCarParkAvailability();
}
