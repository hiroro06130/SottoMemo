package com.example.sottomemo;

import androidx.room.Entity;
import androidx.room.Index;

// 2つのIDを主キーとする、中間テーブルの定義
@Entity(primaryKeys = {"memoId", "categoryId"},indices = {@Index(value = "categoryId")})
public class MemoCategoryCrossRef {
    public long memoId;
    public long categoryId;
}