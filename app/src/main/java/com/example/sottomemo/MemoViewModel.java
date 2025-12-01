package com.example.sottomemo;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData; // ★復活
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations; // ★復活

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors; // ★復活

public class MemoViewModel extends AndroidViewModel {

    private final MemoRepository mRepository;

    // 基本データ
    private final LiveData<List<Todo>> mAllTodos;
    private final LiveData<List<Category>> mAllCategories;

    // --- ★復活: フィルター用のLiveData ---
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Long> filterCategoryId = new MutableLiveData<>(null);

    // --- ★復活: 最終的にUIに表示するメモリスト ---
    private final LiveData<List<MemoWithCategories>> mFilteredMemos;

    // カレンダー選択日に関連するデータ
    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private final LiveData<List<Event>> eventsForSelectedDate;

    public MemoViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MemoRepository(application);

        mAllTodos = mRepository.getAllTodos();
        mAllCategories = mRepository.getAllCategories();

        // ★★★ フィルターロジックの復活 ★★★
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
        // 「カテゴリで絞り込んだリスト」と「検索クエリ」の両方を監視する
        ((MediatorLiveData<List<MemoWithCategories>>) mFilteredMemos).addSource(memosByCategory, memos -> {
            ((MediatorLiveData<List<MemoWithCategories>>) mFilteredMemos).setValue(filterBySearchQuery(memos, searchQuery.getValue()));
        });
        ((MediatorLiveData<List<MemoWithCategories>>) mFilteredMemos).addSource(searchQuery, query -> {
            ((MediatorLiveData<List<MemoWithCategories>>) mFilteredMemos).setValue(filterBySearchQuery(memosByCategory.getValue(), query));
        });

        // 今日の日付を初期値に設定
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        selectedDate.setValue(today.getTimeInMillis());

        eventsForSelectedDate = Transformations.switchMap(selectedDate, date -> {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(date);
            long startOfDay = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            long endOfDay = cal.getTimeInMillis();
            return mRepository.getEventsForDay(startOfDay, endOfDay);
        });
    }

    // ★★★ 復活: テキスト検索を実行するヘルパーメソッド ★★★
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
    // ★ 復活: UIはこのメソッドを使ってリストを取得する
    public LiveData<List<MemoWithCategories>> getFilteredMemos() { return mFilteredMemos; }

    // ★ 復活: 全件リスト取得（必要な場合のために残す）
    public LiveData<List<MemoWithCategories>> getAllMemosWithCategories() { return mRepository.getAllMemosWithCategories(); }

    // ★ 復活: 検索クエリとカテゴリフィルタの設定メソッド
    public void setSearchQuery(String query) { searchQuery.setValue(query); }
    public void setCategoryFilter(Long categoryId) { filterCategoryId.setValue(categoryId); }

    public void insert(Memo memo, List<Long> categoryIds) {
        mRepository.insertAndAnalyze(memo, categoryIds);
    }

    public void update(Memo memo, List<Long> categoryIds) {
        mRepository.updateAndAnalyze(memo, categoryIds);
    }

    public void delete(Memo memo) {
        mRepository.delete(memo);
    }

    public void deleteMemos(List<Memo> memos) {
        mRepository.deleteMemos(memos);
    }

    // --- ToDo関連 ---
    public LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    public void insert(Todo todo) { mRepository.insert(todo); }
    public void update(Todo todo) { mRepository.update(todo); }
    public void delete(Todo todo) { mRepository.delete(todo); }

    // --- カテゴリ関連 ---
    public LiveData<List<Category>> getAllCategories() { return mAllCategories; }
    public void insert(Category category) { mRepository.insert(category); }
    public void update(Category category) { mRepository.update(category); }
    public void delete(Category category) { mRepository.delete(category); }

    // --- Event関連 ---
    public LiveData<List<Event>> getEventsForSelectedDate() {
        return eventsForSelectedDate;
    }

    public void setSelectedDate(long date) {
        selectedDate.setValue(date);
    }

    public void insert(Event event) { mRepository.insert(event); }
    public void update(Event event) { mRepository.update(event); }
    public void delete(Event event) { mRepository.delete(event); }

    // ★ 新機能: 次の予定を取得するメソッド
    public LiveData<Event> getNextEvent() {
        return mRepository.getNextEvent();
    }
}