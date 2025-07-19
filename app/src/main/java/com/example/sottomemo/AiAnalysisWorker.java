package com.example.sottomemo;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class AiAnalysisWorker extends Worker {

    public static final String KEY_MEMO_ID = "MEMO_ID";

    public AiAnalysisWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // このWorkerに渡されたメモのIDを取得
        long memoId = getInputData().getLong(KEY_MEMO_ID, -1);
        if (memoId == -1) {
            Log.e("AiAnalysisWorker", "無効なメモIDが渡されました。");
            return Result.failure();
        }

        Log.d("AiAnalysisWorker", "メモID: " + memoId + " のバックグラウンド解析を開始します。");

        // Repositoryのインスタンスを取得し、AI解析メソッドを呼び出す
        MemoRepository repository = new MemoRepository((android.app.Application) getApplicationContext());
        try {
            // ここで実際にAI解析が実行される
            repository.analyzeAndSaveFromWorker(memoId);
            Log.d("AiAnalysisWorker", "メモID: " + memoId + " の解析が成功しました。");
            return Result.success();
        } catch (Exception e) {
            Log.e("AiAnalysisWorker", "メモID: " + memoId + " の解析中にエラーが発生しました。", e);
            return Result.failure();
        }
    }
}