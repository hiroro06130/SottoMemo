package com.example.sottomemo;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// @Databaseは、このクラスがRoomデータベースであることを示す
// entitiesには、このデータベースに含まれるテーブル(Entity)のクラスを指定
// versionはデータベースのバージョン。今後テーブル構造を変えるときに数字を上げる
@Database(entities = {Memo.class}, version = 1, exportSchema = false)
public abstract class MemoRoomDatabase extends RoomDatabase {

    // このデータベースが提供するDAOを取得するための抽象メソッド
    public abstract MemoDao memoDao();

    // --- ここから下は、データベースのインスタンスを一つだけ生成するためのコード（シングルトンパターン）---

    // データベースのインスタンスを保持する変数
    private static volatile MemoRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    // データベース操作をバックグラウンドで行うためのスレッドプール
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // データベースのインスタンスを取得するためのメソッド
    static MemoRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MemoRoomDatabase.class) {
                if (INSTANCE == null) {
                    // データベースのインスタンスを生成
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MemoRoomDatabase.class, "memo_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}