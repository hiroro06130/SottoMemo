package com.example.sottomemo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MemoAdapter extends ListAdapter<MemoWithCategories, MemoAdapter.MemoViewHolder> {

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private boolean isSelectionMode = false;
    private HashSet<MemoWithCategories> selectedItems = new HashSet<>();

    public MemoAdapter() {
        super(DIFF_CALLBACK);
    }

    public void startSelectionMode() { isSelectionMode = true; }
    public void finishSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
    }
    public void toggleSelection(MemoWithCategories item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        notifyDataSetChanged();
    }
    public int getSelectedItemCount() { return selectedItems.size(); }
    public List<Memo> getSelectedItems() {
        List<Memo> memos = new ArrayList<>();
        for (MemoWithCategories item : selectedItems) {
            memos.add(item.memo);
        }
        return memos;
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
        MemoWithCategories currentItem = getItem(position);
        holder.bind(currentItem);
    }

    public class MemoViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle, textViewExcerpt, textViewDate;
        public MaterialCardView cardView;
        public ChipGroup chipGroup;

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewExcerpt = itemView.findViewById(R.id.text_view_excerpt);
            textViewDate = itemView.findViewById(R.id.text_view_date);
            chipGroup = itemView.findViewById(R.id.chip_group_item_categories);
            cardView = (MaterialCardView) itemView;

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

        void bind(MemoWithCategories item) {
            Memo memo = item.memo;
            Context context = itemView.getContext();

            textViewTitle.setText(memo.getTitle());
            textViewExcerpt.setText(memo.getExcerpt());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            textViewDate.setText(sdf.format(new Date(memo.getLastModified())));

            if (selectedItems.contains(item)) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_selection_light_gray));
            } else {
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
                cardView.setCardBackgroundColor(typedValue.data);
            }

            chipGroup.removeAllViews();
            if (item.categories != null && !item.categories.isEmpty()) {
                chipGroup.setVisibility(View.VISIBLE);
                for (Category category : item.categories) {
                    Chip chip = new Chip(context);
                    chip.setText(category.name);

                    int categoryColor = category.color;
                    chip.setChipBackgroundColor(ColorStateList.valueOf(categoryColor).withAlpha(40));
                    chip.setChipStrokeWidth(0);
                    chip.setTextColor(categoryColor);

                    chipGroup.addView(chip);
                }
            } else {
                chipGroup.setVisibility(View.GONE);
            }
        }
    }

    private static final DiffUtil.ItemCallback<MemoWithCategories> DIFF_CALLBACK = new DiffUtil.ItemCallback<MemoWithCategories>() {
        @Override
        public boolean areItemsTheSame(@NonNull MemoWithCategories oldItem, @NonNull MemoWithCategories newItem) {
            return oldItem.memo.getId() == newItem.memo.getId();
        }
        @Override
        public boolean areContentsTheSame(@NonNull MemoWithCategories oldItem, @NonNull MemoWithCategories newItem) {
            if (!oldItem.memo.getExcerpt().equals(newItem.memo.getExcerpt())) return false;
            if (oldItem.memo.getLastModified() != newItem.memo.getLastModified()) return false;
            return new HashSet<>(oldItem.categories).equals(new HashSet<>(newItem.categories));
        }
    };

    public interface OnItemClickListener { void onItemClick(MemoWithCategories item); }
    public interface OnItemLongClickListener { void onItemLongClick(MemoWithCategories item); }
    public void setOnItemClickListener(OnItemClickListener listener) { this.clickListener = listener; }
    public void setOnItemLongClickListener(OnItemLongClickListener listener) { this.longClickListener = listener; }
}