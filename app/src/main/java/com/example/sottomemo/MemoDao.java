package com.example.sottomemo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Memo memo); // 戻り値をlongに変更

    @Update
    void update(Memo memo);

    @Delete
    void delete(Memo memo);

    @Delete
    void deleteMemos(List<Memo> memos);

    // 中間テーブルにデータを追加するための命令
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMemoCategoryCrossRef(MemoCategoryCrossRef crossRef);

    // @Transactionアノテーションは、2つのクエリをまとめて実行してくれる
    // これにより、メモと、それに関連するカテゴリを一度に取得できる
    @Transaction
    @Query("SELECT * FROM memo_table ORDER BY last_modified DESC")
    LiveData<List<MemoWithCategories>> getAllMemosWithCategories();

    @Transaction
    @Query("SELECT * FROM memo_table WHERE title LIKE :searchQuery OR excerpt LIKE :searchQuery ORDER BY last_modified DESC")
    LiveData<List<MemoWithCategories>> searchMemosWithCategories(String searchQuery);
}