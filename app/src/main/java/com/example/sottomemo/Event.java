package com.example.sottomemo;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "event_table")
public class Event {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String time;
    public long eventDate;
    public long memoId;

    // Roomが使うための空のコンストラクタ
    public Event() {}

    // 私たちがプログラムで使うコンストラクタに@Ignoreを付ける
    @Ignore
    public Event(String title, String time, long eventDate, long memoId) {
        this.title = title;
        this.time = time;
        this.eventDate = eventDate;
        this.memoId = memoId;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public long getEventDate() { return eventDate; }
    public void setEventDate(long eventDate) { this.eventDate = eventDate; }
}