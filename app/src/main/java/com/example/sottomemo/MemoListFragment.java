package com.example.sottomemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoListFragment extends Fragment {

    private MemoViewModel mMemoViewModel;
    private MemoAdapter adapter;
    private MaterialCardView cardNextSchedule;
    private TextView textNextScheduleTitle;
    private TextView textNextScheduleTime;

    // 複数選択モード管理用
    private ActionMode actionMode;

    // ★修正: registerForActivityResult はここで定義（初期化）する
    private final ActivityResultLauncher<Intent> memoEditLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    long id = data.getLongExtra(MemoEditActivity.EXTRA_ID, -1L);
                    String memoText = data.getStringExtra(MemoEditActivity.EXTRA_EXCERPT);

                    Serializable serializableExtra = data.getSerializableExtra("SELECTED_CATEGORY_IDS");
                    List<Long> selectedCategoryIds = (serializableExtra instanceof List) ? (List<Long>) serializableExtra : new ArrayList<>();

                    if (memoText != null && !memoText.isEmpty()) {
                        long currentTime = System.currentTimeMillis();
                        // ViewModelはonViewCreatedで初期化されているので、ここでは利用可能
                        if (mMemoViewModel != null) {
                            if (id == -1) {
                                Memo newMemo = new Memo(memoText, currentTime);
                                mMemoViewModel.insert(newMemo, selectedCategoryIds);
                                Toast.makeText(requireContext(), "メモが保存されました", Toast.LENGTH_SHORT).show();
                            } else {
                                Memo updatedMemo = new Memo(memoText, currentTime);
                                updatedMemo.setId(id);
                                mMemoViewModel.update(updatedMemo, selectedCategoryIds);
                                Toast.makeText(requireContext(), "メモが更新されました", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_memo_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        Toolbar toolbar = view.findViewById(R.id.toolbar_memo_list);
        if (toolbar != null && requireActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        }

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_memos);
        adapter = new MemoAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        cardNextSchedule = view.findViewById(R.id.card_next_schedule);
        textNextScheduleTitle = view.findViewById(R.id.text_next_schedule_title);
        textNextScheduleTime = view.findViewById(R.id.text_next_schedule_time);

        mMemoViewModel = new ViewModelProvider(requireActivity()).get(MemoViewModel.class);

        mMemoViewModel.getFilteredMemos().observe(getViewLifecycleOwner(), memos -> {
            adapter.submitList(memos);
        });

        mMemoViewModel.getNextEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                cardNextSchedule.setVisibility(View.VISIBLE);
                textNextScheduleTitle.setText(event.getTitle());
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd(E)", Locale.JAPAN);
                String dateStr = sdf.format(new Date(event.getEventDate()));
                String timeStr = "終日".equals(event.getTime()) ? "終日" : event.getTime();
                textNextScheduleTime.setText(dateStr + " " + timeStr);
            } else {
                cardNextSchedule.setVisibility(View.GONE);
            }
        });

        FloatingActionButton fab = view.findViewById(R.id.fab_add_memo);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), MemoEditActivity.class);
            intent.putExtra(MemoEditActivity.EXISTING_CATEGORY_IDS, new ArrayList<Long>());
            memoEditLauncher.launch(intent);
        });

        // リストアイテムクリック・長押しの処理
        adapter.setOnItemClickListener(new MemoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MemoWithCategories memo) {
                if (actionMode != null) {
                    toggleSelection(memo.memo.getId());
                } else {
                    Intent intent = new Intent(requireActivity(), MemoEditActivity.class);
                    intent.putExtra(MemoEditActivity.EXTRA_ID, memo.memo.getId());
                    intent.putExtra(MemoEditActivity.EXTRA_EXCERPT, memo.memo.getExcerpt());

                    if (memo.categories != null && !memo.categories.isEmpty()) {
                        ArrayList<Long> categoryIds = new ArrayList<>();
                        for (Category cat : memo.categories) {
                            categoryIds.add(cat.categoryId);
                        }
                        intent.putExtra(MemoEditActivity.EXISTING_CATEGORY_IDS, categoryIds);
                    }
                    memoEditLauncher.launch(intent);
                }
            }

            @Override
            public void onItemLongClick(MemoWithCategories memo) {
                if (actionMode == null) {
                    AppCompatActivity activity = (AppCompatActivity) requireActivity();
                    actionMode = activity.startSupportActionMode(actionModeCallback);
                }
                toggleSelection(memo.memo.getId());
            }
        });

        setupSwipeToDelete(recyclerView);
    }

    private void setupSwipeToDelete(RecyclerView recyclerView) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                MemoWithCategories memoToDelete = adapter.getMemoAt(position);

                // 削除実行
                mMemoViewModel.delete(memoToDelete.memo);

                Snackbar.make(recyclerView, "メモを削除しました", Snackbar.LENGTH_LONG)
                        .setAction("元に戻す", v -> {
                            List<Long> catIds = new ArrayList<>();
                            for (Category c : memoToDelete.categories) catIds.add(c.categoryId);
                            mMemoViewModel.insert(memoToDelete.memo, catIds);
                        }).show();
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void toggleSelection(long memoId) {
        adapter.toggleSelection(memoId);
        int count = adapter.getSelectedCount();

        if (count == 0) {
            if (actionMode != null) actionMode.finish();
        } else {
            if (actionMode != null) {
                actionMode.setTitle(count + "件選択中");
                actionMode.invalidate();
            }
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_selection, menu);
            adapter.setMultiSelectMode(true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete_selection) {
                List<Memo> memosToDelete = adapter.getSelectedMemos();
                if (!memosToDelete.isEmpty()) {
                    mMemoViewModel.deleteMemos(memosToDelete);
                    Toast.makeText(requireContext(), memosToDelete.size() + "件を削除しました", Toast.LENGTH_SHORT).show();
                }
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.setMultiSelectMode(false);
            actionMode = null;
            adapter.clearSelection();
        }
    };

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.memo_list_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                mMemoViewModel.setSearchQuery(newText);
                return true;
            }
        });

        // 設定ボタン等は不要であれば削除、または実装
    }
}