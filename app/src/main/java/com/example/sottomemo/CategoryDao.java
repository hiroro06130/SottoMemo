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
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    // ★修正: これを追加しないとエラーになります
    @Query("DELETE FROM category_table")
    void deleteAll();

    @Query("SELECT * FROM category_table ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();
}