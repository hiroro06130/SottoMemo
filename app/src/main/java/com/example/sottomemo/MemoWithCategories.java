package com.example.sottomemo;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;
import java.util.List;

public class MemoWithCategories {

    @Embedded
    public Memo memo;

    @Relation(
            parentColumn = "id", // 親テーブル(Memo)の主キー列
            entityColumn = "categoryId", // 関連付けられるテーブル(Category)の主キー列
            associateBy = @Junction(
                    value = MemoCategoryCrossRef.class,
                    parentColumn = "memoId", // 中間テーブルで親を参照する列
                    entityColumn = "categoryId" // 中間テーブルで子を参照する列
            )
    )
    public List<Category> categories;
}