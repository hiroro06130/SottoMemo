package com.example.sottomemo;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class MemoViewModel extends AndroidViewModel {

    private final MemoRepository mRepository;
    private final LiveData<List<Todo>> mAllTodos;
    private final LiveData<List<Category>> mAllCategories;

    // --- フィルター用のLiveData ---
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Long> filterCategoryId = new MutableLiveData<>(null); // nullは「全表示」を示す

    // --- 最終的にUIに表示するメモリスト ---
    private final LiveData<List<MemoWithCategories>> mFilteredMemos;

    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private final LiveData<List<Event>> eventsForSelectedDate;

    public MemoViewModel (@NonNull Application application) {
        super(application);
        mRepository = new MemoRepository(application);

        mAllTodos = mRepository.getAllTodos();
        mAllCategories = mRepository.getAllCategories();

        // ★★★ フィルター処理の心臓部 ★★★
        // カテゴリIDが変更されたら、それに紐づくメモのLiveDataに切り替える
        LiveData<List<MemoWithCategories>> memosByCategory = Transformations.switchMap(filterCategoryId, categoryId -> {
            if (categoryId == null) {
                return mRepository.getAllMemosWithCategories(); // 全表示
            } else {
                return mRepository.getMemosByCategoryId(categoryId); // カテゴリで絞り込み
            }
        });

        // 最終的な表示リスト(mFilteredMemos)を準備
        mFilteredMemos = new MediatorLiveData<>();
        // 上記の「カテゴリで絞り込んだリスト」と「検索クエリ」の両方を監視する
        ((MediatorLiveData<List<MemoWithCategories>>) mFilteredMemos).addSource(memosByCategory, memos -> {
            // カテゴリ絞り込み後のリストと、現在の検索クエリで、最終的なリストを作る
            ((MediatorLiveData<List<MemoWithCategories>>) mFilteredMemos).setValue(filterBySearchQuery(memos, searchQuery.getValue()));
        });
        ((MediatorLiveData<List<MemoWithCategories>>) mFilteredMemos).addSource(searchQuery, query -> {
            // 検索クエリと、現在のカテゴリ絞り込みリストで、最終的なリストを作る
            ((MediatorLiveData<List<MemoWithCategories>>) mFilteredMemos).setValue(filterBySearchQuery(memosByCategory.getValue(), query));
        });


        // --- Event関連のロジック (ここは変更なし) ---
        eventsForSelectedDate = Transformations.switchMap(selectedDate, date -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(date);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startOfDay = calendar.getTimeInMillis();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            long endOfDay = calendar.getTimeInMillis();
            return mRepository.getEventsForDay(startOfDay, endOfDay);
        });
    }

    // テキスト検索を実行するヘルパーメソッド
    private List<MemoWithCategories> filterBySearchQuery(List<MemoWithCategories> memos, String query) {
        if (memos == null) {
            return null;
        }
        if (query == null || query.isEmpty()) {
            return memos; // クエリがなければ、そのまま返す
        }
        String lowerCaseQuery = query.toLowerCase();
        return memos.stream()
                .filter(memoWithCategories -> memoWithCategories.memo.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        memoWithCategories.memo.getExcerpt().toLowerCase().contains(lowerCaseQuery))
                .collect(Collectors.toList());
    }


    // --- メモ関連 ---
    public LiveData<List<MemoWithCategories>> getFilteredMemos() { return mFilteredMemos; }
    public void setSearchQuery(String query) { searchQuery.setValue(query); }
    public void setCategoryFilter(Long categoryId) { filterCategoryId.setValue(categoryId); } // ★追加
    public void insert(Memo memo, List<Long> categoryIds) {
        mRepository.insertAndAnalyze(memo, categoryIds);
    }
    public void update(Memo memo, List<Long> categoryIds) {
        mRepository.updateAndAnalyze(memo, categoryIds);
    }
    public void delete(Memo memo) { mRepository.delete(memo); }
    public void deleteMemos(List<Memo> memos) { mRepository.deleteMemos(memos); }

    // --- ToDo関連 ---
    public LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    public void insert(Todo todo) { mRepository.insert(todo); }
    public void update(Todo todo) { mRepository.update(todo); }
    public void delete(Todo todo) { mRepository.delete(todo); }

    // --- カテゴリ関連 ---
    public LiveData<List<Category>> getAllCategories() { return mAllCategories; }
    public void insert(Category category) { mRepository.insert(category); }

    // --- Event関連 ---
    public LiveData<List<Event>> getEventsForSelectedDate() { return eventsForSelectedDate; }
    public void setSelectedDate(long date) { selectedDate.setValue(date); }
    public void insert(Event event) { mRepository.insert(event); }
    public void update(Event event) { mRepository.update(event); }
    public void delete(Event event) { mRepository.delete(event); }
}