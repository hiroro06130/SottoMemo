package com.example.sottomemo.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AiParsedData {

    @SerializedName("todos")
    public List<AiTodo> todos;

    @SerializedName("events")
    public List<AiEvent> events;

    public static class AiTodo {
        @SerializedName("description")
        public String description;
    }

    public static class AiEvent {
        @SerializedName("summary")
        public String summary;

        @SerializedName("date")
        public String date;

        @SerializedName("time")
        public String time;
    }
}