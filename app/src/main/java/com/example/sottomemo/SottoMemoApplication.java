package com.example.sottomemo;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log; // ★ Logをimport

import androidx.preference.PreferenceManager;
// ★ WorkManager関連のimportは不要になったので削除

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SottoMemoApplication extends Application {

    public static final String EVENT_REMINDER_CHANNEL_ID = "EVENT_REMINDER_CHANNEL";
    public static final String SUMMARY_CHANNEL_ID = "SUMMARY_CHANNEL";

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String themePreference = sharedPreferences.getString("theme", ThemeManager.SYSTEM_DEFAULT);
        ThemeManager.applyTheme(themePreference);

        createNotificationChannels();

        // ★★★ 1. AlarmManagerで毎日のまとめ通知を予約する ★★★
        ReminderManager.scheduleDailySummary(this);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel eventChannel = new NotificationChannel(
                    EVENT_REMINDER_CHANNEL_ID, "予定のリマインダー", NotificationManager.IMPORTANCE_HIGH
            );
            eventChannel.setDescription("登録された予定の時間が近づくと通知します。");

            NotificationChannel summaryChannel = new NotificationChannel(
                    SUMMARY_CHANNEL_ID, "毎日のまとめ", NotificationManager.IMPORTANCE_DEFAULT
            );
            summaryChannel.setDescription("毎朝、その日の予定とToDoの概要を通知します。");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(eventChannel);
                manager.createNotificationChannel(summaryChannel);
            }
        }
    }

    // ★★★ 2. WorkManagerを使った古いscheduleDailySummaryメソッドは不要なので削除 ★★★
}