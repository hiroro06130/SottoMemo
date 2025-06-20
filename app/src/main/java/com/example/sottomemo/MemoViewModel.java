package com.example.sottomemo;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class MemoViewModel extends AndroidViewModel {

    private MemoRepository mRepository;
    private final LiveData<List<Memo>> mAllMemos;
    private final LiveData<List<Todo>> mAllTodos;

    public MemoViewModel (Application application) {
        super(application);
        mRepository = new MemoRepository(application);
        mAllMemos = mRepository.getAllMemos();
        mAllTodos = mRepository.getAllTodos();
    }

    // --- Memo関連 ---
    LiveData<List<Memo>> getAllMemos() { return mAllMemos; }
    public void insert(Memo memo) { mRepository.insert(memo); }
    public void update(Memo memo) { mRepository.update(memo); }
    public void delete(Memo memo) { mRepository.delete(memo); }
    public void deleteMemos(List<Memo> memos) { mRepository.deleteMemos(memos); }

    // --- Todo関連 ---
    LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    public void update(Todo todo) { mRepository.update(todo); }
}