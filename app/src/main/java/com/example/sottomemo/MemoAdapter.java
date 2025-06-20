package com.example.sottomemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoViewHolder> {

    private List<Memo> memoList;

    // コンストラクタ：表示したいデータのリストを受け取る
    public MemoAdapter(List<Memo> memoList) {
        this.memoList = memoList;
    }

    // ViewHolder：カードレイアウトの中の各部品（TextViewなど）を保持するクラス
    public static class MemoViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle;
        public TextView textViewExcerpt;
        public TextView textViewDate;

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            // レイアウトファイル（list_item_memo.xml）から各TextViewを見つけてくる
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewExcerpt = itemView.findViewById(R.id.text_view_excerpt);
            textViewDate = itemView.findViewById(R.id.text_view_date);
        }
    }

    // 1. 新しいカード（ViewHolder）が作られるときに呼ばれる
    @NonNull
    @Override
    public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // レイアウトファイルからViewを生成
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_memo, parent, false);
        return new MemoViewHolder(itemView);
    }

    // 2. カードにデータを表示するときに呼ばれる
    @Override
    public void onBindViewHolder(@NonNull MemoViewHolder holder, int position) {
        // リストから指定された位置のメモデータを取得
        Memo currentMemo = memoList.get(position);
        // ViewHolderのTextViewにデータをセット
        holder.textViewTitle.setText(currentMemo.getTitle());
        holder.textViewExcerpt.setText(currentMemo.getExcerpt());
        holder.textViewDate.setText(currentMemo.getDate());
    }

    // 3. リストに表示するアイテムの総数を返す
    @Override
    public int getItemCount() {
        return memoList.size();
    }
}