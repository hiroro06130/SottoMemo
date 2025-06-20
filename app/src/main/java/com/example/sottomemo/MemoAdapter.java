package com.example.sottomemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

// ListAdapterを継承するように変更
public class MemoAdapter extends ListAdapter<Memo, MemoAdapter.MemoViewHolder> {

    // ListAdapterを使うためのコンストラクタ
    public MemoAdapter() {
        super(DIFF_CALLBACK);
    }

    // 2つのリストの差分を計算するための「設計図」
    // これがあるおかげで、リストの更新が非常に効率的になる
    private static final DiffUtil.ItemCallback<Memo> DIFF_CALLBACK = new DiffUtil.ItemCallback<Memo>() {
        @Override
        public boolean areItemsTheSame(@NonNull Memo oldItem, @NonNull Memo newItem) {
            // IDが同じなら、同じアイテムと見なす
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Memo oldItem, @NonNull Memo newItem) {
            // タイトルと日付が同じなら、内容も同じと見なす
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getDate().equals(newItem.getDate());
        }
    };

    @NonNull
    @Override
    public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_memo, parent, false);
        return new MemoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoViewHolder holder, int position) {
        // 表示するデータを取得する方法が、getItem(position)に変わる
        Memo currentMemo = getItem(position);
        holder.textViewTitle.setText(currentMemo.getTitle());
        holder.textViewExcerpt.setText(currentMemo.getExcerpt());
        holder.textViewDate.setText(currentMemo.getDate());
    }

    // ViewHolderクラス（変更なし）
    public static class MemoViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle;
        public TextView textViewExcerpt;
        public TextView textViewDate;

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewExcerpt = itemView.findViewById(R.id.text_view_excerpt);
            textViewDate = itemView.findViewById(R.id.text_view_date);
        }
    }
}