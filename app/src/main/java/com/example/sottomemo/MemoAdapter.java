package com.example.sottomemo;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MemoAdapter extends ListAdapter<Memo, MemoAdapter.MemoViewHolder> {

    // --- ここからが、今回の改造で追加・変更する部分 ---

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    private boolean isSelectionMode = false;
    private HashSet<Memo> selectedItems = new HashSet<>();

    // 選択モードを開始する
    public void startSelectionMode() {
        isSelectionMode = true;
    }

    // 選択モードを終了し、選択状態を全てクリアする
    public void finishSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        notifyDataSetChanged(); // 全てのアイテムの表示を更新して元に戻す
    }

    // アイテムの選択状態を切り替える
    public void toggleSelection(Memo memo) {
        if (selectedItems.contains(memo)) {
            selectedItems.remove(memo);
        } else {
            selectedItems.add(memo);
        }
        notifyDataSetChanged(); // アイテムの表示を更新
    }

    // 選択されているアイテムの数を取得する
    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    // 選択されているアイテムのリストを取得する
    public List<Memo> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }


    @Override
    public void onBindViewHolder(@NonNull MemoViewHolder holder, int position) {
        Memo currentMemo = getItem(position);
        holder.textViewTitle.setText(currentMemo.getTitle());
        holder.textViewExcerpt.setText(currentMemo.getExcerpt());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        String formattedDate = sdf.format(new Date(currentMemo.getLastModified()));
        holder.textViewDate.setText(formattedDate);

        // 選択状態に応じて、カードの見た目を変更する
        if (selectedItems.contains(currentMemo)) {
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.color_selection_light_gray));
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
        }
    }

    // --- 改造部分ここまで ---

    public MemoAdapter() {
        super(DIFF_CALLBACK);
    }

    // (DIFF_CALLBACKは変更なし)
    private static final DiffUtil.ItemCallback<Memo> DIFF_CALLBACK = new DiffUtil.ItemCallback<Memo>() {
        @Override
        public boolean areItemsTheSame(@NonNull Memo oldItem, @NonNull Memo newItem) {
            return oldItem.getId() == newItem.getId();
        }
        @Override
        public boolean areContentsTheSame(@NonNull Memo oldItem, @NonNull Memo newItem) {
            return oldItem.getExcerpt().equals(newItem.getExcerpt()) &&
                    oldItem.getLastModified() == newItem.getLastModified();
        }
    };

    @NonNull
    @Override
    public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_memo, parent, false);
        return new MemoViewHolder(itemView);
    }

    public class MemoViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle;
        public TextView textViewExcerpt;
        public TextView textViewDate;
        public CardView cardView; // カード全体を特定するために追加

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewExcerpt = itemView.findViewById(R.id.text_view_excerpt);
            textViewDate = itemView.findViewById(R.id.text_view_date);
            cardView = (CardView) itemView; // itemViewをCardViewとして扱う

            // --- クリックと長押しの処理を更新 ---
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (clickListener != null && position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(getItem(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(getItem(position));
                    return true; // trueを返して、長押しイベントをここで完了させる
                }
                return false;
            });
        }
    }

    // --- クリックと長押しをMainActivityに伝えるためのインターフェース ---
    public interface OnItemClickListener {
        void onItemClick(Memo memo);
    }
    public interface OnItemLongClickListener {
        void onItemLongClick(Memo memo);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }
}