package com.example.sottomemo;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.sottomemo.api.AiParsedData;
import com.example.sottomemo.api.ApiClient;
import com.example.sottomemo.api.GeminiRequest;
import com.example.sottomemo.api.GeminiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import retrofit2.Response;

public class MemoRepository {

    private static final String AI_DEBUG_TAG = "AI_ANALYSIS";

    private final MemoDao mMemoDao;
    private final TodoDao mTodoDao;
    private final CategoryDao mCategoryDao;
    private final EventDao mEventDao;
    private final LiveData<List<MemoWithCategories>> mAllMemos;
    private final LiveData<List<Todo>> mAllTodos;
    private final LiveData<List<Category>> mAllCategories;
    private final WorkManager mWorkManager;
    private final Application mApplication;

    MemoRepository(Application application) {
        mApplication = application;
        MemoRoomDatabase db = MemoRoomDatabase.getDatabase(application);
        mMemoDao = db.memoDao();
        mTodoDao = db.todoDao();
        mCategoryDao = db.categoryDao();
        mEventDao = db.eventDao();
        mAllMemos = mMemoDao.getAllMemosWithCategories();
        mAllTodos = mTodoDao.getAllTodos();
        mAllCategories = mCategoryDao.getAllCategories();
        mWorkManager = WorkManager.getInstance(application);
    }

    // --- メモ関連（ViewModelから呼び出す処理） ---
    LiveData<List<MemoWithCategories>> getAllMemosWithCategories() { return mAllMemos; }
    LiveData<List<MemoWithCategories>> getMemosByCategoryId(long categoryId) { return mMemoDao.getMemosByCategoryId(categoryId); }
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
            startAiAnalysisWorker(memoId);
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
            startAiAnalysisWorker(memo.getId());
        });
    }

    private void startAiAnalysisWorker(long memoId) {
        Data inputData = new Data.Builder()
                .putLong(AiAnalysisWorker.KEY_MEMO_ID, memoId)
                .build();
        OneTimeWorkRequest analysisWorkRequest = new OneTimeWorkRequest.Builder(AiAnalysisWorker.class)
                .setInputData(inputData)
                .build();
        mWorkManager.enqueue(analysisWorkRequest);
        Log.d("MemoRepository", "AiAnalysisWorkerをキューに追加しました。MemoID: " + memoId);
    }

    // --- ToDo・Event・カテゴリの単純操作 ---
    LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    void insert(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.insert(todo)); }
    void update(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.update(todo)); }
    void delete(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.delete(todo)); }

    LiveData<List<Category>> getAllCategories() { return mCategoryDao.getAllCategories(); }
    void insert(Category category) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mCategoryDao.insert(category)); }

    void update(Category category) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mCategoryDao.update(category)); }
    void delete(Category category) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mCategoryDao.delete(category)); }

    LiveData<List<Event>> getEventsForDay(long startOfDay, long endOfDay) { return mEventDao.getEventsForDay(startOfDay, endOfDay); }
    void insert(Event event) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mEventDao.insert(event)); }
    void update(Event event) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mEventDao.update(event)); }
    void delete(Event event) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mEventDao.delete(event)); }


    // --- AI解析のコアロジック（Workerから呼び出される） ---
    public void analyzeAndSaveFromWorker(long memoId) {
        Log.d(AI_DEBUG_TAG, "==================================================");
        Log.d(AI_DEBUG_TAG, "AI解析処理を開始します。対象メモID: " + memoId);
        Log.d(AI_DEBUG_TAG, "==================================================");

        Memo memo = mMemoDao.getMemoById(memoId);
        if (memo == null) {
            Log.e(AI_DEBUG_TAG, "処理中断: 指定されたIDのメモが見つかりませんでした。 MemoID: " + memoId);
            return;
        }
        Log.d(AI_DEBUG_TAG, "解析対象のメモを取得しました: " + memo.getExcerpt());


        SimpleDateFormat promptSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN);
        String today = promptSdf.format(new Date());

        // ★★★★★★★★★★★★★★★★★★★ ここからがAIへの命令文の修正箇所です ★★★★★★★★★★★★★★★★★★★
        String prompt = "あなたは入力された日本語のテキストを解析し、含まれる「予定(event)」と「ToDo(todo)」を抽出するエキスパートです。" +
                "以下のルールと事例に厳密に従い、JSON形式で出力してください。\n\n" +
                "### 今日の日付\n" +
                today + "\n\n" +
                "### ルール\n" +
                "1. **予定の定義**: カレンダーの特定の日時を確保するような、具体的な日付（例: 「明日」「10月25日」）や時刻（例: 「15時」「午前中」）が指定されているものを「events」へ分類すること。\n" +
                "2. **ToDoの定義**: 純粋なタスク、または「今週中」「近いうちに」のような**曖昧な期限**を持つものは「todos」へ分類すること。\n" +
                "3. **文脈の維持**: 文中で一度「明日」や「来週月曜」などの日付が指定された場合、後続の文で新しい日付が指定されるまで、その日付が適用されるものとして解釈すること。\n" +
                "4. **日付の正規化**: 「明日」「来週」などの相対的な日付は、今日の日付を基準に絶対的な「YYYY-MM-DD」形式に変換すること。「週末」は次の土曜日と解釈すること。\n" +
                "5. **Eventの形式**: eventsには「summary」(件名)、「date」(YYYY-MM-DD)、「time」(HH:mm)を必ず含める。時間がなければtimeは「終日」とすること。「午前中」なども「終日」と解釈してよい。\n" +
                "6. **ToDoの形式**: todosには「description」(内容)を必ず含めること。\n" +
                "7. **空の場合**: 該当がなければ `{\"events\":[], \"todos\":[]}` を返すこと。\n" +
                "8. **出力形式**: あなたの回答はJSONオブジェクトのみとし、説明は一切不要とすること。\n\n" +
                "### 事例\n" +
                "入力テキスト: 「明日の15時から鈴木さんと打ち合わせ。今週中にクリーニングを取りに行く」\n" +
                "出力JSON: `{\"events\":[{\"summary\":\"鈴木さんと打ち合わせ\",\"date\":\"" + "（明日の日付）" + "\",\"time\":\"15:00\"}],\"todos\":[{\"description\":\"クリーニングを取りに行く\"}]}`\n\n" +
                "入力テキスト: 「明日の午前中に企画書を提出。15時から鈴木さんと打ち合わせ。新しいイヤホンを買う」\n" +
                "出力JSON: `{\"events\":[{\"summary\":\"企画書を提出\",\"date\":\"" + "（明日の日付）" + "\",\"time\":\"終日\"}, {\"summary\":\"鈴木さんと打ち合わせ\",\"date\":\"" + "（明日の日付）" + "\",\"time\":\"15:00\"}],\"todos\":[{\"description\":\"新しいイヤホンを買う\"}]}`\n\n" +
                "### 解析対象テキスト\n" +
                "「" + memo.getExcerpt() + "」";
        // ★★★★★★★★★★★★★★★★★★★ AIへの命令文の修正ここまで ★★★★★★★★★★★★★★★★★★★

        Log.d(AI_DEBUG_TAG, "[リクエスト] AIへのプロンプト:\n" + prompt);

        ApiClient.getApiService()
                .generateContent(BuildConfig.GEMINI_API_KEY, new GeminiRequest(prompt))
                .enqueue(new retrofit2.Callback<GeminiResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<GeminiResponse> call, Response<GeminiResponse> response) {
                        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
                            if (response.isSuccessful() && response.body() != null) {
                                String jsonResponse = response.body().getResponseText();
                                Log.d(AI_DEBUG_TAG, "[レスポンス] AIからの生JSON:\n" + jsonResponse);

                                if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                                    Log.e(AI_DEBUG_TAG, "処理中断: AIからのレスポンスが空です。");
                                    return;
                                }

                                String cleanedJson = jsonResponse.replace("```json", "").replace("```", "").trim();

                                AiParsedData result;
                                try {
                                    result = new Gson().fromJson(cleanedJson, AiParsedData.class);
                                } catch (JsonSyntaxException e) {
                                    Log.e(AI_DEBUG_TAG, "処理中断: JSONの形式が正しくありません。", e);
                                    return;
                                }

                                if (result == null) {
                                    Log.e(AI_DEBUG_TAG, "処理中断: JSONからJavaオブジェクトへの変換に失敗しました。");
                                    return;
                                }

                                long currentMemoId = memo.getId();

                                // ToDoの処理
                                if (result.todos != null && !result.todos.isEmpty()) {
                                    Log.d(AI_DEBUG_TAG, "[DB保存] ToDoの処理を開始します。件数: " + result.todos.size());
                                    for (AiParsedData.AiTodo aiTodo : result.todos) {
                                        if(aiTodo.description != null && !aiTodo.description.isEmpty()){
                                            Todo newTodo = new Todo(aiTodo.description, false, currentMemoId);
                                            mTodoDao.insert(newTodo);
                                            Log.d(AI_DEBUG_TAG, "  -> ToDoを保存しました: " + newTodo.getTitle());
                                        } else {
                                            Log.w(AI_DEBUG_TAG, "  -> スキップ: ToDoの説明が空です。");
                                        }
                                    }
                                } else {
                                    Log.d(AI_DEBUG_TAG, "[DB保存] 抽出されたToDoはありませんでした。");
                                }

                                // Eventの処理
                                if (result.events != null && !result.events.isEmpty()) {
                                    Log.d(AI_DEBUG_TAG, "[DB保存] Eventの処理を開始します。件数: " + result.events.size());
                                    for (AiParsedData.AiEvent aiEvent : result.events) {
                                        if (aiEvent.date == null || aiEvent.summary == null || aiEvent.time == null) {
                                            Log.w(AI_DEBUG_TAG, "  -> スキップ: Eventのデータが不完全です。 summary=" + aiEvent.summary + ", date=" + aiEvent.date + ", time=" + aiEvent.time);
                                            continue;
                                        }

                                        try {
                                            Date eventDate;
                                            String displayTime = aiEvent.time;
                                            if ("終日".equals(aiEvent.time)) {
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                                eventDate = sdf.parse(aiEvent.date);
                                            } else {
                                                String dateTimeString = aiEvent.date + " " + aiEvent.time;
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                                eventDate = sdf.parse(dateTimeString);
                                            }

                                            if (eventDate != null) {
                                                Event newEvent = new Event(aiEvent.summary, displayTime, eventDate.getTime(), currentMemoId);
                                                mEventDao.insert(newEvent);
                                                Log.d(AI_DEBUG_TAG, "  -> Eventを保存しました: " + newEvent.getTitle());
                                            }
                                        } catch (ParseException e) {
                                            Log.e(AI_DEBUG_TAG, "  -> エラー: 日付/時刻の解析に失敗しました。 date=" + aiEvent.date + ", time=" + aiEvent.time, e);
                                        }
                                    }
                                } else {
                                    Log.d(AI_DEBUG_TAG, "[DB保存] 抽出されたEventはありませんでした。");
                                }

                                Log.d(AI_DEBUG_TAG, "==================================================");
                                Log.d(AI_DEBUG_TAG, "AI解析とDB保存処理が正常に完了しました。");
                                Log.d(AI_DEBUG_TAG, "==================================================");

                            } else {
                                try {
                                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                                    Log.e(AI_DEBUG_TAG, "[APIエラー] " + response.code() + ": " + errorBody);
                                } catch (IOException e) {
                                    Log.e(AI_DEBUG_TAG, "[APIエラー] エラーレスポンスの読み込みに失敗しました。", e);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(retrofit2.Call<GeminiResponse> call, Throwable t) {
                        Log.e(AI_DEBUG_TAG, "[通信エラー] APIとの通信自体に失敗しました。", t);
                    }
                });
    }
}