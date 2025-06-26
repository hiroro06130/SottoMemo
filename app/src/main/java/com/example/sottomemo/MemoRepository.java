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
import java.util.List;

import retrofit2.Response;

public class MemoRepository {

    private MemoDao mMemoDao;
    private TodoDao mTodoDao;
    private CategoryDao mCategoryDao;
    private EventDao mEventDao;
    private LiveData<List<MemoWithCategories>> mAllMemos;
    private LiveData<List<Todo>> mAllTodos;
    private LiveData<List<Category>> mAllCategories;

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
    AiParsedData analyzeTextWithAi(String text) {
        String prompt = "以下の文章から、予定やToDoを抽出し、JSON形式で出力してください。もし何もなければ、空のJSON `{}` を返してください。\n\n" + text;
        try {
            Response<GeminiResponse> response = ApiClient.getApiService()
                    .generateContent(BuildConfig.GEMINI_API_KEY, new GeminiRequest(prompt))
                    .execute();
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().getResponseText();
                if (jsonResponse != null) {
                    jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
                    return new Gson().fromJson(jsonResponse, AiParsedData.class);
                }
            } else {
                Log.e("AI_RESPONSE", "API Error: " + response.code());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("AI_RESPONSE", "Error during AI analysis: " + e.getMessage());
        }
        return null;
    }
}