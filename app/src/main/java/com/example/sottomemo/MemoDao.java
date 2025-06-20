package com.example.sottomemo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update; // @Updateのインポートを追加

import java.util.List;

@Dao
public interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Memo memo);

    // このメソッドが不足していました
    @Update
    void update(Memo memo);

    @Query("SELECT * FROM memo_table ORDER BY last_modified DESC")
    LiveData<List<Memo>> getAllMemos();

    @Query("DELETE FROM memo_table")
    void deleteAll();
}