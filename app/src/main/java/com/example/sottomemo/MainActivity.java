package com.example.sottomemo;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabNewMemo;

    // ViewModelの変数を宣言
    private MemoViewModel mMemoViewModel;
    // Adapterの変数を宣言
    private MemoAdapter memoAdapter;

    private ActivityResultLauncher<Intent> memoEditLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- RecyclerViewとAdapterのセットアップ ---
        recyclerView = findViewById(R.id.recycler_view_memos);
        // 新しくなったAdapterをここで生成
        memoAdapter = new MemoAdapter();
        recyclerView.setAdapter(memoAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // --- ViewModelのセットアップ ---
        // ViewModelを取得
        mMemoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);

        // ViewModel内のデータ（getAllMemos）を「監視」する
        // データベースのデータに変更があると、中の処理が自動で実行される
        mMemoViewModel.getAllMemos().observe(this, memos -> {
            // 新しいメモのリストをAdapterに渡して、画面を更新
            memoAdapter.submitList(memos);
        });

        // --- 結果受け取りランチャーのセットアップ ---
        memoEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String newMemoText = result.getData().getStringExtra(MemoEditActivity.EXTRA_NEW_MEMO_TEXT);
                        if (newMemoText != null && !newMemoText.isEmpty()) {
                            // 新しいメモをデータベースに追加するよう、ViewModelにお願いする
                            String title = newMemoText.split("\n")[0];
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                            String currentDate = sdf.format(new Date());
                            Memo newMemo = new Memo(title, newMemoText, currentDate);
                            mMemoViewModel.insert(newMemo);
                        }
                    }
                });

        // --- FABのセットアップ ---
        fabNewMemo = findViewById(R.id.fab_new_memo);
        fabNewMemo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MemoEditActivity.class);
            memoEditLauncher.launch(intent);
        });
    }
}