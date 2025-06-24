package com.example.sottomemo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
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

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private boolean isSelectionMode = false;
    private HashSet<Memo> selectedItems = new HashSet<>();

    public MemoAdapter() {
        super(DIFF_CALLBACK);
    }

    public void startSelectionMode() {
        isSelectionMode = true;
    }

    public void finishSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public void toggleSelection(Memo memo) {
        if (selectedItems.contains(memo)) {
            selectedItems.remove(memo);
        } else {
            selectedItems.add(memo);
        }
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Memo> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    @NonNull
    @Override
    public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_memo, parent, false);
        return new MemoViewHolder(itemView);
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
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.color_selection_light_gray, null));
        } else {
            // 選択されていないアイテムの背景色を、現在のテーマに合わせて設定する
            TypedValue typedValue = new TypedValue();
            Context context = holder.itemView.getContext();
            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
            holder.cardView.setCardBackgroundColor(typedValue.data);
        }
    }

    public class MemoViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle;
        public TextView textViewExcerpt;
        public TextView textViewDate;
        public CardView cardView;

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewExcerpt = itemView.findViewById(R.id.text_view_excerpt);
            textViewDate = itemView.findViewById(R.id.text_view_date);
            cardView = (CardView) itemView;

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
                    return true;
                }
                return false;
            });
        }
    }

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