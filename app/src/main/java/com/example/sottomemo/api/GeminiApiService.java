package com.example.sottomemo.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    // ★★★ 高速な「gemini-2.5-flash」モデルを指定します ★★★
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest requestBody
    );
}