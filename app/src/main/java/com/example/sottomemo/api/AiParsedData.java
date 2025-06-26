package com.example.sottomemo.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// AIからのJSONレスポンス全体に対応するクラス
public class AiParsedData {

    @SerializedName("todos")
    public List<AiTodo> todos;
    // TODO: 将来的には List<AiEvent> events; もここに追加する

    // 中のToDoリストのアイテムに対応するクラス
    public static class AiTodo {
        @SerializedName("description")
        public String description;

        @SerializedName("time")
        public String time; // 日付も含む可能性があるためStringで受け取る
    }
}