package com.example.sottomemo;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.sottomemo.api.AiParsedData;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MemoViewModel extends AndroidViewModel {

    private final MemoRepository mRepository;

    // メモ関連
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final LiveData<List<MemoWithCategories>> mFilteredMemos;

    // ToDoとカテゴリ関連
    private final LiveData<List<Todo>> mAllTodos;
    private final LiveData<List<Category>> mAllCategories;

    // Event関連
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

    // --- メモ関連のメソッド ---
    public LiveData<List<MemoWithCategories>> getFilteredMemos() { return mFilteredMemos; }
    public void setSearchQuery(String query) { searchQuery.setValue(query); }
    public LiveData<MemoWithCategories> getMemoWithCategories(long memoId) { return mRepository.getMemoWithCategories(memoId); }
    public void insert(Memo memo, List<Long> categoryIds) {
        mRepository.insert(memo, categoryIds);
        analyzeMemo(memo);
    }
    public void update(Memo memo, List<Long> categoryIds) {
        mRepository.update(memo, categoryIds);
        analyzeMemo(memo);
    }
    public void delete(Memo memo) { mRepository.delete(memo); }
    public void deleteMemos(List<Memo> memos) { mRepository.deleteMemos(memos); }

    // --- ToDo関連のメソッド ---
    public LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    public void insert(Todo todo) { mRepository.insert(todo); }
    public void update(Todo todo) { mRepository.update(todo); }

    // --- カテゴリ関連のメソッド ---
    public LiveData<List<Category>> getAllCategories() { return mAllCategories; }
    public void insert(Category category) { mRepository.insert(category); }

    // --- Event関連のメソッド ---
    public LiveData<List<Event>> getEventsForSelectedDate() { return eventsForSelectedDate; }
    public void setSelectedDate(long date) { selectedDate.setValue(date); }
    public void insert(Event event) { mRepository.insert(event); }

    // --- AI関連のメソッド ---
    private void analyzeMemo(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            AiParsedData result = mRepository.analyzeTextWithAi(memo.getExcerpt());
            if (result == null) { return; }

            if (result.todos != null && !result.todos.isEmpty()) {
                for (AiParsedData.AiTodo aiTodo : result.todos) {
                    insert(new Todo(aiTodo.description, false));
                }
            }
            if (result.events != null && !result.events.isEmpty()) {
                for (AiParsedData.AiEvent aiEvent : result.events) {
                    try {
                        String dateTimeString = aiEvent.date + " " + aiEvent.time;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date eventDate = sdf.parse(dateTimeString);
                        if (eventDate != null) {
                            Event newEvent = new Event(aiEvent.summary, aiEvent.time, eventDate.getTime());
                            insert(newEvent);
                            Log.d("AI_RESPONSE", "Adding Event: " + newEvent.title);
                        }
                    } catch (ParseException e) {
                        Log.e("AI_RESPONSE", "Failed to parse date-time from AI response", e);
                    }
                }
            }
        });
    }
}