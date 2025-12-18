package com.example.sottomemo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "memos")
public class Memo {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "excerpt")
    public String excerpt;

    // 以前のコードとの互換性のため、updatedDateを追加
    @ColumnInfo(name = "updated_date")
    public long updatedDate;

    // コンストラクタ
    public Memo(String excerpt, long updatedDate) {
        this.excerpt = excerpt;
        this.updatedDate = updatedDate;
    }

    // Getter / Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getExcerpt() { return excerpt; }

    // ★エラーの原因だったメソッドを追加
    public long getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(long updatedDate) { this.updatedDate = updatedDate; }

    // getTitleメソッドも念のため追加（詳細画面などで使う場合あり）
    public String getTitle() {
        if (excerpt == null) return "";
        // 最初の改行まで、または全文字をタイトルとする簡易実装
        int index = excerpt.indexOf("\n");
        return index > 0 ? excerpt.substring(0, index) : excerpt;
    }
}