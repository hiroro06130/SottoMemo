package com.example.sottomemo.api;

import java.util.List;

class Content {
    List<Part> parts;

    public Content(String text) {
        this.parts = List.of(new Part(text));
    }
}