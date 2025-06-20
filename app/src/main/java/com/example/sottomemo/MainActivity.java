package com.example.sottomemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
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
        mMemoViewModel.getAllMemos().observe(this, memos -> memoAdapter.submitList(memos));

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

        // --- Adapterのクリックリスナーをセットアップ（編集処理） ---
        memoAdapter.setOnItemClickListener(memo -> {
            Intent intent = new Intent(MainActivity.this, MemoEditActivity.class);
            intent.putExtra(MemoEditActivity.EXTRA_ID, memo.getId());
            intent.putExtra(MemoEditActivity.EXTRA_TITLE, memo.getTitle());
            intent.putExtra(MemoEditActivity.EXTRA_EXCERPT, memo.getExcerpt());
            memoEditLauncher.launch(intent);
        });

        // --- スワイプして削除する機能を追加 ---
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // スワイプされたアイテムの位置を取得
                final int position = viewHolder.getAdapterPosition();
                // アダプターに、スワイプされたビューを元の位置に戻すよう通知する
                // これをしないと、キャンセルしたときにアイテムが消えたままになる
                memoAdapter.notifyItemChanged(position);

                // 確認ダイアログを作成して表示
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("削除の確認")
                        .setMessage("このメモを削除しますか？")
                        .setPositiveButton("はい", (dialog, which) -> {
                            // 「はい」が押されたときの処理
                            // その位置にあるメモオブジェクトをAdapterから取得
                            Memo memoToDelete = memoAdapter.getCurrentList().get(position);
                            // ViewModelに削除を依頼
                            mMemoViewModel.delete(memoToDelete);
                            Toast.makeText(MainActivity.this, "メモを削除しました", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("いいえ", null) // 「いいえ」が押されたときは何もしない（ダイアログが閉じるだけ）
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }
}