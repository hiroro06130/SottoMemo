package com.example.sottomemo;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Memo.class, Todo.class, Category.class, MemoCategoryCrossRef.class, Event.class}, version = 5, exportSchema = false)
public abstract class MemoRoomDatabase extends RoomDatabase {

    public abstract MemoDao memoDao();
    public abstract TodoDao todoDao();
    public abstract CategoryDao categoryDao();
    public abstract EventDao eventDao();

    private static volatile MemoRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                // 初回起動時にダミーのカテゴリだけを作成する
                CategoryDao categoryDao = INSTANCE.categoryDao();
                categoryDao.insert(new Category("仕事", Color.parseColor("#A8D8C9")));
                categoryDao.insert(new Category("プライベート", Color.parseColor("#F7CACA")));
                categoryDao.insert(new Category("アイデア", Color.parseColor("#B7D7E8")));
            });
        }
    };

    static MemoRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MemoRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MemoRoomDatabase.class, "memo_database")
                            .addCallback(sRoomDatabaseCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}