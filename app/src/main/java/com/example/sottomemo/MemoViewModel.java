package com.example.sottomemo;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

public class MemoViewModel extends AndroidViewModel {

    private MemoRepository mRepository;

    // メモ関連
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final LiveData<List<MemoWithCategories>> mFilteredMemos;

    // ToDoとカテゴリ関連
    private final LiveData<List<Todo>> mAllTodos;
    private final LiveData<List<Category>> mAllCategories;

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

    // --- メモ関連のメソッド ---
    LiveData<List<MemoWithCategories>> getFilteredMemos() {
        return mFilteredMemos;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<MemoWithCategories> getMemoWithCategories(long memoId) {
        return mRepository.getMemoWithCategories(memoId);
    }

    public void insert(Memo memo, List<Long> categoryIds) {
        mRepository.insert(memo, categoryIds);
    }

    public void update(Memo memo, List<Long> categoryIds) {
        mRepository.update(memo, categoryIds);
    }

    public void delete(Memo memo) {
        mRepository.delete(memo);
    }

    public void deleteMemos(List<Memo> memos) {
        mRepository.deleteMemos(memos);
    }


    // --- ToDo関連のメソッド ---
    LiveData<List<Todo>> getAllTodos() {
        return mAllTodos;
    }

    public void update(Todo todo) {
        mRepository.update(todo);
    }


    // --- カテゴリ関連のメソッド ---
    LiveData<List<Category>> getAllCategories() {
        return mAllCategories;
    }

    public void insert(Category category) {
        mRepository.insert(category);
    }
}