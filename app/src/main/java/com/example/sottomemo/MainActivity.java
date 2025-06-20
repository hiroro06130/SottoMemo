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
    private MemoViewModel mMemoViewModel;
    private MemoAdapter memoAdapter;
    private ActivityResultLauncher<Intent> memoEditLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- RecyclerViewとAdapterのセットアップ ---
        recyclerView = findViewById(R.id.recycler_view_memos);
        memoAdapter = new MemoAdapter();
        recyclerView.setAdapter(memoAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // --- ViewModelのセットアップ ---
        mMemoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);
        mMemoViewModel.getAllMemos().observe(this, memos -> {
            memoAdapter.submitList(memos);
        });

        // --- 結果受け取りランチャーのセットアップ ---
        memoEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        int id = data.getIntExtra(MemoEditActivity.EXTRA_ID, -1);
                        String memoText = data.getStringExtra(MemoEditActivity.EXTRA_EXCERPT);

                        if (memoText != null && !memoText.isEmpty()) {
                            String title = memoText.split("\n")[0];
                            long currentTime = System.currentTimeMillis();

                            if (id == -1) { // 新規作成
                                Memo newMemo = new Memo(title, memoText, currentTime);
                                mMemoViewModel.insert(newMemo);
                            } else { // 更新
                                Memo updatedMemo = new Memo(title, memoText, currentTime);
                                updatedMemo.setId(id);
                                mMemoViewModel.update(updatedMemo);
                            }
                        }
                    }
                });

        // --- FABのセットアップ（新規作成ボタンの処理） ---
        fabNewMemo = findViewById(R.id.fab_new_memo);
        fabNewMemo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MemoEditActivity.class);
            memoEditLauncher.launch(intent);
        });

        // ★★★ おそらく、この部分が抜けています ★★★
        // --- Adapterのクリックリスナーをセットアップ（編集処理） ---
        memoAdapter.setOnItemClickListener(memo -> {
            Intent intent = new Intent(MainActivity.this, MemoEditActivity.class);
            intent.putExtra(MemoEditActivity.EXTRA_ID, memo.getId());
            intent.putExtra(MemoEditActivity.EXTRA_TITLE, memo.getTitle());
            intent.putExtra(MemoEditActivity.EXTRA_EXCERPT, memo.getExcerpt());
            memoEditLauncher.launch(intent);
        });
    }
}