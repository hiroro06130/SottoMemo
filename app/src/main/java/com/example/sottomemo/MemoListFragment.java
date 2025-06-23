package com.example.sottomemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MemoListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabNewMemo;
    private MemoViewModel mMemoViewModel;
    private MemoAdapter memoAdapter;
    private ActivityResultLauncher<Intent> memoEditLauncher;
    private ActionMode mActionMode;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.contextual_action_menu, menu);
            memoAdapter.startSelectionMode();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            int selectedCount = memoAdapter.getSelectedItemCount();
            mode.setTitle(selectedCount + "個選択中");
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                new AlertDialog.Builder(requireContext())
                        .setMessage(memoAdapter.getSelectedItemCount() + "個のメモを削除しますか？")
                        .setPositiveButton("はい", (dialog, which) -> {
                            mMemoViewModel.deleteMemos(memoAdapter.getSelectedItems());
                            Toast.makeText(requireContext(), "削除しました", Toast.LENGTH_SHORT).show();
                            mode.finish();
                        })
                        .setNegativeButton("いいえ", null)
                        .show();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            memoAdapter.finishSelectionMode();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // このフラグメントがメニューを持つことをシステムに知らせる
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_memo_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_memos);
        fabNewMemo = view.findViewById(R.id.fab_new_memo);
        memoAdapter = new MemoAdapter();
        recyclerView.setAdapter(memoAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        mMemoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);
        mMemoViewModel.getFilteredMemos().observe(getViewLifecycleOwner(), memos -> {
            memoAdapter.submitList(memos);
        });

        memoEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        int id = data.getIntExtra(MemoEditActivity.EXTRA_ID, -1);
                        String memoText = data.getStringExtra(MemoEditActivity.EXTRA_EXCERPT);
                        if (memoText != null && !memoText.isEmpty()) {
                            String title = memoText.split("\n")[0];
                            long currentTime = System.currentTimeMillis();
                            if (id == -1) {
                                Memo newMemo = new Memo(title, memoText, currentTime);
                                mMemoViewModel.insert(newMemo);
                            } else {
                                Memo updatedMemo = new Memo(title, memoText, currentTime);
                                updatedMemo.setId(id);
                                mMemoViewModel.update(updatedMemo);
                            }
                        }
                    }
                });

        fabNewMemo.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), MemoEditActivity.class);
            memoEditLauncher.launch(intent);
        });

        // メニューのセットアップは、より新しい addMenuProvider を使った方法に変更します
        setupMenuProvider();
        setupClickListeners();
        setupSwipeToDelete();
    }

    // フラグメントでのメニューの扱いの、よりモダンな方法
    private void setupMenuProvider() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.memo_list_menu, menu);

                MenuItem searchItem = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) searchItem.getActionView();

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        mMemoViewModel.setSearchQuery(newText);
                        return true;
                    }
                });
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                // ★★★ ここが今回の修正点です ★★★
                if (menuItem.getItemId() == R.id.action_settings) {
                    // 設定アイコンが押されたら、SettingsActivityを開く
                    Intent intent = new Intent(requireActivity(), SettingsActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void setupClickListeners() {
        memoAdapter.setOnItemClickListener(memo -> {
            if (mActionMode != null) {
                memoAdapter.toggleSelection(memo);
                mActionMode.invalidate();
            } else {
                Intent intent = new Intent(requireActivity(), SettingsActivity.class);
                intent.putExtra(MemoEditActivity.EXTRA_ID, memo.getId());
                intent.putExtra(MemoEditActivity.EXTRA_TITLE, memo.getTitle());
                intent.putExtra(MemoEditActivity.EXTRA_EXCERPT, memo.getExcerpt());
                memoEditLauncher.launch(intent);
            }
        });

        memoAdapter.setOnItemLongClickListener(memo -> {
            if (mActionMode == null) {
                mActionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(mActionModeCallback);
                memoAdapter.toggleSelection(memo);
                if (mActionMode != null) {
                    mActionMode.invalidate();
                }
            }
        });
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                memoAdapter.notifyItemChanged(position);
                new AlertDialog.Builder(requireContext())
                        .setTitle("削除の確認")
                        .setMessage("このメモを削除しますか？")
                        .setPositiveButton("はい", (dialog, which) -> {
                            Memo memoToDelete = memoAdapter.getCurrentList().get(position);
                            mMemoViewModel.delete(memoToDelete);
                            Toast.makeText(requireContext(), "メモを削除しました", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("いいえ", null)
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }
}