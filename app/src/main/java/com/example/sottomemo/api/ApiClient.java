package com.example.sottomemo.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static Retrofit retrofit = null;

    public static GeminiApiService getApiService() {
        return getClient().create(GeminiApiService.class);
    }

    private static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}