package com.example.sottomemo;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import com.example.sottomemo.api.AiParsedData;
import com.example.sottomemo.api.ApiClient;
import com.example.sottomemo.api.GeminiRequest;
import com.example.sottomemo.api.GeminiResponse;
import com.google.gson.Gson;
import java.io.IOException;
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

    // --- メモ関連 ---
    LiveData<List<MemoWithCategories>> getAllMemosWithCategories() { return mAllMemos; }
    LiveData<List<MemoWithCategories>> searchMemosWithCategories(String searchQuery) { return mMemoDao.searchMemosWithCategories(searchQuery); }
    LiveData<MemoWithCategories> getMemoWithCategories(long memoId) { return mMemoDao.getMemoWithCategories(memoId); }
    void insert(Memo memo, List<Long> categoryIds) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            long memoId = mMemoDao.insert(memo);
            if (categoryIds != null) {
                for (Long categoryId : categoryIds) {
                    MemoCategoryCrossRef crossRef = new MemoCategoryCrossRef();
                    crossRef.memoId = memoId;
                    crossRef.categoryId = categoryId;
                    mMemoDao.insertMemoCategoryCrossRef(crossRef);
                }
            }
        });
    }
    void update(Memo memo, List<Long> categoryIds) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            mMemoDao.update(memo);
            mMemoDao.deleteCrossRefsForMemo(memo.getId());
            if (categoryIds != null) {
                for (Long categoryId : categoryIds) {
                    MemoCategoryCrossRef crossRef = new MemoCategoryCrossRef();
                    crossRef.memoId = memo.getId();
                    crossRef.categoryId = categoryId;
                    mMemoDao.insertMemoCategoryCrossRef(crossRef);
                }
            }
        });
    }
    void delete(Memo memo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.delete(memo)); }
    void deleteMemos(List<Memo> memos) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mMemoDao.deleteMemos(memos)); }

    // --- ToDo関連 ---
    LiveData<List<Todo>> getAllTodos() { return mAllTodos; }
    void insert(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.insert(todo)); }
    void update(Todo todo) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mTodoDao.update(todo)); }

    // --- カテゴリ関連 ---
    LiveData<List<Category>> getAllCategories() { return mCategoryDao.getAllCategories(); }
    void insert(Category category) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mCategoryDao.insert(category)); }

    // --- Event関連 ---
    LiveData<List<Event>> getEventsForDay(long startOfDay, long endOfDay) { return mEventDao.getEventsForDay(startOfDay, endOfDay); }
    void insert(Event event) { MemoRoomDatabase.databaseWriteExecutor.execute(() -> mEventDao.insert(event)); }

    // --- AI関連 ---
    void analyzeAndSave(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            String prompt = "あなたは入力された日本語のテキストを解析し、含まれる「予定(event)」と「ToDo(todo)」を抽出するエキスパートです。" +
                    "以下のルールに厳密に従い、JSON形式で出力してください。\n\n" +
                    "### ルール\n" +
                    "1. 日時が明確なものは「events」の配列へ。それ以外のタスクは「todos」の配列へ。\n" +
                    "2. eventsには「summary」(件名)、「date」(YYYY-MM-DD)、「time」(HH:mm)を必ず含める。時間がなければtimeは「終日」。\n" +
                    "3. todosには「description」(内容)を必ず含める。「明日部活」のような名詞句からも「部活」のように適切に内容を抽出する。\n" +
                    "4. 該当がなければ `{\"events\":[], \"todos\":[]}` を返す。\n" +
                    "5. あなたの回答はJSONオブジェクトのみ。説明は一切不要。\n\n" +
                    "### 解析対象テキスト\n" +
                    "「" + memo.getExcerpt() + "」";
            try {
                Response<GeminiResponse> response = ApiClient.getApiService()
                        .generateContent(BuildConfig.GEMINI_API_KEY, new GeminiRequest(prompt))
                        .execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().getResponseText();
                    if (jsonResponse == null) return;

                    jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
                    AiParsedData result = new Gson().fromJson(jsonResponse, AiParsedData.class);

                    if (result == null) return;

                    if (result.todos != null && !result.todos.isEmpty()) {
                        for (AiParsedData.AiTodo aiTodo : result.todos) {
                            if(aiTodo.description != null && !aiTodo.description.isEmpty()){
                                mTodoDao.insert(new Todo(aiTodo.description, false));
                                Log.d("AI_SAVE", "Saved ToDo: " + aiTodo.description);
                            }
                        }
                    }
                    if (result.events != null && !result.events.isEmpty()) {
                        for (AiParsedData.AiEvent aiEvent : result.events) {
                            try {
                                if (aiEvent.date == null || aiEvent.summary == null || aiEvent.time == null) continue;
                                String dateTimeString = aiEvent.date + " " + aiEvent.time;
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date eventDate = sdf.parse(dateTimeString);
                                if (eventDate != null) {
                                    Event newEvent = new Event(aiEvent.summary, aiEvent.time, eventDate.getTime());
                                    mEventDao.insert(newEvent);
                                    Log.d("AI_SAVE", "Saved Event: " + newEvent.title);
                                }
                            } catch (ParseException e) {
                                Log.e("AI_SAVE", "Failed to parse date-time: " + aiEvent.date + " " + aiEvent.time, e);
                            }
                        }
                    }
                } else {
                    Log.e("AI_RESPONSE", "API Error: " + response.code() + " " + response.message());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("AI_RESPONSE", "Error during AI analysis: " + e.getMessage());
            }
        });
    }
}