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
    long insert(Memo memo);

    @Update
    void update(Memo memo);

    @Delete
    void delete(Memo memo);

    @Delete
    void deleteMemos(List<Memo> memos);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMemoCategoryCrossRef(MemoCategoryCrossRef crossRef);

    @Query("DELETE FROM memo_category_cross_ref WHERE memoId = :memoId")
    void deleteCrossRefsForMemo(long memoId);

    @Transaction
    @Query("SELECT * FROM memo_table ORDER BY last_modified DESC")
    LiveData<List<MemoWithCategories>> getAllMemosWithCategories();

    @Transaction
    @Query("SELECT * FROM memo_table WHERE title LIKE :searchQuery OR excerpt LIKE :searchQuery ORDER BY last_modified DESC")
    LiveData<List<MemoWithCategories>> searchMemosWithCategories(String searchQuery);

    @Transaction
    @Query("SELECT * FROM memo_table WHERE id = :memoId")
    LiveData<MemoWithCategories> getMemoWithCategories(long memoId);

    @Transaction
    @Query("SELECT T.* FROM memo_table AS T " +
            "INNER JOIN memo_category_cross_ref AS C ON T.id = C.memoId " +
            "WHERE C.categoryId = :categoryId " +
            "ORDER BY T.last_modified DESC")
    LiveData<List<MemoWithCategories>> getMemosByCategoryId(long categoryId);

    // ★★★ このメソッドが不足していました ★★★
    @Query("SELECT * FROM memo_table WHERE id = :memoId")
    Memo getMemoById(long memoId);
}