package com.example.sottomemo;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;

public class MemoRepository {

    private MemoDao mMemoDao;
    private LiveData<List<Memo>> mAllMemos;

    MemoRepository(Application application) {
        MemoRoomDatabase db = MemoRoomDatabase.getDatabase(application);
        mMemoDao = db.memoDao();
        mAllMemos = mMemoDao.getAllMemos();
    }

    LiveData<List<Memo>> getAllMemos() {
        return mAllMemos;
    }

    void insert(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            mMemoDao.insert(memo);
        });
    }

    void update(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            mMemoDao.update(memo);
        });
    }

    void delete(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            mMemoDao.delete(memo);
        });
    }
}