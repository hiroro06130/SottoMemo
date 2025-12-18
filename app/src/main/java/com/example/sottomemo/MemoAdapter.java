package com.example.sottomemo;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MemoAdapter extends ListAdapter<MemoWithCategories, MemoAdapter.MemoViewHolder> {

    // クリックリスナーのインターフェース
    public interface OnItemClickListener {
        void onItemClick(MemoWithCategories memo);
        void onItemLongClick(MemoWithCategories memo);
    }

    private OnItemClickListener listener;

    // ★ 複数選択機能のための変数
    private boolean isMultiSelectMode = false;
    private Set<Long> selectedMemoIds = new HashSet<>();

    public MemoAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // --- 複数選択モード制御メソッド ---

    public void setMultiSelectMode(boolean enabled) {
        isMultiSelectMode = enabled;
        if (!enabled) {
            selectedMemoIds.clear();
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(long memoId) {
        if (selectedMemoIds.contains(memoId)) {
            selectedMemoIds.remove(memoId);
        } else {
            selectedMemoIds.add(memoId);
        }
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedMemoIds.size();
    }

    public List<Memo> getSelectedMemos() {
        List<Memo> selectedMemos = new ArrayList<>();
        for (MemoWithCategories item : getCurrentList()) {
            if (selectedMemoIds.contains(item.memo.getId())) {
                selectedMemos.add(item.memo);
            }
        }
        return selectedMemos;
    }

    public void clearSelection() {
        selectedMemoIds.clear();
        notifyDataSetChanged();
    }

    public boolean isSelected(long memoId) {
        return selectedMemoIds.contains(memoId);
    }

    // ---------------------------

    private static final DiffUtil.ItemCallback<MemoWithCategories> DIFF_CALLBACK = new DiffUtil.ItemCallback<MemoWithCategories>() {
        @Override
        public boolean areItemsTheSame(@NonNull MemoWithCategories oldItem, @NonNull MemoWithCategories newItem) {
            return oldItem.memo.getId() == newItem.memo.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull MemoWithCategories oldItem, @NonNull MemoWithCategories newItem) {
            // カテゴリの変更も検知するために簡易的な比較
            return oldItem.memo.getExcerpt().equals(newItem.memo.getExcerpt()) &&
                    oldItem.memo.getUpdatedDate() == newItem.memo.getUpdatedDate() &&
                    oldItem.categories.size() == newItem.categories.size();
        }
    };

    @NonNull
    @Override
    public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item, parent, false);
        return new MemoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoViewHolder holder, int position) {
        MemoWithCategories current = getItem(position);
        holder.bind(current);
    }

    // スワイプ削除などで位置からメモを取得する用
    public MemoWithCategories getMemoAt(int position) {
        return getItem(position);
    }

    class MemoViewHolder extends RecyclerView.ViewHolder {
        private final TextView memoItemView;
        private final TextView dateItemView;
        private final ChipGroup chipGroup;
        private final MaterialCardView cardView; // 背景色変更用

        private MemoViewHolder(View itemView) {
            super(itemView);
            memoItemView = itemView.findViewById(R.id.textView);
            dateItemView = itemView.findViewById(R.id.text_date);
            chipGroup = itemView.findViewById(R.id.chip_group_item);

            // レイアウトにCardViewが使われている前提。もしLinearLayoutなら適切に変更してください
            // (通常はrecyclerview_itemのルート要素)
            if (itemView instanceof MaterialCardView) {
                cardView = (MaterialCardView) itemView;
            } else {
                cardView = null;
            }

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemLongClick(getItem(position));
                    return true;
                }
                return false;
            });
        }

        public void bind(MemoWithCategories memoWithCategories) {
            memoItemView.setText(memoWithCategories.memo.getExcerpt());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            dateItemView.setText(sdf.format(memoWithCategories.memo.getUpdatedDate()));

            chipGroup.removeAllViews();
            for (Category category : memoWithCategories.categories) {
                Chip chip = new Chip(itemView.getContext());
                chip.setText(category.name);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(category.color));
                chip.setTextColor(Color.WHITE);
                chip.setEnsureMinTouchTargetSize(false);
                chip.setTextSize(10);
                chipGroup.addView(chip);
            }

            // ★ 選択状態に応じた背景色の変更
            if (isMultiSelectMode && selectedMemoIds.contains(memoWithCategories.memo.getId())) {
                // 選択中：少し暗い色にするか、アクセントカラーを薄く乗せる
                itemView.setBackgroundColor(Color.LTGRAY);
                if(cardView != null) cardView.setStrokeWidth(4);
                if(cardView != null) cardView.setStrokeColor(Color.BLUE);
            } else {
                // 通常時：背景を戻す
                itemView.setBackgroundColor(Color.WHITE); // またはテーマのデフォルト色
                if(cardView != null) cardView.setStrokeWidth(0);
            }
        }
    }
}}