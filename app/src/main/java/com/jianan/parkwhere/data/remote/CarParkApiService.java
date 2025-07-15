package com.jianan.parkwhere.data.remote;

import com.jianan.parkwhere.data.model.CarParkApiResponse;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit API interface for retrieving car park availability data from the remote endpoint.
 */
public interface CarParkApiService {
    /**
     * Creates a GET request to Car Park API endpoint, this method is named fetchCarParkAvailability()
     *
     * @return a {@link Call} object for an asynchronous HTTP request that retrieves {@link CarParkApiResponse} data
     */
    @GET("transport/carpark-availability")
    Call<CarParkApiResponse> fetchCarParkAvailability();
}
