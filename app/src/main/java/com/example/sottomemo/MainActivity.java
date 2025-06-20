package com.example.sottomemo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MemoAdapter memoAdapter;
    private List<Memo> memoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. レイアウトからRecyclerViewを見つける
        recyclerView = findViewById(R.id.recycler_view_memos);

        // 2. 表示するための仮データを作成
        createDummyData();

        // 3. アダプターを作成し、データを渡す
        memoAdapter = new MemoAdapter(memoList);

        // 4. RecyclerViewにレイアウトマネージャーとアダプターを設定
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(memoAdapter);
    }

    // 仮のメモデータを作成するメソッド
    private void createDummyData() {
        memoList = new ArrayList<>();
        memoList.add(new Memo("来週の打ち合わせ", "田中さんとプロジェクトXの件で。資料準備を忘れないように。", "2025/06/20"));
        memoList.add(new Memo("買い物リスト", "牛乳、卵、食パン、それからクリーニングを受け取りに行く。", "2025/06/19"));
        memoList.add(new Memo("読みたい本", "『思考は現実化する』ナポレオン・ヒル著。本屋で探してみる。", "2025/06/18"));
    }
}