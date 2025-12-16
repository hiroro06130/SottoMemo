package com.example.sottomemo.api;

import java.util.List;

public class GeminiRequest {
    private List<Content> contents;

    public GeminiRequest() {
    }

    public GeminiRequest(String text) {
        this.contents = List.of(new Content(text));
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }
}