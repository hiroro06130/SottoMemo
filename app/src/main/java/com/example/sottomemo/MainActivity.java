package com.example.sottomemo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.bottom_nav_view);

        // 初回起動時はメモリストを表示
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new MemoListFragment())
                    .commit();
        }

        // 下部メニュー（ボトムナビゲーション）の切り替え処理
        if (navView != null) {
            navView.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                // メニューIDに合わせて表示するフラグメントを切り替える
                if (itemId == R.id.navigation_memo_list) {
                    selectedFragment = new MemoListFragment();
                } else if (itemId == R.id.navigation_schedule) {
                    selectedFragment = new ScheduleFragment();
                }
                // ★エラーの原因だった「設定」の分岐を削除しました

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            });
        }
    }
}