package com.example.sottomemo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category_table")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public long categoryId;

    public String name;
    public int color; // 色は #FFFFFF のような数値として保存します

    public Category(String name, int color) {
        this.name = name;
        this.color = color;
    }
}
