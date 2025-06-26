package com.example.sottomemo.api;

import java.util.List;

public class GeminiRequest {
    List<Content> contents;

    public GeminiRequest(String text) {
        this.contents = List.of(new Content(text));
    }
}