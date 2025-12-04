package com.example.sottomemo;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

// リストデータを提供するサービス
public class DailyListWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new DailyListRemoteViewsFactory(this.getApplicationContext());
    }
}

// 実際にデータを作ってViewにセットする工場クラス
class DailyListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private List<Event> eventList = new ArrayList<>();

    public DailyListRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        // 初期化処理
    }

    @Override
    public void onDataSetChanged() {
        // データが更新されたときに呼ばれる
        long startOfDay = getStartOfDay();
        long endOfDay = getEndOfDay();

        MemoRoomDatabase db = MemoRoomDatabase.getDatabase(context);
        eventList = db.eventDao().getEventsForDaySync(startOfDay, endOfDay);
    }

    @Override
    public void onDestroy() {
        eventList.clear();
    }

    @Override
    public int getCount() {
        return eventList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= eventList.size()) return null;

        Event event = eventList.get(position);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list_item);

        String timeStr = "終日".equals(event.getTime()) ? "終日" : event.getTime();
        views.setTextViewText(R.id.widget_item_time, timeStr);
        views.setTextViewText(R.id.widget_item_title, event.getTitle());

        // ★ 前回のエラー修正点：getMemoId() メソッドを使用
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(MemoEditActivity.EXTRA_ID, event.getMemoId());
        views.setOnClickFillInIntent(R.id.widget_item_title, fillInIntent);
        views.setOnClickFillInIntent(R.id.widget_item_time, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

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