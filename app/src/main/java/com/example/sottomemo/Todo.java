package com.example.sottomemo;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "todo_table")
public class Todo {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private boolean isCompleted;
    private long memoId;

    // ★★★★★★★★★ 1. 完了時刻を保存する変数を追加 ★★★★★★★★★
    private long completionTimestamp;

    public Todo() {}

    @Ignore
    public Todo(String title, boolean isCompleted, long memoId) {
        this.title = title;
        this.isCompleted = isCompleted;
        this.memoId = memoId;
        this.completionTimestamp = 0; // 初期値は0
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
    public long getMemoId() { return memoId; }
    public void setMemoId(long memoId) { this.memoId = memoId; }

    // ★★★★★★★★★ 2. 新しい変数のためのメソッドを追加 ★★★★★★★★★
    public long getCompletionTimestamp() { return completionTimestamp; }
    public void setCompletionTimestamp(long completionTimestamp) { this.completionTimestamp = completionTimestamp; }
}