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
import android.widget.Toast; // ★ Toastをimport

import androidx.activity.result.ActivityResultLauncher; // ★ 追加
import androidx.activity.result.contract.ActivityResultContracts; // ★ 追加
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.Serializable; // ★ 追加
import java.text.SimpleDateFormat;
import java.util.ArrayList; // ★ 追加
import java.util.Date;
import java.util.List; // ★ 追加
import java.util.Locale;

public class MemoListFragment extends Fragment {

    private MemoViewModel mMemoViewModel;
    private MemoAdapter adapter;
    private MaterialCardView cardNextSchedule;
    private TextView textNextScheduleTitle;
    private TextView textNextScheduleTime;

    // ★★★ 復活: メモ保存の結果を受け取るランチャー ★★★
    private ActivityResultLauncher<Intent> memoEditLauncher;

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
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_memos);
        adapter = new MemoAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        cardNextSchedule = view.findViewById(R.id.card_next_schedule);
        textNextScheduleTitle = view.findViewById(R.id.text_next_schedule_title);
        textNextScheduleTime = view.findViewById(R.id.text_next_schedule_time);

        mMemoViewModel = new ViewModelProvider(requireActivity()).get(MemoViewModel.class);

        // ★★★ 復活: 保存処理の実装 ★★★
        memoEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
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

        // データの監視
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

        // FAB（追加ボタン）の処理
        FloatingActionButton fab = view.findViewById(R.id.fab_add_memo);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), MemoEditActivity.class);
            // ★ startActivityではなく、memoEditLauncher.launch を使う
            intent.putExtra(MemoEditActivity.EXISTING_CATEGORY_IDS, new ArrayList<Long>());
            memoEditLauncher.launch(intent);
        });

        // リストアイテムクリック時の処理
        adapter.setOnItemClickListener(memoWithCategories -> {
            Intent intent = new Intent(requireActivity(), MemoEditActivity.class);
            intent.putExtra(MemoEditActivity.EXTRA_ID, memoWithCategories.memo.getId());
            intent.putExtra(MemoEditActivity.EXTRA_EXCERPT, memoWithCategories.memo.getExcerpt());

            if (memoWithCategories.categories != null && !memoWithCategories.categories.isEmpty()) {
                ArrayList<Long> categoryIds = new ArrayList<>();
                for (Category cat : memoWithCategories.categories) {
                    categoryIds.add(cat.categoryId);
                }
                intent.putExtra(MemoEditActivity.EXISTING_CATEGORY_IDS, categoryIds);
            }

            // ★ こちらも memoEditLauncher.launch を使う
            memoEditLauncher.launch(intent);
        });
    }

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

        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        settingsItem.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(requireActivity(), SettingsActivity.class);
            startActivity(intent);
            return true;
        });
    }
}