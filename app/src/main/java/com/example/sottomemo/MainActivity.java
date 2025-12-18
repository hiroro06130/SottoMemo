package com.example.sottomemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MemoViewModel mMemoViewModel;
    private MemoAdapter adapter;
    private ActionMode actionMode; // 複数選択モード管理用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        adapter = new MemoAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mMemoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);
        mMemoViewModel.getAllMemosWithCategories().observe(this, memos -> {
            adapter.submitList(memos);
        });

        // クリックリスナーの設定
        adapter.setOnItemClickListener(new MemoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MemoWithCategories memo) {
                if (actionMode != null) {
                    // 複数選択モード中は、クリックで選択トグル
                    toggleSelection(memo.memo.getId());
                } else {
                    // 通常時は編集画面へ
                    Intent intent = new Intent(MainActivity.this, MemoEditActivity.class);
                    intent.putExtra(MemoEditActivity.EXTRA_ID, memo.memo.getId());
                    intent.putExtra(MemoEditActivity.EXTRA_EXCERPT, memo.memo.getExcerpt());
                    // カテゴリIDのリストを渡す
                    java.util.ArrayList<Long> categoryIds = new java.util.ArrayList<>();
                    for (Category cat : memo.categories) {
                        categoryIds.add(cat.categoryId);
                    }
                    intent.putExtra(MemoEditActivity.EXISTING_CATEGORY_IDS, categoryIds);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(MemoWithCategories memo) {
                if (actionMode == null) {
                    // 長押しで複数選択モード開始
                    actionMode = startSupportActionMode(actionModeCallback);
                }
                toggleSelection(memo.memo.getId());
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MemoEditActivity.class);
            startActivity(intent);
        });

        // ★スワイプ削除機能の設定
        setupSwipeToDelete(recyclerView);
    }

    // --- スワイプ削除の実装 ---
    private void setupSwipeToDelete(RecyclerView recyclerView) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // ドラッグ＆ドロップはしない
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                MemoWithCategories memoToDelete = adapter.getMemoAt(position);

                // DBから削除
                mMemoViewModel.delete(memoToDelete.memo);

                // Undo機能付きのスナックバー表示
                Snackbar.make(recyclerView, "メモを削除しました", Snackbar.LENGTH_LONG)
                        .setAction("元に戻す", v -> {
                            // 元に戻す処理 (新規作成と同じ扱いだがID等は自動生成に任せるか、厳密にはinsertし直す)
                            // 簡易的にはinsertAndAnalyzeで戻すが、IDが変わる可能性があるため
                            // 厳密なUndoにはdelete前のIDを保持してinsertが必要。
                            // ここではシンプルに再作成します。
                            List<Long> catIds = new java.util.ArrayList<>();
                            for(Category c : memoToDelete.categories) catIds.add(c.categoryId);
                            mMemoViewModel.insert(memoToDelete.memo, catIds);
                        }).show();
            }
        }).attachToRecyclerView(recyclerView);
    }

    // --- 複数選択モードの制御ロジック ---
    private void toggleSelection(long memoId) {
        adapter.toggleSelection(memoId);
        int count = adapter.getSelectedCount();

        if (count == 0) {
            actionMode.finish(); // 選択が0になったらモード終了
        } else {
            actionMode.setTitle(count + "件選択中");
            actionMode.invalidate();
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // ステップ1で作ったメニューを表示
            getMenuInflater().inflate(R.menu.menu_selection, menu);
            adapter.setMultiSelectMode(true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete_selection) {
                // まとめて削除実行
                List<Memo> memosToDelete = adapter.getSelectedMemos();
                if (!memosToDelete.isEmpty()) {
                    // ダイアログを出してもいいが、ここでは即削除＆Undo
                    mMemoViewModel.deleteMemos(memosToDelete);
                    Toast.makeText(MainActivity.this, memosToDelete.size() + "件のメモを削除しました", Toast.LENGTH_SHORT).show();
                }
                mode.finish(); // モード終了
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.setMultiSelectMode(false);
            actionMode = null;
        }
    };
}}