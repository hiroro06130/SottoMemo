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

    public Todo() {}

    @Ignore
    public Todo(String title, boolean isCompleted) {
        this.title = title;
        this.isCompleted = isCompleted;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
}