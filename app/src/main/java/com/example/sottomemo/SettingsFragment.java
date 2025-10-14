package com.example.sottomemo;

import android.content.Intent;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // --- 1. テーマ設定の処理 ---
        ListPreference themePreference = findPreference("theme");
        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String themeOption = (String) newValue;
                ThemeManager.applyTheme(themeOption);
                return true;
            });
        }

        // --- 2. カテゴリ管理画面を開く処理 ---
        Preference manageCategoriesPreference = findPreference("manage_categories");
        if (manageCategoriesPreference != null) {
            manageCategoriesPreference.setOnPreferenceClickListener(preference -> {
                // CategoryManageActivity を開くための Intent を作成
                Intent intent = new Intent(getActivity(), CategoryManageActivity.class);
                startActivity(intent);
                return true;
            });
        }
    }
}