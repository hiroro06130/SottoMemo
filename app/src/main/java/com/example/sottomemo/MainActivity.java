package com.example.sottomemo;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavView;
    private Fragment memoListFragment;
    private Fragment scheduleFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- ここからが追加するコード ---
        // レイアウトからToolbarを見つける
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        // このToolbarを公式のアクションバーとして設定する
        setSupportActionBar(toolbar);
        // タイトルを設定
        getSupportActionBar().setTitle("Sotto Memo");

        // 各フラグメントのインスタンスを生成
        memoListFragment = new MemoListFragment();
        scheduleFragment = new ScheduleFragment();

        // BottomNavigationViewを見つけて、リスナーをセット
        bottomNavView = findViewById(R.id.bottom_nav_view);
        bottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // タップされたタブのIDに応じて、表示するフラグメントを決定
                if (item.getItemId() == R.id.navigation_memo_list) {
                    loadFragment(memoListFragment);
                    return true;
                } else if (item.getItemId() == R.id.navigation_schedule) {
                    loadFragment(scheduleFragment);
                    return true;
                }
                return false;
            }
        });

        // アプリ起動時に、最初に表示するフラグメントを設定
        if (savedInstanceState == null) {
            loadFragment(memoListFragment);
        }
    }

    // フラグメントを実際にコンテナに表示するためのメソッド
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // R.id.nav_host_fragment というIDのコンテナに、指定されたfragmentを表示する
        fragmentTransaction.replace(R.id.nav_host_fragment, fragment);
        fragmentTransaction.commit();
    }
}