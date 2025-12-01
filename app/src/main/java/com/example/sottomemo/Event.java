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

    // ★★★ 1. 個別リマインダー設定を保存する変数を追加 ★★★
    // -2: デフォルト設定を使用, -1: 通知しない, 0以上: 指定した分数
    public int customReminderMinutes = -2;

    public Event() {}

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

    // ★★★ 2. 新しい変数のためのメソッドを追加 ★★★
    public int getCustomReminderMinutes() { return customReminderMinutes; }
    public void setCustomReminderMinutes(int minutes) { this.customReminderMinutes = minutes; }
}