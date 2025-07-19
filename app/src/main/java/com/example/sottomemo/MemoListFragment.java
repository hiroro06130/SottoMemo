package com.example.sottomemo;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
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

// ★★★ 不足していたimport文をここに追加しました ★★★
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
// ★★★ ここまで ★★★

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MemoListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabNewMemo;
    private MemoViewModel mMemoViewModel;
    private MemoAdapter memoAdapter;
    private ActivityResultLauncher<Intent> memoEditLauncher;
    private ActionMode mActionMode;
    private ChipGroup chipGroupFilter;

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
        return inflater.inflate(R.layout.fragment_memo_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_memos);
        fabNewMemo = view.findViewById(R.id.fab_new_memo);
        chipGroupFilter = view.findViewById(R.id.chip_group_filter);
        memoAdapter = new MemoAdapter();
        recyclerView.setAdapter(memoAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        mMemoViewModel = new ViewModelProvider(requireActivity()).get(MemoViewModel.class);
        mMemoViewModel.getFilteredMemos().observe(getViewLifecycleOwner(), memoWithCategories -> {
            memoAdapter.submitList(memoWithCategories);
        });

        setupCategoryFilterChips();

        memoEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("ACTIVITY_RESULT", "結果を受け取りました。Result Code: " + result.getResultCode());
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        long id = data.getLongExtra(MemoEditActivity.EXTRA_ID, -1L);
                        String memoText = data.getStringExtra(MemoEditActivity.EXTRA_EXCERPT);

                        Serializable serializableExtra = data.getSerializableExtra("SELECTED_CATEGORY_IDS");
                        List<Long> selectedCategoryIds = (serializableExtra instanceof List) ? (List<Long>) serializableExtra : new ArrayList<>();

                        if (memoText != null && !memoText.isEmpty()) {
                            String title = memoText.split("\n")[0];
                            long currentTime = System.currentTimeMillis();
                            if (id == -1) {
                                Memo newMemo = new Memo(title, memoText, currentTime);
                                mMemoViewModel.insert(newMemo, selectedCategoryIds);
                                Toast.makeText(requireContext(), "メモが保存されました", Toast.LENGTH_SHORT).show();
                            } else {
                                Memo updatedMemo = new Memo(title, memoText, currentTime);
                                updatedMemo.setId(id);
                                mMemoViewModel.update(updatedMemo, selectedCategoryIds);
                                Toast.makeText(requireContext(), "メモが更新されました", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        fabNewMemo.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), MemoEditActivity.class);
            intent.putExtra("EXISTING_CATEGORY_IDS", new ArrayList<Long>());
            memoEditLauncher.launch(intent);
        });
        setupMenuProvider();
        setupClickListeners();
        setupSwipeToDelete();
    }

    private void setupCategoryFilterChips() {
        mMemoViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            chipGroupFilter.removeAllViews();

            Chip allChip = new Chip(requireContext());
            allChip.setText("すべて");
            allChip.setCheckable(true);
            allChip.setChecked(true);
            allChip.setId(View.generateViewId());
            chipGroupFilter.addView(allChip);

            for (Category category : categories) {
                Chip chip = new Chip(requireContext());
                chip.setText(category.name);
                chip.setTag(category.categoryId);
                chip.setCheckable(true);
                chip.setChipBackgroundColor(ColorStateList.valueOf(category.color).withAlpha(40));
                chip.setChipStrokeWidth(0);
                chip.setTextColor(category.color);
                chip.setId(View.generateViewId());
                chipGroupFilter.addView(chip);
            }

            chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == View.NO_ID) { // 何も選択されていない状態 (API 26+)
                    mMemoViewModel.setCategoryFilter(null);
                    allChip.setChecked(true); // 強制的に「すべて」に戻す
                    return;
                }

                Chip checkedChip = group.findViewById(checkedId);
                if(checkedChip == null) return; // 念のため

                if (checkedChip.getText().toString().equals("すべて")) {
                    mMemoViewModel.setCategoryFilter(null);
                } else {
                    Long categoryId = (Long) checkedChip.getTag();
                    mMemoViewModel.setCategoryFilter(categoryId);
                }
            });
        });
    }


    private void setupMenuProvider() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.memo_list_menu, menu);

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
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_settings) {
                    Intent intent = new Intent(requireActivity(), SettingsActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void setupClickListeners() {
        memoAdapter.setOnItemClickListener(item -> {
            if (mActionMode != null) {
                memoAdapter.toggleSelection(item);
                if(mActionMode != null) mActionMode.invalidate();
            } else {
                Intent intent = new Intent(requireActivity(), MemoEditActivity.class);
                intent.putExtra(MemoEditActivity.EXTRA_ID, item.memo.getId());
                intent.putExtra(MemoEditActivity.EXTRA_EXCERPT, item.memo.getExcerpt());

                ArrayList<Long> categoryIds = new ArrayList<>();
                for(Category cat : item.categories) {
                    categoryIds.add(cat.categoryId);
                }
                intent.putExtra("EXISTING_CATEGORY_IDS", categoryIds);

                memoEditLauncher.launch(intent);
            }
        });

        memoAdapter.setOnItemLongClickListener(item -> {
            if (mActionMode == null) {
                mActionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(mActionModeCallback);
                memoAdapter.toggleSelection(item);
                if (mActionMode != null) mActionMode.invalidate();
            }
        });
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                MemoWithCategories itemToDelete = memoAdapter.getCurrentList().get(position);
                Memo memoToDelete = itemToDelete.memo;

                ArrayList<Long> categoryIds = new ArrayList<>();
                for(Category cat : itemToDelete.categories) {
                    categoryIds.add(cat.categoryId);
                }

                mMemoViewModel.delete(memoToDelete);

                Snackbar.make(requireActivity().findViewById(R.id.memo_list_coordinator_layout), "メモを削除しました", Snackbar.LENGTH_LONG)
                        .setAction("元に戻す", v -> mMemoViewModel.insert(memoToDelete, categoryIds))
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }
}