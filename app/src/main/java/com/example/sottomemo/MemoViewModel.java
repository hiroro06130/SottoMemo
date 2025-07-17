package com.example.sottomemo;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class MemoViewModel extends AndroidViewModel {

    private final MemoRepository mRepository;
    private final LiveData<List<Todo>> mAllTodos;
    private final LiveData<List<Category>> mAllCategories;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final LiveData<List<MemoWithCategories>> mFilteredMemos;
    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private final LiveData<List<Event>> eventsForSelectedDate;

    public MemoViewModel (@NonNull Application application) {
        super(application);
        mRepository = new MemoRepository(application);

        mFilteredMemos = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return mRepository.getAllMemosWithCategories();
            } else {
                return mRepository.searchMemosWithCategories("%" + query + "%");
            }
        });

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

        mAllTodos = mRepository.getAllTodos();
        mAllCategories = mRepository.getAllCategories();
    }

    // --- メモ関連 ---
    public LiveData<List<MemoWithCategories>> getFilteredMemos() { return mFilteredMemos; }
    public void setSearchQuery(String query) { searchQuery.setValue(query); }
    public LiveData<MemoWithCategories> getMemoWithCategories(long memoId) { return mRepository.getMemoWithCategories(memoId); }

    // ★修正：insertとupdateに処理を集約
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