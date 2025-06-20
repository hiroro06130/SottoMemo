package com.example.sottomemo;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

// AndroidViewModelを継承するのがポイント
public class MemoViewModel extends AndroidViewModel {

    private MemoRepository mRepository;
    private final LiveData<List<Memo>> mAllMemos;

    public MemoViewModel (Application application) {
        super(application);
        mRepository = new MemoRepository(application);
        mAllMemos = mRepository.getAllMemos();
    }

    // MainActivityがこのメソッドを使って最新のメモリストを取得する
    LiveData<List<Memo>> getAllMemos() {
        return mAllMemos;
    }

    // MainActivityがこのメソッドを使って新しいメモをデータベースに追加する
    public void insert(Memo memo) {
        mRepository.insert(memo);
    }
}