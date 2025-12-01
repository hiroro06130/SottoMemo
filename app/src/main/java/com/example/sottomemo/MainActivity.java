package com.example.sottomemo;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavView;
    private Fragment memoListFragment;
    private Fragment scheduleFragment;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // 通知の許可OK
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ★ 重複していた Toolbar の設定処理を削除しました ★

        checkNotificationPermissions();

        memoListFragment = new MemoListFragment();
        scheduleFragment = new ScheduleFragment();

        bottomNavView = findViewById(R.id.bottom_nav_view);
        bottomNavView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_memo_list) {
                loadFragment(memoListFragment);
                return true;
            } else if (item.getItemId() == R.id.navigation_schedule) {
                loadFragment(scheduleFragment);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            loadFragment(memoListFragment);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.nav_host_fragment, fragment);
        fragmentTransaction.commit();
    }

    private void checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("通知機能の許可が必要です")
                        .setMessage("リマインダー機能を正しく動作させるために、「アラームとリマインダー」の許可をオンにしてください。")
                        .setPositiveButton("設定画面へ", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("後で", null)
                        .show();
            }
        }
    }
}