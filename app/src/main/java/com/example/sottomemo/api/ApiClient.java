package com.example.sottomemo.api;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient; // ★この行が重要です
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
            // ★★★ ここからが修正箇所です ★★★
            // 通信のタイムアウト時間を60秒に設定した、新しい通信クライアントを作成
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS) // 接続待機時間
                    .readTimeout(60, TimeUnit.SECONDS)    // 読み込み待機時間
                    .writeTimeout(60, TimeUnit.SECONDS)   // 書き込み待機時間
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // 作成したクライアントをセット
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            // ★★★ 修正箇所ここまで ★★★
        }
        return retrofit;
    }
}