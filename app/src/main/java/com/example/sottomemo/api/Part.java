package com.example.sottomemo.api;

// ★ classの前に public をつけました
public class Part {
    private String text;

    // 空のコンストラクタ（重要）
    public Part() {}

    // 以前のコードとの互換性用（念のため）
    public Part(String text) {
        this.text = text;
    }

    // GetterとSetter
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}