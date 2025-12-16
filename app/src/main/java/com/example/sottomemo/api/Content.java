package com.example.sottomemo.api;

import java.util.List;

// ★ classの前に public をつけました
public class Content {
    private List<Part> parts;

    // 空のコンストラクタ（重要）
    public Content() {}

    // 以前のコードとの互換性用（念のため）
    public Content(String text) {
        this.parts = List.of(new Part(text));
    }

    // GetterとSetter
    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }
}