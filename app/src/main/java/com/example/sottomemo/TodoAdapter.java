package com.example.sottomemo;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class TodoAdapter extends ListAdapter<Todo, TodoAdapter.TodoViewHolder> {

    private OnTodoCheckedChangeListener listener;
    private OnItemClickListener clickListener; // タップイベント用のリスナーを追加

    protected TodoAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_todo, parent, false);
        return new TodoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Todo currentTodo = getItem(position);
        // bindメソッドにclickListenerを渡すように変更
        holder.bind(currentTodo, listener, clickListener);
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBox;
        private final TextView textViewTitle;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_todo);
            textViewTitle = itemView.findViewById(R.id.text_view_todo_title);
        }

        // bindメソッドの引数にclickListenerを追加
        public void bind(final Todo todo, final OnTodoCheckedChangeListener listener, final OnItemClickListener clickListener) {
            textViewTitle.setText(todo.getTitle());

            if (todo.isCompleted()) {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textViewTitle.setAlpha(0.5f);
            } else {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                textViewTitle.setAlpha(1.0f);
            }

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(todo.isCompleted());

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTodoCheckedChanged(todo, isChecked);
                }
            });

            // ★ここからが追加部分★
            // itemView（リストの各行）がタップされた時の処理
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(todo);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Todo> DIFF_CALLBACK = new DiffUtil.ItemCallback<Todo>() {
        @Override
        public boolean areItemsTheSame(@NonNull Todo oldItem, @NonNull Todo newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Todo oldItem, @NonNull Todo newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.isCompleted() == newItem.isCompleted();
        }
    };

    // ★ここからが追加部分★
    // 外部（Fragment）からタップイベントを受け取るためのインターフェース
    public interface OnItemClickListener {
        void onItemClick(Todo todo);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
    // ★追加部分ここまで★

    public interface OnTodoCheckedChangeListener {
        void onTodoCheckedChanged(Todo todo, boolean isChecked);
    }

    public void setOnTodoCheckedChangeListener(OnTodoCheckedChangeListener listener) {
        this.listener = listener;
    }
}