package com.example.sottomemo.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    // ★★★ 宛先のモデルを、最新で最も安定している「gemini-1.5-pro-latest」に変更します ★★★
    @POST("v1beta/models/gemini-1.5-pro-latest:generateContent")
    Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest requestBody
    );
}