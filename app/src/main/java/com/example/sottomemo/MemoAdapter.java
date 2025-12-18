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

    // クリックリスナーの定義
    public interface OnItemClickListener {
        void onItemClick(MemoWithCategories memo);
        void onItemLongClick(MemoWithCategories memo);
    }

    private OnItemClickListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<Long> selectedMemoIds = new HashSet<>();

    private static final DiffUtil.ItemCallback<MemoWithCategories> DIFF_CALLBACK = new DiffUtil.ItemCallback<MemoWithCategories>() {
        @Override
        public boolean areItemsTheSame(@NonNull MemoWithCategories oldItem, @NonNull MemoWithCategories newItem) {
            return oldItem.memo.getId() == newItem.memo.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull MemoWithCategories oldItem, @NonNull MemoWithCategories newItem) {
            return oldItem.memo.getExcerpt().equals(newItem.memo.getExcerpt()) &&
                    oldItem.memo.getUpdatedDate() == newItem.memo.getUpdatedDate() &&
                    oldItem.categories.size() == newItem.categories.size();
        }
    };

    public MemoAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // --- 複数選択モード制御 ---
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

    public MemoWithCategories getMemoAt(int position) {
        return getItem(position);
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
        MemoWithCategories current = getItem(position);
        holder.bind(current);
    }

    class MemoViewHolder extends RecyclerView.ViewHolder {
        // list_item_memo.xml のIDに合わせる
        private final TextView titleView;
        private final TextView excerptView;
        private final TextView dateView;
        private final ChipGroup chipGroup;
        private final MaterialCardView cardView; // 背景色変更用

        private MemoViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.text_view_title);
            excerptView = itemView.findViewById(R.id.text_view_excerpt);
            dateView = itemView.findViewById(R.id.text_view_date);
            chipGroup = itemView.findViewById(R.id.chip_group_item_categories);

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
            // タイトルと本文の設定
            // MemoクラスにgetTitle()を追加したのでそれを使う、なければ抜粋を表示
            if (titleView != null) {
                titleView.setText(memoWithCategories.memo.getTitle());
            }
            if (excerptView != null) {
                excerptView.setText(memoWithCategories.memo.getExcerpt());
            }

            if (dateView != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                dateView.setText(sdf.format(memoWithCategories.memo.getUpdatedDate()));
            }

            if (chipGroup != null) {
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
            }

            // 選択状態の見た目
            if (isMultiSelectMode && selectedMemoIds.contains(memoWithCategories.memo.getId())) {
                if (cardView != null) {
                    cardView.setCardBackgroundColor(Color.LTGRAY);
                    cardView.setStrokeWidth(4);
                    cardView.setStrokeColor(Color.BLUE);
                } else {
                    itemView.setBackgroundColor(Color.LTGRAY);
                }
            } else {
                if (cardView != null) {
                    // デフォルトの色に戻す（テーマ属性から取得するのが理想ですが、一旦白などで固定）
                    cardView.setCardBackgroundColor(Color.WHITE); // 必要に応じて修正
                    cardView.setStrokeWidth(0);
                } else {
                    itemView.setBackgroundColor(Color.WHITE);
                }
            }
        }
    }
}