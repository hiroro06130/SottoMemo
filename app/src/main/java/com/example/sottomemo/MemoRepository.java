package com.example.sottomemo;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;

public class MemoRepository {

    private MemoDao mMemoDao;
    private LiveData<List<Memo>> mAllMemos;

    // コンストラクタ
    MemoRepository(Application application) {
        MemoRoomDatabase db = MemoRoomDatabase.getDatabase(application);
        mMemoDao = db.memoDao();
        mAllMemos = mMemoDao.getAllMemos();
    }

    // 全てのメモを取得するメソッド（UIに公開する用）
    LiveData<List<Memo>> getAllMemos() {
        return mAllMemos;
    }

    // メモを「追加」するメソッド
    // データベースへの書き込みはバックグラウンドで行う必要があるため、
    // データベースクラスが持っているExecutorServiceを使う
    void insert(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            mMemoDao.insert(memo);
        });
    }
}