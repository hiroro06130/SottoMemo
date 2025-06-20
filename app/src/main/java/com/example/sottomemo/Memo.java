package com.example.sottomemo;

public class Memo {

    private String title;
    private String excerpt;
    private String date;

    // コンストラクタ：Memoオブジェクトを生成する際に呼ばれるメソッド
    public Memo(String title, String excerpt, String date) {
        this.title = title;
        this.excerpt = excerpt;
        this.date = date;
    }

    // 各情報を取得するためのメソッド（ゲッター）
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