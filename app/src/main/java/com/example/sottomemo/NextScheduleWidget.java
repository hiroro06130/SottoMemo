package com.example.sottomemo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NextScheduleWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, NextScheduleWidget.class));
        for (int id : ids) {
            updateAppWidget(context, manager, id);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_next_schedule);

        // --- 1. マイクボタンの設定 ---
        Intent voiceIntent = new Intent(context, MemoEditActivity.class);
        voiceIntent.putExtra("EXTRA_START_VOICE_INPUT", true);
        voiceIntent.setData(Uri.parse("custom://widget/voice")); // ユニークにする
        voiceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent voicePendingIntent = PendingIntent.getActivity(
                context, 1, voiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_button_voice, voicePendingIntent);

        // --- 2. プラスボタンの設定 ---
        Intent addIntent = new Intent(context, MemoEditActivity.class);
        addIntent.setData(Uri.parse("custom://widget/add")); // ユニークにする
        addIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent addPendingIntent = PendingIntent.getActivity(
                context, 2, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_button_add, addPendingIntent);

        // --- 3. メイン画面を開く設定 ---
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                context, 3, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.layout_schedule_info, mainPendingIntent);


        // --- 4. データの取得と表示 ---
        MemoRoomDatabase.databaseWriteExecutor.execute(() -> {
            MemoRoomDatabase db = MemoRoomDatabase.getDatabase(context);
            Event nextEvent = db.eventDao().getNextEventSync(System.currentTimeMillis());

            if (nextEvent != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd(E) HH:mm", Locale.JAPAN);
                String timeStr = sdf.format(new Date(nextEvent.getEventDate()));
                if ("終日".equals(nextEvent.getTime())) {
                    timeStr = timeStr.split(" ")[0] + " 終日";
                }

                views.setTextViewText(R.id.widget_text_time, timeStr);
                views.setTextViewText(R.id.widget_text_title, nextEvent.getTitle());
            } else {
                views.setTextViewText(R.id.widget_text_time, "");
                views.setTextViewText(R.id.widget_text_title, "予定はありません");
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        });
    }
}