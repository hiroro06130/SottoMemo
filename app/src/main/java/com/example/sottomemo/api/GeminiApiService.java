package com.example.sottomemo.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    // ★ユーザー様のご指定通り「gemini-2.0-flash」で固定します
    // 429エラーが出た場合は「少し待ってから再試行」してください
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest requestBody
    );
}