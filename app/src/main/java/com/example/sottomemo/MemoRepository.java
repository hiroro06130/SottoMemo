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
import retrofit2.Response;

public class MemoRepository {

    // ★★★ ここにAPIキーを入れてください ★★★
    private static final String API_KEY = "AIzaSyC_qmCOhNd5YJ2rfCzfq0ZDsftVXjQaqSI";

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

    // --- メモ関連 (変更なし) ---
    LiveData<List<MemoWithCategories>> getAllMemosWithCategories() { return mAllMemos; }
    LiveData<List<MemoWithCategories>> getMemosByCategoryId(long categoryId) { return mMemoDao.getMemosByCategoryId(categoryId); }

    void delete(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            List<Event> eventsToDelete = mEventDao.getEventsByMemoIdSync(memo.getId());
            for (Event event : eventsToDelete) {
                ReminderManager.cancelEventReminder(mApplication, event);
            }
            mEventDao.deleteEventsByMemoId(memo.getId());
            mTodoDao.deleteTodosByMemoId(memo.getId());
            mMemoDao.delete(memo);
        });
    }

    void deleteMemos(List<Memo> memos) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            for (Memo memo : memos) {
                List<Event> eventsToDelete = mEventDao.getEventsByMemoIdSync(memo.getId());
                for (Event event : eventsToDelete) {
                    ReminderManager.cancelEventReminder(mApplication, event);
                }
                mEventDao.deleteEventsByMemoId(memo.getId());
                mTodoDao.deleteTodosByMemoId(memo.getId());
            }
            mMemoDao.deleteMemos(memos);
        });
    }

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

            List<Event> oldEvents = mEventDao.getEventsByMemoIdSync(memo.getId());
            for (Event oldEvent : oldEvents) {
                ReminderManager.cancelEventReminder(mApplication, oldEvent);
            }

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

    // --- ToDo・カテゴリの単純操作 (変更なし) ---
    LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    void insert(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.insert(todo)); }
    void update(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.update(todo)); }
    void delete(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.delete(todo)); }

    LiveData<List<Category>> getAllCategories() { return mCategoryDao.getAllCategories(); }
    void insert(Category category) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mCategoryDao.insert(category)); }
    void update(Category category) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mCategoryDao.update(category)); }
    void delete(Category category) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mCategoryDao.delete(category)); }

    // --- Event関連 ---
    LiveData<List<Event>> getEventsForDay(long startOfDay, long endOfDay) { return mEventDao.getEventsForDay(startOfDay, endOfDay); }

    LiveData<Event> getNextEvent() {
        return mEventDao.getNextEvent(System.currentTimeMillis());
    }
    void insert(Event event) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            long id = mEventDao.insert(event);
            event.setId(id);
            ReminderManager.scheduleEventReminder(mApplication, event);
        });
    }
    void update(Event event) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            mEventDao.update(event);
            ReminderManager.scheduleEventReminder(mApplication, event);
        });
    }
    void delete(Event event) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            ReminderManager.cancelEventReminder(mApplication, event);
            mEventDao.delete(event);
        });
    }

    // --- AI解析のコアロジック ---
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

        // ★★★ プロンプトを強化: 深夜時間の扱いを厳格に指定 ★★★
        String prompt = "あなたは入力された日本語のテキストを解析し、含まれる「予定(event)」と「ToDo(todo)」を抽出するエキスパートです。" +
                "以下のルールと事例に厳密に従い、JSON形式で出力してください。\n\n" +
                "### 今日の日付\n" +
                today + "\n\n" +
                "### ルール\n" +
                "1. **予定の定義**: カレンダーの特定の日時を確保するようなものを「events」へ分類する。\n" +
                "2. **ToDoの定義**: 純粋なタスクは「todos」へ分類する。\n" +
                "3. **日付の正規化**: 「明日」「来週」などは今日を基準に「YYYY-MM-DD」形式にする。\n" +
                "4. **深夜・翌日の扱い**: テキストに「24時」「25時」や「翌1時」「翌2時」などの表記がある場合は、**必ず日付を翌日（+1日）**とし、時刻を0:00〜23:59の形式（例: 25:00→翌日の01:00）に変換すること。「今日の25時」は「明日の1:00」として扱う。\n" +
                "5. **Eventの形式**: 「summary」(件名)、「date」(YYYY-MM-DD)、「time」(HH:mm)を含める。時間がなければtimeは「終日」。\n" +
                "6. **ToDoの形式**: 「description」(内容)を含める。\n" +
                "7. **出力形式**: JSONオブジェクトのみを出力する。\n" +
                "### 事例\n" +
                "今日が2025-12-16の場合:\n" +
                "入力: 「明日の15時 打ち合わせ。今日の25時にメンテナンス」\n" +
                "出力: `{\"events\":[{\"summary\":\"打ち合わせ\",\"date\":\"2025-12-17\",\"time\":\"15:00\"}, {\"summary\":\"メンテナンス\",\"date\":\"2025-12-17\",\"time\":\"01:00\"}],\"todos\":[]}`\n\n" +
                "### 解析対象テキスト\n" +
                "「" + memo.getExcerpt() + "」";

        Log.d(AI_DEBUG_TAG, "[リクエスト] AIへのプロンプト:\n" + prompt);

        ApiClient.getService()
                .generateContent(API_KEY, new GeminiRequest(prompt))
                .enqueue(new retrofit2.Callback<GeminiResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<GeminiResponse> call, Response<GeminiResponse> response) {
                        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
                            if (response.isSuccessful() && response.body() != null) {
                                String jsonResponse = null;
                                GeminiResponse body = response.body();
                                if (body.getCandidates() != null && !body.getCandidates().isEmpty()) {
                                    if (body.getCandidates().get(0).getContent() != null &&
                                            body.getCandidates().get(0).getContent().getParts() != null &&
                                            !body.getCandidates().get(0).getContent().getParts().isEmpty()) {
                                        jsonResponse = body.getCandidates().get(0).getContent().getParts().get(0).getText();
                                    }
                                }

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
                                if (result.todos != null && !result.todos.isEmpty()) {
                                    Log.d(AI_DEBUG_TAG, "[DB保存] ToDoの処理を開始します。件数: " + result.todos.size());
                                    for (AiParsedData.AiTodo aiTodo : result.todos) {
                                        if(aiTodo.description != null && !aiTodo.description.isEmpty()){
                                            Todo newTodo = new Todo(aiTodo.description, false, currentMemoId);
                                            mTodoDao.insert(newTodo);
                                        }
                                    }
                                }
                                if (result.events != null && !result.events.isEmpty()) {
                                    Log.d(AI_DEBUG_TAG, "[DB保存] Eventの処理を開始します。件数: " + result.events.size());
                                    for (AiParsedData.AiEvent aiEvent : result.events) {
                                        if (aiEvent.date == null || aiEvent.summary == null || aiEvent.time == null) {
                                            continue;
                                        }
                                        try {
                                            Date eventDate;
                                            String displayTime = aiEvent.time;

                                            if ("終日".equals(aiEvent.time)) {
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                                eventDate = sdf.parse(aiEvent.date);
                                            } else {
                                                String dateTimeString = aiEvent.date + " " + aiEvent.time;
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                                eventDate = sdf.parse(dateTimeString);
                                            }

                                            if (eventDate != null) {
                                                Event newEvent = new Event(aiEvent.summary, displayTime, eventDate.getTime(), currentMemoId);
                                                long id = mEventDao.insert(newEvent);
                                                newEvent.setId(id);
                                                ReminderManager.scheduleEventReminder(mApplication, newEvent);
                                            }
                                        } catch (ParseException e) {
                                            Log.e(AI_DEBUG_TAG, "  -> エラー: 日付/時刻の解析に失敗しました。", e);
                                        }
                                    }
                                }
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