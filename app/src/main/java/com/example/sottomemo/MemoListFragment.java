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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView; // ★ SearchViewをimport
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MemoListFragment extends Fragment {

    private MemoViewModel mMemoViewModel;
    private MemoAdapter adapter;
    private MaterialCardView cardNextSchedule;
    private TextView textNextScheduleTitle;
    private TextView textNextScheduleTime;

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

        // ★★★ 修正点1: 監視するデータを「フィルター済みリスト」に変更 ★★★
        // getAllMemosWithCategories() ではなく getFilteredMemos() を監視する
        mMemoViewModel.getFilteredMemos().observe(getViewLifecycleOwner(), memos -> {
            adapter.submitList(memos);
        });

        // 次の予定を表示するロジック
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
            startActivity(intent);
        });

        adapter.setOnItemClickListener(memoWithCategories -> {
            Intent intent = new Intent(requireActivity(), MemoEditActivity.class);
            intent.putExtra(MemoEditActivity.EXTRA_ID, memoWithCategories.memo.getId());
            intent.putExtra(MemoEditActivity.EXTRA_EXCERPT, memoWithCategories.memo.getExcerpt());

            if (memoWithCategories.categories != null && !memoWithCategories.categories.isEmpty()) {
                java.util.ArrayList<Long> categoryIds = new java.util.ArrayList<>();
                for (Category cat : memoWithCategories.categories) {
                    categoryIds.add(cat.categoryId);
                }
                intent.putExtra(MemoEditActivity.EXISTING_CATEGORY_IDS, categoryIds);
            }

            startActivity(intent);
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.memo_list_menu, menu);

        // ★★★ 修正点2: 検索機能を有効化 ★★★
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // ViewModelに検索ワードを渡して、リストを絞り込んでもらう
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