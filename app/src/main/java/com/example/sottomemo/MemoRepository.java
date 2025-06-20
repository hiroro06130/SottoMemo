package com.example.sottomemo;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;

public class MemoRepository {

    private MemoDao mMemoDao;
    private TodoDao mTodoDao;
    private LiveData<List<Memo>> mAllMemos;
    private LiveData<List<Todo>> mAllTodos;

    MemoRepository(Application application) {
        MemoRoomDatabase db = MemoRoomDatabase.getDatabase(application);
        mMemoDao = db.memoDao();
        mTodoDao = db.todoDao();
        mAllMemos = mMemoDao.getAllMemos();
        mAllTodos = mTodoDao.getAllTodos();
    }

    // --- Memo関連 ---
    LiveData<List<Memo>> getAllMemos() { return mAllMemos; }
    void insert(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.insert(memo));
    }
    void update(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.update(memo));
    }
    void delete(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.delete(memo));
    }
    void deleteMemos(List<Memo> memos) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.deleteMemos(memos));
    }

    // --- Todo関連 ---
    LiveData<List<Todo>> getAllTodos() { return mAllTodos; }

    void insert(Todo todo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.insert(todo));
    }

    void update(Todo todo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.update(todo));
    }
}