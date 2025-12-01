package com.example.sottomemo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.preference.PreferenceManager;
// ★ 1. WorkManager関連のimport文を追加
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit; // ★ 2. TimeUnitをimport

public class ReminderManager {

    private static final String TAG = "ReminderManager";
    public static final int DAILY_SUMMARY_REQUEST_CODE = 999;

    // ★ 3. WorkManager用のタグプレフィックスを定義
    private static final String WORK_TAG_PREFIX = "event_reminder_";

    // ★★★★★★★★★ ここからが【個別通知】の新しいロジックです ★★★★★★★★★
    // --- 1. イベントごとのリマインダーを「WorkManager」で予約する ---
    public static void scheduleEventReminder(Context context, Event event) {

        // --- 通知タイミングを計算 ---
        int reminderMinutes;
        int customReminder = event.getCustomReminderMinutes();

        if (customReminder != -2) { // 「デフォルト」以外が設定されている場合
            reminderMinutes = customReminder;
        } else { // 「デフォルト」の場合
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            reminderMinutes = prefs.getInt("default_reminder_time", 10);
        }

        // 「通知しない」(-1) が選択されている場合は、キャンセル処理だけして終了
        if (reminderMinutes == -1) {
            cancelEventReminder(context, event);
            Log.d(TAG, "[WorkManager] イベント「" + event.getTitle() + "」は通知しない設定です。キャンセルします。");
            return;
        }

        long eventTime = event.getEventDate();
        long reminderTime = eventTime - (long) reminderMinutes * 60 * 1000;
        long delay = reminderTime - System.currentTimeMillis(); // 現在時刻からの遅延を計算

        // すでに過ぎた時刻なら何もしない
        if (delay < 0) {
            Log.d(TAG, "[WorkManager] イベント「" + event.getTitle() + "」の通知時刻は過去のため、予約しません。");
            return;
        }

        // --- Workerに渡すデータ（イベントIDのみ）を作成 ---
        Data inputData = new Data.Builder()
                .putInt(NotificationWorker.KEY_EVENT_ID, (int) event.getId())
                .build();

        // --- WorkManagerに予約 ---
        OneTimeWorkRequest reminderWorkRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(getWorkTag(event.getId())) // キャンセル用にタグを付ける
                .build();

        // 同じ予定の予約がすでにあれば、新しい予約で上書きする
        WorkManager.getInstance(context).enqueueUniqueWork(
                getWorkTag(event.getId()),
                ExistingWorkPolicy.REPLACE, // 既存の予約を置き換える
                reminderWorkRequest
        );

        Log.d(TAG, "[WorkManager] イベント「" + event.getTitle() + "」のリマインダーを予約しました。実行まで " + delay / 1000 / 60 + " 分");
    }

    // --- 2. 「WorkManager」の予約をキャンセルする ---
    public static void cancelEventReminder(Context context, Event event) {
        String workTag = getWorkTag(event.getId());
        WorkManager.getInstance(context).cancelUniqueWork(workTag);
        Log.d(TAG, "[WorkManager] イベント「" + event.getTitle() + "」のリマインダーをキャンセルしました。 Tag=" + workTag);
    }

    // --- 3. WorkManager用のユニークなタグを生成するヘルパー ---
    private static String getWorkTag(long eventId) {
        return WORK_TAG_PREFIX + eventId;
    }

    // ★★★★★★★★★ ここからは【まとめ通知】のロジック（AlarmManager）です ★★★★★★★★★
    // (この部分は一切変更ありません)

    private static final String ACTION_DAILY_SUMMARY = "com.example.sottomemo.ACTION_DAILY_SUMMARY";

    public static void scheduleDailySummary(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "[AlarmManager] 時間厳守のアラーム許可がないため、まとめ通知を予約できません。");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int hourOfDay = prefs.getInt("summary_notification_hour", 8);
        int minuteOfHour = prefs.getInt("summary_notification_minute", 0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minuteOfHour);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_DAILY_SUMMARY);
        intent.putExtra(AlarmReceiver.KEY_NOTIFICATION_ID, DAILY_SUMMARY_REQUEST_CODE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, DAILY_SUMMARY_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent showAppIntent = new Intent(context, MainActivity.class);
        PendingIntent showAppPendingIntent = PendingIntent.getActivity(
                context, DAILY_SUMMARY_REQUEST_CODE + 1, showAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), showAppPendingIntent);
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);

        Log.d(TAG, "[AlarmManager] 毎日のまとめ通知を(setAlarmClockで)予約しました。初回実行: " + calendar.getTime());
    }

    public static void cancelDailySummary(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_DAILY_SUMMARY);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, DAILY_SUMMARY_REQUEST_CODE, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "[AlarmManager] 毎日のまとめ通知をキャンセルしました。");
        }
    }
}