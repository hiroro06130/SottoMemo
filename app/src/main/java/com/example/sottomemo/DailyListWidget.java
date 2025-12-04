package com.example.sottomemo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class DailyListWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    // 外部から更新をリクエストされた時に呼ぶメソッド
    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, DailyListWidget.class));

        // リストデータの更新を通知
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view);

        for (int id : ids) {
            updateAppWidget(context, manager, id);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_daily_list);

        // --- 1. リストの設定 ---
        Intent intent = new Intent(context, DailyListWidgetService.class);
        views.setRemoteAdapter(R.id.widget_list_view, intent);
        views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view);

        // --- 2. リスト項目タップ時のテンプレート設定 ---
        // 個々のアイテムがクリックされたらアプリを開く
        Intent appIntent = new Intent(context, MainActivity.class); // とりあえずメイン画面へ
        PendingIntent appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setPendingIntentTemplate(R.id.widget_list_view, appPendingIntent);

        // --- 3. タイトルバータップでアプリ起動 ---
        Intent titleIntent = new Intent(context, MainActivity.class);
        PendingIntent titlePendingIntent = PendingIntent.getActivity(
                context, 1, titleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_title, titlePendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}