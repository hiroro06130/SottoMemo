package com.example.sottomemo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "memo_table")
public class Memo {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "excerpt")
    private String excerpt;

    // String型からlong型に変更
    @ColumnInfo(name = "last_modified")
    private long lastModified;

    // コンストラクタの引数も変更
    public Memo(String title, String excerpt, long lastModified) {
        this.title = title;
        this.excerpt = excerpt;
        this.lastModified = lastModified;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getExcerpt() {
        return excerpt;
    }

    // ゲッターも変更
    public long getLastModified() {
        return lastModified;
    }
}