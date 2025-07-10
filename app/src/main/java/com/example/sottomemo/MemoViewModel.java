package com.example.sottomemo;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import android.util.Log;
import com.example.sottomemo.api.AiParsedData;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
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
    public void insert(Memo memo, List<Long> categoryIds) {
        mRepository.insert(memo, categoryIds);
        mRepository.analyzeAndSave(memo);
    }
    public void update(Memo memo, List<Long> categoryIds) {
        mRepository.update(memo, categoryIds);
        mRepository.analyzeAndSave(memo);
    }
    public void delete(Memo memo) { mRepository.delete(memo); }
    public void deleteMemos(List<Memo> memos) { mRepository.deleteMemos(memos); }

    // --- ToDo関連 ---
    public LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    public void update(Todo todo) { mRepository.update(todo); }

    // --- カテゴリ関連 ---
    public LiveData<List<Category>> getAllCategories() { return mAllCategories; }
    public void insert(Category category) { mRepository.insert(category); }

    // --- Event関連 ---
    public LiveData<List<Event>> getEventsForSelectedDate() { return eventsForSelectedDate; }
    public void setSelectedDate(long date) { selectedDate.setValue(date); }
}