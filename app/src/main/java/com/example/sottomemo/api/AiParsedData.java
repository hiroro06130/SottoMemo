package com.example.sottomemo.api;

import java.util.List;

public class AiParsedData {
    public List<AiEvent> events;
    public List<AiTodo> todos;

    public static class AiEvent {
        public String summary;   // 予定のタイトル
        public String date;      // YYYY-MM-DD
        public String time;      // HH:mm または "終日"

        // ★追加: 場所や同席者などの補足情報
        public String location;
        public String people;
    }

    public static class AiTodo {
        public String description; // ToDoの内容

        // ★追加: 締め切りや期限の情報
        public String deadline;
    }
}