package com.example.sottomemo;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;

public class MemoRepository {

    private MemoDao mMemoDao;
    private TodoDao mTodoDao;
    private CategoryDao mCategoryDao;
    private LiveData<List<MemoWithCategories>> mAllMemos;
    private LiveData<List<Todo>> mAllTodos;
    private LiveData<List<Category>> mAllCategories;

    MemoRepository(Application application) {
        MemoRoomDatabase db = MemoRoomDatabase.getDatabase(application);
        mMemoDao = db.memoDao();
        mTodoDao = db.todoDao();
        mCategoryDao = db.categoryDao();
        mAllMemos = mMemoDao.getAllMemosWithCategories();
        mAllTodos = mTodoDao.getAllTodos();
        mAllCategories = mCategoryDao.getAllCategories();
    }

    // --- メモ関連 ---
    LiveData<List<MemoWithCategories>> getAllMemosWithCategories() {
        return mAllMemos;
    }

    LiveData<List<MemoWithCategories>> searchMemosWithCategories(String searchQuery) {
        return mMemoDao.searchMemosWithCategories(searchQuery);
    }

    LiveData<MemoWithCategories> getMemoWithCategories(long memoId) {
        return mMemoDao.getMemoWithCategories(memoId);
    }

    void insert(Memo memo, List<Long> categoryIds) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            long memoId = mMemoDao.insert(memo);
            if (categoryIds != null) {
                for (Long categoryId : categoryIds) {
                    MemoCategoryCrossRef crossRef = new MemoCategoryCrossRef();
                    crossRef.memoId = memoId;
                    crossRef.categoryId = categoryId;
                    mMemoDao.insertMemoCategoryCrossRef(crossRef);
                }
            }
        });
    }

    void update(Memo memo, List<Long> categoryIds) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            mMemoDao.update(memo);
            mMemoDao.deleteCrossRefsForMemo(memo.getId());
            if (categoryIds != null) {
                for (Long categoryId : categoryIds) {
                    MemoCategoryCrossRef crossRef = new MemoCategoryCrossRef();
                    crossRef.memoId = memo.getId();
                    crossRef.categoryId = categoryId;
                    mMemoDao.insertMemoCategoryCrossRef(crossRef);
                }
            }
        });
    }

    void delete(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.delete(memo));
    }

    void deleteMemos(List<Memo> memos) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.deleteMemos(memos));
    }


    // --- ToDo関連 ---
    LiveData<List<Todo>> getAllTodos() { return mAllTodos; }

    void update(Todo todo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.update(todo));
    }

    // --- カテゴリ関連 ---
    LiveData<List<Category>> getAllCategories() { return mCategoryDao.getAllCategories(); }

    void insert(Category category) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> mCategoryDao.insert(category));
    }
}