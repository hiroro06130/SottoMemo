package com.example.sottomemo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

// @Daoは、このインターフェースがDAOであることを示す
@Dao
public interface MemoDao {

    // @Insertは、データを「追加」する命令
    // onConflict = OnConflictStrategy.IGNORE は、もし同じデータがあったら無視するという設定
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Memo memo);

    // @Queryは、データを「取得」する命令。()の中にSQL文を書く
    // "SELECT * FROM memo_table ORDER BY id DESC" は、
    // 「memo_tableから全データをIDの降順（新しいものが先頭）で取得せよ」という意味
    @Query("SELECT * FROM memo_table ORDER BY id DESC")
    LiveData<List<Memo>> getAllMemos(); // LiveDataは、データが変更されたら自動で通知してくれる便利な箱

    // 全てのメモを削除する命令（後で使うかも）
    @Query("DELETE FROM memo_table")
    void deleteAll();
}