package com.example.sottomemo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

// ★★★ このクラスは「毎日のまとめ通知」専用になりました ★★★
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    // KEY_TITLE と KEY_TEXT は不要になりました
    public static final String KEY_NOTIFICATION_ID = "KEY_NOTIFICATION_ID";
    public static final String KEY_NOTIFICATION_CHANNEL_ID = "KEY_NOTIFICATION_CHANNEL_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AlarmReceiverが起動しました。（まとめ通知用）");

        final PendingResult pendingResult = goAsync();
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                int id = intent.getIntExtra(KEY_NOTIFICATION_ID, 0);

                // ★★★ 1. 個別通知(id > 0)の処理ロジックを削除 ★★★
                if (id == ReminderManager.DAILY_SUMMARY_REQUEST_CODE) {
                    // --- 毎日のまとめ通知の場合 ---
                    Log.d(TAG, "毎日のまとめ通知を作成します。");
                    String channelId = SottoMemoApplication.SUMMARY_CHANNEL_ID;

                    MemoRoomDatabase db = MemoRoomDatabase.getDatabase(context);
                    List<Event> todayEvents = db.eventDao().getEventsForDaySync(getStartOfDay(), getEndOfDay());
                    int todoCount = db.todoDao().getIncompleteTodoCountSync();

                    int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    String title;
                    if (hourOfDay >= 4 && hourOfDay < 12) title = "おはようございます！";
                    else if (hourOfDay >= 12 && hourOfDay < 18) title = "こんにちは！";
                    else title = "こんばんは！";

                    String text;
                    if (!todayEvents.isEmpty() && todoCount > 0) {
                        text = "今日は" + todayEvents.size() + "件の予定と、" + todoCount + "件の未完了ToDoがあります。";
                    } else if (!todayEvents.isEmpty()) {
                        text = "今日は" + todayEvents.size() + "件の予定があります。";
                    } else if (todoCount > 0) {
                        text = "未完了のToDoが" + todoCount + "件あります。";
                    } else {
                        Log.d(TAG, "今日は予定もToDoもありません。通知をスキップします。");
                        return;
                    }

                    Intent mainIntent = new Intent(context, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, id, mainIntent, PendingIntent.FLAG_IMMUTABLE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(R.drawable.ic_schedule)
                            .setContentTitle(title)
                            .setContentText(text)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // ★ まとめ通知なので重要度をDefaultに
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "通知の権限がありません！");
                        return;
                    }

                    NotificationManagerCompat.from(context).notify(id, builder.build());
                    Log.d(TAG, "まとめ通知が正常に発行されました。");

                    Log.d(TAG, "次の日のまとめ通知を再予約します。");
                    ReminderManager.scheduleDailySummary(context);

                } else {
                    Log.w(TAG, "不明な通知IDを受け取りました: " + id);
                }

            } finally {
                pendingResult.finish();
                Log.d(TAG, "非同期処理が完了し、Receiverを終了します。");
            }
        });
    }

    // (getStartOfDay, getEndOfDay メソッドは変更なし)
    private long getStartOfDay() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDay() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTimeInMillis();
    }
}