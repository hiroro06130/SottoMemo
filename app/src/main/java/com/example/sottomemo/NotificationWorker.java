package com.example.sottomemo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

// WorkManagerで動作する、個別通知専用の配達員
public class NotificationWorker extends Worker {

    private static final String TAG = "NotificationWorker";

    // Workerに渡されるデータのキー
    public static final String KEY_EVENT_ID = "KEY_EVENT_ID";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "WorkManagerによる個別通知Workerが起動しました。");

        int id = getInputData().getInt(KEY_EVENT_ID, 0);
        if (id == 0) {
            Log.e(TAG, "イベントIDが0のため、処理を中断します。");
            return Result.failure();
        }

        // WorkManagerは既にバックグラウンドスレッドで動いているため、
        // ここでデータベースに直接アクセスしても安全です。
        try {
            MemoRoomDatabase db = MemoRoomDatabase.getDatabase(getApplicationContext());
            Event event = db.eventDao().getEventByIdSync(id);

            if (event == null) {
                Log.w(TAG, "ID=" + id + "のイベントが見つかりません。削除された可能性があります。");
                return Result.success(); // 失敗ではないのでsuccess
            }

            Log.d(TAG, "イベント「" + event.getTitle() + "」の通知を作成します。");
            String title = event.getTitle();
            String text = "予定の時刻です: " + event.getTime();
            String channelId = SottoMemoApplication.EVENT_REMINDER_CHANNEL_ID;

            Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), id, mainIntent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                    .setSmallIcon(R.drawable.ic_schedule)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "通知の権限がありません！");
                return Result.failure();
            }

            NotificationManagerCompat.from(getApplicationContext()).notify(id, builder.build());
            Log.d(TAG, "WorkManagerによる個別通知が正常に発行されました。");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "WorkManagerでの通知作成中にエラーが発生しました。", e);
            return Result.failure();
        }
    }
}