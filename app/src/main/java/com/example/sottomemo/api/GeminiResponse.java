package com.example.sottomemo.api;

import java.util.List;

public class GeminiResponse {
    List<Candidate> candidates;

    public String getResponseText() {
        if (candidates != null && !candidates.isEmpty()) {
            return candidates.get(0).content.parts.get(0).text;
        }
        return null;
    }
}