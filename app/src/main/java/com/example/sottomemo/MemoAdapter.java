package com.example.sottomemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MemoAdapter extends ListAdapter<Memo, MemoAdapter.MemoViewHolder> {

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Memo memo);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public MemoAdapter() {
        super(DIFF_CALLBACK);
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
    }

    public class MemoViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle;
        public TextView textViewExcerpt;
        public TextView textViewDate;

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewExcerpt = itemView.findViewById(R.id.text_view_excerpt);
            textViewDate = itemView.findViewById(R.id.text_view_date);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position));
                }
            });
        }
    }
}