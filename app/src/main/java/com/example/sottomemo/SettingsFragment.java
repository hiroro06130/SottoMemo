package com.example.sottomemo;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // ★★★ 1. 「テーマ」に安全装置を追加 ★★★
        Preference themePreference = findPreference("theme");
        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener((p, n) -> {
                ThemeManager.applyTheme((String) n);
                return true;
            });
        }

        // ★★★ 2. 「カテゴリ管理」に安全装置を追加 ★★★
        Preference manageCategoriesPreference = findPreference("manage_categories");
        if (manageCategoriesPreference != null) {
            manageCategoriesPreference.setOnPreferenceClickListener(p -> {
                startActivity(new Intent(getActivity(), CategoryManageActivity.class));
                return true;
            });
        }

        // --- デフォルト通知タイミングの処理 (ここは安全でした) ---
        Preference defaultReminderPref = findPreference("default_reminder_time");
        if (defaultReminderPref != null) {
            updateDefaultReminderTimeSummary(defaultReminderPref);
            defaultReminderPref.setOnPreferenceClickListener(preference -> {
                showNumberPickerDialog(preference);
                return true;
            });
        }

        // --- サマリー通知の時刻設定処理 (ここは安全でした) ---
        Preference summaryTimePref = findPreference("summary_notification_time");
        if (summaryTimePref != null) {
            updateSummaryTimeSummary(summaryTimePref);
            summaryTimePref.setOnPreferenceClickListener(p -> {
                showSummaryTimePickerDialog(p);
                return true;
            });
        }
    }

    // (以下のメソッドは変更ありません)

    private void showNumberPickerDialog(Preference preference) {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_number_picker, null);
        final NumberPicker numberPicker = dialogView.findViewById(R.id.number_picker);

        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(120);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int currentValue = prefs.getInt("default_reminder_time", 10);
        numberPicker.setValue(currentValue);

        new AlertDialog.Builder(getContext())
                .setTitle("通知タイミング（分前）")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    int newValue = numberPicker.getValue();
                    prefs.edit().putInt("default_reminder_time", newValue).apply();
                    updateDefaultReminderTimeSummary(preference);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void showSummaryTimePickerDialog(Preference preference) {
        if(getContext() == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int currentHour = prefs.getInt("summary_notification_hour", 8);
        int currentMinute = prefs.getInt("summary_notification_minute", 0);

        new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            prefs.edit()
                    .putInt("summary_notification_hour", hourOfDay)
                    .putInt("summary_notification_minute", minute)
                    .apply();
            updateSummaryTimeSummary(preference);

            ReminderManager.cancelDailySummary(requireContext());
            ReminderManager.scheduleDailySummary(requireContext());

        }, currentHour, currentMinute, DateFormat.is24HourFormat(getContext())).show();
    }

    private void updateDefaultReminderTimeSummary(Preference preference) {
        if (preference != null && getContext() != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int minutes = prefs.getInt("default_reminder_time", 10);
            preference.setSummary(minutes + "分前");
        }
    }

    private void updateSummaryTimeSummary(Preference preference) {
        if(getContext() == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int hour = prefs.getInt("summary_notification_hour", 8);
        int minute = prefs.getInt("summary_notification_minute", 0);
        preference.setSummary(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }
}