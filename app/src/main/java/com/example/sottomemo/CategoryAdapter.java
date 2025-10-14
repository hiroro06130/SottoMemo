package com.example.sottomemo;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.CategoryViewHolder> {

    private OnCategoryMenuClickListener listener;

    public CategoryAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_category, parent, false);
        return new CategoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category currentCategory = getItem(position);
        holder.bind(currentCategory, listener);
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final View viewCategoryColor;
        private final TextView textViewCategoryName;
        private final ImageView buttonCategoryMenu;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategoryColor = itemView.findViewById(R.id.view_category_color);
            textViewCategoryName = itemView.findViewById(R.id.text_view_category_name);
            buttonCategoryMenu = itemView.findViewById(R.id.button_category_menu);
        }

        public void bind(final Category category, final OnCategoryMenuClickListener listener) {
            textViewCategoryName.setText(category.name);
            viewCategoryColor.setBackgroundTintList(ColorStateList.valueOf(category.color));

            buttonCategoryMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMenuClick(v, category);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Category> DIFF_CALLBACK = new DiffUtil.ItemCallback<Category>() {
        @Override
        public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.categoryId == newItem.categoryId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.name.equals(newItem.name) && oldItem.color == newItem.color;
        }
    };

    public interface OnCategoryMenuClickListener {
        void onMenuClick(View view, Category category);
    }

    public void setOnCategoryMenuClickListener(OnCategoryMenuClickListener listener) {
        this.listener = listener;
    }
}