package com.example.sottomemo;

// これはデータベース用のクラスではないので、アノテーションは不要です
public class Event {
    private final long id;
    private final String time;
    private final String title;

    public Event(long id, String time, String title) {
        this.id = id;
        this.time = time;
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public String getTime() {
        return time;
    }

    public String getTitle() {
        return title;
    }
}
