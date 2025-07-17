package com.example.sottomemo;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import com.example.sottomemo.api.AiParsedData;
import com.example.sottomemo.api.ApiClient;
import com.example.sottomemo.api.GeminiRequest;
import com.example.sottomemo.api.GeminiResponse;
import com.google.gson.Gson;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import retrofit2.Response;

public class MemoRepository {

    private final MemoDao mMemoDao;
    private final TodoDao mTodoDao;
    private final CategoryDao mCategoryDao;
    private final EventDao mEventDao;
    private final LiveData<List<MemoWithCategories>> mAllMemos;
    private final LiveData<List<Todo>> mAllTodos;
    private final LiveData<List<Category>> mAllCategories;

    MemoRepository(Application application) {
        MemoRoomDatabase db = MemoRoomDatabase.getDatabase(application);
        mMemoDao = db.memoDao();
        mTodoDao = db.todoDao();
        mCategoryDao = db.categoryDao();
        mEventDao = db.eventDao();
        mAllMemos = mMemoDao.getAllMemosWithCategories();
        mAllTodos = mTodoDao.getAllTodos();
        mAllCategories = mCategoryDao.getAllCategories();
    }

    // --- メモ関連（ViewModelから呼び出す処理） ---
    LiveData<List<MemoWithCategories>> getAllMemosWithCategories() { return mAllMemos; }
    LiveData<List<MemoWithCategories>> searchMemosWithCategories(String searchQuery) { return mMemoDao.searchMemosWithCategories(searchQuery); }
    LiveData<MemoWithCategories> getMemoWithCategories(long memoId) { return mMemoDao.getMemoWithCategories(memoId); }
    void delete(Memo memo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.delete(memo)); }
    void deleteMemos(List<Memo> memos) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.deleteMemos(memos)); }


    void insertAndAnalyze(Memo memo, List<Long> categoryIds) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            long memoId = mMemoDao.insert(memo);
            memo.setId(memoId);

            if (categoryIds != null) {
                for (Long categoryId : categoryIds) {
                    MemoCategoryCrossRef crossRef = new MemoCategoryCrossRef();
                    crossRef.memoId = memoId;
                    crossRef.categoryId = categoryId;
                    mMemoDao.insertMemoCategoryCrossRef(crossRef);
                }
            }
            // ★変更：privateメソッドのanalyzeAndSaveを呼び出す
            analyzeAndSave(memo);
        });
    }

    void updateAndAnalyze(Memo memo, List<Long> categoryIds) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            mMemoDao.update(memo);

            mMemoDao.deleteCrossRefsForMemo(memo.getId());
            mEventDao.deleteEventsByMemoId(memo.getId());
            mTodoDao.deleteTodosByMemoId(memo.getId());

            if (categoryIds != null) {
                for (Long categoryId : categoryIds) {
                    MemoCategoryCrossRef crossRef = new MemoCategoryCrossRef();
                    crossRef.memoId = memo.getId();
                    crossRef.categoryId = categoryId;
                    mMemoDao.insertMemoCategoryCrossRef(crossRef);
                }
            }
            // ★変更：privateメソッドのanalyzeAndSaveを呼び出す
            analyzeAndSave(memo);
        });
    }


    // --- ToDo・Event・カテゴリの単純操作 ---
    LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    void insert(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.insert(todo)); }
    void update(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.update(todo)); }
    void delete(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.delete(todo)); }

    LiveData<List<Category>> getAllCategories() { return mCategoryDao.getAllCategories(); }
    void insert(Category category) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mCategoryDao.insert(category)); }

    LiveData<List<Event>> getEventsForDay(long startOfDay, long endOfDay) { return mEventDao.getEventsForDay(startOfDay, endOfDay); }
    void insert(Event event) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mEventDao.insert(event)); }
    void update(Event event) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mEventDao.update(event)); }
    void delete(Event event) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mEventDao.delete(event)); }


    // ★修正：AI解析のコアロジックをprivateメソッドに分離し、デバッグログを完全復元
    private void analyzeAndSave(Memo memo) {
        // このメソッドはバックグラウンドスレッドから呼び出される想定
        SimpleDateFormat promptSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN);
        String today = promptSdf.format(new Date());

        String prompt = "あなたは入力された日本語のテキストを解析し、含まれる「予定(event)」と「ToDo(todo)」を抽出するエキスパートです。" +
                "以下のルールと事例に厳密に従い、JSON形式で出力してください。\n\n" +
                "### 今日の日付\n" +
                today + "\n\n" +
                "### ルール\n" +
                "1. **予定の結合**: 一つの文の中に日付や曜日と具体的な時間の両方が含まれる場合、それらを**必ず一つの予定として結合し、決して二つの予定に分割しないこと。**\n" +
                "2. **文脈の維持**: 文中で一度「明日」や「来週月曜」などの日付が指定された場合、後続の文で新しい日付が指定されるまで、その日付が適用されるものとして解釈すること。\n" +
                "3. **明確な振り分け**: 日付や曜日（「週末」なども含む）が少しでも言及されているものは「events」へ。純粋なタスクのみを「todos」へ振り分けること。\n" +
                "4. **日付の正規化**: 「明日」「来週」などの相対的な日付は、今日の日付を基準に絶対的な「YYYY-MM-DD」形式に変換すること。「週末」は次の土曜日と解釈すること。\n" +
                "5. **Eventの形式**: eventsには「summary」(件名)、「date」(YYYY-MM-DD)、「time」(HH:mm)を必ず含める。時間がなければtimeは「終日」とすること。「午前中」なども「終日」と解釈してよい。\n" +
                "6. **ToDoの形式**: todosには「description」(内容)を必ず含めること。\n" +
                "7. **空の場合**: 該当がなければ `{\"events\":[], \"todos\":[]}` を返すこと。\n" +
                "8. **出力形式**: あなたの回答はJSONオブジェクトのみとし、説明は一切不要とすること。\n\n" +
                "### 事例\n" +
                "入力テキスト: 「明日の15時から鈴木さんと打ち合わせ」\n" +
                "出力JSON: `{\"events\":[{\"summary\":\"鈴木さんと打ち合わせ\",\"date\":\"" + "（明日の日付）" + "\",\"time\":\"15:00\"}],\"todos\":[]}`\n\n" +
                "入力テキスト: 「明日の午前中に企画書を提出。15時から鈴木さんと打ち合わせ。新しいイヤホンを買う」\n" +
                "出力JSON: `{\"events\":[{\"summary\":\"企画書を提出\",\"date\":\"" + "（明日の日付）" + "\",\"time\":\"終日\"}, {\"summary\":\"鈴木さんと打ち合わせ\",\"date\":\"" + "（明日の日付）" + "\",\"time\":\"15:00\"}],\"todos\":[{\"description\":\"新しいイヤホンを買う\"}]}`\n\n" +
                "### 解析対象テキスト\n" +
                "「" + memo.getExcerpt() + "」";

        try {
            Log.d("AI_DEBUG", "--------------------------------------");
            Log.d("AI_DEBUG", "AIへのリクエストを開始します。対象メモID: " + memo.getId());
            Log.d("AI_DEBUG", "Prompt: \n" + prompt);

            Response<GeminiResponse> response = ApiClient.getApiService()
                    .generateContent(BuildConfig.GEMINI_API_KEY, new GeminiRequest(prompt))
                    .execute();

            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().getResponseText();
                Log.d("AI_DEBUG", "AIからの生レスポンス: " + jsonResponse);

                if (jsonResponse == null) {
                    Log.e("AI_DEBUG", "AIからのレスポンスが空です。");
                    return;
                }

                jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
                Log.d("AI_DEBUG", "クリーンアップしたJSON: " + jsonResponse);

                AiParsedData result = new Gson().fromJson(jsonResponse, AiParsedData.class);

                if (result == null) {
                    Log.e("AI_DEBUG", "JSONからAiParsedDataへの変換に失敗しました。");
                    return;
                }

                long memoId = memo.getId(); // 親となるメモのIDを取得

                if (result.todos != null && !result.todos.isEmpty()) {
                    Log.d("AI_DEBUG", "ToDoの処理を開始します。件数: " + result.todos.size());
                    for (AiParsedData.AiTodo aiTodo : result.todos) {
                        if(aiTodo.description != null && !aiTodo.description.isEmpty()){
                            mTodoDao.insert(new Todo(aiTodo.description, false, memoId));
                            Log.d("AI_DEBUG", "ToDoを保存しました: " + aiTodo.description);
                        }
                    }
                } else {
                    Log.d("AI_DEBUG", "ToDoは見つかりませんでした。");
                }

                if (result.events != null && !result.events.isEmpty()) {
                    Log.d("AI_DEBUG", "Eventの処理を開始します。件数: " + result.events.size());
                    for (AiParsedData.AiEvent aiEvent : result.events) {
                        try {
                            if (aiEvent.date == null || aiEvent.summary == null || aiEvent.time == null) {
                                Log.w("AI_DEBUG", "Eventのデータが不完全です。スキップします。: " + aiEvent.summary);
                                continue;
                            }
                            Log.d("AI_DEBUG", "処理中のEvent: " + aiEvent.summary + " / " + aiEvent.date + " / " + aiEvent.time);

                            Date eventDate;
                            String displayTime = aiEvent.time;

                            if ("終日".equals(aiEvent.time)) {
                                Log.d("AI_DEBUG", "終日の予定として処理します。");
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                eventDate = sdf.parse(aiEvent.date);
                            } else {
                                Log.d("AI_DEBUG", "時間指定の予定として処理します。");
                                String dateTimeString = aiEvent.date + " " + aiEvent.time;
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                eventDate = sdf.parse(dateTimeString);
                            }

                            if (eventDate != null) {
                                Event newEvent = new Event(aiEvent.summary, displayTime, eventDate.getTime(), memoId);
                                mEventDao.insert(newEvent);
                                Log.d("AI_DEBUG", "Eventを保存しました: " + newEvent.title);
                            }
                        } catch (ParseException e) {
                            Log.e("AI_DEBUG", "日付/時刻の解析に失敗しました: " + aiEvent.date + " " + aiEvent.time, e);
                        }
                    }
                } else {
                    Log.d("AI_DEBUG", "Eventは見つかりませんでした。");
                }
            } else {
                Log.e("AI_DEBUG", "APIエラー: " + response.code() + " " + response.errorBody().string());
            }
        } catch (Exception e) {
            Log.e("AI_DEBUG", "AI解析処理中に予期せぬエラーが発生しました。", e);
        }
    }
}