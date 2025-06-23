package com.example.sottomemo;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

public class MemoViewModel extends AndroidViewModel {

    private MemoRepository mRepository;
    private final LiveData<List<Todo>> mAllTodos;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    // ここでは変数を宣言するだけ
    private final LiveData<List<Memo>> mFilteredMemos;

    public MemoViewModel (Application application) {
        super(application);
        // まず、mRepositoryを準備する
        mRepository = new MemoRepository(application);
        mAllTodos = mRepository.getAllTodos();

        // mRepositoryの準備ができた後で、それを使ってmFilteredMemosを準備する
        mFilteredMemos = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return mRepository.getAllMemos();
            } else {
                return mRepository.searchMemos("%" + query + "%");
            }
        });
    }

    // --- メモ関連のメソッド ---
    LiveData<List<Memo>> getFilteredMemos() {
        return mFilteredMemos;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void insert(Memo memo) {
        mRepository.insert(memo);
    }

    public void update(Memo memo) {
        mRepository.update(memo);
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
}