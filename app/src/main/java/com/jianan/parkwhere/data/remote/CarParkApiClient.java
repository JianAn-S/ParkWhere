package com.jianan.parkwhere.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CarParkApiClient {
    private static final String BASE_URL = "https://api.data.gov.sg/v1/";
    private static Retrofit retrofit;

    /**
     * Returns a singleton instance of {@link CarParkApiService}, which defines the API endpoints for Retrofit to implement
     *
     * @return a configured instance of CarParkApiService
     */
    public static CarParkApiService getService() {
        // Only build the Retrofit instance once (lazy initialisation)
        if (retrofit == null) {

            // Build OkHttpClient and attach the logging interceptor
            OkHttpClient client = new OkHttpClient.Builder().build();

            // Build the Retrofit instance using the base URL and custom OkHttp client
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }

        // Return an implementation of the API interface created by Retrofit
        return retrofit.create(CarParkApiService.class);
    }
}
