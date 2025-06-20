package com.example.sottomemo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Memo.class, Todo.class}, version = 2, exportSchema = false)
public abstract class MemoRoomDatabase extends RoomDatabase {

    public abstract MemoDao memoDao();
    public abstract TodoDao todoDao();

    private static volatile MemoRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // データベース作成時に、初期データを投入するためのコールバック
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                // アプリの初回起動時にのみ、ダミーのToDoをデータベースに書き込む
                TodoDao dao = INSTANCE.todoDao();
                dao.insert(new Todo("牛乳を買う", false));
                dao.insert(new Todo("レポートを提出する", true));
                dao.insert(new Todo("ジムに行く", false));
                dao.insert(new Todo("クリーニングを受け取る", false));
            });
        }
    };

    static MemoRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MemoRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MemoRoomDatabase.class, "memo_database")
                            .addCallback(sRoomDatabaseCallback) // コールバックを追加
                            .fallbackToDestructiveMigration() // マイグレーションを簡単にするため（開発中のみ）
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}