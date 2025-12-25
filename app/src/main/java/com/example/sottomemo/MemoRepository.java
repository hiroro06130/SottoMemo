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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Response;

public class MemoRepository {

    // ★★★ ここにAPIキーを入れてください ★★★
    private static final String API_KEY = "AIzaSyDh4m-_jw8wVBWGn2xK5jaHdkBM_lC9T88";

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

    // --- メモ関連 ---
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

    // --- ToDo・カテゴリ操作 ---
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
    LiveData<Event> getNextEvent() { return mEventDao.getNextEvent(System.currentTimeMillis()); }

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

    // --- AI解析のコアロジック（日本語フォーマット版） ---
    public void analyzeAndSaveFromWorker(long memoId) {
        Log.d(AI_DEBUG_TAG, "==================================================");
        Log.d(AI_DEBUG_TAG, "AI解析処理を開始します。対象メモID: " + memoId);
        Memo memo = mMemoDao.getMemoById(memoId);
        if (memo == null) {
            return;
        }

        SimpleDateFormat promptSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN);
        String today = promptSdf.format(new Date());

        String prompt = "あなたは優秀な秘書です。以下のメモから「スケジュール(events)」と「ToDo(todos)」を抽出し、JSON形式で出力してください。\n" +
                "日記、感想、過去の出来事は徹底的に無視し、未来のアクションのみを抽出してください。\n\n" +
                "### 今日の日付\n" +
                today + "\n\n" +
                "### 抽出ルール（厳守）\n" +
                "1. **Event**: \n" +
                "   - 日付が特定できる予定。\n" +
                "   - **時刻**: 指定がない場合は必ず \"終日\" と出力する。\n" +
                "   - **分離**: 場所(location)や人(people)は、summary(件名)には含めず、必ず別のフィールドに入れる。\n" +
                "2. **ToDo**: \n" +
                "   - やるべきタスク。期限(deadline)があれば抽出。\n" +
                "3. **正規化**: \n" +
                "   - 日付はYYYY-MM-DD形式。\n" +
                "   - 深夜(25時等)は翌日の時刻に変換。\n\n" +
                "### 解析の思考プロセス（例）\n" +
                "入力: 「明日の13時にカフェで田中さんと打ち合わせ。あと、再来週の日曜にディズニーランドに行く。」\n" +
                "出力:\n" +
                "```json\n" +
                "{\n" +
                "  \"events\": [\n" +
                "    {\"summary\": \"打ち合わせ\", \"date\": \"(明日の日付)\", \"time\": \"13:00\", \"location\": \"カフェ\", \"people\": \"田中さん\"},\n" +
                "    {\"summary\": \"ディズニーランド\", \"date\": \"(再来週の日曜の日付)\", \"time\": \"終日\", \"location\": \"\", \"people\": \"\"}\n" +
                "  ],\n" +
                "  \"todos\": []\n" +
                "}\n" +
                "```\n\n" +
                "### 本番フォーマット (JSON)\n" +
                "{\n" +
                "  \"events\": [{\"summary\": String, \"date\": \"YYYY-MM-DD\", \"time\": \"HH:mm\" or \"終日\", \"location\": String, \"people\": String}],\n" +
                "  \"todos\": [{\"description\": String, \"deadline\": String}]\n" +
                "}\n\n" +
                "### 解析対象テキスト\n" +
                "「" + memo.getExcerpt() + "」";

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
                                            String finalTitle = aiTodo.description;
                                            if (aiTodo.deadline != null && !aiTodo.deadline.isEmpty()) {
                                                finalTitle += " [期限: " + aiTodo.deadline + "]";
                                            }

                                            Todo newTodo = new Todo(finalTitle, false, currentMemoId);
                                            mTodoDao.insert(newTodo);
                                        }
                                    }
                                }

                                // Event保存
                                if (result.events != null) {
                                    for (AiParsedData.AiEvent aiEvent : result.events) {
                                        if (aiEvent.date == null || aiEvent.summary == null) continue;

                                        try {
                                            Date eventDate;
                                            String displayTime = (aiEvent.time == null || aiEvent.time.isEmpty()) ? "終日" : aiEvent.time;

                                            if ("終日".equals(displayTime)) {
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                                eventDate = sdf.parse(aiEvent.date);
                                            } else {
                                                try {
                                                    String dateTimeString = aiEvent.date + " " + displayTime;
                                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                                    eventDate = sdf.parse(dateTimeString);
                                                } catch (ParseException e) {
                                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                                    eventDate = sdf.parse(aiEvent.date);
                                                    displayTime = "終日";
                                                }
                                            }

                                            if (eventDate != null) {
                                                StringBuilder titleBuilder = new StringBuilder(aiEvent.summary);

                                                // ★改良点: 日本語スタイル（・）で連結する
                                                boolean hasExtra = false;
                                                if ((aiEvent.people != null && !aiEvent.people.isEmpty()) ||
                                                        (aiEvent.location != null && !aiEvent.location.isEmpty())) {

                                                    titleBuilder.append("（");

                                                    if (aiEvent.people != null && !aiEvent.people.isEmpty()) {
                                                        titleBuilder.append(aiEvent.people);
                                                        hasExtra = true;
                                                    }

                                                    if (aiEvent.location != null && !aiEvent.location.isEmpty()) {
                                                        if (hasExtra) {
                                                            titleBuilder.append("・");
                                                        }
                                                        titleBuilder.append(aiEvent.location);
                                                    }

                                                    titleBuilder.append("）");
                                                }

                                                Event newEvent = new Event(titleBuilder.toString(), displayTime, eventDate.getTime(), currentMemoId);
                                                long id = mEventDao.insert(newEvent);
                                                newEvent.setId(id);
                                                ReminderManager.scheduleEventReminder(mApplication, newEvent);
                                            }
                                        } catch (ParseException e) {
                                            Log.e(AI_DEBUG_TAG, "日付解析エラー: " + aiEvent.date, e);
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