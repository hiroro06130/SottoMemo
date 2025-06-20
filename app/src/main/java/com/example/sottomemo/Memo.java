package com.example.sottomemo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// @Entityは、このクラスがデータベースの「テーブル（表）」であることを示す
@Entity(tableName = "memo_table")
public class Memo {

    // @PrimaryKeyは、このフィールドが「主キー（データを一意に識別するための番号）」であることを示す
    // autoGenerate = true は、IDを自動で連番にしてくれる設定
    @PrimaryKey(autoGenerate = true)
    private int id;

    // @ColumnInfoは、テーブルの「カラム（列）」の名前を指定する
    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "excerpt")
    private String excerpt;

    @ColumnInfo(name = "date")
    private String date;

    // コンストラクタ：IDは自動生成なので、コンストラクタには含めない
    public Memo(String title, String excerpt, String date) {
        this.title = title;
        this.excerpt = excerpt;
        this.date = date;
    }

    // --- IDを扱うためのメソッドを追加 ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // --- 既存のゲッターメソッド ---
    public String getTitle() {
        return title;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public String getDate() {
        return date;
    }
}