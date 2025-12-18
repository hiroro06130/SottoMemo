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
    private static final String API_KEY = "AIzaSyCQPDdpm0qrsqgLID2qtjkYVi5p6zoDX54";

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
        Memo memo = mMemoDao.getMemoById(memoId);
        if (memo == null) {
            return;
        }

        SimpleDateFormat promptSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN);
        String today = promptSdf.format(new Date());

        // ★★★ 抽出能力を極限まで高めたプロンプト ★★★
        String prompt = "あなたは長文のメモからスケジュールとタスクだけを抽出する高度なAIスケジューラーです。\n" +
                "以下の「解析対象テキスト」を読み込み、将来実行すべき「予定(events)」と「ToDo(todos)」のみをJSONで出力してください。\n" +
                "日記、感想、過去の出来事、単なるメモ書きはノイズとして完全に無視してください。\n\n" +
                "### 今日の日付\n" +
                today + "\n\n" +
                "### 厳格な抽出ルール\n" +
                "1. **未来のみ抽出**: 過去形（「〜した」「〜に行った」）は絶対に抽出しない。「〜する予定」「〜したい」など未来の行動のみ抽出する。\n" +
                "2. **ノイズ除去**: 「楽しかった」「美味しかった」などの感想や、「昨日は雨だった」などの事実は無視する。\n" +
                "3. **Event(カレンダー)の条件**: 日時が特定できるもの（「明日」「来週月曜」「12/25」など）。\n" +
                "4. **ToDo(タスク)の条件**: 日時は不明確だが、やるべき行動（「電球を替える」「本を買う」など）。\n" +
                "5. **深夜の正規化**: 「25時」「26時」などは「翌1:00」「翌2:00」として計算し、日付を+1日すること。\n" +
                "6. **相対日付の計算**: 「来週の水曜」などは、今日の日付(" + today + ")を基準に正確な日付(YYYY-MM-DD)に変換すること。\n\n" +
                "### 複雑な文章の解析例\n" +
                "入力: 「昨日は田中さんと焼肉に行った。美味しかった。明日は10時に佐藤さんと会議があるから資料を作らなきゃ。あと、来週の土曜日は映画を見に行く予定。」\n" +
                "思考プロセス:\n" +
                " - 「焼肉に行った」→ 過去のことなので無視。\n" +
                " - 「佐藤さんと会議」→ 明日10時という日時があるのでEvent。\n" +
                " - 「資料を作る」→ 日時指定はないがやるべきことなのでToDo。\n" +
                " - 「映画を見に行く」→ 来週土曜という日時があるのでEvent。\n" +
                "出力: `{\"events\":[{\"summary\":\"佐藤さんと会議\",\"date\":\"(明日の日付)\",\"time\":\"10:00\"}, {\"summary\":\"映画鑑賞\",\"date\":\"(来週土曜の日付)\",\"time\":\"終日\"}], \"todos\":[{\"description\":\"会議資料を作成\"}]}`\n\n" +
                "### 出力フォーマット\n" +
                "JSONオブジェクトのみを出力すること。\n" +
                "- events: [{\"summary\":String, \"date\":String(YYYY-MM-DD), \"time\":String(HH:mm or \"終日\")}]\n" +
                "- todos: [{\"description\":String}]\n\n" +
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

                                if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                                    return;
                                }
                                String cleanedJson = jsonResponse.replace("```json", "").replace("```", "").trim();
                                AiParsedData result;
                                try {
                                    result = new Gson().fromJson(cleanedJson, AiParsedData.class);
                                } catch (JsonSyntaxException e) {
                                    Log.e(AI_DEBUG_TAG, "JSONパースエラー", e);
                                    return;
                                }

                                if (result == null) return;

                                long currentMemoId = memo.getId();

                                // ToDo保存
                                if (result.todos != null) {
                                    for (AiParsedData.AiTodo aiTodo : result.todos) {
                                        if(aiTodo.description != null && !aiTodo.description.isEmpty()){
                                            Todo newTodo = new Todo(aiTodo.description, false, currentMemoId);
                                            mTodoDao.insert(newTodo);
                                            Log.d(AI_DEBUG_TAG, "抽出ToDo: " + newTodo.getTitle());
                                        }
                                    }
                                }

                                // Event保存
                                if (result.events != null) {
                                    for (AiParsedData.AiEvent aiEvent : result.events) {
                                        if (aiEvent.date == null || aiEvent.summary == null || aiEvent.time == null) continue;
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
                                                Log.d(AI_DEBUG_TAG, "抽出Event: " + newEvent.getTitle() + " " + aiEvent.date);
                                            }
                                        } catch (ParseException e) {
                                            Log.e(AI_DEBUG_TAG, "日付解析エラー", e);
                                        }
                                    }
                                }
                            }
                        });
                    }
                    @Override
                    public void onFailure(retrofit2.Call<GeminiResponse> call, Throwable t) {
                        Log.e(AI_DEBUG_TAG, "API通信エラー", t);
                    }
                });
    }
}