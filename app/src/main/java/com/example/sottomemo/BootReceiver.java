package com.example.sottomemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // スマホの起動が完了した合図かどうかをチェック
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "スマホの起動を検知しました。アラームを再設定します。");

            // 毎日のまとめ通知のアラームを再予約する
            ReminderManager.scheduleDailySummary(context);

            // (注: 個別のイベントリマインダーは、AIがメモを解析・保存するたびに
            //  自動で予約/更新されるため、ここでは再設定不要です)
        }
    }
}