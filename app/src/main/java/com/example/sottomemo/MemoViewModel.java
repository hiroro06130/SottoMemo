package com.example.sottomemo;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import android.util.Log;
import com.example.sottomemo.api.AiParsedData;
import java.util.List;
import java.util.ArrayList;

public class MemoViewModel extends AndroidViewModel {

    private MemoRepository mRepository;
    private final LiveData<List<Todo>> mAllTodos;
    private final LiveData<List<Category>> mAllCategories;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final LiveData<List<MemoWithCategories>> mFilteredMemos;

    public MemoViewModel (Application application) {
        super(application);
        mRepository = new MemoRepository(application);

        mFilteredMemos = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return mRepository.getAllMemosWithCategories();
            } else {
                return mRepository.searchMemosWithCategories("%" + query + "%");
            }
        });
        mAllTodos = mRepository.getAllTodos();
        mAllCategories = mRepository.getAllCategories();
    }

    // --- メモ関連 ---
    LiveData<List<MemoWithCategories>> getFilteredMemos() { return mFilteredMemos; }
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

    // --- ToDo関連 ---
    LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    public void insert(Todo todo) { mRepository.insert(todo); }
    public void update(Todo todo) { mRepository.update(todo); }

    // --- カテゴリ関連 ---
    LiveData<List<Category>> getAllCategories() { return mAllCategories; }
    public void insert(Category category) { mRepository.insert(category); }

    // --- Event関連 ---
    public LiveData<List<Event>> getEventsForDay(long startOfDay, long endOfDay) { return mRepository.getEventsForDay(startOfDay, endOfDay); }
    public void insert(Event event) { mRepository.insert(event); }

    // --- AI関連 ---
    private void analyzeMemo(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            AiParsedData result = mRepository.analyzeTextWithAi(memo.getExcerpt());
            if (result != null && result.todos != null && !result.todos.isEmpty()) {
                for (AiParsedData.AiTodo aiTodo : result.todos) {
                    insert(new Todo(aiTodo.description, false));
                }
            }
            // TODO: Eventの処理を追加
        });
    }
}