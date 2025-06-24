package com.example.sottomemo;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class SottoMemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 保存されている設定値を取得
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // "theme"というキーで保存されている値を取得（なければ"system"をデフォルトにする）
        String themePreference = sharedPreferences.getString("theme", ThemeManager.SYSTEM_DEFAULT);
        // テーマを適用
        ThemeManager.applyTheme(themePreference);
    }
}