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
import java.util.ArrayList;
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

    public AiParsedData analyzeTextWithAi(String text) {
        // このメソッドの本体は、analyzeAndSaveに任せるので、今は何もしなくて良い
        return null; // 後で修正します
    }

    void analyzeAndSave(Memo memo) {
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            String prompt = "あなたは、入力されたテキストから「予定」と「ToDo」を抽出する、非常に優秀なアシスタントです。" +
                    "以下のルールと例に厳密に従って、結果をJSON形式で出力してください。\n\n" +
                    "ルール:\n" +
                    "1. 予定は 'events' キー、ToDoは 'todos' キーに、それぞれ配列として格納してください。\n" +
                    "2. 予定には 'summary' (件名), 'date' (YYYY-MM-DD形式), 'time' (HH:mm形式) を含めてください。\n" +
                    "3. ToDoには 'description' (内容) を含めてください。\n" +
                    "4. 該当する予定やToDoが一つもない場合は、必ず `{\"events\":[], \"todos\":[]}` という空のJSONを返してください。\n" +
                    "5. 日付や曜日だけが書かれた名詞のメモ（例：「月曜 課題」「明日 会議」）も、文脈から判断してToDoや予定として解釈してください。\n" +
                    "6. 余計な説明や前置きは一切不要です。JSONオブジェクトだけを出力してください。\n\n" +
                    "例1:\n" +
                    "入力テキスト: 「来週水曜14時にクライアントと打ち合わせ。あと、牛乳を買って帰る」\n" +
                    "出力JSON: {\\\"events\\\":[{\\\"summary\\\":\\\"クライアントと打ち合わせ\\\",\\\"date\\\":\\\"2025-07-23\\\",\\\"time\\\":\\\"14:00\\\"}],\\\"todos\\\":[{\\\"description\\\":\\\"牛乳を買って帰る\\\"}]}\n\n" +
                    "例2:\n" +
                    "入力テキスト: 「明日部活」\n" +
                    "出力JSON: {\\\"todos\\\":[{\\\"description\\\":\\\"部活\\\"}]}\n\n" +
                    "例3:\n" +
                    "入力テキスト: 「金曜 燃えるゴミ」\n" +
                    "出力JSON: {\\\"todos\\\":[{\\\"description\\\":\\\"燃えるゴミを出す\\\"}]}\n\n" +
                    "では、本番です。\n" +
                    "入力テキスト:\n" +
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
                            mTodoDao.insert(new Todo(aiTodo.description, false));
                        }
                    }
                    if (result.events != null && !result.events.isEmpty()) {
                        for (AiParsedData.AiEvent aiEvent : result.events) {
                            try {
                                String dateTimeString = aiEvent.date + " " + aiEvent.time;
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date eventDate = sdf.parse(dateTimeString);
                                if (eventDate != null) {
                                    Event newEvent = new Event(aiEvent.summary, aiEvent.time, eventDate.getTime());
                                    mEventDao.insert(newEvent);
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