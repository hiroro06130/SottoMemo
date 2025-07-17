package com.example.sottomemo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TodoDao {
    @Insert
    void insert(Todo todo);

    @Update
    void update(Todo todo);

    @Delete // この行を追加
    void delete(Todo todo);

    @Query("DELETE FROM todo_table WHERE memoId = :memoId")
    void deleteTodosByMemoId(long memoId);

    @Query("SELECT * from todo_table ORDER BY id ASC")
    LiveData<List<Todo>> getAllTodos();
}