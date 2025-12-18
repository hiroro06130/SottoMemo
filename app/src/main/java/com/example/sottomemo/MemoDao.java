package com.example.sottomemo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.RoomWarnings; // 追加

import java.util.List;

@Dao
public interface MemoDao {

    // ★修正: 警告を抑制するアノテーションを追加
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("SELECT * FROM memos ORDER BY updated_date DESC")
    LiveData<List<MemoWithCategories>> getAllMemosWithCategories();

    // ★修正: 警告を抑制するアノテーションを追加
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("SELECT * FROM memos INNER JOIN memo_category_cross_ref ON memos.id = memo_category_cross_ref.memoId WHERE memo_category_cross_ref.categoryId = :categoryId ORDER BY updated_date DESC")
    LiveData<List<MemoWithCategories>> getMemosByCategoryId(long categoryId);

    @Query("SELECT * FROM memos WHERE id = :id")
    Memo getMemoById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
}