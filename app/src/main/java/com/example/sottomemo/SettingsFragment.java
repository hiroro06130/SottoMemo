package com.example.sottomemo;

import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // "theme"というキーを持つListPreferenceを見つける
        ListPreference themePreference = findPreference("theme");
        if (themePreference != null) {
            // 設定値が変更されたときのリスナーをセット
            themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // 新しい値（"light", "dark", "system"のいずれか）を取得
                    String themeOption = (String) newValue;
                    // ThemeManagerを使って、テーマを即座に適用
                    ThemeManager.applyTheme(themeOption);
                    return true; // trueを返すと、変更が保存される
                }
            });
        }
    }
}