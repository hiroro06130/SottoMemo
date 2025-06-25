package com.example.sottomemo;

import androidx.room.Entity;
import androidx.room.Index;

// tableNameを明示的に指定し、インデックスも追加
@Entity(tableName = "memo_category_cross_ref",
        primaryKeys = {"memoId", "categoryId"},
        indices = {@Index(value = "categoryId")})
public class MemoCategoryCrossRef {
    public long memoId;
    public long categoryId;
}