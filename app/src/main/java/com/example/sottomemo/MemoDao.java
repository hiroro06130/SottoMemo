package com.example.sottomemo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Memo memo);

    @Update
    void update(Memo memo);

    @Delete
    void delete(Memo memo);

    @Delete
    void deleteMemos(List<Memo> memos);

    @Query("SELECT * FROM memo_table ORDER BY last_modified DESC")
    LiveData<List<Memo>> getAllMemos();

    // キーワード(searchQuery)を含むメモを、更新日順で検索する命令
    @Query("SELECT * FROM memo_table WHERE title LIKE :searchQuery OR excerpt LIKE :searchQuery ORDER BY last_modified DESC")
    LiveData<List<Memo>> searchMemos(String searchQuery);

    @Query("DELETE FROM memo_table")
    void deleteAll();
}
